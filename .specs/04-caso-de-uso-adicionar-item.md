# 04 — Caso de uso: Adicionar Item (ponta a ponta)

## Contexto

`CriarPedido` (spec 02) e os adapters JPA/REST (spec 03) já existem e funcionam ponta a ponta para criação de pedido. Esta spec adiciona um segundo caso de uso, `AdicionarItem`, que busca um pedido já existente pelo port `Pedidos`, usa o domínio para incluir um novo item, salva pelo mesmo port, e expõe isso via um novo endpoint HTTP. `CriarPedido` não é alterado.

## Tarefa

Implementar `AdicionarItem` como novo caso de uso da camada de aplicação, mais o adapter REST correspondente:

- Um port de entrada `AdicionarItem` descreve a operação: adicionar um item a um pedido existente, identificado por UUID.
- `AdicionarItemService` implementa `AdicionarItem`: busca o pedido pelo port `Pedidos`, aplica `Pedido.adicionarItem(...)` (domínio, já existente), salva o resultado pelo mesmo port `Pedidos`, e devolve o pedido salvo (mesma disciplina da spec 02: devolve o retorno de `Pedidos.salvar`, não uma cópia local).
- Pedido inexistente: `AdicionarItemService` lança `PedidoNaoEncontradoException` (nova, na camada de aplicação, Java puro).
- Pedido em status diferente de ABERTO: o próprio domínio (`Pedido.adicionarItem`, spec 01) já recusa com `IllegalStateException` — reaproveitado aqui, sem exceção nova.
- Quantidade ou preço inválidos: o domínio (`ItemPedido`, spec 01/03) já recusa com `ItemInvalidoException` — reaproveitado, sem mudança.
- `POST /pedidos/{id}/itens` expõe o caso de uso via HTTP, devolvendo `200 OK` com o `PedidoResponse` atualizado (mesmo DTO da spec 03, refletindo o novo total).
- O `PedidoExceptionHandler` existente (spec 03) é **ampliado**, não recriado: adiciona mapeamento para `PedidoNaoEncontradoException` (404) e para `IllegalStateException` vindo de pedido fechado (409).

## Regras

- Port de entrada: `br.com.pedidos.api.aplicacao.pedido.AdicionarItem`, interface com um método, ex.: `Pedido adicionar(UUID pedidoId, ItemPedido item)`.
- `AdicionarItemService implements AdicionarItem`, depende apenas de `Pedidos` (port de saída) e do domínio, recebido por construtor — mesma disciplina de `CriarPedidoService`.
- Port de saída `Pedidos` ganha um novo método `Optional<Pedido> buscarPorId(UUID id)` (extensão da interface existente da spec 02) — continua sem mencionar JPA/SQL.
- `PedidosJpaAdapter` (spec 03) passa a implementar esse novo método da interface — ele já tinha um `buscarPorId` interno; a mudança é declará-lo na interface `Pedidos`.
- `PedidoNaoEncontradoException` vive em `br.com.pedidos.api.aplicacao.pedido`, `RuntimeException` pura, sem import de framework.
- Nenhum import de `org.springframework.*`, `jakarta.persistence.*` ou `jakarta.servlet.*` em `domain/` ou `aplicacao/` (regra herdada de todas as specs anteriores).
- `CriarPedido`, `CriarPedidoService` e o contrato de `POST /pedidos` **não são alterados**.
- Novo endpoint em `PedidoController` (mesma classe da spec 03, método novo) ou um segundo controller no mesmo pacote `adapter/entrada/rest` — decisão de organização fica para o plano de implementação, sem impacto de regra.
- Sem Lombok, sem MapStruct, sem H2, sem dependência nova no `pom.xml`.

## Contrato HTTP

### `POST /pedidos/{id}/itens`

**Request** (`ItemRequest`, mesmo DTO da spec 03):
```json
{ "sku": "CAFE-500", "quantidade": 1, "precoUnitario": 18.90 }
```

**Response 200** (`PedidoResponse`, mesmo DTO da spec 03, refletindo o pedido após o novo item):
```json
{
  "id": "963fcc3e-f671-4dbe-86e1-a248a54de3c8",
  "clienteId": null,
  "itens": [
    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90, "total": 37.80 },
    { "sku": "CAFE-500", "quantidade": 1, "precoUnitario": 18.90, "total": 18.90 }
  ],
  "status": "ABERTO",
  "total": 56.70
}
```

- `id`: mesmo UUID do pedido original — não muda ao adicionar item.
- `total`: agregado recalculado (soma de todos os itens, incluindo o novo).
- `clienteId`: como o domínio `Pedido` não persiste cliente (ambiguidade já registrada nas specs 02/03), a resposta deste endpoint não tem um `clienteId` de request para ecoar — ver ambiguidade abaixo.

**Erros**:

| Situação | Status | Mecanismo |
|---|---|---|
| `quantidade` ou `precoUnitario` ≤ 0 | 422 | `ItemInvalidoException` (já existente, spec 01/03) |
| Pedido não está ABERTO (PAGO/CANCELADO) | 409 | `IllegalStateException` (já existente, spec 01), novo mapeamento no handler |
| Pedido não encontrado (UUID inexistente) | 404 | `PedidoNaoEncontradoException` (nova) |
| JSON malformado / campo obrigatório ausente | 400 | mecanismos já existentes do `PedidoExceptionHandler` (spec 03), sem mudança |

