package banco;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

// Os metodos sao synchronized porque o Tomcat atende os pedidos em varias threads.
// Isto tambem garante que a transferencia e atomica.
@RestController
@RequestMapping("/api/conta")
public class ContaController {

    private static class Conta {
        final String numero;
        final String titular;
        double saldo;

        Conta(String numero, String titular, double saldo) {
            this.numero = numero;
            this.titular = titular;
            this.saldo = saldo;
        }
    }

    private final Map<String, Conta> contas = new HashMap<>();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public synchronized Map<String, Object> criarConta(@RequestParam String numero,
                                                       @RequestParam String titular,
                                                       @RequestParam double saldoInicial) {
        if (contas.containsKey(numero)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conta duplicada: " + numero);
        }
        if (!(saldoInicial >= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo inicial deve ser positivo ou zero");
        }
        contas.put(numero, new Conta(numero, titular, saldoInicial));
        return saldoDe(contas.get(numero));
    }

    @GetMapping("/{numero}/saldo")
    public synchronized Map<String, Object> consultarSaldo(@PathVariable String numero) {
        return saldoDe(buscar(numero));
    }

    @PostMapping("/{numero}/depositar")
    public synchronized Map<String, Object> depositar(@PathVariable String numero, @RequestParam double valor) {
        validarValor(valor);
        Conta conta = buscar(numero);
        conta.saldo += valor;
        return saldoDe(conta);
    }

    @PostMapping("/{numero}/sacar")
    public synchronized Map<String, Object> sacar(@PathVariable String numero, @RequestParam double valor) {
        validarValor(valor);
        Conta conta = buscar(numero);
        if (conta.saldo < valor) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente na conta " + numero);
        }
        conta.saldo -= valor;
        return saldoDe(conta);
    }

    @PostMapping("/transferir")
    public synchronized void transferir(@RequestParam String origem,
                                        @RequestParam String destino,
                                        @RequestParam double valor) {
        validarValor(valor);
        Conta contaOrigem = buscar(origem);
        Conta contaDestino = buscar(destino);
        if (origem.equals(destino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contas de origem e destino devem ser diferentes");
        }
        if (contaOrigem.saldo < valor) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente na conta " + origem);
        }
        contaOrigem.saldo -= valor;
        contaDestino.saldo += valor;
    }

    private Conta buscar(String numero) {
        Conta conta = contas.get(numero);
        if (conta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta inexistente: " + numero);
        }
        return conta;
    }

    private void validarValor(double valor) {
        if (!(valor > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor deve ser positivo");
        }
    }

    private Map<String, Object> saldoDe(Conta conta) {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("numero", conta.numero);
        resposta.put("saldo", conta.saldo);
        return resposta;
    }
}
