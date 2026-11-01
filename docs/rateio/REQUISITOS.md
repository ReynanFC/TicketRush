# Plataforma de Pagamentos com Split — Requisitos e Regras de Negócio

Oct 21, 2026 · @Reynan

## 1. O problema real

Um marketplace recebe o dinheiro de uma venda, mas esse dinheiro não é dele: precisa ser dividido entre vendedores, entregadores e a própria comissão, descontando taxas, respeitando prazos e sobrevivendo a estornos. A plataforma (nome provisório: **Rateio**) faz essa divisão automaticamente e de forma auditável.

**Cenário de referência:** a *Feira Digital Baiana*, um marketplace fictício de artesãos de Salvador. Uma compra pode ter produtos de três artesãos diferentes e um frete feito por um entregador parceiro.

### Como o marketplace sofre hoje, sem split

- **Todo o dinheiro cai na conta do marketplace**, que depois transfere manualmente para cada artesão. Ele passa a guardar dinheiro de terceiros, o que gera risco jurídico e tributário (o valor inteiro parece receita dele).
- **Conciliação em planilha:** alguém confere extrato contra pedidos todo dia. Erros de centavos viram horas de investigação.
- **Estorno depois do repasse vira prejuízo:** o comprador pede reembolso, mas o artesão já recebeu. O marketplace paga do próprio bolso.
- **O artesão não sabe quando vai receber**, nem quanto foi descontado de taxa. Isso gera atrito e suporte.
- **Chargeback chega semanas depois** e ninguém sabe de quem cobrar.

### O que a plataforma resolve

1. Divide cada pagamento entre os recebedores no momento da confirmação, segundo regras definidas na cobrança.
2. Mantém um **saldo individual por recebedor**, com prazos de liberação por método de pagamento.
3. Desfaz a divisão de forma proporcional em estornos e contestações, cobrando de quem é responsável.
4. Registra todo movimento em um **ledger de partidas dobradas**: nenhum saldo é "atualizado", ele é sempre a soma de lançamentos imutáveis. Isso torna qualquer centavo rastreável.
5. Avisa o marketplace de cada mudança por webhooks confiáveis, com reentrega automática.

> Valores de taxas, prazos e limites neste documento são **parâmetros simulados**, inspirados em práticas do mercado brasileiro. Não há integração com bancos ou bandeiras reais.

## 2. Glossário

Esses termos são a linguagem ubíqua do projeto: use exatamente esses nomes no código, nas filas e nos eventos.

| Termo | Significado no projeto |
| --- | --- |
| Marketplace | Cliente da plataforma. Integra via API e cria cobranças em nome dos seus vendedores. |
| Recebedor | Quem recebe parte do dinheiro: vendedor, entregador ou o próprio marketplace. Tem saldo próprio. |
| Cobrança | Pedido de pagamento feito ao comprador, com valor, método e regras de split. |
| Pagamento | A confirmação de que o comprador pagou uma cobrança. Uma cobrança tem no máximo um pagamento confirmado. |
| Split | Regra que divide o valor de uma cobrança entre recebedores. |
| MDR | Taxa cobrada sobre cada transação (percentual + valor fixo). É a receita principal da plataforma. |
| Custo de processamento | O que a plataforma paga ao provedor de pagamento (simulado). MDR menos custo = margem. |
| Recebível | Um valor futuro que um recebedor tem direito a receber em uma data. Cartão parcelado gera um recebível por parcela. |
| Liquidação | O momento em que um recebível vira saldo disponível. Ex.: D+30 = 30 dias após o pagamento. |
| Saldo pendente | Soma dos recebíveis ainda não liquidados. |
| Saldo disponível | Dinheiro que o recebedor já pode sacar. |
| Saldo bloqueado | Valor retido por contestação ou reserva de risco. Não pode ser sacado. |
| Saque | Transferência do saldo disponível para a conta bancária do recebedor. |
| Estorno | Devolução ao comprador, total ou parcial, iniciada pelo marketplace. |
| Contestação (chargeback) | Disputa aberta pelo portador do cartão junto ao banco emissor. Não é iniciada pelo marketplace. |
| Responsável por perdas | Recebedor que absorve estornos não cobertos e chargebacks perdidos. |
| Ledger | Livro contábil de lançamentos imutáveis em partidas dobradas. Fonte da verdade dos saldos. |
| Conta contábil | Uma "caixinha" do ledger, ex.: `recebedor:42:disponivel`. |
| Idempotency key | Chave enviada pelo cliente para que a mesma requisição repetida não gere efeito duplicado. |
| Dia útil | Dia que não é sábado, domingo ou feriado nacional. Liquidações só acontecem em dias úteis. |

