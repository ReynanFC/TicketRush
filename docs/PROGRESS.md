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
 ⬜ Testes do adapter web (@WebMvcTest)   ← começar por aqui
 ⬜ ADR 0001
 ⬜ PR → main
```

69 testes verdes (`./gradlew test`). O adapter web ainda não tem teste próprio; foi verificado na mão com a app rodando (`./gradlew bootTestRun`).

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

Testes: `domain/model/` (domínio), `application/service/` (casos de uso com fake), `adapter/out/persistence/JpaEventRepositoryTest` (Postgres real, sem transação de teste), `TicketRushApplicationTests` (fluxo completo via Spring), `fakes/InMemoryEventRepository`, `fixtures/`.

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

## Próximos passos

1. **Testes do adapter web** com `@WebMvcTest(EventController::class)` e os 4 use cases mockados (sem banco).
   - Caminho feliz de cada rota: status, `Location` e corpo.
   - `@Valid`: campo em branco, `startsAt` no passado -> 400 com `fieldErrors`.
   - JSON sem campo obrigatório / UUID inválido na URL -> 400.
   - Use case lançando `EventNotFoundException` -> 404 e `DuplicateSectorNameException` -> 422.
   - O `@WebMvcTest` já carrega o `CatalogExceptionHandler` (é `@RestControllerAdvice`), então os testes de erro passam por ele.
2. **ADR `docs/adr/0001-hexagonal-catalog.md`** — por que hexagonal no catálogo, comparando com o layered do Caraíva. Incluir a decisão do `restore`.
3. **PR** `feat/catalog` -> `main` com CI verde.

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
