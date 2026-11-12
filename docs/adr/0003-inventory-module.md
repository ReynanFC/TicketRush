# 0003. Módulo `inventory` separado do catálogo

- **Status:** aceito
- **Data:** 2026-11-12

## Contexto

O passo 3 do roadmap traz a reserva de ingressos, com uma regra que não pode falhar: **nunca vender mais do que o
estoque**. A primeira pergunta é onde o estoque mora: dentro do `catalog/`, como um campo do `Lot`, ou num módulo
próprio.

Catálogo e estoque falam do mesmo "lote", mas têm padrões de uso opostos:

| | Catálogo | Estoque |
|---|---|---|
| Pergunta | *O que* está à venda (nome, preço, janela de venda) | *Quantos* ainda restam |
| Quem escreve | O organizador, poucas vezes | Milhares de compradores ao mesmo tempo |
| Quando muda | Só em `DRAFT`; depois do `publish()` fica congelado | O tempo todo durante a venda |
| Custo de um erro | Um texto errado, que dá para corrigir | Ingresso vendido que não existe |

No DDD, isso indica dois **bounded contexts**: o mesmo assunto, com modelos diferentes para cada contexto.

Se o estoque ficasse dentro do `Event`, a reserva seguiria o padrão dos services do catálogo (`findById` do aggregate
inteiro, depois mudar em memória, depois `save` do aggregate inteiro), o que traz dois problemas:

1. **Lost update.** Dois compradores leem `quantity = 1`, os dois passam na checagem e os dois gravam `0`. Resultado:
   dois ingressos vendidos.
2. **Lock grande demais.** Qualquer lock (por exemplo, `@Version` no `Event`) protege o evento inteiro. Um comprador da
   Pista entraria em conflito com um comprador do Camarote, e o organizador editando o evento entraria em conflito com as
   vendas. Seria um único caixa para o estádio inteiro.

O aggregate deve ser o **menor** conjunto de dados que precisa ficar consistente junto. "Quantos restam no lote X" não
depende do nome do evento nem do lote Y.

## Decisão

### 1. Pacote `inventory/` separado

Ele usa a mesma organização hexagonal do catálogo ([ADR 0001](0001-hexagonal-catalog.md)): `domain/`,
`application/port/{in,out}`, `application/service/`, `adapter/`, `config/`. O aggregate root é o `LotStock` (um por
lote), com as reservas (`Reservation`) do lote. O modelo segue o `MODELAGEM.md`, seção 7.3.

### 2. Referência só por ID, sem imports entre os domínios

O `inventory` **não importa** `catalog.domain` (nem o contrário). Ele conhece o lote só pelo `lotId` e guarda **a
própria cópia** do que precisa:

```
catalog.Lot                 inventory.LotStock
  id  ────────────────────────► lotId        (referência por ID)
  quantity ──(copiado 1 vez)──► total
                                available    (só o inventory muda)
```

Cada informação tem um único dono: depois que o estoque é criado, `Lot.quantity` significa "quantos foram colocados à
venda" e não muda mais. Quem muda é `available`.

### 3. O estoque nasce na publicação do evento

O `publish()` é o momento certo: antes dele, nada está à venda; depois dele, `ensureDraft()` impede mudar setores e
lotes, então o `total` está estável.

### 4. A comunicação acontece por um evento de integração do Spring, síncrono e na mesma transação

```
PublishEventService ──► porta de saída (interface no catalog)
                              ▲
                              │ implementa
                    adapter com ApplicationEventPublisher
                              │
                              ▼  evento de integração (ex.: CatalogEventPublished)
              listener no inventory (adapter de entrada, @EventListener)
                              │
                              ▼
              use case do inventory: cria um LotStock por lote
```

Regras que acompanham a decisão:

- **O core do catalog não conhece o Spring.** O `PublishEventService` chama uma porta de saída, e o adapter dela usa o
  `ApplicationEventPublisher`. Mantém a regra 1 do ADR 0001.
- **O evento de integração é um contrato público, não domínio.** Ele carrega só dados simples (`eventId` e, por lote,
  `lotId`, `sectorId` e `quantity`), nunca `Event` ou `Lot`. É a única classe do catalog que o inventory pode importar,
  e por isso fica num pacote que deixe isso claro, fora de `catalog/domain`. No passo 6, ela vira o corpo da mensagem.
- **O listener é `@EventListener` síncrono**, e roda na mesma thread e na mesma transação do `publish`. Se a criação do
  estoque falhar, a publicação sofre rollback: ou tudo acontece, ou nada.
- **Pré-requisito: transação no nível do use case** (issue #18). Hoje o `findById` e o `save` têm transações separadas
  dentro do `JpaEventRepository`, e o `save` comitaria sozinho antes do listener.

## Alternativas rejeitadas

- **Estoque dentro do `Lot` (`quantity` decrementado na venda).** Causa lost update e um lock no aggregate inteiro,
  como descrito no contexto.
- **Inventory lendo as tabelas do catalog.** Acopla os módulos pelo banco, de forma invisível no código. Quebra no
  passo 6, quando cada serviço tiver o próprio banco.
- **Porta do catalog chamando o use case do inventory direto.** Funcionaria e daria a mesma garantia de transação.
  Ficou de fora porque o catalog passaria a saber que o inventory existe, enquanto o evento deixa o catalog só avisar
  "publiquei". Também já prepara o caminho do passo 5.
- **`@TransactionalEventListener` (`AFTER_COMMIT`).** O listener roda depois do commit. Se ele falhar, o evento fica
  `PUBLISHED` sem estoque, sem aviso nenhum. Além disso, sem uma transação ativa no momento da publicação, o Spring
  **não chama o listener** (a menos que se use `fallbackExecution = true`). Hoje não há transação no use case, então o
  estoque nunca seria criado.

## Consequências

**Positivas**

- **O lock fica do tamanho certo.** A disputa acontece só entre quem quer o mesmo lote. A estratégia de lock em si
  (update condicional, `@Version` ou `SELECT ... FOR UPDATE`) será escolhida no #16, com um ADR próprio.
- **Extrair o `inventory-service` (passo 6) fica barato:** quase só mover o pacote e trocar o adapter da porta.
- **O passo 5 troca só o adapter.** Em vez de `ApplicationEventPublisher`, ele grava na outbox, e um relay publica no
  RabbitMQ. O `PublishEventService` não muda.

**Negativas**

- **Antecipa um pedaço do passo 5** (eventos do Spring) e cria uma classe de contrato e uma porta a mais.
- **O #18 vira pré-requisito** de criar o estoque.
- **A garantia de "tudo ou nada" é provisória.** Ela existe porque os dois módulos dividem o mesmo processo e o mesmo
  banco. Quando forem serviços separados, não haverá transação comum. É esse problema que o **outbox** (passo 5) e a
  **saga** (passo 6) resolvem.
- **Dados duplicados.** O `total` do estoque é uma cópia do `quantity` do lote. Isso é aceitável porque o lote fica
  congelado depois da publicação.

## Referências

- [MODELAGEM.md](../MODELAGEM.md), seções 5.2 (máquina de estados da reserva) e 7.3 (inventory-service)
- [ADR 0001: Arquitetura hexagonal no catálogo](0001-hexagonal-catalog.md)
- [Spring: Transaction-bound Events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- Martin Fowler, [Bounded Context](https://martinfowler.com/bliki/BoundedContext.html)