## 3. Atores

São seis atores; três são pessoas com interface no sistema e três são sistemas que conversam por API ou fila.

| Ator | Tipo | O que quer | Como interage |
| --- | --- | --- | --- |
| Marketplace (ex.: Feira Digital Baiana) | Empresa cliente | Vender sem tocar no dinheiro dos vendedores, ver quanto ganhou de comissão | API REST + webhooks; painel web para configurar e acompanhar |
| Recebedor (artesão, entregador) | Pessoa física ou jurídica | Saber quanto vai receber, quando, e sacar | Painel web: extrato, agenda de recebíveis, saques |
| Comprador | Pessoa física | Pagar de forma simples e ter reembolso quando cabível | Página de pagamento (checkout) gerada pela cobrança |
| Operador da plataforma | Equipe interna | Aprovar recebedores, revisar fraudes, mediar contestações, ver receita | Painel administrativo |
| Provedor de pagamento (simulado) | Sistema externo | Processar cartão, Pix e boleto | Adaptadores; responde por API e por notificações assíncronas |
| Motor antifraude (Python) | Serviço interno | Pontuar o risco de cada cobrança | Consome eventos da fila e publica o resultado |

**Observação de modelagem:** o próprio marketplace também é um recebedor. É assim que a comissão dele vira saldo e entra no mesmo fluxo de liquidação e saque dos demais.

## 4. Escopo

A versão 1 cobre o ciclo completo de um pagamento com split, do checkout ao saque, incluindo estorno e contestação. Integrações reais com bancos ficam de fora; tudo que depende de terceiros é simulado por trás de interfaces.

**Dentro da v1**

- Cadastro de marketplaces, recebedores e contas bancárias, com análise simplificada
- Cobranças por Pix, boleto e cartão de crédito (à vista e parcelado sem juros para o comprador)
- Split por percentual ou valor fixo, com regras de arredondamento e responsabilidade
- Taxas por método de pagamento e por número de parcelas
- Recebíveis, calendário de dias úteis e liquidação diária
- Saldo (pendente, disponível, bloqueado), extrato e saques
- Estorno total e parcial
- Contestação de cartão com prazo de defesa
- Antifraude por regras e score, em serviço Python
- Webhooks com assinatura, reentrega e fila de mensagens mortas (DLQ)
- Ledger de partidas dobradas e conciliação diária

**Simulado (atrás de um Adapter)**

- Provedor de pagamento: aprova ou recusa cartões por regras de teste (ex.: final 0002 = recusado)
- Confirmação de Pix e boleto: um endpoint de simulação dispara a notificação de "pago"
- Transferência bancária do saque: sucesso ou falha simulada
- Abertura de contestação: disparada pelo painel do operador

**Fora da v1 (candidatos a v2)**

- Antecipação de recebíveis
- Parcelado com juros pagos pelo comprador
- Assinaturas e cobrança recorrente
- Mecanismo Especial de Devolução do Pix (MED)
- Reserva de risco variável por recebedor
- Assistente com LLM para o lojista e explicação de decisões de risco
- Emissão de nota fiscal e obrigações tributárias

## 5. O dinheiro de ponta a ponta: um exemplo

Uma compra de R$ 200,00 no cartão à vista termina com R$ 158,40 para o artesão, R$ 20,00 para o entregador, R$ 14,60 para o marketplace e R$ 7,00 de taxa para a plataforma, liberados em 23/11/2026.

**A compra:** Maria compra um cesto de palha (R$ 180,00) do artesão João, com frete de R$ 20,00 feito pelo entregador Caio, em 21/10/2026.

**Regras definidas pelo marketplace na cobrança:**

- Comissão do marketplace: 12% sobre o valor do produto
- Frete: 100% para o entregador
- Taxa da plataforma: paga pelo marketplace
- Responsável por perdas e por resto de arredondamento: o marketplace

**O cálculo** (valores simulados: MDR do crédito à vista 3,5%; custo do provedor 2,2%):