## Casos de teste (definição de pronto)

**Unitários (`AdicionarItemServiceTest`, sem Spring, sem banco, `Pedidos` em memória):**

1. **Adiciona item a pedido existente e ABERTO**: pedido salvo previamente com um item `CAFE-500` (2×18.90); `adicionar(id, ItemPedido("CAFE-500", 1, 18.90))` devolve pedido com dois itens.
2. **Total após adicionar**: pedido resultante tem itens cuja soma dos totais é `56.70` (`37.80 + 18.90`).
3. **Mesmo UUID preservado**: o UUID do pedido devolvido é igual ao UUID do pedido original.
4. **Pedido salvo pelo port**: a implementação em memória de `Pedidos` reflete o pedido atualizado (com dois itens) após a chamada.
5. **Pedido inexistente lança `PedidoNaoEncontradoException`**: `adicionar(UUID.randomUUID(), item)` quando não há pedido salvo com esse id.
6. **Pedido PAGO recusa item**: pedido salvo previamente com status PAGO; `adicionar(...)` lança `IllegalStateException`; `Pedidos` em memória não reflete alteração.
7. **Pedido CANCELADO recusa item**: mesmo caso acima, com status CANCELADO.
8. **Quantidade inválida recusada pelo domínio**: `adicionar(id, ItemPedido inválido)` propaga `ItemInvalidoException` (constrói o `ItemPedido` já dispara a exceção, antes mesmo de chamar `adicionar`).

**Cenário HTTP (via `PedidoControllerIT` ampliado ou novo `*IT`, execução explícita contra Postgres real):**

9. **201 → 200 encadeados**: `POST /pedidos` cria pedido com `CAFE-500` 2×18.90 (`total: 37.80`); em seguida `POST /pedidos/{id}/itens` com `CAFE-500` 1×18.90 devolve `200` e `total: 56.70`, mesmo `id`.
10. **quantidade inválida → 422**: `POST /pedidos/{id}/itens` com `quantidade: 0` no pedido criado acima → `422`; nenhuma linha nova em `item_pedido` para esse pedido além das já existentes.
11. **pedido inexistente → 404**: `POST /pedidos/{uuid-aleatorio}/itens` → `404`.
12. **pedido fechado → 409**: (cenário manual/])script, já que não há endpoint de pagar/cancelar exposto via HTTP ainda) — coberto apenas no teste unitário (caso 6/7) nesta spec; validação HTTP de 409 fica registrada como ambiguidade abaixo, já que não existe hoje uma forma de fechar um pedido via API.

## Limites de arquivo

Para manter o caso de uso enxuto, cada arquivo novo deve caber nestas faixas aproximadas (guia, não regra rígida):

- `AdicionarItem.java` (port): ≤ 15 linhas.
- `PedidoNaoEncontradoException.java`: ≤ 10 linhas.
- `AdicionarItemService.java`: ≤ 40 linhas.
- Alteração em `Pedidos.java`: +1 método (≤ 5 linhas adicionadas).
- Alteração em `PedidosJpaAdapter.java`: `implements` do método já existente, sem novo arquivo.
- Alteração em `PedidoController.java` ou novo método/controller: ≤ 30 linhas adicionadas.
- Alteração em `PedidoExceptionHandler.java`: +2 métodos (≤ 20 linhas adicionadas).
- `AdicionarItemServiceTest.java`: ≤ 100 linhas.

## Ambiguidades a decidir

- **`clienteId` na resposta de `POST /pedidos/{id}/itens`**: como o domínio não persiste cliente, não há de onde ler um `clienteId` para devolver nesse endpoint (diferente de `POST /pedidos`, que ecoa o valor do request). Vou devolver `clienteId: null` no `PedidoResponse` deste endpoint, a menos que você prefira outra solução (ex.: omitir o campo, ou finalmente resolver a ambiguidade de cliente no domínio).
- **Prova HTTP do 409 (pedido fechado)**: não existe hoje endpoint para pagar/cancelar um pedido via API, então o cenário "pedido fechado → 409" só pode ser testado de ponta a ponta se eu inserir um pedido PAGO/CANCELADO diretamente no banco antes do teste `*IT` (bypassando a API), ou se essa prova ficar restrita ao teste unitário. Vou assumir a segunda opção (teste unitário apenas) nesta spec, a menos que você quera que eu insira dados diretamente via SQL no `*IT` para also provar o 409 via HTTP.
- **Reaproveitar `IllegalStateException` para 409**: optei por não criar uma exceção nova para "pedido fechado" (o pedido não pediu isso explicitamente, ao contrário de `PedidoNaoEncontradoException`), mapeando o `IllegalStateException` já lançado pelo domínio diretamente para 409 no handler. Se preferir uma exceção nomeada (ex.: `PedidoFechadoException`), avise antes do plano.
