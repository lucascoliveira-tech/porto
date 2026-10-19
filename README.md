# Teste Tecnico - Microservico CNAE

Projeto Spring Boot criado para avaliacao de candidatos a vagas de Lider Tecnico e Analista.

## Stack

- Java 25
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- H2 Database
- Lombok
- Maven

## Como executar

```bash
mvn spring-boot:run
```

H2 Console:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:cnaedb
User: sa
Password:
```

## Endpoints

```http
GET /api/cnaes
GET /api/cnaes/buscar?termo=programas
GET /api/cnaes/codigo?codigo=6201-5/01
GET /api/cadastros-secundarios
GET /api/cadastros-secundarios/validar-cnae?codigoCnae=6201-5/01
POST /api/cadastros-secundarios
```

Comportamento esperado:

- `GET /api/cnaes` deve retornar todas as atividades cadastradas.
- `GET /api/cnaes/buscar?termo={texto}` deve buscar CNAEs que contenham o texto informado em qualquer parte da descricao, ignorando maiusculas e minusculas.
- `GET /api/cnaes/codigo?codigo={codigo}` deve retornar o CNAE do codigo informado.
- Codigos CNAE inexistentes devem retornar uma resposta HTTP adequada para recurso nao encontrado.
- `POST /api/cadastros-secundarios` deve criar um cadastro vinculado a um CNAE existente.
- `GET /api/cadastros-secundarios/validar-cnae?codigoCnae={codigo}` deve validar se o CNAE informado pode ser usado no cadastro.
- Cadastros secundários com CNAE inexistente nao devem ser criados.

Exemplo:

```bash
curl http://localhost:8080/api/cnaes
curl "http://localhost:8080/api/cnaes/buscar?termo=programas"
curl "http://localhost:8080/api/cnaes/codigo?codigo=6201-5/01"
curl -X POST http://localhost:8080/api/cadastros-secundarios \
  -H "Content-Type: application/json" \
  -d '{"nomeFantasia":"Tech Porto","documento":"12345678000199","codigoCnae":"6201-5/01"}'