| Linha | Conta | Valor |
| --- | --- | --- |
| Valor pago pelo comprador |  | R$ 200,00 |
| Artesão João | 88% de R$ 180,00 | R$ 158,40 |
| Entregador Caio | 100% do frete | R$ 20,00 |
| Comissão bruta do marketplace | 12% de R$ 180,00 | R$ 21,60 |
| Taxa da plataforma (MDR) | 3,5% de R$ 200,00, descontada do marketplace | − R$ 7,00 |
| Comissão líquida do marketplace | R$ 21,60 − R$ 7,00 | R$ 14,60 |
| Custo do provedor (pago pela plataforma) | 2,2% de R$ 200,00 | R$ 4,40 |
| Margem da plataforma | R$ 7,00 − R$ 4,40 | R$ 2,60 |

Conferência: 158,40 + 20,00 + 14,60 + 7,00 = 200,00. **A soma das partes sempre fecha com o valor pago, ou o sistema rejeita a operação.**

**Quando cada um recebe:** cartão liquida em D+30. Trinta dias após 21/10/2026 é 20/11/2026, feriado nacional (Dia da Consciência Negra). A liquidação vai para o próximo dia útil: **segunda, 23/11/2026**. Até lá, os valores aparecem como saldo pendente.

**Como isso fica no ledger** (contas de recebedor representam dinheiro que a plataforma deve; crédito aumenta o saldo):

| Momento | Débito | Crédito | Valor |
| --- | --- | --- | --- |
| Pagamento confirmado | `custodia:a_receber_provedor` | `recebedor:joao:pendente` | R$ 158,40 |
| Pagamento confirmado | `custodia:a_receber_provedor` | `recebedor:caio:pendente` | R$ 20,00 |
| Pagamento confirmado | `custodia:a_receber_provedor` | `recebedor:feira:pendente` | R$ 14,60 |
| Pagamento confirmado | `custodia:a_receber_provedor` | `plataforma:receita_mdr` | R$ 7,00 |
| Custo registrado | `plataforma:custo_processamento` | `custodia:a_receber_provedor` | R$ 4,40 |
| Provedor repassa | `custodia:banco` | `custodia:a_receber_provedor` | R$ 195,60 |
| Liquidação (23/11) | `recebedor:joao:pendente` | `recebedor:joao:disponivel` | R$ 158,40 |

As liquidações do Caio e da Feira seguem o mesmo padrão da linha do João. Cada transação tem débitos iguais aos créditos; por isso o ledger inteiro sempre soma zero.

## 6. Regras de negócio

Cada regra tem um código (RN-xx) para ser citada em testes, ADRs e commits. Valores entre colchetes são parâmetros configuráveis; o número mostrado é o padrão.

### 6.1 Marketplaces e recebedores

- **RN-01** Todo recebedor pertence a exatamente um marketplace. Um marketplace só pode usar no split os seus próprios recebedores.
- **RN-02** Recebedor tem CPF ou CNPJ com dígito verificador válido. O mesmo documento não pode ser cadastrado duas vezes no mesmo marketplace.
- **RN-03** Estados do recebedor: `PENDENTE_ANALISE` → `ATIVO` | `REPROVADO`; `ATIVO` ↔ `BLOQUEADO`; qualquer estado → `ENCERRADO`.
- **RN-04** Só recebedor `ATIVO` pode ser incluído em uma nova cobrança.
- **RN-05** Recebedor `BLOQUEADO` continua recebendo créditos de cobranças já pagas, mas não pode sacar.
- **RN-06** Recebedor só pode ser `ENCERRADO` com saldo pendente, disponível e bloqueado iguais a zero.
- **RN-07** Saque exige conta bancária cadastrada cujo titular tem o mesmo documento do recebedor.
- **RN-08** Ao criar um marketplace, o sistema cria automaticamente um recebedor que representa o próprio marketplace.

### 6.2 Cobrança e seu ciclo de vida

- **RN-10** Valores são guardados em centavos, como inteiros (`Long`). Nunca `Double`.
- **RN-11** Valor da cobrança entre \[R$ 1,00\] e \[R$ 50.000,00\].
- **RN-12** Toda criação de cobrança exige `Idempotency-Key`. A mesma chave com o mesmo corpo devolve a cobrança já criada; a mesma chave com corpo diferente é rejeitada.
- **RN-13** O valor da cobrança é a soma dos seus itens. As regras de split são validadas na criação (seção 6.5) e ficam **imutáveis depois do pagamento**.
- **RN-14** Cobrança só pode ser cancelada antes de ser paga.
- **RN-15** Pix expira em \[60 minutos\]; boleto vence em \[3 dias corridos\]. Sem pagamento até lá, a cobrança vai para `EXPIRADA`.
- **RN-16** Uma cobrança tem no máximo um pagamento confirmado. Uma segunda notificação de pagamento para a mesma cobrança é ignorada e registrada para auditoria.
- **RN-17** Toda mudança de estado gera um evento de domínio com a data/hora e o estado anterior.

