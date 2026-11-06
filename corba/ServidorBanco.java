import SistemaBancario.*;
import org.omg.CosNaming.*;
import org.omg.PortableServer.*;
import org.omg.CORBA.ORB;

import java.util.HashMap;
import java.util.Map;

class Conta {
    final String numero;
    final String titular;
    double saldo;

    Conta(String numero, String titular, double saldo) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldo;
    }
}

// Os metodos sao synchronized porque o ORB atende os pedidos em varias threads.
// Isto tambem garante que a transferencia e atomica.
class BancoImpl extends BancoPOA {
    private final Map<String, Conta> contas = new HashMap<>();

    public synchronized void criarConta(String numero, String titular, double saldoInicial)
            throws OperacaoInvalida {
        if (contas.containsKey(numero)) {
            throw new OperacaoInvalida("Conta duplicada: " + numero);
        }
        if (!(saldoInicial >= 0)) {
            throw new OperacaoInvalida("Saldo inicial deve ser positivo ou zero");
        }
        contas.put(numero, new Conta(numero, titular, saldoInicial));
    }

    public synchronized double consultarSaldo(String numero) throws OperacaoInvalida {
        return buscar(numero).saldo;
    }

    public synchronized void depositar(String numero, double valor) throws OperacaoInvalida {
        validarValor(valor);
        buscar(numero).saldo += valor;
    }

    public synchronized void sacar(String numero, double valor)
            throws SaldoInsuficiente, OperacaoInvalida {
        validarValor(valor);
        Conta conta = buscar(numero);
        if (conta.saldo < valor) {
            throw new SaldoInsuficiente("Saldo insuficiente na conta " + numero);
        }
        conta.saldo -= valor;
    }

    public synchronized void transferir(String origem, String destino, double valor)
            throws SaldoInsuficiente, OperacaoInvalida {
        validarValor(valor);
        Conta contaOrigem = buscar(origem);
        Conta contaDestino = buscar(destino);
        if (origem.equals(destino)) {
            throw new OperacaoInvalida("Contas de origem e destino devem ser diferentes");
        }
        if (contaOrigem.saldo < valor) {
            throw new SaldoInsuficiente("Saldo insuficiente na conta " + origem);
        }
        contaOrigem.saldo -= valor;
        contaDestino.saldo += valor;
    }

    private Conta buscar(String numero) throws OperacaoInvalida {
        Conta conta = contas.get(numero);
        if (conta == null) {
            throw new OperacaoInvalida("Conta inexistente: " + numero);
        }
        return conta;
    }

    private void validarValor(double valor) throws OperacaoInvalida {
        if (!(valor > 0)) {
            throw new OperacaoInvalida("Valor deve ser positivo");
        }
    }
}

public class ServidorBanco {
    public static void main(String[] args) {
        try {
            ORB orb = ORB.init(args, null);
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(new BancoImpl());
            Banco href = BancoHelper.narrow(ref);

            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            NameComponent[] path = ncRef.to_name("Banco");
            ncRef.rebind(path, href);

            System.out.println("Servidor CORBA pronto e aguardando...");
            orb.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
