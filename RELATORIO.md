# Laboratório 6 – Serviços Web Distribuídos

**Tema:** Sistema de Gestão Bancária: CORBA (Java IDL) vs. REST (Spring Boot)

## 1. Implementação

As duas versões expõem as mesmas operações:

| Operação | CORBA (`Banco.idl`) | REST (`/api/conta`) |
|---|---|---|
| Criar conta | `criarConta(numero, titular, saldoInicial)` | `POST /api/conta?numero=&titular=&saldoInicial=` |
| Consultar saldo | `consultarSaldo(numero)` | `GET /api/conta/{numero}/saldo` |
| Depositar | `depositar(numero, valor)` | `POST /api/conta/{numero}/depositar?valor=` |
| Sacar | `sacar(numero, valor)` | `POST /api/conta/{numero}/sacar?valor=` |
| Transferir | `transferir(origem, destino, valor)` | `POST /api/conta/transferir?origem=&destino=&valor=` |

- **Fundos insuficientes:** o CORBA lança a exceção `SaldoInsuficiente`; o REST responde HTTP 409.
- **Pedidos inválidos:** o CORBA lança `OperacaoInvalida`; o REST responde 400 (valor inválido), 404 (conta inexistente) ou 409 (conta duplicada).
- **Transferência atómica:** os métodos são `synchronized` e todas as validações são feitas antes de alterar os saldos. Uma transferência que falha não altera nenhuma conta.

## 2. Testes de desempenho

**Método:** `teste/TesteDesempenho.java` é um único cliente Java que chama `consultarSaldo` nas duas tecnologias e mede o tempo com `System.nanoTime()`. Cada ronda tem 10.000 chamadas. Em cada execução há uma ronda de aquecimento não contabilizada, seguida de 5 rondas medidas. O teste foi executado 3 vezes, com cliente e servidores na mesma máquina (`localhost`).

| Execução | CORBA (ms/ronda) | REST (ms/ronda) | CORBA (µs/chamada) | REST (µs/chamada) | CORBA (ops/s) | REST (ops/s) | REST / CORBA |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1219,6 | 1563,3 | 122,0 | 156,3 | 8199 | 6397 | 1,28 |
| 2 | 1198,1 | 1499,0 | 119,8 | 149,9 | 8347 | 6671 | 1,25 |
| 3 | 1229,6 | 1516,4 | 123,0 | 151,6 | 8133 | 6595 | 1,23 |
| **Média** | **1215,8** | **1526,2** | **121,6** | **152,6** | **8225** | **6552** | **1,26** |

As fórmulas usadas foram: tempo médio = tempo total / número de chamadas; throughput = número de chamadas / tempo total em segundos.

**Limitações das medições:**

- **Versões do Java diferentes:** o servidor e o cliente CORBA correm em Java 8, porque o CORBA foi removido do JDK no Java 11. O servidor REST corre em Java 21, porque o Spring Boot 4 exige Java 17 ou superior. A diferença medida inclui, portanto, a diferença entre as JVMs.
- **Rede local:** sem rede real, as medições não incluem a latência nem o custo de transmissão de uma rede física.
- **Otimização do Spring Boot:** por omissão, o `bootRun` arranca a JVM com `-XX:TieredStopAtLevel=1`. Com essa opção, o REST pareceu cerca de 2,4 vezes mais lento do que o CORBA. A opção foi desativada em `build.gradle`, e os valores acima já não têm essa limitação.

## 3. Comparação de tecnologias

| Critério de comparação | CORBA (fase clássica) | Serviços Web (REST/JSON) |
|---|---|---|
| **Protocolo base** | GIOP/IIOP sobre TCP | HTTP sobre TCP |
| **Formato dos dados** | Binário (CDR) | Texto (JSON) |
| **Acoplamento** | Forte: o cliente depende dos stubs gerados a partir da IDL | Fraco: o cliente só precisa de conhecer o URL e o JSON |
| **Desempenho (latência)** | Melhor: 121,6 µs por chamada, 8225 ops/s | Pior: 152,6 µs por chamada, 6552 ops/s |
| **Passagem por firewalls** | Difícil: usa a porta do Name Service e a porta própria do servidor, que por omissão é dinâmica | Fácil: usa uma única porta HTTP (8080 neste projeto; 80 ou 443 em produção) |
| **Complexidade de setup** | Alta: exige compilar a IDL com `idlj`, iniciar o `orbd` e usar um JDK até à versão 8 | Baixa: um único processo Spring Boot, sem serviço de nomes nem geração de stubs |

## 4. Perguntas de avaliação

### 4.1. Análise de desempenho

O **CORBA** apresentou menor latência: 121,6 µs por chamada contra 152,6 µs no REST, ou seja, o REST demorou cerca de 1,26 vezes mais.

A explicação está coerente com o formato de serialização:

- **CORBA:** o `double` viaja em binário (CDR) com tamanho fixo, por isso basta copiar os bytes, sem conversão para texto.
- **REST:** o servidor converte o resultado em texto JSON e o cliente volta a convertê-lo em `double`. Cada resposta tem também cabeçalhos HTTP em texto, que é preciso produzir e interpretar.

O teste mede o tempo total de cada chamada e por isso não isola a serialização. A diferença inclui também o custo das pilhas do ORB e do Spring/Tomcat e as diferentes versões da JVM. A mensagem usada é muito pequena, pelo que a diferença tende a crescer com volumes maiores de dados, mas isso não foi medido aqui.

### 4.2. Acoplamento e manutenção

O **CORBA** exigiria maior esforço. A assinatura `double consultarSaldo(...)` teria de passar a devolver uma estrutura com saldo e moeda, o que obriga a:

1. alterar a IDL;
2. voltar a gerar os stubs e skeletons com `idlj`;
3. alterar e recompilar o servidor e todos os clientes;
4. redistribuir o novo código a todos os clientes ao mesmo tempo, porque os antigos deixam de ser compatíveis com a nova interface.

No **REST**, basta acrescentar um campo à resposta no servidor, por exemplo `"moeda": "EUR"`. Os clientes existentes continuam a funcionar sem recompilação, desde que ignorem os campos que não conhecem (como faz o cliente de teste deste projeto). Só os clientes que queiram usar a moeda precisam de ser alterados.

### 4.3. Segurança e infraestrutura

As redes corporativas costumam deixar passar o tráfego web, por isso o HTTP funciona sem configuração especial:

- **Portas conhecidas:** o HTTP usa as portas 80 e 443, normalmente já abertas. O IIOP usa a porta do serviço de nomes e portas próprias dos servidores, muitas vezes dinâmicas, que seria preciso abrir uma a uma.
- **Inspeção do tráfego:** proxies, balanceadores e gateways compreendem HTTP e conseguem filtrar por URL, método ou cabeçalhos. O IIOP é binário e a maior parte destes equipamentos não o interpreta, por isso costuma ser bloqueado.
- **Endereços e NAT:** a referência de um objeto CORBA (IOR) inclui o endereço e a porta do servidor, que podem deixar de ser válidos quando atravessam NAT. No HTTP, o cliente só precisa do URL público.
- **Encriptação:** o HTTPS (TLS) é uma solução padrão e amplamente suportada. A proteção do IIOP é possível, mas exige configuração específica do ORB.