Estados e transições válidas (qualquer transição fora deste diagrama é rejeitada):

&#91;embedded content: ciclo de vida da cobrança · 11 estados, 5 finais\]

Cancelamento vale para cobranças Criada ou Aguardando pagamento. Contestação pode ser aberta a partir de Paga ou Estornada parcialmente; se for ganha, a cobrança volta ao estado em que estava.

### 6.3 Métodos de pagamento

Cada método muda três coisas: como o pagamento é confirmado, quanto custa e quando o dinheiro é liberado.

| Método | Confirmação | MDR simulado | Custo do provedor | Liquidação | Estorno | Contestação |
| --- | --- | --- | --- | --- | --- | --- |
| Pix | Assíncrona (notificação) | 0,99% | 0,40% | D+0 | Devolução Pix em até \[90 dias\] | Não |
| Boleto | Assíncrona, após compensação | R$ 3,49 fixo | R$ 1,50 fixo | D+1 útil após a compensação | Por transferência à conta do comprador | Não |
| Crédito à vista | Síncrona (autorização) | 3,5% | 2,2% | D+30 | Em até \[180 dias\] | Sim |
| Crédito 2 a 6x | Síncrona | 4,5% | 2,8% | Cada parcela em D+30 × n | Em até \[180 dias\] | Sim |
| Crédito 7 a 12x | Síncrona | 5,5% | 3,4% | Cada parcela em D+30 × n | Em até \[180 dias\] | Sim |

- **RN-20** Boleto pago após o vencimento é aceito em até \[5 dias\], com multa de \[2%\] e juros de \[1% ao mês\] pró-rata. A diferença vai integralmente para o recebedor responsável pelo resto de arredondamento.
- **RN-21** Boleto pago com valor diferente do esperado (fora da regra de multa e juros) não confirma a cobrança. O valor fica em `custodia:pagamentos_divergentes` para análise do operador.
- **RN-22** Parcela mínima de \[R$ 5,00\]. Cobrança de R$ 30,00 aceita no máximo 6 parcelas.
- **RN-23** No parcelado, o valor total é dividido igualmente; o resto de centavos vai para a primeira parcela. Ex.: R$ 100,00 em 3x = R$ 33,34 + R$ 33,33 + R$ 33,33.

### 6.4 Taxas

- **RN-25** Taxa = percentual × valor, arredondada para o centavo mais próximo (meio para cima), + valor fixo do método.
- **RN-26** A taxa é calculada e **congelada na criação da cobrança**. Uma mudança na tabela não afeta cobranças já criadas.
- **RN-27** Um marketplace pode ter tabela negociada, que substitui a padrão método a método.
- **RN-28** Em estorno, a taxa da plataforma \[não é devolvida\]. O custo do provedor também não volta para a plataforma.
- **RN-29** Tarifas avulsas: saque \[R$ 2,00\], com \[1\] saque grátis por mês por recebedor; contestação perdida \[R$ 15,00\].

### 6.5 Split

- **RN-30** Uma cobrança define o split por **percentual** (duas casas decimais) ou por **valor fixo** em centavos. Na v1, os dois tipos não se misturam na mesma cobrança.
- **RN-31** Percentuais somam exatamente 100,00%. Valores fixos somam exatamente o valor da cobrança.
- **RN-32** Entre \[1\] e \[10\] recebedores por cobrança, sem repetir recebedor.
- **RN-33** Exatamente um recebedor é o **responsável pelo resto**: recebe os centavos que sobram do arredondamento.
- **RN-34** Exatamente um recebedor é o **responsável pela taxa**: a taxa é descontada da parte dele. Se a parte dele for menor que a taxa, a cobrança é rejeitada na criação.
- **RN-35** Exatamente um recebedor é o **responsável por perdas**: absorve contestações perdidas (seção 6.9).
- **RN-36** Ordem de cálculo: (1) parte de cada um = valor × percentual, arredondada para baixo; (2) sobra vai para o responsável pelo resto; (3) taxa é descontada do responsável pela taxa.
- **RN-37** No parcelado, a parte de cada recebedor é dividida em parcelas pela regra RN-23, e a taxa é distribuída do mesmo jeito sobre as parcelas do responsável pela taxa.

