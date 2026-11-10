# 0002. Virtual threads em vez de coroutines, no Java 25

- **Status:** aceito
- **Data:** 2026-11-10

## Contexto

O TicketRush é uma aplicação Spring MVC que fala com o Postgres pelo Spring Data JPA, que usa JDBC. O JDBC é
bloqueante: enquanto a query roda, a thread que a chamou fica esperando.

Na configuração padrão, o Tomcat atende cada requisição numa platform thread (uma thread do sistema operacional) de um
pool de 200. Uma requisição esperando o banco ocupa uma dessas threads. Sob carga, a requisição 201 espera na fila mesmo
com a CPU ociosa.

Duas formas de evitar que I/O bloqueado desperdice threads do SO foram consideradas:

1. **Coroutines do Kotlin** (funções `suspend`).
2. **Virtual threads** (Project Loom, estáveis desde o Java 21).

## Decisão

Usar **virtual threads** e **não usar coroutines** no código de produção.

- Ativar `spring.threads.virtual.enabled: true`. Assim o Tomcat, o `@Async`, o `@Scheduled` e os listeners do RabbitMQ
  passam a rodar em virtual threads.
- Subir o toolchain e o CI do Java 21 para o **Java 25** (LTS).
- Manter o pool de conexões do HikariCP no tamanho padrão (10) até que uma medição indique o contrário.

## Motivos

**Coroutines não ajudam com JDBC.** Uma função `suspend` só libera a thread quando o código por baixo é não bloqueante.
O JDBC não é, então o padrão comum é `withContext(Dispatchers.IO) { repository.save(...) }`, que apenas move a chamada
bloqueante para outro pool de threads. Isso espalha `suspend` e dispatchers pelo código sem ganho real. Coroutines
compensam numa stack inteiramente não bloqueante (WebFlux + R2DBC), que não é o caso deste projeto.

**Virtual threads encaixam no código que já escrevemos.** Quando uma virtual thread bloqueia em I/O, a JVM a desmonta da
sua carrier (a thread do SO), e a carrier passa a executar outro trabalho. O código continua simples e síncrono: os
mesmos controllers, os mesmos services, o mesmo `@Transactional`. Milhares de virtual threads custam pouca memória.

**Java 25 em vez de 21.** No Java 21, uma virtual thread dentro de um bloco `synchronized` fica *pinned*: não consegue
desmontar e mantém a carrier ocupada. O JDK 24 removeu essa limitação (JEP 491), e o 25 é o LTS seguinte. O Gradle 9.8
e o Kotlin 2.3 suportam o Java 25, e o projeto ainda é pequeno, então a atualização é barata agora.

**O pool fica em 10.** Virtual threads tornam a *espera* barata. Elas não fazem o banco executar mais queries ao mesmo
tempo. Com elas, o limite de requisições simultâneas deixa de ser "200 threads do Tomcat" e passa a ser "10 conexões com
o banco". Um pool maior não é automaticamente mais rápido: o banco tem poucos núcleos de CPU, e o Postgres permite 100
conexões por padrão, compartilhadas por todas as instâncias. O passo 3 (reserva com concorrência) vai medir se as
requisições esperam por conexão (`hikaricp.connections.pending`) antes de qualquer mudança.

## Consequências

- Nada de `suspend` no código de produção. Os testes ainda podem usar coroutines para *gerar* concorrência (por exemplo,
  disparar muitas reservas ao mesmo tempo no passo 3).
- Virtual threads não protegem regras de negócio sob concorrência. "Nunca vender mais que o estoque" é garantido pelo
  banco (locks, updates condicionais, constraints), não pela forma como as requisições são executadas.
- Nunca fazer pool de virtual threads. Crie uma por tarefa; o Spring já faz isso.
- `ThreadLocal` continua funcionando, mas agora cada requisição tem a sua própria virtual thread, então evite guardar
  objetos caros nele como cache.
- Uma transação longa segura uma conexão pelo tempo inteiro. Chamar serviços externos lentos (por exemplo, o gateway de
  pagamento) dentro de um `@Transactional` esgotaria o pool. Isso orienta o desenho dos passos 4 e 5.
- É preciso ter o JDK 25 localmente. O CI usa o Temurin 25.

## Referências

- [Spring Boot: virtual threads](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
- [HikariCP: About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
