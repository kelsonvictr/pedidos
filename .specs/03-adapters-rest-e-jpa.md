# 03 — Adapters: persistência JPA e REST

## Contexto

Domínio (`.specs/01`) e caso de uso `CriarPedido` (`.specs/02`) já existem, puros, sem framework. Falta ligar isso à infraestrutura real: um banco Postgres (já disponível via `infra/docker-compose.yml`) e, depois, um endpoint HTTP. Esta spec cobre os dois adapters de saída/entrada que faltam, organizados em checkpoints menores para que cada prova seja validada isoladamente antes de avançar.

Este trabalho se prova em três frentes independentes:

1. **Banco disponível** — o Postgres do `infra/docker-compose.yml` responde (`pg_isready`, `SELECT 1`) — já coberto na etapa anterior, citado aqui como pré-requisito.
2. **Persistência pelo adapter** — um `PedidosJpaAdapter` grava e recupera um `Pedido` real no Postgres, através de JPA, sem vazar `jakarta.persistence.*` para `domain/` ou `aplicacao/`.
3. **HTTP** — um endpoint REST expõe `CriarPedido` para o mundo externo. **Fora do escopo desta etapa** (ver Checkpoints).

## Checkpoints

- **4A** — Postgres disponível via `infra/docker-compose.yml` (concluído).
- **4B** — adapter JPA de persistência (`PedidosJpaAdapter` + entidades + mapper + `application.yml`) (concluído).
- **4C** — endpoint REST (Controller, DTOs, tratamento de erro). **Escopo desta implementação.**

## Tarefa (checkpoint 4C)

Expor `CriarPedido` via HTTP, sem o controller conhecer JPA nem os detalhes de persistência:

- `POST /pedidos` recebe `PedidoRequest` (JSON) e devolve `PedidoResponse` (JSON) com `201 Created` quando aceito.
- `PedidoController` depende apenas de `CriarPedido` (port de entrada da aplicação) — nenhum import de `jakarta.persistence.*` ou de qualquer classe do pacote `adapter.saida.persistencia`.
- `config/CasosDeUsoConfig` é a única classe autorizada a ligar `CriarPedidoService` (aplicação) com `PedidosJpaAdapter` (adapter de saída), expondo `CriarPedido` como bean Spring.
- Erros de negócio (`ItemInvalidoException`, `PedidoSemItensException`) tornam-se `422 Unprocessable Entity` com uma mensagem, via `PedidoExceptionHandler` em `adapter/entrada/rest`.
- JSON malformado ou campos obrigatórios ausentes tornam-se `400 Bad Request`, via `@Valid` no controller — apenas formato/presença, nunca a regra de quantidade/preço positivos (que continua exclusivamente no domínio).

## Contrato HTTP

### `POST /pedidos`

**Request** (`PedidoRequest`):
```json
{
  "clienteId": "c-1",
  "itens": [
    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 }
  ]
}
```

- `clienteId`: `String`, obrigatório (`@NotBlank` — presença/formato, 400 se ausente).
- `itens`: `List`, obrigatório que o campo exista (`@NotNull` — 400 se ausente/JSON malformado); pode chegar **vazio** — lista vazia não é erro de formato, é erro de negócio (`PedidoSemItensException` → 422).
- Cada item (`ItemRequest`): `sku` (`@NotBlank`), `quantidade` (`@NotNull Integer` — presença, não positividade), `precoUnitario` (`@NotNull BigDecimal` — presença, não positividade). Valores presentes mas inválidos (zero/negativo) não são pegos aqui — chegam ao domínio e voltam como `ItemInvalidoException` → 422.

**Response 201** (`PedidoResponse`):
```json
{
  "id": "963fcc3e-f671-4dbe-86e1-a248a54de3c8",
  "clienteId": "c-1",
  "itens": [
    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90, "total": 37.80 }
  ],
  "status": "ABERTO",
  "total": 37.80
}
```

- `id`: UUID do `Pedido` salvo.
- `clienteId`: ecoa o valor recebido no request (o domínio `Pedido` não persiste cliente — ambiguidade já registrada nas specs 01/02; o controller apenas repassa o dado de entrada na resposta, sem gravá-lo).
- `itens[].total` e `total` (agregado do pedido): números decimais no JSON (não string), calculados a partir de `BigDecimal`, nunca lidos de uma coluna própria.
- `status`: `"ABERTO"` para todo pedido recém-criado (única transição possível neste momento, ver spec 01).

**Erros**:

| Situação | Status | Mecanismo |
|---|---|---|
| `quantidade` ou `precoUnitario` ≤ 0 | 422 | `ItemInvalidoException` (nova, lançada pelo domínio no lugar do `IllegalArgumentException` genérico usado até agora — resolve a ambiguidade da spec 01/02 sobre tipo de exceção) |
| Lista de itens vazia | 422 | `PedidoSemItensException` (nova, lançada por `CriarPedidoService` no lugar do `IllegalArgumentException` genérico) |
| JSON malformado (sintaxe inválida) | 400 | `HttpMessageNotReadableException` (Spring), capturada por `PedidoExceptionHandler` |
| Campo obrigatório ausente (`clienteId`, `itens` nulo, `sku`/`quantidade`/`precoUnitario` de um item nulo) | 400 | Falha de `@Valid` (`MethodArgumentNotValidException`), capturada por `PedidoExceptionHandler` |

## Tarefa (checkpoint 4B)

Implementar o adapter de saída que satisfaz o port `Pedidos` (`.specs/02`) usando Spring Data JPA contra o Postgres local, sem alterar `domain/` nem `aplicacao/`.

- Mapear `Pedido` para uma tabela `pedido` e `ItemPedido` para uma estrutura de itens associada, usando entidades JPA dedicadas (`PedidoJpaEntity`, `ItemJpaEntity`) — nunca anotar as classes de domínio.
- O ID da entidade é o mesmo UUID do domínio (`Pedido.id()`); a entidade não gera outro identificador.
- O total não é uma coluna própria — é sempre derivado a partir dos itens (na tabela, calculado a partir de `quantidade` e `precoUnitario` de cada item; no domínio, via `ItemPedido.total()`).
- Cada item persistido guarda `sku` (código do produto), `quantidade` e `precoUnitario`.
- Um mapper manual (sem MapStruct) converte `Pedido`/`ItemPedido` (domínio) ↔ `PedidoJpaEntity`/`ItemJpaEntity` (persistência), nos dois sentidos.
- `PedidosJpaAdapter implements Pedidos`, mapeia os itens all dentro de uma transação (antes de a sessão fechar) para evitar `LazyInitializationException`, com `spring.jpa.open-in-view=false`.
- `application.yml` configura a conexão com o Postgres local (host, porta, banco, usuário, senha compatíveis com `infra/docker-compose.yml`: `pedidos`/`pedidos`/`pedidos`), sem H2.

## Regras

- Nenhum import de `jakarta.persistence.*`, `org.springframework.*` ou `org.hibernate.*` em `domain/` ou `aplicacao/` — essas camadas continuam intactas.
- Entidades JPA (`PedidoJpaEntity`, `ItemJpaEntity`) vivem em um novo pacote de adapter, ex.: `br.com.pedidos.api.adapter.saida.persistencia`.
- Sem Lombok (regra já existente em `AGENTS.md`), sem MapStruct, sem H2.
- Sem gerar ID novo na entidade — `@Id` recebe o UUID vindo do domínio.
- `open-in-view=false` explícito no `application.yml`.
- Mapeamento de itens ocorre dentro do método transacional do adapter, nunca fora dele.
- Nenhum endpoint HTTP, Controller ou DTO de request/response nesta etapa (fica para 4C).
- Dependências novas no `pom.xml`: **apenas** `spring-boot-starter-data-jpa` e o driver PostgreSQL — e somente após aprovação explícita (conforme `AGENTS.md`). **Nota**: uma inspeção ao `pom.xml` mostra que essas duas dependências já estão declaradas desde o início do projeto (achado já registrado no diagnóstico da primeira etapa); portanto, ao chegar na implementação, é provável que nenhuma edição de dependência seja de fato necessária — isso será confirmado no plano antes de qualquer edição.
- Testes unitários existentes (`domain/`, `aplicacao/`) continuam passando sem banco.
- Teste de integração usa a convenção de sufixo `*IT` (execução explícita, não faz parte do `./mvnw test` padrão): `PedidosJpaAdapterIT` (novo) e o teste de contexto gerado pelo Spring Initializr, renomeado para essa mesma convenção (`ApiApplicationTests` → `ApiApplicationIT`), sem apagar nem desativar seu conteúdo.

## Definição de pronto

Cada regra abaixo corresponde a um caso de teste (unitário ou `*IT`) que deve existir e passar:

1. **Testes unitários preservados**: `ItemPedidoTest`, `PedidoTest`, `CriarPedidoServiceTest` continuam passando via `./mvnw test`, sem exigir banco.
2. **Domínio/aplicação intactos**: nenhum import de framework introduzido em `domain/` (aplicação passa a importar apenas o novo tipo de exceção `PedidoSemItensException`, que é Java puro, sem framework).
3. **Salvar grava no Postgres real**: `PedidosJpaAdapterIT` salva um pedido do cliente `c-1` com item `CAFE-500`, quantidade 2, preço `18.90`, via `PedidosJpaAdapter.salvar(...)`.
4. **Recuperar em nova transação**: o mesmo `PedidosJpaAdapterIT` recupera o pedido salvo por UUID, em uma transação separada da que salvou (prova de que os itens foram carregados antes da sessão fechar, sem depender de `open-in-view`).
5. **Itens corretos após ida e volta**: o pedido recuperado contém exatamente um item, `sku = "CAFE-500"`, `quantidade = 2`, `precoUnitario = 18.90`.
6. **Status ABERTO preservado**: o pedido recuperado tem `status = ABERTO`.
7. **Total derivado e correto**: o total do item recuperado (calculado, não lido de coluna) é `37.80`.
8. **ID preservado**: o UUID do pedido recuperado é igual ao UUID do pedido original — nenhum novo ID foi gerado pela entidade.
9. **`open-in-view` desabilitado**: `application.yml` contém `spring.jpa.open-in-view: false`.
10. **Teste de contexto adaptado, não removido**: `ApiApplicationIT` existe, sobe o contexto Spring completo contra o Postgres real, e não foi apagado nem marcado `@Disabled`.
11. **Sem H2**: nenhuma dependência H2 no `pom.xml`.
12. **POST /pedidos com item válido → 201**: `CAFE-500`, quantidade 2, `precoUnitario` 18.90 → `201`, corpo com `status: "ABERTO"`, `total: 37.80`, e uma linha correspondente gravada no Postgres.
13. **quantidade zero → 422**: request com `quantidade: 0` → `422`, mensagem de erro no corpo, e nenhuma linha nova em `pedido`/`item_pedido`.
14. **itens vazios → 422**: request com `itens: []` → `422`, mensagem de erro no corpo, e nenhuma linha nova em `pedido`/`item_pedido`.
15. **JSON malformado → 400**: corpo com sintaxe JSON inválida → `400`, e nenhuma linha nova em `pedido`/`item_pedido`.
16. **Persistência sobrevive a reinício**: após reiniciar o processo da aplicação, o mesmo UUID consultado no banco ainda retorna o pedido e seus itens.
17. **Controller não conhece JPA**: nenhum import de `jakarta.persistence.*` ou de `br.com.pedidos.api.adapter.saida.persistencia.*` em `PedidoController`.

## Ambiguidades a decidir (checkpoint 4C)

- **`clienteId` na resposta**: como `Pedido` (domínio) não guarda cliente, `PedidoResponse.clienteId` ecoa o valor do request, não um valor persistido. Se o pedido for consultado depois por outro meio (não implementado nesta etapa), esse dado não estaria disponível — assumi que isso é aceitável para este checkpoint.
- **Total agregado do pedido**: `Pedido` (domínio) não tem um método `total()` agregado (ambiguidade já registrada na spec 01). Vou calcular o total agregado somando `ItemPedido.total()` de cada item **na camada de adapter** (ao montar `PedidoResponse`), sem adicionar esse método ao domínio, para não alterar a spec 01 sem pedido explícito.

## Ambiguidades a decidir

- **Nome da tabela de itens**: a spec não define explicitamente o nome da tabela/coleção de itens (ex.: `item_pedido`). Vou assumir `item_pedido` com chave estrangeira `pedido_id`, a menos que você prefira outro nome.
- **Tipo de coluna para dinheiro**: assumirei `numeric(19,2)` para `precoUnitario` no Postgres, compatível com `BigDecimal`. Avise se quiser outra precisão/escala.
- **DDL**: não foi especificado se o schema é criado via `spring.jpa.hibernate.ddl-auto` (ex.: `validate` ou `update`) ou via script de migração dedicado. Assumirei `ddl-auto: update` apenas para esta etapa de desenvolvimento local, deixando migração formal (Flyway/Liquibase) para uma spec futura, já que nenhuma dependência nova além de Data JPA/driver foi autorizada.
- **`cliente` do pedido**: a spec 02 já registrou que `Pedido` (domínio) não tem campo `cliente`. O cenário "c-1" desta spec só é usado como identificador de contexto no teste — não haverá coluna `cliente` na tabela `pedido`, a menos que você confirme que deve existir.