Exemplo de arredondamento: cobrança de R$ 10,01, split 50% / 50%. Cada parte = 500,5 centavos → 500 centavos para cada. Sobra 1 centavo, que vai para o responsável pelo resto: R$ 5,01 + R$ 5,00.

### 6.6 Recebíveis e liquidação

- **RN-40** Cada pagamento confirmado gera um recebível por recebedor e por parcela, com valor e data prevista.
- **RN-41** Data prevista = data do pagamento + prazo do método. Se cair em sábado, domingo ou feriado nacional, vai para o próximo dia útil. Os feriados ficam em uma tabela mantida pelo operador.
- **RN-42** A liquidação roda uma vez por dia, às \[06:00\], e processa todo recebível `PENDENTE` com data prevista até hoje. Rodar duas vezes no mesmo dia não pode liquidar nada duas vezes.
- **RN-43** Estados do recebível: `PENDENTE` → `LIQUIDADO`; `PENDENTE` → `CANCELADO` (estorno antes da liquidação); `PENDENTE` ↔ `BLOQUEADO` (contestação).
- **RN-44** Recebível de recebedor `BLOQUEADO` liquida normalmente; o bloqueio impede apenas o saque (RN-05).

### 6.7 Saldo e saque

- **RN-50** O saldo nunca é uma coluna atualizada: é a soma dos lançamentos no ledger, separada em pendente, disponível, bloqueado e em saque.
- **RN-51** Saque mínimo de \[R$ 10,00\]. Valor do saque + tarifa ≤ saldo disponível.
- **RN-52** Ao solicitar o saque, o valor sai do disponível e vai para `em_saque` **na mesma transação**. Isso impede que duas solicitações simultâneas saquem o mesmo dinheiro.
- **RN-53** A transferência é processada de forma assíncrona. Sucesso: o valor sai da custódia. Falha: o valor e a tarifa voltam ao disponível.
- **RN-54** No máximo \[1\] saque em processamento por recebedor.
- **RN-55** O saldo disponível só fica negativo por estorno ou contestação, nunca por saque. Enquanto estiver negativo, novas liquidações cobrem a dívida primeiro e saques ficam bloqueados.

### 6.8 Estornos

- **RN-60** Só o marketplace inicia um estorno, para cobrança `PAGA` ou `ESTORNADA_PARCIALMENTE`, dentro do prazo do método (seção 6.3).
- **RN-61** Valor do estorno ≤ saldo estornável = valor pago − estornos anteriores.
- **RN-62** Por padrão, o estorno é distribuído **proporcionalmente** às partes originais de cada recebedor. O marketplace pode informar a distribuição explícita (ex.: devolver só o frete), desde que some o valor do estorno.
- **RN-63** Para cada recebedor, o débito segue a ordem: (1) reduz ou cancela recebíveis pendentes daquela cobrança; (2) debita do saldo disponível; (3) o que faltar deixa o saldo negativo (RN-55).
- **RN-64** Como a taxa não é devolvida (RN-28), num estorno total o responsável pela taxa também devolve a taxa ao comprador.
- **RN-65** Execução por método: Pix por devolução; cartão por cancelamento no provedor (parcelas futuras deixam de existir); boleto por transferência para uma conta informada pelo comprador.
- **RN-66** Cobrança `EM_CONTESTACAO` não aceita estorno.

Exemplo, estorno total da compra da seção 5: o comprador recebe R$ 200,00. João devolve R$ 158,40 e Caio R$ 20,00. A Feira devolve sua comissão bruta de R$ 21,60, pois os R$ 7,00 de taxa ficam com a plataforma. Como a Feira só tinha R$ 14,60, seu saldo termina em **− R$ 7,00**.

### 6.9 Contestações (chargeback)

Estorno e contestação são diferentes: o estorno é uma decisão do marketplace; a contestação é imposta pelo banco do comprador, chega semanas depois e pode ser disputada.

- **RN-70** Só existe para cartão. Na v1 é aberta pelo operador, simulando a bandeira, com um motivo: fraude, não reconhecida, produto não recebido ou produto diferente do anunciado.
- **RN-71** Ao abrir, o valor contestado é **bloqueado** do responsável por perdas: primeiro de recebíveis pendentes dele, depois do disponível, e o que faltar deixa o saldo negativo.
- **RN-72** O marketplace tem \[7 dias corridos\] para enviar defesa (texto + anexos, ex.: comprovante de entrega). Sem defesa no prazo, a contestação é perdida automaticamente.
- **RN-73** O operador registra o resultado. **Ganha:** o valor é desbloqueado. **Perdida:** o valor bloqueado sai definitivamente e é cobrada a tarifa de \[R$ 15,00\].
- **RN-74** Recebedor ou marketplace com contestações acima de \[1%\] das transações de cartão em 30 dias gera alerta ao operador.

