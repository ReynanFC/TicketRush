# Progresso — onde paramos

> Atualizado em 2026-11-09. Branch: `feat/catalog`. Roadmap: **passo 2 — Catálogo em estilo hexagonal**.

```
Passo 2 – Catálogo
 ✅ Domínio (Event, Sector, Lot)
 ✅ Porta de saída (EventRepository) + 4 casos de uso
 ✅ Adapter de persistência (JPA + Testcontainers)
 ✅ Ligação no Spring (@Configuration)
 ⬜ Adapter web (controller)        ← começar por aqui
 ⬜ ADR 0001
 ⬜ PR → main
```

69 testes verdes (`./gradlew test`). Árvore limpa no último commit (`d8033b7`).

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
  service/           um *Service por caso de uso, recebendo EventRepository no construtor
adapter/
  in/web/            (vazio — próximo passo)
  out/persistence/
    entity/          EventEntity, SectorEntity, LotEntity (@OneToMany unidirecional + @JoinColumn)
    EventPersistenceMapper      domínio ↔ entity (volta usa restore)
    SpringDataEventRepository   JpaRepository<EventEntity, UUID>, interno ao adapter
    JpaEventRepository          @Repository, implementa a porta EventRepository
config/
  CatalogConfiguration          @Bean de cada use case (devolve a interface)
```

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

## Próximos passos

1. **Adapter web** (`adapter/in/web`).
   - `EventController` recebendo os 4 use cases (interfaces) no construtor.
   - Endpoints:
     | Método | Rota | Use case |
     |---|---|---|
     | `POST` | `/events` | `CreateEventUseCase` |
     | `POST` | `/events/{eventId}/sectors` | `AddSectorUseCase` |
     | `POST` | `/events/{eventId}/sectors/{sectorId}/lots` | `AddLotUseCase` |
     | `POST` | `/events/{eventId}/publish` | `PublishEventUseCase` |
   - `dto/`: requests e responses. **DTO ≠ command**: ids vêm da URL, não do corpo; o controller traduz DTO → command.
   - `CatalogExceptionHandler` (`@RestControllerAdvice`): `EventNotFoundException`/`SectorNotFoundException` → 404, demais `CatalogException` → 422, `IllegalArgumentException` → 400.
   - Teste com `@WebMvcTest` e use cases mockados (sem banco).
2. **ADR `docs/adr/0001-hexagonal-catalog.md`** — por que hexagonal no catálogo, comparando com o layered do Caraíva. Incluir a decisão do `restore`.
3. **PR** `feat/catalog` → `main` com CI verde.

## Pontos de atenção para depois

- **Transação do caso de uso:** cada service faz `findById` e `save` em transações separadas (cada método do adapter tem a sua). Dois usuários editando o mesmo evento ao mesmo tempo podem sobrescrever um ao outro. Resolver no passo 3 (concorrência): `@Transactional` no use case e/ou versionamento otimista (`@Version`).
- **ID atribuído + Spring Data:** `save` faz `SELECT` antes do `INSERT` (merge). Otimizável com `Persistable` se virar problema.
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
