import SistemaBancario.*;
import org.omg.CosNaming.*;
import org.omg.CORBA.ORB;

public class ClienteBanco {
    public static void main(String[] args) {
        try {
            ORB orb = ORB.init(args, null);
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            Banco banco = BancoHelper.narrow(ncRef.resolve_str("Banco"));

            // Numeros unicos para o cliente poder ser executado varias vezes no mesmo servidor
            String a = "A" + System.currentTimeMillis();
            String b = "B" + System.currentTimeMillis();

            long inicio = System.nanoTime();
            banco.criarConta(a, "Ana", 1000.0);
            banco.criarConta(b, "Bruno", 500.0);
            banco.depositar(a, 150.0);
            banco.sacar(a, 50.0);
            banco.transferir(a, b, 300.0);
            double saldoA = banco.consultarSaldo(a);
            double saldoB = banco.consultarSaldo(b);
            long fim = System.nanoTime();

            System.out.println("Saldo de " + a + " (esperado 800.0): " + saldoA);
            System.out.println("Saldo de " + b + " (esperado 800.0): " + saldoB);
            System.out.println("Tempo de execucao: " + (fim - inicio) / 1e6 + " ms");

            try {
                banco.sacar(a, 1000000.0);
            } catch (SaldoInsuficiente e) {
                System.out.println("Excecao ao sacar: " + e.mensagem);
            }
            try {
                banco.transferir(a, b, 1000000.0);
            } catch (SaldoInsuficiente e) {
                System.out.println("Excecao ao transferir: " + e.mensagem);
            }
            try {
                banco.depositar(a, -10.0);
            } catch (OperacaoInvalida e) {
                System.out.println("Excecao ao depositar: " + e.mensagem);
            }
            try {
                banco.consultarSaldo("NAO-EXISTE");
            } catch (OperacaoInvalida e) {
                System.out.println("Excecao ao consultar: " + e.mensagem);
            }
            System.out.println("Saldos apos as falhas: " + banco.consultarSaldo(a) + " e " + banco.consultarSaldo(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