Por que o responsável por perdas é, normalmente, o marketplace: o artesão já pode ter sacado o dinheiro há semanas. O marketplace absorve a perda e resolve com o vendedor pelos seus próprios contratos.

### 6.10 Antifraude

- **RN-80** Toda cobrança de cartão passa pelo antifraude antes da autorização. Pix e boleto são avaliados após o pagamento só para monitoramento, sem bloquear.
- **RN-81** O antifraude devolve um score de 0 a 100. Abaixo de \[30\]: aprova. De \[30\] a \[70\]: revisão manual. Acima de \[70\]: recusa.
- **RN-82** Revisão manual deve ser decidida pelo operador em até \[24 h\]. Sem decisão, a cobrança é recusada.
- **RN-83** Contingência: se o antifraude não responder em \[5 s\], cobranças até \[R$ 500,00\] são aprovadas e as demais vão para revisão manual.
- **RN-84** Regras iniciais (pontos somados, limitado a 100):
  - valor maior que 5× a média do comprador: +30
  - mais de 3 tentativas com o mesmo cartão em 10 minutos: +40
  - comprador sem histórico e valor acima de R$ 1.000,00: +20
  - documento do comprador em lista de bloqueio: score = 100
- **RN-85** Toda decisão guarda score, regras disparadas, versão das regras ou do modelo e o tempo de resposta. Isso permite auditar e explicar cada recusa.

### 6.11 Webhooks

- **RN-90** O marketplace cadastra uma URL e recebe um segredo. Cada entrega é assinada com HMAC-SHA256 em um cabeçalho.
- **RN-91** Entrega bem-sucedida = resposta HTTP 2xx em até \[10 s\].
- **RN-92** Falhas são reenviadas após 1 min, 5 min, 30 min, 2 h e 12 h. Depois da 5ª falha, o evento vai para a fila de mensagens mortas (DLQ), aparece no painel e pode ser reenviado manualmente.
- **RN-93** Entrega **pelo menos uma vez**: o mesmo evento pode chegar duas vezes. Cada evento tem um id único para o marketplace deduplicar.
- **RN-94** A ordem não é garantida. Cada evento traz a data/hora em que ocorreu e a versão da cobrança.
- **RN-95** Após \[20\] falhas seguidas, o endpoint fica `DEGRADADO` e o marketplace é avisado no painel. Os eventos continuam sendo gerados.

### 6.12 Ledger

- **RN-100** Lançamentos são imutáveis: nunca `UPDATE` nem `DELETE`. Correção é feita por uma transação reversa.
- **RN-101** Toda transação tem pelo menos dois lançamentos, e a soma dos débitos é igual à dos créditos.
- **RN-102** Toda transação aponta para a sua origem (cobrança, estorno, saque, contestação) e tem chave única de origem + tipo. A mesma origem nunca lança duas vezes.
- **RN-103** Saldo de uma conta = soma dos seus lançamentos. Fotografias diárias de saldo podem existir como otimização, nunca como fonte da verdade.
- **RN-104** Conciliação diária: (a) o ledger inteiro soma zero; (b) o valor a receber do provedor bate com o extrato simulado dele; (c) a soma dos recebíveis pendentes de cada recebedor bate com o seu saldo pendente. Qualquer divergência gera alerta.

## 7. Requisitos funcionais

São 32 requisitos agrupados por ator; cada um aponta para as regras que o governam.

**Marketplace (API e painel)**

- **RF-01** Cadastrar recebedores e suas contas bancárias (RN-01 a RN-08).
- **RF-02** Criar cobrança com itens, método, parcelas e regras de split (RN-10 a RN-13, RN-30 a RN-37).
- **RF-03** Consultar cobrança, seus pagamentos, estornos e histórico de estados.
- **RF-04** Cancelar cobrança ainda não paga (RN-14).
- **RF-05** Simular uma cobrança antes de criá-la: devolve taxa, parte líquida de cada recebedor e datas de liquidação.
- **RF-06** Solicitar estorno total ou parcial, com distribuição proporcional ou explícita (RN-60 a RN-66).
- **RF-07** Enviar defesa de contestação com texto e anexos (RN-72).
- **RF-08** Cadastrar URL de webhook, ver entregas, falhas e a DLQ, e reenviar eventos (RN-90 a RN-95).
- **RF-09** Ver relatório de comissão: bruto, taxas pagas e líquido por período.
- **RF-10** Gerar e revogar chaves de API.

