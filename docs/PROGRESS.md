# Progresso — onde paramos

> Atualizado em 2026-11-09. Branch: `feat/catalog`. Roadmap: **passo 2 — Catálogo em estilo hexagonal**.

## Feito

Domínio + portas + 4 casos de uso do catálogo, tudo testado sem Spring e sem banco (58 testes verdes, `./gradlew test`).

### Estrutura de pacotes (`catalog/`)

```
domain/
  model/       Event (aggregate root), Sector, Lot
  enums/       EventStatus
  exception/   CatalogException (sealed) + uma exceção por regra
application/
  port/in/command/   CreateEventCommand, AddSectorCommand, AddLotCommand, PublishEventCommand
  port/in/usecase/   CreateEventUseCase, AddSectorUseCase, AddLotUseCase, PublishEventUseCase
  port/out/          EventRepository (save, findById)
  service/           um *Service por caso de uso, recebendo EventRepository no construtor
adapter/
  in/web/            (vazio — passo 3)
  out/persistence/   (vazio — próximo passo)
```

Testes: `domain/model/` (domínio), `application/service/` (casos de uso), `fakes/InMemoryEventRepository`, `fixtures/` (`anEvent`, `aSector`, `aLot`, `a*Command`).

### Regras do domínio

| Classe | Regras |
|---|---|
| `Lot` | Janela de venda `[startsAt, endsAt)`, `isOnSaleAt(moment)`, `overlaps(other)`. |
| `Sector` | `internal fun addLot`: rejeita `sortOrder` repetido e janelas sobrepostas. |
| `Event` | `status` com `private set`. `addSector` e `addLot(sectorId, lot)` só em DRAFT. `publish()` exige setores e lotes. |

Decisões tomadas:
- `Lot.quantity` continua `var` (escolha do Reynan).
- Validação de entrada (`init`) usa `require` → `IllegalArgumentException`. Regras de negócio usam exceções próprias com `if` + `throw`.
- Lotes entram **pelo aggregate root** (`event.addLot`), e `Sector.addLot` é `internal`. Fecha a brecha de adicionar lote em evento publicado.
- Command = dados + "endereço" (`eventId`, `sectorId`). Não tem `id` nem `status`: quem decide é o domínio.
- Core (`domain` + `application`) não importa nada de `org.springframework`.
- A V1 foi editada porque ainda não chegou na `main`. Depois do merge, toda mudança vira V2.

## Próximos passos

1. **Adapter de persistência** (`adapter/out/persistence`).
   - Decidir: JPA (entidades separadas do domínio) ou `JdbcClient`.
   - Resolver a **reconstituição**: como recriar um `Event` que já está `PUBLISHED` no banco se `status` tem `private set` e `addLot` bloqueia evento publicado?
   - Mapear composição (domínio: lote dentro do setor) ↔ FK (banco: `lots.sector_id`).
   - Teste de integração com Testcontainers.
   - Atenção: o fake guarda a mesma instância; o Postgres não. Testes de persistência pegam o que o fake esconde.
2. **Ligar no Spring**: criar os beans dos services (`@Configuration` fora do core) para o core continuar sem anotações.
3. **Adapter web** (`adapter/in/web`).
   - Controller + DTOs de request/response.
   - `@RestControllerAdvice` mapeando `CatalogException` → HTTP 422 (`EventNotFoundException`/`SectorNotFoundException` → 404) e `IllegalArgumentException` → 400.
   - Teste com `@WebMvcTest`.
4. **ADR `docs/adr/0001-hexagonal-catalog.md`** — por que hexagonal no catálogo, comparando com o layered do Caraíva.
5. **PR** `feat/catalog` → `main` com CI verde.

## Perguntas em aberto

1. Faz sentido aceitar um lote cuja venda termina **depois** do `startsAt` do evento? Se não, a regra fica no `Event` ou no `Sector`?
2. "Pista" e "pista" devem ser considerados o mesmo nome de setor?

Respondidas nesta sessão:
- ~~Brecha de `event.sectors[0].addLot(...)`~~ → fechada com `Event.addLot` + `internal`.
- ~~Por que "Expected an exception of class..."~~ → o `assertFailsWith` recebeu uma exceção de outro tipo (ex.: `NotImplementedError` do `TODO()`), não a esperada.

## Conceitos vistos nesta etapa

Função pura (`moment` como parâmetro em vez de `Instant.now()`), intervalo semiaberto, testes de borda, AAA, `@Nested`, `@ParameterizedTest`, fixtures, lifecycle do JUnit, backing property (`_lots`), `private set`, aggregate root, `sealed class`, `require` vs `if` + `throw`, `firstOrNull { }?.let { }`, *Tell, Don't Ask*.

Sessão de 2026-11-09: ports & adapters (porta de entrada × saída), inversão × injeção de dependência, command object, repository, use case/application service, fake em memória (e seu limite: mesma instância), operador Elvis `?:` com `throw`, `internal`, cada commit deve compilar sozinho, TDD com `TODO()` (teste vermelho → verde).
