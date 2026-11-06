import SistemaBancario.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.ORB;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Cliente unificado: mede as mesmas chamadas a consultarSaldo em CORBA e em REST.
// Corre em Java 8 porque as classes CORBA foram removidas do JDK no Java 11.
public class TesteDesempenho {
    private static final int CHAMADAS = 10000;
    private static final int RONDAS = 5;
    private static final double SALDO = 1000.0;
    private static final String BASE = "http://localhost:8080/api/conta";

    interface Consulta {
        double consultarSaldo() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        String numero = "T" + System.currentTimeMillis();

        ORB orb = ORB.init(args, null);
        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        Banco banco = BancoHelper.narrow(ncRef.resolve_str("Banco"));
        banco.criarConta(numero, "Teste", SALDO);

        pedir("POST", BASE + "?numero=" + numero + "&titular=Teste&saldoInicial=" + SALDO, 201);
        URL urlSaldo = new URL(BASE + "/" + numero + "/saldo");

        Consulta corba = () -> banco.consultarSaldo(numero);
        Consulta rest = () -> extrairSaldo(pedir("GET", urlSaldo.toString(), 200));

        // Uma ronda inicial nao contabilizada, para o aquecimento do JIT nao penalizar a primeira tecnologia
        executar(corba);
        executar(rest);

        long totalCorba = 0;
        long totalRest = 0;
        System.out.println("Ronda | CORBA (ms) | REST (ms)");
        for (int i = 1; i <= RONDAS; i++) {
            long tempoCorba = executar(corba);
            long tempoRest = executar(rest);
            totalCorba += tempoCorba;
            totalRest += tempoRest;
            System.out.printf("%5d | %10.1f | %9.1f%n", i, tempoCorba / 1e6, tempoRest / 1e6);
        }

        System.out.println();
        imprimirResumo("CORBA", totalCorba);
        imprimirResumo("REST", totalRest);
        System.out.printf("REST / CORBA (tempo): %.2f vezes%n", (double) totalRest / totalCorba);

        orb.shutdown(false);
    }

    private static long executar(Consulta consulta) throws Exception {
        long inicio = System.nanoTime();
        for (int i = 0; i < CHAMADAS; i++) {
            if (consulta.consultarSaldo() != SALDO) {
                throw new IllegalStateException("Saldo inesperado");
            }
        }
        return System.nanoTime() - inicio;
    }

    private static void imprimirResumo(String nome, long totalNanos) {
        long operacoes = (long) CHAMADAS * RONDAS;
        System.out.printf("%-5s: %.1f ms por ronda | %.1f us por chamada | %.0f ops/s%n",
                nome,
                totalNanos / 1e6 / RONDAS,
                totalNanos / 1e3 / operacoes,
                operacoes / (totalNanos / 1e9));
    }

    private static String pedir(String metodo, String url, int estadoEsperado) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod(metodo);
        int estado = con.getResponseCode();
        if (estado != estadoEsperado) {
            throw new IllegalStateException("HTTP " + estado + " em " + url);
        }
        // Ler a resposta ate ao fim permite reutilizar a ligacao (keep-alive)
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream corpo = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                corpo.write(buffer, 0, lidos);
            }
            return new String(corpo.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    // Converte o JSON em double, tal como o stub CORBA devolve um double ja desserializado
    private static double extrairSaldo(String json) {
        int inicio = json.indexOf("\"saldo\":") + "\"saldo\":".length();
        int fim = inicio;
        while (fim < json.length() && "0123456789.-+eE".indexOf(json.charAt(fim)) >= 0) {
            fim++;
        }
        return Double.parseDouble(json.substring(inicio, fim));
    }
}
