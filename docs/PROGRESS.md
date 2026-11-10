# Progresso — onde paramos

> Atualizado em 2026-11-10. Branch: `feat/catalog`. Roadmap: **passo 2 — Catálogo em estilo hexagonal**.

```
Passo 2 – Catálogo
 ✅ Domínio (Event, Sector, Lot)
 ✅ Porta de saída (EventRepository) + 4 casos de uso
 ✅ Adapter de persistência (JPA + Testcontainers)
 ✅ Ligação no Spring (@Configuration)
 ✅ Adapter web (controller, DTOs, mapper, exception handler, Swagger)
 ✅ Logs nos services (SLF4J)
 ✅ Testes do adapter web (@WebMvcTest)
 ✅ Virtual threads + Java 25 (ADR 0002, PR #2 aberto contra a main)
 ⬜ Branch develop + pipeline só de testes   ← começar por aqui
 ⬜ ADR 0001
 ⬜ PR feat/catalog → develop
```

87 testes verdes (`./gradlew test`). A `feat/catalog` já tem o merge local da `chore/virtual-threads-java-25` e ainda não foi enviada ao GitHub.

## Feito

### Estrutura de pacotes (`catalog/`)

```
domain/
  model/       Event (aggregate root), Sector, Lot
  enums/       EventStatus
  exception/   CatalogException (sealed) + uma exceção por regra (inclui EventNotFound, SectorNotFound)
application/
  port/in/command/   CreateEventCommand, AddSectorCommand, AddLotCommand, PublishEventCommand
  port/in/usecase/   CreateEventUseCase, AddSectorUseCase, AddLotUseCase, PublishEventUseCase
  port/out/          EventRepository (save, findById)
  service/           um *Service por caso de uso, recebendo EventRepository no construtor (com logs SLF4J)
adapter/
  in/web/
    EventController             implementa EventControllerDocs; DTO -> mapper -> use case -> mapper -> ResponseEntity
    docs/            EventControllerDocs — anotações do Swagger (springdoc), fora do controller
    dto/request/     *RequestDTO com Bean Validation (@field:...) e @Schema
    dto/response/    *ResponseDTO (status é String, não o enum do domínio)
    dto/error/       ErrorResponseDTO, ValidationErrorResponseDTO (+ FieldErrorDTO)
    exception/       CatalogExceptionHandler (@RestControllerAdvice)
    mapper/          EventWebMapper — toCommand (request -> command) e toResponse (domínio -> response)
  out/persistence/
    entity/          EventEntity, SectorEntity, LotEntity (@OneToMany unidirecional + @JoinColumn)
    mapper/          EventPersistenceMapper — domínio ↔ entity (volta usa restore)
    repository/      SpringDataEventRepository — JpaRepository<EventEntity, UUID>, interno ao adapter
    JpaEventRepository          @Repository, implementa a porta EventRepository (o adapter em si)
config/
  CatalogConfiguration          @Bean de cada use case (devolve a interface)
```

Fora do `catalog/`: `config/OpenApiConfiguration` (título e convenções da API no Swagger UI, `/swagger-ui.html`).

### API

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| `POST` | `/events` | 201 + `Location` | 400 |
| `POST` | `/events/{eventId}/sectors` | 201 + `Location` | 400, 404, 422 |
| `POST` | `/events/{eventId}/sectors/{sectorId}/lots` | 201 + `Location` | 400, 404, 422 |
| `POST` | `/events/{eventId}/publish` | 200 | 404, 422 |

Erros no `CatalogExceptionHandler`: `CatalogException` -> `when` exaustivo (404 not found, 422 o resto); `@Valid`, JSON ilegível e UUID inválido na URL -> 400 com `fieldErrors`; `IllegalArgumentException` (require do domínio) -> 400; erros do Spring (405, 415) mantêm o status; resto -> 500 com stack trace só no log.

