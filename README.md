# Laboratório 6 – Sistema de Gestão Bancária

Os resultados e as respostas estão em [RELATORIO.md](RELATORIO.md).

## Estrutura

- `corba/`: versão CORBA (`Banco.idl`, `ServidorBanco.java`, `ClienteBanco.java`)
- `rest/`: versão Spring Boot (`ContaController.java`, `cliente/ClienteREST.java`)
- `teste/`: cliente unificado de testes (`TesteDesempenho.java`)

## Requisitos

- **JDK 8** como `java` predefinido no `PATH`, com `idlj` e `orbd` (CORBA e testes)
- **JDK 17 ou superior** e **Gradle 9** (REST)

Os scripts em `rest/` usam `C:\Program Files\Java\jdk-21.0.11`. Noutra máquina, ajuste esse caminho em `iniciar-servidor.bat` e `executar-cliente.bat`.

## Como executar

Execute cada servidor num terminal próprio, a partir desta pasta.

**CORBA**

```bat
corba\compilar.bat
corba\iniciar-naming.bat
corba\iniciar-servidor.bat
corba\executar-cliente.bat
```

**REST**

```bat
rest\iniciar-servidor.bat
rest\executar-cliente.bat
```

**Interface web** (com o servidor REST a correr): abra <http://localhost:8080/> no navegador. A página está em `rest/src/main/resources/static/index.html` e faz todas as operações através da API REST. Os navegadores não falam IIOP, por isso a interface não usa o servidor CORBA.

**Testes de desempenho** (com o Name Service e os dois servidores a correr)

```bat
teste\executar.bat
```

Todos os componentes CORBA usam `127.0.0.1` (`-ORBServerHost 127.0.0.1`), para que os IORs não contenham o IP da rede. O `iniciar-naming.bat` apaga `corba\orb.db` antes de arrancar o `orbd`.

**Entrega**: `criar-entrega.bat` cria `entrega-lab6.zip` sem a pasta `corba\orb.db`.