**Recebedor (painel)**

- **RF-11** Ver saldos pendente, disponível, bloqueado e em saque.
- **RF-12** Ver extrato com cada lançamento e a sua origem (cobrança, estorno, saque, contestação).
- **RF-13** Ver agenda de recebíveis futuros por data.
- **RF-14** Solicitar saque e acompanhar seu status (RN-51 a RN-55).

**Comprador**

- **RF-15** Abrir a página de pagamento de uma cobrança e pagar por Pix, boleto ou cartão.
- **RF-16** Ver o status do pagamento em tempo real na página.

**Operador da plataforma**

- **RF-17** Aprovar, reprovar, bloquear e desbloquear recebedores (RN-03).
- **RF-18** Revisar cobranças em análise manual (RN-82).
- **RF-19** Abrir contestação simulada e registrar o seu resultado (RN-70, RN-73).
- **RF-20** Manter tabelas de taxas padrão e negociadas (RN-25 a RN-27).
- **RF-21** Manter o calendário de feriados (RN-41).
- **RF-22** Ver receita da plataforma: MDR, custo do provedor e margem por período e por método.
- **RF-23** Ver alertas de conciliação, de contestações e de endpoints degradados.
- **RF-24** Tratar pagamentos divergentes de boleto (RN-21).

**Processos automáticos**

- **RF-25** Expirar cobranças Pix e boleto vencidas (RN-15).
- **RF-26** Gerar recebíveis ao confirmar pagamento (RN-40, RN-41).
- **RF-27** Liquidar recebíveis diariamente (RN-42).
- **RF-28** Processar saques de forma assíncrona (RN-53).
- **RF-29** Avaliar risco das cobranças de cartão e aplicar contingência (RN-80 a RN-85).
- **RF-30** Entregar webhooks com reentrega e DLQ (RN-92).
- **RF-31** Encerrar contestações sem defesa no prazo (RN-72).
- **RF-32** Executar a conciliação diária (RN-104).

## 8. Requisitos não funcionais

O requisito que manda em todos os outros: **nenhum centavo pode ser criado, perdido ou duplicado**, mesmo com falhas de rede, mensagens repetidas e processos rodando em paralelo.

| Código | Tema | Requisito |
| --- | --- | --- |
| RNF-01 | Consistência | Mudança de estado da cobrança e lançamentos no ledger acontecem na mesma transação de banco. |
| RNF-02 | Eventos confiáveis | Eventos são gravados numa tabela de saída (outbox) na mesma transação e publicados na fila depois. Uma queda do broker não perde eventos. |
| RNF-03 | Idempotência | Todo consumidor de fila tolera a mesma mensagem duas vezes sem efeito duplicado. Toda escrita da API pública aceita `Idempotency-Key`. |
| RNF-04 | Concorrência | Duas operações simultâneas sobre o mesmo saldo não podem gastá-lo duas vezes (bloqueio otimista ou pessimista; decisão em ADR). |
| RNF-05 | Auditoria | Todo lançamento, mudança de estado e ação de operador guarda quem, quando e de onde. Nada é apagado. |
| RNF-06 | Segurança | Autenticação por chave de API (marketplace) e JWT (painéis), com papéis por ator. Segredos de webhook e dados de cartão nunca aparecem em logs. |
| RNF-07 | Dados de cartão | O sistema não guarda número completo do cartão; usa um token devolvido pelo provedor simulado e os últimos 4 dígitos. |
| RNF-08 | Desempenho | Criação de cobrança em menos de \[300 ms\] no p95, sem contar o antifraude. |
| RNF-09 | Disponibilidade | Se o antifraude ou o serviço de webhooks cair, a criação e o pagamento de cobranças continuam (RN-83, outbox). |
| RNF-10 | Observabilidade | Logs estruturados com um id de correlação que atravessa API, filas e serviços; métricas de tamanho de fila, DLQ, latência e taxa de recusa. |
| RNF-11 | Testabilidade | Regras de domínio testáveis sem banco nem broker; integração testada com Testcontainers (PostgreSQL e RabbitMQ). |
| RNF-12 | Tempo | Toda data/hora gravada em UTC; dias úteis e vencimentos calculados no fuso America/Sao\_Paulo. O relógio é injetável para testes. |
| RNF-13 | Dinheiro | Valores em centavos (`Long`); arredondamentos sempre explícitos e cobertos por testes. |