Testes: `domain/model/` (domínio), `application/service/` (casos de uso com fake), `adapter/out/persistence/JpaEventRepositoryTest` (Postgres real, sem transação de teste), `adapter/in/web/EventControllerTest` (`@WebMvcTest`, use cases mockados: status, `Location`, corpo e tradução de erros; fixtures dos requests em `fixtures/web/`), `TicketRushApplicationTests` (fluxo completo via Spring), `fakes/InMemoryEventRepository`, `fixtures/`.

### Regras do domínio

| Classe | Regras |
|---|---|
| `Lot` | Janela de venda `[startsAt, endsAt)`, `isOnSaleAt(moment)`, `overlaps(other)`. |
| `Sector` | `internal fun addLot`: rejeita `sortOrder` repetido e janelas sobrepostas. `restore(id, name, lots)`. |
| `Event` | `status` com `private set`. `addSector` e `addLot(sectorId, lot)` só em DRAFT. `publish()` exige setores e lotes. `restore(...)` recria um evento existente (ex.: PUBLISHED). |

### Decisões tomadas

- `Lot.quantity` continua `var` (escolha do Reynan).
- Validação de entrada (`init`) usa `require` → `IllegalArgumentException`. Regras de negócio usam exceções próprias com `if` + `throw`.
- Lotes entram **pelo aggregate root** (`event.addLot`), e `Sector.addLot` é `internal`.
- Command = dados + "endereço" (`eventId`, `sectorId`). Não tem `id` nem `status`: quem decide é o domínio.
- **Reconstituição:** factory `restore` no `companion object` (acessa `private`). Rejeitadas: status no construtor (abre brecha), `@Entity` no domínio (quebra o hexagonal), reflection (frágil).
- **JPA** no adapter, com entidades separadas do domínio. `ddl-auto: validate` confere as entidades contra a V1.
- Services **sem `@Service`**: ligados via `CatalogConfiguration`, fora do core.
- Core (`domain` + `application`) não importa nada de `org.springframework`.
- A V1 foi editada porque ainda não chegou na `main`. Depois do merge, toda mudança vira V2.
- DTOs com sufixo `DTO` (preferência do Reynan). Mapeamento fora do controller, em `EventWebMapper`.
- Erros com DTO próprio (`ErrorResponseDTO`/`ValidationErrorResponseDTO`), não `ProblemDetail`.
- `endsAt > startsAt` fica no domínio (`require` no `Lot`), não no DTO: regra entre campos, já garantida lá.
- Logs: `DEBUG` na entrada do service, `INFO` depois do `save`; recusas logadas uma vez no handler (`WARN`), não com try/catch nos services. SLF4J no core é aceito (fachada, não framework).
- Testes do controller mockam os use cases e forçam as exceções com `willThrow`: testam só a tradução para HTTP. A regra em si é testada no domínio (ex.: overlap no `SectorTest`); o fluxo ponta a ponta fica para um teste de integração.
- **Virtual threads, não coroutines** (ADR 0002): JDBC é bloqueante, então `suspend` não traria ganho. Java 25 para evitar *pinning* em `synchronized`. Pool do Hikari fica em 10 até medir no passo 3.
- **Fluxo de branches:** `feat/...` / `chore/...` → PR para a `develop` (pipeline só de testes) → `main` só quando o passo estiver pronto.

## Próximos passos

1. **Branch `develop` + pipeline só de testes.**
   - Publicar a `develop` no GitHub (hoje ela só existe localmente, igual à `main`).
   - Workflow que roda `./gradlew test` em PR e push para a `develop`. O workflow atual (`ci.yml`, `./gradlew build`) continua para a `main`.
   - Proteger a `develop` com o check de testes obrigatório.
   - Trocar a base do PR #2 (`chore/virtual-threads-java-25`) de `main` para `develop`.
2. **ADR `docs/adr/0001-hexagonal-catalog.md`** — por que hexagonal no catálogo, comparando com o layered do Caraíva. Incluir a decisão do `restore`.
3. **PR** `feat/catalog` -> `develop` com os testes verdes. Para a `main` só quando o passo 2 estiver fechado.

