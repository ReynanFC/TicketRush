# Progresso — onde paramos

> Atualizado em 2026-11-06. Branch: `feat/catalog`. Roadmap: **passo 2 — Catálogo em estilo hexagonal**.

## Feito

Domínio do catálogo completo e testado (35 testes verdes, `./gradlew test`).

| Arquivo | O que é |
|---|---|
| `db/migration/V1__create_catalog.sql` | Tabelas `events`, `sectors`, `lots`. Status em inglês (`DRAFT`, `PUBLISHED`, `CLOSED`). |
| `catalog/domain/Lot.kt` | Lote com janela de venda `[startsAt, endsAt)`. `isOnSaleAt(moment)` e `overlaps(other)`. |
| `catalog/domain/Sector.kt` | Setor com lista protegida de lotes. `addLot` rejeita `sortOrder` repetido e janelas sobrepostas. |
| `catalog/domain/Event.kt` | **Aggregate root.** `status` com `private set`. `addSector` (só em DRAFT, nome único) e `publish()` (DRAFT → PUBLISHED, exige setores e lotes). |
| `catalog/domain/EventStatus.kt` | Enum `DRAFT`, `PUBLISHED`, `CLOSED` (front-end traduz). |
| `catalog/domain/exception/` | `CatalogException` (sealed) + uma exceção por regra violada. |
| `test/.../catalog/fixtures/` | `LotFixtures`, `SectorFixtures`, `EventFixtures` (dados falsos válidos por padrão). |
| `test/.../catalog/domain/` | `LotTest`, `SectorTest`, `EventTest` (`@Nested`, AAA, `@ParameterizedTest`). |

Decisões tomadas:
- `Lot.quantity` continua `var` (escolha do Reynan).
- Validação de entrada (`init`) usa `require` → `IllegalArgumentException`. Regras de negócio entre objetos usam exceções próprias com `if` + `throw`.
- A V1 foi editada porque ainda não chegou na `main`. Depois do merge, toda mudança vira V2.

## Pendências de revisão no `Event.kt` (pequenas)

- [x] `ensureDraft()` está público → deve ser `private`.
- [x] `firstOrNull() { ... }` → tirar os `()`.
- [x] Linha em branco sobrando no começo de `publish()`.
- [x] `?.let {` + `sector ->` na linha de baixo → convenção é `?.let { sector ->` na mesma linha.

Commit sugerido: `refactor(catalog): tidy up Event`.

## Próximos passos

1. **Portas (ports)** — onde o hexagonal aparece de verdade.
   - Porta de saída: interface `EventRepository` (`save`, `findById`) no lado do domínio/aplicação, sem Spring.
   - Porta de entrada: caso de uso `CreateEvent` (e depois `AddSector`, `AddLot`, `PublishEvent`).
   - Teste do caso de uso com um **fake em memória** do `EventRepository`.
2. **Adapter de persistência** (`adapter/out/persistence`).
   - Decidir: JPA (entidades separadas do domínio) ou `JdbcClient`.
   - Resolver a **reconstituição**: como recriar um `Event` que já está `PUBLISHED` no banco se `status` tem `private set`?
   - Teste de integração com Testcontainers (aqui entra o `@BeforeEach` para limpar tabelas).
3. **Adapter web** (`adapter/in/web`).
   - Controller + DTOs de request/response.
   - `@RestControllerAdvice` mapeando `CatalogException` → HTTP 422 e `IllegalArgumentException` → 400.
   - Teste com `@WebMvcTest`.
4. **ADR `docs/adr/0001-hexagonal-catalog.md`** — por que hexagonal no catálogo, comparando com o layered do Caraíva.
5. **PR** `feat/catalog` → `main` com CI verde.

## Perguntas em aberto (responder no início da próxima sessão)

1. Faz sentido aceitar um lote cuja venda termina **depois** do `startsAt` do evento? Se não, a regra fica no `Event` ou no `Sector`?
2. Depois de publicado, `event.sectors[0].addLot(...)` ainda funciona direto no setor. Que brecha isso abre no aggregate? Como fechar?
3. "Pista" e "pista" devem ser considerados o mesmo nome de setor?
4. Por que o `should reject an event without sectors` falhou com "Expected an exception of class..." enquanto os outros falharam com `EventNotInDraftException`?

## Conceitos vistos nesta etapa

Função pura (`moment` como parâmetro em vez de `Instant.now()`), intervalo semiaberto, testes de borda e "prova de fogo", AAA, `@Nested`, `@ParameterizedTest`, fixtures, lifecycle do JUnit (por que não precisou de `@BeforeEach`), backing property (`_lots`), `private set`, aggregate root, `sealed class`, `require` vs `if` + `throw`, `firstOrNull { }?.let { }`, *Tell, Don't Ask*.