## 9. Casos de borda

Cada caso abaixo deve virar pelo menos um teste automatizado. São eles que provam que as regras se sustentam.

| # | Situação | Comportamento esperado |
| --- | --- | --- |
| 1 | Notificação de pagamento Pix chega duas vezes | Segunda é ignorada; nenhum lançamento duplicado (RN-16, RN-102). |
| 2 | Pix pago 1 segundo depois de expirar | Cobrança `EXPIRADA` não volta a `PAGA`; valor vai para pagamentos divergentes e é devolvido ao pagador. |
| 3 | Marketplace cancela no mesmo instante em que o pagamento é confirmado | Só uma transição vence (controle de concorrência); se o pagamento venceu, o cancelamento é rejeitado. |
| 4 | Dois saques simultâneos com saldo para apenas um | Um é aceito, o outro rejeitado por saldo insuficiente (RN-52). |
| 5 | Estorno parcial depois de parte das parcelas já liquidadas | Primeiro reduz parcelas pendentes, depois debita disponível (RN-63). |
| 6 | Estorno total quando o artesão já sacou tudo | Saldo do artesão fica negativo e é compensado em vendas futuras (RN-55). |
| 7 | Contestação aberta contra cobrança já estornada parcialmente | Valor contestado limitado ao que ainda não foi estornado. |
| 8 | Recebedor bloqueado depois de a cobrança ser criada, antes do pagamento | Pagamento é aceito; o recebedor recebe saldo, mas não saca (RN-05). |
| 9 | Liquidação cai em feriado seguido de fim de semana | Vai para o primeiro dia útil seguinte (RN-41). |
| 10 | Job de liquidação reinicia no meio da execução | Ao rodar de novo, liquida só o que faltou (RN-42). |
| 11 | Broker fora do ar quando o pagamento é confirmado | Pagamento confirma; evento fica no outbox e é publicado quando o broker voltar (RNF-02). |
| 12 | Antifraude responde depois do tempo limite | Decisão de contingência já tomada prevalece; resposta tardia é registrada, não aplicada (RN-83). |
| 13 | Webhook do marketplace responde 200 depois de 15 s | Conta como falha e é reenviado; marketplace precisa deduplicar (RN-91, RN-93). |
| 14 | Taxa maior que a parte do responsável pela taxa | Cobrança rejeitada na criação (RN-34). |
| 15 | Split de R$ 0,03 entre 2 recebedores com 50% cada | R$ 0,02 para o responsável pelo resto e R$ 0,01 para o outro (RN-36). |
| 16 | Mudança na tabela de taxas com cobranças em aberto | Cobranças abertas mantêm a taxa congelada (RN-26). |
| 17 | Boleto pago com R$ 0,50 a menos | Cobrança não confirma; valor vai para divergentes (RN-21). |
| 18 | Transferência de saque falha | Valor e tarifa voltam ao disponível (RN-53). |

## 10. Decisões em aberto

Estas decisões mudam regras ou modelagem; vale fechar cada uma antes de codar o módulo correspondente e registrar em ADR.

- [ ] Permitir misturar valor fixo e percentual no mesmo split? (ex.: frete fixo de R$ 20,00 + percentuais sobre o resto)
- [ ] Estorno parcial deve devolver parte da taxa proporcionalmente, ou a taxa nunca volta (RN-28)?
- [ ] Na contestação perdida, a perda fica toda com o responsável por perdas ou é repassada proporcionalmente, como no estorno?
- [ ] O saldo negativo de um recebedor pode ser compensado com saldo de outro recebedor do mesmo marketplace?
- [ ] Bloqueio otimista (versão na linha) ou pessimista (`SELECT ... FOR UPDATE`) para saldo e cobrança?
- [ ] O antifraude responde por fila com espera de até 5 s, ou por chamada HTTP síncrona, deixando a fila só para monitoramento?
- [ ] Haverá reserva de segurança fixa (ex.: reter 10% por 30 dias) para recebedores novos, ou fica para a v2?
- [ ] Quais dados mínimos o recebedor informa no cadastro, e o que a análise simplificada verifica?
- [ ] O checkout do comprador é uma página do próprio painel Angular ou uma aplicação separada?
