# AGENTS.md

Fonte principal de regras para qualquer agente (humano ou IA) que trabalhe neste repositório.

## Objetivo do serviço

API de pedidos (`br.com.pedidos.api`): serviço backend para gestão de pedidos.

## Stack

- Java 21
- Spring Boot 4.1.1

## Comandos (sempre via Maven Wrapper)

- Rodar testes: `./mvnw test`
- Build: `./mvnw clean package`
- Rodar a aplicação: `./mvnw spring-boot:run`
- Nunca usar um `mvn` global instalado na máquina — usar sempre `./mvnw` (ou `mvnw.cmd` no Windows).

## Arquitetura

Arquitetura hexagonal (ports and adapters):

- **Domínio** e **aplicação** (casos de uso) não podem depender de Spring, JPA ou qualquer framework de infraestrutura. São Java puro, testável sem contexto de Spring.
- Frameworks e infraestrutura (Spring, JPA, Web, banco) vivem apenas nos adapters, nunca no núcleo do domínio.

## Regras de código

- Valores monetários sempre em `BigDecimal`. Nunca `float`/`double` para dinheiro.
- Proibido usar Lombok.
- Nenhuma dependência nova no `pom.xml` sem pedir e obter aprovação explícita antes.

## Fluxo de trabalho obrigatório

1. Ler a spec indicada (quando houver, em `.specs/`).
2. Planejar a mudança e apresentar o plano.
3. Aguardar OK explícito antes de editar qualquer arquivo.
4. Implementar.
5. Testar (`./mvnw test`).
6. Mostrar o diff das mudanças.
