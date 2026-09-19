# 01 — Domínio de Pedido

## Contexto

Primeiro recorte de domínio do serviço de pedidos, antes de qualquer infraestrutura (sem Spring, sem JPA, sem persistência). Estabelece o comportamento de `Pedido`, `ItemPedido` e `StatusPedido` como base para casos de uso futuros.

Exemplo de referência usado na aula: o cliente `c-1` compra `CAFE-500`, quantidade 2, preço unitário `18.90`, totalizando `37.80`.

## Tarefa

Modelar o domínio de pedido descrevendo comportamento (o que o pedido faz e permite) antes de qualquer detalhe de implementação:

- Um pedido pode ser criado como um rascunho novo, vazio, no estado ABERTO.
- Um pedido aberto aceita receber itens; ao receber um item, o resultado é um novo pedido (o original não é alterado).
- Um item de pedido tem uma quantidade e um preço unitário; o total do item é derivado desses dois valores, nunca informado diretamente.
- Um pedido aberto pode ser pago ou cancelado.
- Um pedido pago ou cancelado não aceita mais itens.
- Um pedido pago ou cancelado não pode mudar de estado novamente (não pode ser pago de novo, nem cancelado, nem reaberto).
- A lista de itens de um pedido não pode ser alterada por quem está fora do pedido (nenhuma referência interna exposta que permita mutação externa).
- Todo valor monetário é representado com `BigDecimal`.

## Regras

- Quantidade de um item deve ser positiva (maior que zero). Quantidade zero ou negativa é rejeitada.
- Preço unitário de um item deve ser positivo (maior que zero). Preço zero ou negativo é rejeitado.
- O total de um item é derivado (quantidade × preço unitário), nunca um campo independente passível de divergir.
- Dinheiro sempre em `BigDecimal` — nunca `float`/`double`.
- `Pedido.novo(...)` cria um pedido rascunho, estado ABERTO, lista de itens vazia, identificado por um UUID gerado na criação.
- `adicionarItem(...)` devolve uma nova instância de `Pedido` (imutabilidade); a instância original permanece inalterada.
- A partir de ABERTO, as únicas transições válidas são para PAGO ou para CANCELADO.
- Qualquer transição a partir de PAGO ou de CANCELADO é recusada (incluindo pagar um pedido já pago, cancelar um pedido já pago, pagar um pedido já cancelado, cancelar um pedido já cancelado, ou tentar voltar a ABERTO).
- Tentar adicionar item a um pedido PAGO ou CANCELADO é recusado.
- A lista de itens exposta pelo pedido deve ser protegida por cópia defensiva — nenhuma modificação externa à lista pode afetar o estado interno do pedido.
- Usar `record` para `Pedido`, `ItemPedido` e para representar dinheiro/quantidade quando aplicável.
- Nenhuma dependência de Spring, JPA ou qualquer framework de infraestrutura neste domínio.
- Nenhuma dependência nova no `pom.xml` sem pedir e obter aprovação explícita antes (conforme `AGENTS.md`).

## Definição de pronto

Cada regra abaixo corresponde a um caso de teste que deve existir e passar:

1. **Criação de pedido novo**: `Pedido.novo(...)` produz um pedido com estado ABERTO, lista de itens vazia e um identificador UUID não nulo.
2. **Quantidade positiva aceita**: adicionar um item com quantidade positiva (ex.: `CAFE-500`, quantidade 2, preço `18.90`) é aceito.
3. **Quantidade zero rejeitada**: adicionar um item com quantidade zero é recusado.
4. **Quantidade negativa rejeitada**: adicionar um item com quantidade negativa é recusado.
5. **Preço positivo aceito**: adicionar um item com preço unitário positivo é aceito.
6. **Preço zero rejeitado**: adicionar um item com preço unitário zero é recusado.
7. **Preço negativo rejeitado**: adicionar um item com preço unitário negativo é recusado.
8. **Total do item derivado**: para `CAFE-500`, quantidade 2, preço `18.90`, o total do item calculado é `37.80`.
9. **Total do item usa BigDecimal**: o total do item é do tipo `BigDecimal`, não `float`/`double`.
10. **Adicionar item devolve novo pedido**: chamar `adicionarItem` retorna uma instância de `Pedido` diferente da original; o pedido original permanece com sua lista de itens anterior inalterada.
11. **Pedido novo começa ABERTO**: um pedido recém-criado tem estado ABERTO.
12. **ABERTO aceita pagar**: um pedido ABERTO pode transicionar para PAGO.
13. **ABERTO aceita cancelar**: um pedido ABERTO pode transicionar para CANCELADO.
14. **PAGO não aceita novo item**: adicionar item a um pedido PAGO é recusado.
15. **CANCELADO não aceita novo item**: adicionar item a um pedido CANCELADO é recusado.
16. **PAGO não aceita pagar de novo**: transicionar para PAGO um pedido já PAGO é recusado.
17. **PAGO não aceita cancelar**: transicionar para CANCELADO um pedido já PAGO é recusado.
18. **CANCELADO não aceita cancelar de novo**: transicionar para CANCELADO um pedido já CANCELADO é recusado.
19. **CANCELADO não aceita pagar**: transicionar para PAGO um pedido já CANCELADO é recusado.
20. **Lista de itens protegida contra mutação externa**: obter a lista de itens do pedido e tentar modificá-la (ex.: `add`/`remove`) não altera o estado interno do pedido (lança exceção de imutabilidade ou não reflete no pedido).
21. **Cenário de referência da aula**: pedido do cliente `c-1` com item `CAFE-500`, quantidade 2, preço `18.90`, resulta em total do item `37.80`.

## Ambiguidades a decidir

- **Total do pedido**: a tarefa pede "total derivado", mas não fica claro se `Pedido` deve expor um total agregado (soma dos totais dos itens) nesta spec, ou se isso fica para uma spec futura junto com casos de uso. Assumi que "total derivado" se refere ao total de cada `ItemPedido` (quantidade × preço); se você quiser um `Pedido.total()` agregado já nesta etapa, preciso que confirme.
- **Tipo de exceção para regras violadas**: não foi especificado se as rejeições (quantidade/preço inválidos, transição inválida, item em pedido fechado) devem lançar uma exceção de domínio específica (ex.: `PedidoInvalidoException`) ou exceções padrão do Java (`IllegalArgumentException`/`IllegalStateException`). Preciso de uma decisão antes de implementar.
- **Identificador do item**: `CAFE-500` sugere um código de produto/SKU, mas não está definido se `ItemPedido` guarda apenas esse código como `String` ou se há um tipo `Produto`/`CodigoProduto` dedicado. Assumi código simples (`String`) até segunda ordem.
- **Igualdade de `Pedido` como record**: como `record` gera `equals`/`hashCode` a partir de todos os componentes, e o pedido carrega uma lista de itens, dois pedidos com mesmo UUID mas item adicionado em ordem diferente (mesma soma) seriam considerados diferentes. Não sei se isso é aceitável ou se `Pedido` deve ter igualdade por identidade (UUID). Preciso de confirmação.
- **Cliente do pedido**: o cenário da aula menciona `c-1` como cliente, mas a tarefa não define explicitamente um campo de cliente em `Pedido`. Não incluí um campo de cliente na modelagem central desta spec por não ter sido pedido; avise se deve ser incluído aqui ou fica para spec futura.