```

## Pre-requisitos

JDK 25 e Maven 3.9+. Com SDKMAN: `sdk install java 25.0.4.1+1-tem` e `export JAVA_HOME=...` (o ambiente tinha apenas JDK 8/17/21, o que impede a compilacao com `release 25`).

## Testes

```bash
mvn test
```

17 testes: unitarios de servico (Mockito) e integracao ponta a ponta (`@SpringBootTest` + MockMvc + H2).

## Diagnostico: causa raiz e correcoes

| # | Problema (antes) | Causa raiz | Correcao |
|---|---|---|---|
| 1 | `GET /api/cnaes/buscar?termo=programas` retornava `[]` | JPQL `like lower(concat(:termo, '%'))` = busca por **prefixo**, contrariando o requisito "contenha o termo" | Derived query `findByDescricaoContainingIgnoreCaseOrderByCodigo`. Alem de contem + case-insensitive, o Spring Data **escapa `%` e `_`** do termo (evita que `termo=%` retorne tudo) |
| 2 | `GET /api/cnaes/codigo` com codigo inexistente retornava 200 com o **primeiro CNAE** | `orElseGet(() -> findAll().getFirst())`: fallback silencioso que mascara erro (e quebraria com `NoSuchElementException`/500 em tabela vazia) | `orElseThrow(CnaeNaoEncontradoException)` mapeada para **404** |
| 3 | `validar-cnae` aprovava qualquer codigo | Mesmo fallback no `buscarCnaeParaValidacao` | Metodo unico `buscarCnaeExistente` com 404 |
| 4 | `POST /api/cadastros-secundarios` com CNAE inexistente **criava** o cadastro vinculado ao CNAE errado (dado corrompido) | Mesmo fallback em `buscarCnaeParaCadastro` (duplicado de #3) | Mesmo metodo unico; nada e persistido (teste garante lista vazia) |
| 5 | Payload `{}` retornava 500; sem contrato de erro | Sem Bean Validation e sem `@RestControllerAdvice`; NPE/violacao de constraint do banco viravam 500 | `spring-boot-starter-validation`, `@Valid` no request, `@NotBlank` nos `@RequestParam`, `GlobalExceptionHandler` (RFC 9457 `ProblemDetail`) com 400 + mapa `erros` por campo e 404 |
| 6 | `CadastroSecundario.cnae` e LAZY e o mapeamento para DTO dependia do *open-in-view* (warning no boot); `findAll` gerava N+1 | Acesso a associacao lazy fora de transacao | `@Transactional(readOnly=true)` nos servicos (escrita em `cadastrar`), `spring.jpa.open-in-view=false`, `@EntityGraph("cnae")` em `findAll` |
| 7 | Build/execucao falhava no ambiente | `java.version=25` e JDK 17 ativo | JDK 25 (Temurin) instalado; documentado nos pre-requisitos |

Decisoes tecnicas:

- **Falhar explicitamente > degradar silenciosamente**: os bugs 2-4 tinham a mesma causa (fallback "inteligente"). Num servico regulatorio, retornar dado errado e pior que retornar erro.
- **404 tambem no POST** com CNAE inexistente: a regra do desafio e "CNAEs inexistentes sao barrados"; o mesmo erro de dominio em todos os endpoints mantem o contrato simples. (422 seria alternativa defensavel.)
- **Validacao de `documento`**: 11 (CPF) ou 14 (CNPJ) digitos numericos, alinhado a coluna `length = 14`. Nao valida digito verificador (fora do escopo).
- **Derived query em vez de `@Query`**: menos codigo, escape de curingas e ordenacao declarativa.
- `spring.jpa.show-sql` desligado por padrao (ruido/risco de log em producao); `ddl-auto=create` mantido por ser H2 em memoria.
- Testes de integracao usam `@DirtiesContext` por metodo para isolar o estado do H2.
- Boot 4 modularizou os starters de teste: foi necessario `spring-boot-starter-webmvc-test` (`@AutoConfigureMockMvc` agora em `org.springframework.boot.webmvc.test.autoconfigure`).

## Evidencias de execucao

```text
GET  /api/cnaes/buscar?termo=programas        -> 200 [6201-5/01, 6202-3/00]   (antes: [])
GET  /api/cnaes/codigo?codigo=6201-5/01       -> 200
GET  /api/cnaes/codigo?codigo=9999-9/99       -> 404 (antes: 200 "Cultivo de arroz")
GET  /api/cadastros-secundarios/validar-cnae?codigoCnae=9999-9/99 -> 404 (antes: 200)
POST /api/cadastros-secundarios (CNAE 9999-9/99) -> 404, nada persistido (antes: 201 vinculado ao CNAE errado)
POST /api/cadastros-secundarios (6201-5/01)   -> 201
POST /api/cadastros-secundarios {}            -> 400 {"erros":{"nomeFantasia":...,"documento":...,"codigoCnae":...}} (antes: 500)
mvn test                                      -> Tests run: 17, Failures: 0, Errors: 0 - BUILD SUCCESS
```

## Evidencias adicionais

Ambiente: Linux, Temurin 25.0.4.1, Tomcat 11.0.22, H2 em memoria, maquina de desenvolvimento (nao e benchmark formal).

### Log de inicializacao (`mvn spring-boot:run`)

```text
Starting TesteCnaeApplication using Java 25.0.4.1 with PID 26706
Starting Servlet engine: [Apache Tomcat/11.0.22]
HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:cnaedb user=SA
Initialized JPA EntityManagerFactory for persistence unit 'default'
Tomcat started on port 8080 (http) with context path '/'
Started TesteCnaeApplication in 2.166 seconds (process running for 2.34)
```

Nao ha mais o warning `spring.jpa.open-in-view is enabled by default` que aparecia antes das correcoes.

### Resultado do `mvn test`

```text
Tests run: 11, Failures: 0 -- CnaeApiIntegrationTest
Tests run: 3,  Failures: 0 -- CadastroSecundarioServiceImplTest
Tests run: 3,  Failures: 0 -- AtividadeEconomicaCnaeServiceImplTest
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  (Total time: 7.5 s)
```

### Tempo de resposta (curl, `time_total`; 1a chamada = fria, media de 50 chamadas seguintes)

| Endpoint | HTTP | Fria | Media (quente) |
|---|---|---|---|
| `GET /api/cnaes` | 200 | 4 ms | 3,4 ms |
| `GET /api/cnaes/buscar?termo=programas` | 200 | 152 ms | 3,4 ms |
| `GET /api/cnaes/codigo?codigo=6201-5/01` | 200 | 10 ms | 2,3 ms |
| `GET /api/cnaes/codigo?codigo=9999-9/99` | 404 | 11 ms | 2,5 ms |
| `GET /api/cadastros-secundarios/validar-cnae?codigoCnae=6201-5/01` | 200 | 5 ms | 2,0 ms |
| `GET /api/cadastros-secundarios` | 200 | 14 ms | 2,3 ms |
| `POST /api/cadastros-secundarios` (valido) | 201 | 61 ms | 3,0 ms |
| `POST /api/cadastros-secundarios` (`{}`) | 400 | 9 ms | 1,6 ms |

Leitura: a latencia quente fica em ~2-3 ms. O pico de 152 ms na primeira busca e o custo unico de inicializacao (parse/plan da query, JIT, serializacao Jackson), nao um problema da consulta. Os erros (404/400) custam o mesmo que um sucesso, ou seja, o tratamento centralizado nao adiciona sobrecarga relevante. Em producao seria util um *warm-up* (ou readiness probe) para evitar que o primeiro usuario pague a chamada fria.

## Possiveis melhorias de codigo e patterns

**Ja aplicados**

- **Layered architecture + DTO**: controller -> service (interface + impl) -> repository; entidades nunca expostas na API.
- **Fail-fast / Null Object evitado**: ausencia vira excecao de dominio, nao dado "padrao".
- **Centralized exception handling** (`@RestControllerAdvice`) com RFC 9457 `ProblemDetail`.
- **DRY**: um unico `buscarCnaeExistente` substitui os dois metodos duplicados.
- **Unit of Work**: fronteira transacional declarada nos servicos.

**Proximos passos recomendados**

| Melhoria | Pattern / ideia | Beneficio |
|---|---|---|
| Normalizar o codigo CNAE (aceitar `6201501` e `6201-5/01`) e validar o formato com `@Pattern` | Value Object `CodigoCnae` (record com validacao e `format()`) | Remove strings "soltas", unica fonte da regra de formato |
| Substituir `String` de erro por codigos estaveis (`CNAE_NAO_ENCONTRADO`) no `ProblemDetail` | Error Code / Problem Details extension | Clientes tratam erro sem depender de texto |
| Validar CPF/CNPJ com digito verificador e unicidade de `documento` (constraint + 409 Conflict) | Custom Bean Validation (`@Documento`) + constraint de banco | Integridade de dados |
| Mapear entidade -> DTO com MapStruct em vez de `fromEntity` estatico | Mapper | Menos codigo repetitivo, DTO desacoplado da entidade |
| Paginacao e ordenacao em `GET /api/cnaes` e `/api/cadastros-secundarios` | `Pageable` / `Page<T>` | Escala com volume real (a base oficial do CNAE tem ~1.300 subclasses) |
| Cache da consulta por codigo (dados de referencia quase imutaveis) | Cache-Aside (`@Cacheable` + Caffeine) | Menos acessos ao banco |
| Remover interfaces de servico com uma unica implementacao | KISS / YAGNI | Menos indirecao (manter so se houver segunda implementacao ou necessidade de mock de fronteira) |
| Se surgirem novas regras de elegibilidade de CNAE (ativo, vedado por produto etc.) | Strategy / Chain of Responsibility para `validarCnae` | Regras evoluem sem inflar o servico |
| `@Version` (optimistic locking) e auditoria (`@CreatedDate`) em `CadastroSecundario` | JPA Auditing | Concorrencia segura e rastreabilidade |
| Perfis `dev`/`prod`, H2 console apenas em `dev`, `ddl-auto=validate` com Flyway em `prod` | Externalized configuration | Seguranca e reprodutibilidade |
| Testes de repositorio (`@DataJpaTest`), contrato (Spring Cloud Contract/OpenAPI) e Testcontainers com Postgres | Test pyramid | Detecta diferencas H2 x banco real |
| Actuator, metricas Micrometer, logs estruturados com correlation-id, springdoc-openapi | Observabilidade | Operacao em producao |

## Pontos de atencao para producao (nao tratados aqui)

- Persistencia real (Postgres) + migrations (Flyway) no lugar de `ddl-auto=create` + `data.sql`; H2 console desabilitado fora de dev.
- Validacao de digito verificador de CPF/CNPJ, unicidade de `documento` e paginacao em `GET /api/cnaes`.
- Observabilidade (Actuator/metricas) e CI executando `mvn verify` com JDK 25.
