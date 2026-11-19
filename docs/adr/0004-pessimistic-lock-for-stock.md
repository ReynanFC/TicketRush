# 0004. Lock pessimista para reservar estoque

- **Status:** aceito
- **Data:** 2026-11-18

## Contexto

O `ReserveTicketsService` faz um read-modify-write: lê o `LotStock`, chama `reserve()` em memória e salva. Mesmo com o
caso de uso inteiro numa transação (`TransactionalReserveTicketsUseCase`), o Postgres roda em `READ COMMITTED`, e duas
transações podem ler o mesmo `available`. Cada uma grava um valor válido, então o `CHECK (available >= 0)` da V2 nunca
dispara. É o **lost update**.

O `ReserveTicketsConcurrencyTest` provou isso: 50 compradores, um lote com 10 ingressos, e **as 50 reservas passaram**.

O [ADR 0003](0003-inventory-module.md) já deixou o lock do tamanho certo: uma linha de `lot_stock` por lote. Falta
escolher **como** proteger essa linha. O padrão de uso é o pior caso possível para conflitos: na abertura das vendas,
muitos compradores disputam o mesmo lote ao mesmo tempo.

## Decisão

**`SELECT ... FOR UPDATE` na leitura do estoque durante a reserva.**

- A porta `LotStockRepository` ganha `findByLotIdForReservation`. O nome fala da intenção, não do lock, então o core não
  sabe que existe `FOR UPDATE`.
- O adapter usa um método do Spring Data com `@Lock(LockModeType.PESSIMISTIC_WRITE)` e `@Query`.
- O lock vale até o fim da transação do caso de uso. Quem chega depois espera e lê o `available` já atualizado.
- A regra "não vender mais do que o estoque" continua no domínio, no `LotStock.reserve()`.

```
T1: SELECT ... FOR UPDATE  → pega o lock da linha
T2: SELECT ... FOR UPDATE  → espera
T1: UPDATE, COMMIT         → solta o lock
T2: lê available atualizado → reserva ou recebe InsufficientStockException
```

Com isso, o `ReserveTicketsConcurrencyTest` passa: exatamente 10 reservas e 40 recusas por falta de estoque.

### Sem a porta `StockGuard` por enquanto

A issue #16 previa uma porta `StockGuard` com um adapter por estratégia, para comparar as três. A comparação foi feita
no papel (tabela abaixo), então a porta ficaria com um adapter só. Ela volta se um dia o update atômico substituir o
lock.

## Alternativas

| | Correção sem retry | Disputa alta | Regra no domínio | Custo principal |
|---|---|---|---|---|
| **Pessimista** (escolhido) | sim | aguenta, com fila | sim | conexões presas na fila |
| Otimista (`@Version`) | não, precisa de retry | ruim | sim | retries e latência |
| `UPDATE` condicional atômico | sim | ótimo | não | regra duplicada no SQL |

- **Lock otimista (`@Version` + retry).** Não trava nada e confere a versão no `UPDATE`. É ótimo quando conflito é
  raro. Na abertura das vendas quase todo mundo perderia o conflito e precisaria de retry, com mais idas ao banco e
  latência maior. Com limite de retries, gente seria recusada mesmo com ingresso sobrando. **Fica para a edição de
  eventos (#18)**, onde o conflito é raro.
- **`UPDATE lot_stock SET available = available - :qty WHERE lot_id = :id AND available >= :qty`.** Uma ida ao banco,
  lock pelo tempo mínimo e sem retry. É o mais rápido. Ficou de fora porque a regra sai do `LotStock` e vai para o SQL:
  o domínio vira enfeite ou a regra fica duplicada, contra a ideia do [ADR 0001](0001-hexagonal-catalog.md). Também
  complica quando a regra cresce (meia-entrada no #20, limite por comprador). **É o próximo passo se a fila do
  pessimista virar gargalo.**

## Consequências

**Positivas**

- Correto sem retry, sem código extra no service.
- A regra de negócio continua no domínio e testável sem banco.
- A disputa fica só entre quem quer o mesmo lote (efeito do ADR 0003).

**Negativas**

- **Cada comprador na fila segura uma conexão do pool.** As virtual threads ([ADR 0002](0002-virtual-threads-instead-of-coroutines.md))
  não ajudam aqui: a thread espera barata, mas a conexão JDBC continua presa. Com o Hikari em 10 conexões, um lote
  muito disputado pode deixar o resto da aplicação sem conexão. Medir antes de mexer no pool.
- **Sem timeout, uma transação travada trava a fila inteira.** Pendente: `jakarta.persistence.lock.timeout`.
- **O lock só protege se houver uma transação aberta antes do `find`.** Hoje o decorator garante isso. Se o método for
  chamado fora dele, o `@Transactional` do adapter abre uma transação que termina no próprio `find`, e o lock some sem
  aviso. Pendente: `Propagation.MANDATORY` no adapter.
- **Risco de deadlock** se um dia uma reserva travar mais de um lote. Nesse caso, travar sempre na mesma ordem (por
  `lotId`).

## Referências

- [ADR 0003: Módulo `inventory` separado do catálogo](0003-inventory-module.md)
- [Spring Data JPA: Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Postgres: Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- [Hibernate: Locking](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#locking)
