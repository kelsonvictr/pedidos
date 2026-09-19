# 02 — Caso de uso: Criar Pedido

## Contexto

Primeiro caso de uso da camada de aplicação, construído sobre o domínio de `Pedido` já implementado (`.specs/01-dominio-pedido.md`). Ainda sem infraestrutura: sem Spring, sem JPA, sem HTTP, sem adapters. Este caso de uso conecta uma requisição de "criar pedido" ao domínio e a uma porta de persistência abstrata.

## Tarefa

Implementar o caso de uso de criação de pedido como arquitetura hexagonal (ports and adapters), na camada de aplicação:

- Um port de entrada `CriarPedido` descreve a operação disponível para quem estiver fora da aplicação (controller, teste, etc.): criar um pedido a partir de um cliente e uma lista de itens.
- Uma implementação `CriarPedidoService` realiza a operação: valida a lista de itens, monta o `Pedido` usando o domínio, salva através do port de saída e devolve o pedido salvo.
- Um port de saída `Pedidos` descreve o que a aplicação precisa para persistir um pedido, sem dizer como (nenhuma menção a JPA, SQL, tabela).
- Lista de itens vazia é recusada — não é permitido criar pedido sem nenhum item.
- A aplicação devolve o pedido **salvo** (o valor de retorno de `Pedidos`, não uma cópia local construída antes de salvar), para não presumir que "salvar" é uma operação identidade.

## Regras

- Pacote: `br.com.pedidos.api.aplicacao.pedido` (ou equivalente sob `aplicacao/`).
- `CriarPedido` é uma interface (port de entrada) com um método, por exemplo `Pedido criar(String cliente, List<ItemPedido> itens)`.
- `CriarPedidoService implements CriarPedido`.
- `Pedidos` é uma interface (port de saída) com um método, por exemplo `Pedido salvar(Pedido pedido)`.
- `CriarPedidoService` depende apenas de `Pedidos` (port de saída) e do domínio (`Pedido`, `ItemPedido`), recebido por construtor.
- Nenhum import de `org.springframework.*`, `jakarta.persistence.*`, `jakarta.servlet.*` ou qualquer pacote de adapter em `aplicacao/`.
- Lista de itens nula ou vazia: `CriarPedidoService` recusa antes de montar o domínio (`IllegalArgumentException`, consistente com o padrão de exceções já usado no domínio — ver ambiguidade já registrada na spec 01).
- Construção do pedido reaproveita o domínio existente: `Pedido.novo()` + `adicionarItem(...)` para cada item da lista, nesta ordem.
- Nenhuma dependência nova no `pom.xml` sem pedir e obter aprovação explícita antes (conforme `AGENTS.md`).

## Definição de pronto

Cada regra abaixo corresponde a um caso de teste que deve existir e passar, usando uma implementação em memória de `Pedidos` definida dentro do teste (sem framework de mock, sem Spring, sem banco):

1. **Criação com itens válidos**: `CriarPedidoService.criar("c-1", List.of(CAFE_500))` devolve um `Pedido` com status ABERTO e contendo o item informado.
2. **Pedido criado é salvo através de `Pedidos`**: a implementação em memória de `Pedidos` recebeu a chamada de salvar (o pedido está presente no repositório em memória após a chamada).
3. **Retorno é o pedido salvo**: o `Pedido` devolvido por `criar(...)` é o mesmo (ou equivalente) ao que `Pedidos.salvar(...)` devolveu — não uma versão à parte não persistida.
4. **Lista de itens vazia é recusada**: `criar("c-1", List.of())` lança `IllegalArgumentException`; nada é salvo em `Pedidos`.
5. **Lista de itens nula é recusada**: `criar("c-1", null)` lança `IllegalArgumentException`; nada é salvo em `Pedidos`.
6. **Item inválido continua recusado pelo domínio**: `criar("c-1", List.of(itemComQuantidadeZero))` propaga a exceção de validação já existente em `ItemPedido`/`Pedido` (regra herdada da spec 01, não duplicada aqui).
7. **Cenário de referência da aula**: `criar("c-1", List.of(CAFE-500 qtd 2 preço 18.90))` resulta em pedido salvo com um item cujo total é `37.80`.

## Ambiguidades a decidir

- **Onde fica o `cliente`**: o caso de uso recebe `cliente`, mas o domínio `Pedido` (spec 01) não tem campo de cliente. Duas opções: (a) `CriarPedido` recebe `cliente` apenas como parâmetro de entrada, sem persistir essa informação em `Pedido` nesta etapa (o que o torna efetivamente não utilizado após a validação); ou (b) estender `Pedido` com um campo `cliente` agora. Assumi a opção (a) — o parâmetro é recebido e validado (não nulo/vazio, ver abaixo) mas não é gravado no domínio, para não alterar a spec 01 sem seu pedido explícito. Preciso que confirme se isso é aceitável ou se devo propor uma mudança na spec 01.
- **Validação do `cliente`**: não foi pedido explicitamente que `cliente` vazio/nulo seja recusado. Não vou validar isso nesta spec, a menos que você confirme que deve.
- **Nome do método de `Pedidos`**: assumi `salvar(Pedido)`; se preferir outro nome (`incluir`, `save`), avise antes da implementação.
