# Caderno de estudo

> O que fazer está nas **issues** do GitHub, agrupadas por milestone (um por passo do roadmap):
> `gh issue list --milestone "Passo 3 — Reserva"`. Pontos de atenção têm a label `tech-debt`.
>
> Este arquivo guarda o que não "fecha": estrutura, decisões tomadas e conceitos aprendidos. Decisões grandes viram ADR em `docs/adr/`.

# Passo 2 — Catálogo

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
- **Lote termina antes do início do evento** (decidido em 2026-11-11, issue #10): `lot.endsAt < event.startsAt`, igualdade recusada. A conta fica no `Event.addLot` (só ele conhece o `startsAt`), lançando `LotEndsTooLateException` (422). Não vira `CHECK` no banco (envolve duas tabelas).
- **Fluxo de branches:** `feat/...` / `chore/...` → PR para a `develop` (pipeline só de testes) → `main` só quando o passo estiver pronto.

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
