# 0001. Arquitetura hexagonal no catálogo

- **Status:** aceito
- **Data:** 2026-11-10

## Contexto

O catálogo (eventos, setores e lotes) é o primeiro módulo de negócio do TicketRush. Os próximos passos do roadmap
(reserva com concorrência, pedido como máquina de estados, outbox + RabbitMQ e a extração em microsserviços) vão
trocar ou adicionar formas de entrada e saída: HTTP hoje, mensagens amanhã, outro banco ou outro serviço depois.

No caraiva-tours-hub foi usado o estilo **em camadas** (layered), organizado por funcionalidade:

```
booking/
  controller/   BookingController
  dto/          request/response
  Booking            @Entity (JPA) com @Setter
  BookingService     recebe Request DTO, devolve DTO, usa o repository direto
  BookingRepository  JpaRepository
```

Esse estilo funciona bem num CRUD, mas mistura as responsabilidades:

- A **entidade de negócio é a entidade JPA**. As regras (`validateBookingStateForCancellation`, por exemplo) dividem a
  classe com anotações de mapeamento, e os setters abertos permitem mudar o estado sem passar pelas regras.
- O **service conhece HTTP e banco ao mesmo tempo**: recebe DTOs de request, chama o Spring Data e monta a resposta.
  Para testá-lo é preciso um banco ou mocks do repository do Spring.
- Trocar a entrada (uma fila em vez de um controller) ou a saída (outro banco, outro serviço) mexe no service.

## Decisão

Organizar o catálogo em **arquitetura hexagonal (ports and adapters)**:

```
catalog/
  domain/        Event (aggregate root), Sector, Lot, exceções de negócio   ← regras
  application/
    port/in/     use cases + commands         ← o que o core oferece
    port/out/    EventRepository              ← o que o core precisa
    service/     um service por use case      ← orquestra: busca → domínio age → salva
  adapter/
    in/web/      controller, DTOs, mapper, exception handler
    out/persistence/  entidades JPA, mapper, Spring Data, JpaEventRepository
  config/        CatalogConfiguration (liga o core no Spring)
```

Regras que acompanham a decisão:

1. **O core (`domain` + `application`) não importa nada de `org.springframework`.** A exceção aceita é o SLF4J, que é
   uma fachada de log, não um framework.
2. **As dependências apontam para dentro.** Os adapters conhecem o core; o core só conhece as próprias portas.
3. **Entidades JPA ficam no adapter**, separadas do domínio (`EventEntity` ≠ `Event`), com um mapper entre os dois.
4. **Services sem `@Service`.** São registrados como `@Bean` em `CatalogConfiguration`, que devolve a interface do
   use case.
5. **Aggregate root:** setores e lotes entram pelo `Event` (`event.addLot`), e `Sector.addLot` é `internal`. Assim as
   regras do evento (só em `DRAFT`) não podem ser puladas.
6. **Command = dados + endereço** (`eventId`, `sectorId`). Ele não traz `id` nem `status`: quem decide isso é o
   domínio.

### Reconstituição: `restore`

Ao ler do banco, o adapter precisa recriar um evento que já existe, por exemplo `PUBLISHED`. Mas o construtor sempre cria
um evento novo em `DRAFT`, e `status` tem `private set`.

A solução é uma factory `restore(...)` no `companion object` do `Event` (e do `Sector`). Ela tem acesso aos membros
`private`, roda a validação de entrada do construtor e define o status e os setores salvos. As regras de negócio não
rodam de novo, porque já foram checadas quando o evento foi salvo.

Alternativas rejeitadas:

- **Status como parâmetro do construtor:** qualquer código poderia criar um evento já `PUBLISHED`, sem setores.
- **`@Entity` direto no domínio:** volta ao modelo do Caraíva e prende o domínio ao JPA.
- **Reflection** (como o Hibernate e o Jackson fazem): frágil, quebra sem aviso quando um campo é renomeado.

## Consequências

**Positivas**

- **Regras testáveis sem infraestrutura.** O domínio tem testes unitários puros, e os services são testados com um fake
  em memória (`InMemoryEventRepository`), sem banco nem Spring.
- **Cada camada de teste responde uma pergunta:** o domínio testa as regras, o service a orquestração, o
  `JpaEventRepositoryTest` o mapeamento no Postgres real e o `@WebMvcTest` a tradução para HTTP.
- **Trocar a entrada ou a saída não toca no core.** Um listener do RabbitMQ (passo 5) seria outro adapter de entrada
  chamando os mesmos use cases. Na extração (passo 6), o core do catálogo muda de lugar sem ser reescrito.
- **Os use cases viram a lista oficial do que o módulo faz.**

**Negativas**

- **Mais classes e mais mapeamento.** O mesmo conceito aparece como request DTO, command, domínio, entity e response DTO,
  com dois mappers no meio. Para um CRUD simples, é custo sem retorno.
- **Ida e volta pelo mapper a cada operação.** O `save` converte o aggregate inteiro para entities; como o id é
  atribuído pelo domínio, o Spring Data faz um `SELECT` antes do `INSERT` (otimizável com `Persistable` se virar
  problema).
- **Transação por método do adapter.** Hoje o `findById` e o `save` de um use case rodam em transações separadas. Isso
  será resolvido no passo 3, com `@Transactional` no use case e/ou `@Version`.
- **Fake guarda a mesma instância.** Um teste de service pode passar mesmo sem chamar `save`; quem pega isso é o teste
  de persistência ou o ponta a ponta.

## Quando não usar

Módulos que são só cadastro, sem regra (por exemplo, uma tabela de categorias), podem continuar em camadas. A decisão
vale para módulos com regras de negócio e estado, como catálogo, reserva e pedido.

## Referências

- Alistair Cockburn, [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Diagrama de classes do catálogo](../catalog/DIAGRAMA-DE-CLASSES.md)
- [ADR 0002: Virtual threads em vez de coroutines](0002-virtual-threads-instead-of-coroutines.md)
