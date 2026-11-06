import java.net.URI;
import java.net.http.*;

public class ClienteREST {
    private static final String BASE = "http://localhost:8080/api/conta";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        // Numeros unicos para o cliente poder ser executado varias vezes no mesmo servidor
        String a = "A" + System.currentTimeMillis();
        String b = "B" + System.currentTimeMillis();

        long inicio = System.nanoTime();
        post(BASE + "?numero=" + a + "&titular=Ana&saldoInicial=1000");
        post(BASE + "?numero=" + b + "&titular=Bruno&saldoInicial=500");
        post(BASE + "/" + a + "/depositar?valor=150");
        post(BASE + "/" + a + "/sacar?valor=50");
        post(BASE + "/transferir?origem=" + a + "&destino=" + b + "&valor=300");
        String saldoA = get(BASE + "/" + a + "/saldo");
        String saldoB = get(BASE + "/" + b + "/saldo");
        long fim = System.nanoTime();

        System.out.println("Saldo de " + a + " (esperado 800.0): " + saldoA);
        System.out.println("Saldo de " + b + " (esperado 800.0): " + saldoB);
        System.out.println("Tempo de execucao: " + (fim - inicio) / 1e6 + " ms");

        System.out.println("Erro ao sacar: " + post(BASE + "/" + a + "/sacar?valor=1000000"));
        System.out.println("Erro ao transferir: " + post(BASE + "/transferir?origem=" + a + "&destino=" + b + "&valor=1000000"));
        System.out.println("Erro ao depositar: " + post(BASE + "/" + a + "/depositar?valor=-10"));
        System.out.println("Erro ao consultar: " + get(BASE + "/NAO-EXISTE/saldo"));
        System.out.println("Saldos apos as falhas: " + get(BASE + "/" + a + "/saldo") + " e " + get(BASE + "/" + b + "/saldo"));
    }

    private static String get(String url) throws Exception {
        return enviar(HttpRequest.newBuilder().uri(URI.create(url)).GET().build());
    }

    private static String post(String url) throws Exception {
        return enviar(HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    private static String enviar(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return "HTTP " + response.statusCode() + " " + response.body();
    }
}