## Pontos de atenção para depois

- **Transação do caso de uso:** cada service faz `findById` e `save` em transações separadas (cada método do adapter tem a sua). Dois usuários editando o mesmo evento ao mesmo tempo podem sobrescrever um ao outro. Resolver no passo 3 (concorrência): `@Transactional` no use case e/ou versionamento otimista (`@Version`).
- **ID atribuído + Spring Data:** `save` faz `SELECT` antes do `INSERT` (merge). Otimizável com `Persistable` se virar problema.
- **Handler global:** `CatalogExceptionHandler` vale para todos os controllers. Quando surgirem outros módulos, mover validação/JSON/500 para um handler compartilhado.
- **`TicketRushApplicationTests`** grava no banco e não limpa. Ok por enquanto (ids novos a cada execução).
- **Fake guarda a mesma instância:** testes de service podem passar sem `save`; quem pega isso é o teste de persistência / ponta a ponta.

## Perguntas em aberto

1. Faz sentido aceitar um lote cuja venda termina **depois** do `startsAt` do evento? Se não, a regra fica no `Event` ou no `Sector`?
2. "Pista" e "pista" devem ser considerados o mesmo nome de setor?

## Conceitos vistos nesta etapa

Sessões anteriores: função pura, intervalo semiaberto, testes de borda, AAA, `@Nested`, `@ParameterizedTest`, fixtures, lifecycle do JUnit, backing property, `private set`, aggregate root, `sealed class`, `require` vs `if` + `throw`, `firstOrNull { }?.let { }`, *Tell, Don't Ask*.

Sessão de 2026-11-09:
- **Hexagonal:** port = interface na fronteira do core. **IN** = o que o core oferece (use case, implementado pelo core, chamado de fora). **OUT** = o que o core precisa (repository, implementado de fora, chamado pelo core). O core define as duas.
- **Use case:** orquestra (buscar → domínio age → salvar), sem regra de negócio. Lista oficial do que o sistema faz; reaproveitável por vários adapters de entrada.
- Inversão × injeção de dependência, command object, repository, fake em memória (e seu limite).
- Reconstituição de aggregate (`restore` em `companion object`); por que mapper (MapStruct) não "pula" `private set` e reflection (Hibernate/Jackson) pula.
- JPA em Kotlin: `plugin.jpa` / `allOpen`, não usar `data class`, `@OneToMany` unidirecional, `cascade` + `orphanRemoval`, `Optional` → `?`.
- `@Transactional(readOnly = true)` e `LazyInitializationException` com `open-in-view: false`.
- Testcontainers + `@ServiceConnection`; `@DataJpaTest` × `@SpringBootTest`; `Propagation.NOT_SUPPORTED` para o teste se comportar como produção.
- `@Configuration` + `@Bean` para ligar o core no Spring sem anotar o core.
- Kotlin: Elvis `?:` com `throw`, `internal`, function reference (`::toDomain`).
- Git: cada commit deve compilar sozinho. TDD com `TODO()` (vermelho → verde).

Sessão de 2026-11-10:
- `@WebMvcTest`: slice só da web, use cases como `@MockitoBean`. `given(...).willReturn` / `willThrow`, `verifyNoInteractions`.
- A nulidade do Kotlin vale em compilação: JSON sem campo obrigatório falha no Jackson (antes do `@Valid`), e um mock do Mockito pode devolver `null` num tipo não nulo quando o stub não casa (`equals` do command).
- `jsonPath` compara tipos: `UUID`/`Instant` precisam de `toString()`. `doesNotExist()` passa com campo ausente ou `null`.
- Cada camada de teste responde uma pergunta: domínio (regra), service (orquestração), controller (tradução HTTP), integração (tudo junto).
- Virtual threads × coroutines, *pinning*, pool de conexões (HikariCP) e por que pool maior não é mais rápido.
- Concorrência: coroutines/threads **geram** concorrência (testes); o banco (locks, update condicional) **protege** a regra.
