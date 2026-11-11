# Catálogo — diagramas de classes

> Estado do código em 2026-11-10 (branch `feat/catalog`). Pacote base: `com.reynanfc.ticketrush.catalog`.
> Quando uma classe mudar, atualize o diagrama no mesmo PR.

O módulo está dividido em quatro diagramas, do centro para fora do hexágono:

1. [Domínio](#1-domínio) — as regras de negócio.
2. [Visão hexagonal](#2-visão-hexagonal) — portas, casos de uso e adapters ligados.
3. [Adapter web](#3-adapter-web) — DTOs, mapper e tratamento de erros.
4. [Adapter de persistência](#4-adapter-de-persistência) — entidades JPA e mapper.

Legenda: `+` público, `-` privado, `~` internal, `$` no fim = estático (`companion object`). `*--` composição (a parte não existe sem o todo), `..|>` implementa, `..>` depende de / usa.

---

## 1. Domínio

`Event` é o aggregate root: setores e lotes só mudam através dele. Nada aqui conhece Spring, JPA ou HTTP.

```mermaid
classDiagram
    direction LR

    class Event {
        +UUID id
        +UUID organizerId
        +String name
        +String venue
        +String city
        +Instant startsAt
        +EventStatus status «private set»
        +List~Sector~ sectors
        +addSector(sector: Sector)
        +addLot(sectorId: UUID, lot: Lot)
        +publish()
        -ensureDraft()
        +restore(...)$
    }

    class Sector {
        +UUID id
        +String name
        +List~Lot~ lots
        ~addLot(lot: Lot)
        +restore(id, name, lots)$
    }

    class Lot {
        +UUID id
        +String name
        +Long priceCents
        +Int quantity «var»
        +Int sortOrder
        +Instant startsAt
        +Instant endsAt
        +isOnSaleAt(moment: Instant) Boolean
        +overlaps(other: Lot) Boolean
    }

    class EventStatus {
        <<enumeration>>
        DRAFT
        PUBLISHED
        CLOSED
    }

    Event "1" *-- "0..*" Sector : sectors
    Sector "1" *-- "0..*" Lot : lots
    Event --> EventStatus : status
```

Regras de cada classe:

| Classe | Regras |
|---|---|
| `Event` | `addSector`, `addLot` e `publish` só em DRAFT. Nome de setor único. `publish` exige setores, e cada setor precisa de pelo menos um lote. |
| `Sector` | `addLot` é `internal`: lotes entram pelo `Event`. `sortOrder` único e janelas de venda sem sobreposição. |
| `Lot` | Janela `[startsAt, endsAt)`. `init` valida nome, quantidade > 0, preço >= 0 e `endsAt > startsAt`. |

### Exceções do domínio

`CatalogException` é `sealed`: o `when` no `CatalogExceptionHandler` é exaustivo.

```mermaid
classDiagram
    direction TB

    class RuntimeException
    class CatalogException {
        <<sealed>>
    }

    RuntimeException <|-- CatalogException

    CatalogException <|-- EventNotFoundException
    CatalogException <|-- SectorNotFoundException
    CatalogException <|-- EventNotInDraftException
    CatalogException <|-- DuplicateSectorNameException
    CatalogException <|-- DuplicateLotSortOrderException
    CatalogException <|-- OverlappingLotWindowException
    CatalogException <|-- EventHasNoSectorsException
    CatalogException <|-- SectorHasNoLotsException

    class EventNotFoundException {
        +UUID eventId
    }
    class SectorNotFoundException {
        +UUID sectorId
    }
    class EventNotInDraftException {
        +UUID eventId
        +EventStatus status
    }
    class DuplicateSectorNameException {
        +String sectorName
    }
    class DuplicateLotSortOrderException {
        +Int sortOrder
    }
    class OverlappingLotWindowException {
        +UUID lotId
        +UUID conflictingLotId
    }
    class EventHasNoSectorsException {
        +UUID eventId
    }
    class SectorHasNoLotsException {
        +UUID sectorId
        +String sectorName
    }
```

| HTTP | Exceções |
|---|---|
| 404 | `EventNotFoundException`, `SectorNotFoundException` |
| 422 | todas as outras |

---

## 2. Visão hexagonal

A estrutura do módulo. As setas de dependência sempre apontam **para dentro** (para o core). O core define as portas, e os adapters as implementam (porta de saída) ou as chamam (porta de entrada).

```mermaid
classDiagram
    direction LR

    namespace adapter_in_web {
        class EventController {
            <<RestController>>
            +createEvent(request) ResponseEntity~EventResponseDTO~
            +addSector(eventId, request) ResponseEntity~SectorResponseDTO~
            +addLot(eventId, sectorId, request) ResponseEntity~LotResponseDTO~
            +publishEvent(eventId) ResponseEntity~EventResponseDTO~
        }
    }

    namespace application_port_in {
        class CreateEventUseCase {
            <<interface>>
            +create(command: CreateEventCommand) Event
        }
        class AddSectorUseCase {
            <<interface>>
            +addSector(command: AddSectorCommand) Sector
        }
        class AddLotUseCase {
            <<interface>>
            +addLot(command: AddLotCommand) Lot
        }
        class PublishEventUseCase {
            <<interface>>
            +publish(command: PublishEventCommand) Event
        }
    }

    namespace application_service {
        class CreateEventService
        class AddSectorService
        class AddLotService
        class PublishEventService
    }

    namespace application_port_out {
        class EventRepository {
            <<interface>>
            +save(event: Event)
            +findById(id: UUID) Event?
        }
    }

    namespace adapter_out_persistence {
        class JpaEventRepository {
            <<Repository>>
        }
    }

    EventController ..> CreateEventUseCase
    EventController ..> AddSectorUseCase
    EventController ..> AddLotUseCase
    EventController ..> PublishEventUseCase

    CreateEventService ..|> CreateEventUseCase
    AddSectorService ..|> AddSectorUseCase
    AddLotService ..|> AddLotUseCase
    PublishEventService ..|> PublishEventUseCase

    CreateEventService ..> EventRepository
    AddSectorService ..> EventRepository
    AddLotService ..> EventRepository
    PublishEventService ..> EventRepository

    JpaEventRepository ..|> EventRepository
```

Como ler:

- **Porta de entrada** (`port/in`): o que o catálogo oferece. Implementada pelos services e chamada pelo controller, que só conhece a interface.
- **Porta de saída** (`port/out`): o que o catálogo precisa. Chamada pelos services e implementada pelo adapter JPA.
- Os services não têm `@Service`. O `CatalogConfiguration` (`@Configuration`) cria cada um como `@Bean` e devolve a interface do use case.
- Os commands (`CreateEventCommand`, `AddSectorCommand`, `AddLotCommand`, `PublishEventCommand`) são `data class` em `port/in/command`. Eles carregam os dados e o "endereço" (`eventId`, `sectorId`), sem `id` nem `status`.

---

## 3. Adapter web

O controller só cuida do HTTP. A tradução fica no `EventWebMapper`, e os erros, no `CatalogExceptionHandler`.

```mermaid
classDiagram
    direction LR

    class EventControllerDocs {
        <<interface>>
        anotações do Swagger
    }
    class EventController {
        <<RestController>>
        -locationOf(id: UUID) URI
    }
    class EventWebMapper {
        <<object>>
        +toCommand(CreateEventRequestDTO) CreateEventCommand
        +toCommand(eventId, AddSectorRequestDTO) AddSectorCommand
        +toCommand(eventId, sectorId, AddLotRequestDTO) AddLotCommand
        +toResponse(Event) EventResponseDTO
        +toResponse(Sector) SectorResponseDTO
        +toResponse(Lot) LotResponseDTO
    }

    EventController ..|> EventControllerDocs
    EventController ..> EventWebMapper

    class CreateEventRequestDTO {
        +UUID organizerId
        +String name
        +String venue
        +String city
        +Instant startsAt
    }
    class AddSectorRequestDTO {
        +String name
    }
    class AddLotRequestDTO {
        +String name
        +Long priceCents
        +Int quantity
        +Int sortOrder
        +Instant startsAt
        +Instant endsAt
    }

    class EventResponseDTO {
        +UUID id
        +UUID organizerId
        +String name
        +String venue
        +String city
        +Instant startsAt
        +String status
        +List~SectorResponseDTO~ sectors
    }
    class SectorResponseDTO {
        +UUID id
        +String name
        +List~LotResponseDTO~ lots
    }
    class LotResponseDTO {
        +UUID id
        +String name
        +Long priceCents
        +Int quantity
        +Int sortOrder
        +Instant startsAt
        +Instant endsAt
    }

    EventWebMapper ..> CreateEventRequestDTO
    EventWebMapper ..> AddSectorRequestDTO
    EventWebMapper ..> AddLotRequestDTO
    EventWebMapper ..> EventResponseDTO
    EventResponseDTO *-- SectorResponseDTO
    SectorResponseDTO *-- LotResponseDTO
```

### Erros

```mermaid
classDiagram
    direction LR

    class CatalogExceptionHandler {
        <<RestControllerAdvice>>
        +handleCatalog(CatalogException) 404 ou 422
        +handleInvalidBody(MethodArgumentNotValidException) 400
        +handleUnreadableBody(HttpMessageNotReadableException) 400
        +handleTypeMismatch(MethodArgumentTypeMismatchException) 400
        +handleIllegalArgument(IllegalArgumentException) 400
        +handleUnexpected(Exception) status do Spring ou 500
    }
    class ErrorResponseDTO {
        +Instant timestamp
        +Int status
        +String error
        +String message
        +String path
    }
    class ValidationErrorResponseDTO {
        +Instant timestamp
        +Int status
        +String error
        +String message
        +String path
        +List~FieldErrorDTO~ fieldErrors
    }
    class FieldErrorDTO {
        +String field
        +String message
        +Any? rejectedValue
    }

    CatalogExceptionHandler ..> ErrorResponseDTO
    CatalogExceptionHandler ..> ValidationErrorResponseDTO
    ValidationErrorResponseDTO *-- FieldErrorDTO
```

---

## 4. Adapter de persistência

As entidades JPA são separadas do domínio. O `EventPersistenceMapper` traduz nos dois sentidos, e a volta usa `restore` para não repetir as regras de negócio.

```mermaid
classDiagram
    direction LR

    class EventRepository {
        <<interface>>
        porta de saída do core
    }
    class JpaEventRepository {
        <<Repository>>
        +save(event: Event)
        +findById(id: UUID) Event?
    }
    class SpringDataEventRepository {
        <<interface>>
        extends JpaRepository of EventEntity and UUID
    }
    class EventPersistenceMapper {
        <<object>>
        +toEntity(Event) EventEntity
        +toDomain(EventEntity) Event
    }

    class EventEntity {
        <<Entity>>
        tabela events
        +UUID id
        +UUID organizerId
        +String name
        +String venue
        +String city
        +Instant startsAt
        +EventStatus status
        +MutableList~SectorEntity~ sectors
    }
    class SectorEntity {
        <<Entity>>
        tabela sectors
        +UUID id
        +String name
        +MutableList~LotEntity~ lots
    }
    class LotEntity {
        <<Entity>>
        tabela lots
        +UUID id
        +String name
        +Long priceCents
        +Int quantity
        +Int sortOrder
        +Instant startsAt
        +Instant endsAt
    }

    JpaEventRepository ..|> EventRepository
    JpaEventRepository ..> SpringDataEventRepository
    JpaEventRepository ..> EventPersistenceMapper
    SpringDataEventRepository ..> EventEntity
    EventEntity "1" *-- "0..*" SectorEntity : OneToMany + JoinColumn event_id
    SectorEntity "1" *-- "0..*" LotEntity : OneToMany + JoinColumn sector_id
```

Os relacionamentos são `@OneToMany` unidirecionais com `cascade = ALL` e `orphanRemoval = true`. Salvar o `EventEntity` salva a árvore inteira, do mesmo jeito que o aggregate é salvo inteiro pela porta.
