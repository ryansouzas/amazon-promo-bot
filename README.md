# Amazon Promo Bot

Aplicação backend em **Java 21 e Spring Boot 4.1.1** que valida produtos, sincroniza o cadastro e o histórico de preços no banco, identifica ofertas com desconto mínimo de 15%, evita publicações repetidas e envia as ofertas a um chat do Telegram.

O processamento é disparado por uma requisição HTTP. A aplicação obtém os produtos da fonte, valida os dados, sincroniza o cadastro, avalia o desconto, verifica a deduplicação, envia a mensagem ao Telegram e, por fim, registra a publicação.

> **Estágio atual:** protótipo. A fonte de produtos ainda é fictícia (`FakeProductSource`) e não consulta ofertas reais da Amazon. Os links usados na demonstração pertencem a `example.com`.

## Sumário

- [Objetivo](#objetivo)
- [Funcionalidades implementadas](#funcionalidades-implementadas)
- [Tecnologias](#tecnologias)
- [Arquitetura e responsabilidades](#arquitetura-e-responsabilidades)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Modelo de dados](#modelo-de-dados)
- [Regras de negócio](#regras-de-negócio)
- [Demonstração do MVP](#demonstração-do-mvp)
- [Configuração e execução](#configuração-e-execução)
- [Endpoints disponíveis](#endpoints-disponíveis)
- [Integração com o Telegram](#integração-com-o-telegram)
- [Tratamento de falhas e logs](#tratamento-de-falhas-e-logs)
- [Testes](#testes)
- [Limitações atuais](#limitações-atuais)
- [Possíveis evoluções](#possíveis-evoluções)
- [Autor](#autor)

## Objetivo

Construir uma base para divulgação de promoções no Telegram, com responsabilidades separadas, histórico de preços persistido e regras de negócio testáveis.

Nesta etapa, a aplicação demonstra o processamento de ofertas com dados controlados. A interface `ProductSource` permite introduzir outra implementação de busca de produtos em uma evolução do projeto.

## Funcionalidades implementadas

| Funcionalidade | Comportamento atual |
| --- | --- |
| Acionamento por HTTP | Executa o processamento por meio de `POST /promotions/test`, de forma síncrona. |
| Cadastro de produtos | Expõe `GET`, `POST` e `PUT` em `/products` para consultar e manter produtos. |
| Fonte de produtos | Carrega uma lista fixa de três produtos por meio de `FakeProductSource`. |
| Validação de produtos | Verifica identificação, nome, preços e URL antes de sincronizar ou avaliar a oferta. |
| Sincronização do cadastro | `ProductService.syncProduct` cria o produto se ele não existir e atualiza seus dados se existir. |
| Histórico de preços | Grava uma linha em `price_history` na criação do produto e sempre que o preço original ou o preço atual muda. |
| Cálculo de desconto | Calcula o percentual com `BigDecimal`, duas casas decimais e `RoundingMode.HALF_UP`. |
| Filtro de ofertas | Seleciona produtos com desconto calculado maior ou igual a 15%. |
| Deduplicação | Compara o preço atual com a última publicação do produto no mesmo chat e decide se publica novamente. |
| Registro de publicações | Grava uma `Promotion` depois do envio bem-sucedido. |
| Formatação de mensagens | Inclui nome, preço original, preço atual, desconto e link do produto. |
| Publicação no Telegram | Envia uma mensagem de texto por oferta aprovada, utilizando `RestClient`. |
| Tratamento de produto inválido | Registra um aviso, ignora o produto e continua o processamento. |
| Tratamento de falha de integração | Converte falhas do cliente HTTP em exceção própria, registra o produto e continua para os próximos. |
| Persistência | JPA/Hibernate com H2 em memória no perfil `test` e PostgreSQL no perfil `dev`. |
| Configuração externa | Obtém o token do bot e o chat de destino por variáveis de ambiente (perfil `test`). |
| Logs | Registra envios concluídos, produtos inválidos, falhas de publicação e falhas do cliente HTTP. |
| Testes automatizados | Cobrem validação, cálculo, deduplicação, sincronização, publicação, cliente HTTP e carregamento do contexto. |

## Tecnologias

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 21 | Linguagem e versão definida no build. |
| Spring Boot 4.1.1 | Inicialização, configuração e gerenciamento de dependências. |
| Spring Web MVC | Exposição dos endpoints REST. |
| Spring Data JPA e Hibernate | Persistência de `Product`, `PriceHistory` e `Promotion`. |
| PostgreSQL | Banco de dados do perfil `dev`. |
| H2 | Banco em memória do perfil `test`, com console web. |
| Spring RestClient | Requisições HTTP para o Telegram. |
| Maven e Maven Wrapper | Gerenciamento de dependências, testes e empacotamento. |
| Lombok | Geração de construtores e suporte a logs. |
| SLF4J | Registro de eventos da aplicação. |
| JUnit Jupiter | Testes e testes parametrizados. |
| Mockito | Simulação de dependências e verificação de interações. |
| MockRestServiceServer | Simulação das respostas HTTP nos testes de `TelegramService`. |

As versões de Java e Spring Boot acima correspondem ao `pom.xml`. A versão Maven do artefato é `0.0.1-SNAPSHOT`.

## Arquitetura e responsabilidades

A aplicação é um monólito organizado em pacotes por responsabilidade. Todos os componentes executam no mesmo processo.

Os controllers delegam para os serviços. `PromotionService` coordena o fluxo de publicação: consulta `ProductSource`, valida cada produto, sincroniza o cadastro por `ProductService`, avalia o desconto, aplica a deduplicação, publica por `PromotionPublisher` e grava a `Promotion`. `ProductService` cuida do cadastro manual (`/products`) e da sincronização usada pelo fluxo de promoções.

| Componente | Responsabilidade |
| --- | --- |
| `PromotionController` | Disponibilizar o endpoint `POST /promotions/test`, que inicia o processamento. |
| `ProductController` | Disponibilizar os endpoints de consulta, criação e atualização de produtos. |
| `ProductSource` | Definir o contrato de obtenção da lista de produtos. |
| `FakeProductSource` | Fornecer os produtos fictícios da demonstração. |
| `ProductValidator` | Garantir que o produto atenda às regras de validade. |
| `ProductService` | Criar, atualizar e sincronizar produtos e registrar o histórico de preços. |
| `ProductMapper` | Converter entre `ProductDTO` e a entidade `Product`. |
| `PromotionEvaluator` | Calcular o desconto e verificar o mínimo exigido. |
| `PromotionService` | Coordenar o fluxo de publicação, aplicar a deduplicação e registrar as publicações. |
| `PromotionPublisher` | Formatar a mensagem de oferta e solicitar seu envio. |
| `TelegramService` | Montar e executar a requisição HTTP para o Telegram. |
| `ResourceExceptionHandler` | Converter exceções de cadastro em respostas HTTP com corpo `StandardError`. |
| `ProductRepository`, `PriceHistoryRepository`, `PromotionRepository` | Acesso ao banco por meio de Spring Data JPA. |
| `TelegramProperties` | Representar as propriedades `telegram.bot.token` e `telegram.bot.chat-id`. |
| `TelegramConfig` | Habilitar o registro de `TelegramProperties`. |
| `TelegramMessageRequest` | Representar o JSON enviado ao Telegram. |
| `InvalidProductException` | Sinalizar violação das regras de validação. |
| `ProductExistsException` | Sinalizar tentativa de cadastrar um `externalId` já existente. |
| `ResourceNotFoundException` | Sinalizar atualização de produto inexistente. |
| `TelegramIntegrationException` | Representar uma falha na comunicação com o Telegram. |

## Estrutura do projeto

Os pacotes Java ficam sob `com.ryansouzas.amazon_promo_bot`.

| Caminho | Conteúdo |
| --- | --- |
| `src/main/java/.../AmazonPromoBotApplication.java` | Ponto de entrada da aplicação. |
| `src/main/java/.../config/` | `TelegramConfig` e `TelegramProperties`. |
| `src/main/java/.../controller/` | `PromotionController` e `ProductController`. |
| `src/main/java/.../controller/exceptions/` | `ResourceExceptionHandler`, `StandardError`, `ValidationError` e `FieldMessage`. |
| `src/main/java/.../dto/` | `ProductDTO` e `TelegramMessageRequest`. |
| `src/main/java/.../entities/` | Entidades JPA `Product`, `PriceHistory` e `Promotion`. |
| `src/main/java/.../exceptions/` | `TelegramIntegrationException`. |
| `src/main/java/.../mapper/` | `ProductMapper`. |
| `src/main/java/.../repositories/` | Repositórios Spring Data JPA. |
| `src/main/java/.../service/` | Fluxo de publicação, cadastro, avaliação, formatação e envio. |
| `src/main/java/.../service/exceptions/` | `InvalidProductException`, `ProductExistsException` e `ResourceNotFoundException`. |
| `src/main/java/.../source/` | Contrato e implementação da fonte de produtos. |
| `src/main/java/.../validation/` | `ProductValidator`. |
| `src/main/resources/application.properties` | Nome da aplicação e perfil ativo (padrão `test`). |
| `src/main/resources/application-dev.properties` | Conexão com PostgreSQL e configuração do Hibernate. |
| `src/main/resources/application-test.properties` | Conexão com H2, console H2 e propriedades do Telegram. |
| `src/test/java/.../service/` | Testes dos serviços. |
| `src/test/java/.../validation/` | Teste do validador. |
| `src/test/java/.../AmazonPromoBotApplicationTests.java` | Teste de carregamento do contexto Spring. |
| `pom.xml` | Dependências e configuração do build. |
| `mvnw` e `mvnw.cmd` | Maven Wrapper para Linux/macOS e Windows. |

## Modelo de dados

### Produto recebido e manipulado pela API

O contrato de entrada e saída dos endpoints de produto, e o objeto que a fonte devolve ao fluxo, é o record `ProductDTO`:

```java
public record ProductDTO(
        String externalId,
        String name,
        BigDecimal originalPrice,
        BigDecimal currentPrice,
        String url
) {
}
```

| Campo | Descrição |
| --- | --- |
| `externalId` | Identificador do produto na fonte de origem. |
| `name` | Nome apresentado na oferta. |
| `originalPrice` | Preço de referência utilizado no cálculo. |
| `currentPrice` | Preço atual do produto. |
| `url` | Endereço incluído na mensagem. |

Os preços utilizam `BigDecimal` para os cálculos decimais.

### Entidades persistidas

| Entidade | Tabela | Campos principais | Observações |
| --- | --- | --- | --- |
| `Product` | `products` | `id` (UUID), `externalId`, `name`, `originalPrice`, `currentPrice`, `url`, `createdAt`, `updatedAt` | `external_id` é obrigatório e único. Preços com precisão 12 e escala 2. |
| `PriceHistory` | `price_history` | `id`, `originalPrice`, `currentPrice`, `recordedAt`, `product` | Relação obrigatória com `Product` (`product_id`). Recebe um registro na criação e a cada alteração de preço. |
| `Promotion` | `promotions` | `id`, `originalPrice`, `publishedPrice`, `discountPercentage`, `telegramChatId`, `publishedAt`, `product` | Gravada após o envio sem exceção. Percentual com precisão 5 e escala 2. Relação obrigatória com `Product` (`product_id`). |

## Regras de negócio

### Validação do produto

Antes de qualquer sincronização ou avaliação, `ProductValidator` aplica as seguintes regras:

| Dado | Regra |
| --- | --- |
| Produto | Não pode ser nulo. |
| Identificador externo | Não pode ser nulo, vazio ou composto apenas por espaços. |
| Nome | Não pode ser nulo, vazio ou composto apenas por espaços. |
| Preço original e preço atual | Não podem ser nulos. |
| Valores dos preços | Devem ser estritamente maiores que zero. |
| Relação entre preços | O preço atual deve ser menor ou igual ao preço original. |
| URL | Deve estar preenchida, ser aceita por `URI.create`, utilizar HTTPS e possuir host. |

Uma violação gera `InvalidProductException`. No fluxo de publicação, o serviço registra um aviso e passa para o próximo produto. Nos endpoints de cadastro, a exceção é convertida em resposta `409`.

Preços iguais são válidos, mas resultam em desconto de 0% e não geram publicação. A validação de URL não consulta o endereço nem exige domínio da Amazon.

### Sincronização do cadastro

Depois de validado, cada produto passa por `ProductService.syncProduct`, que procura o produto pelo `externalId`:

- **Produto inexistente:** cria o registro em `products` e grava a primeira linha em `price_history`.
- **Produto existente:** atualiza nome, preço original, preço atual e URL. Grava uma nova linha em `price_history` somente se o preço original ou o preço atual mudou.

A sincronização ocorre para todo produto válido, inclusive para os que não atingem o desconto mínimo.

### Cálculo e seleção da promoção

O desconto é calculado pela expressão:

```text
desconto (%) = ((preço original - preço atual) × 100) / preço original
```

O resultado possui duas casas decimais, com arredondamento `HALF_UP`.

| Preço original | Preço atual | Desconto calculado | Publicação |
| --- | --- | --- | --- |
| R$ 200.00 | R$ 100.00 | 50.00% | Sim |
| R$ 200.00 | R$ 170.00 | 15.00% | Sim |
| R$ 300.00 | R$ 270.00 | 10.00% | Não |
| R$ 200.00 | R$ 200.00 | 0.00% | Não |

O limite de **15% é inclusivo** e está definido na constante `MIN_DISCOUNT`, em `PromotionEvaluator`. Atualmente, sua alteração exige modificar o código.

A comparação utiliza o percentual já arredondado. Portanto, um valor matemático ligeiramente abaixo de 15% pode ser aceito se o arredondamento resultar em `15.00`.

### Deduplicação

Para produtos que atingem o desconto mínimo, `PromotionService.shouldPublish` decide se a oferta será enviada. A decisão considera a última `Promotion` do mesmo produto gravada para o mesmo chat (`TELEGRAM_CHAT_ID`):

| Situação | Decisão |
| --- | --- |
| Nunca publicado para o chat | Publica. |
| Preço atual menor que o último preço publicado | Publica. |
| Preço atual maior que o último preço publicado | Não publica. |
| Preço igual ao último publicado | Publica somente se já passaram 24 horas desde a última publicação. |

### Ordem do fluxo de publicação

`PromotionService.publishPromotions` processa os produtos nesta ordem:

1. Obtém a lista de `ProductSource.findProducts()`.
2. Valida o produto com `ProductValidator`.
3. Sincroniza o cadastro com `ProductService.syncProduct`.
4. Avalia se o produto é promoção (`PromotionEvaluator.isPromotion`). Se não for, passa para o próximo.
5. Calcula o desconto e busca a última publicação do produto no chat.
6. Verifica a deduplicação (`shouldPublish`). Se não publicar, passa para o próximo.
7. Publica a mensagem por `PromotionPublisher.publish`, que delega ao `TelegramService`.
8. Grava a `Promotion` (`registerPublication`).

A `Promotion` é gravada somente depois do envio. Se a gravação falhar após o envio, a mensagem já foi entregue e uma chamada futura pode publicá-la de novo (ver [Limitações atuais](#limitações-atuais)).

## Demonstração do MVP

A implementação `FakeProductSource` retorna os seguintes produtos:

| Identificador | Produto | Preço original | Preço atual | Desconto | Resultado |
| --- | --- | --- | --- | --- | --- |
| `AMZ-001` | Mouse Gamer | R$ 200.00 | R$ 100.00 | 50.00% | Enviar |
| `AMZ-002` | Teclado Mecânico | R$ 300.00 | R$ 270.00 | 10.00% | Cadastrar, sem enviar |
| `AMZ-003` | Monitor Gamer | R$ 1500.00 | R$ 900.00 | 40.00% | Enviar |

Com a integração funcionando e o banco vazio, a primeira chamada ao endpoint envia **duas mensagens**, uma para o mouse e outra para o monitor. Os três produtos são cadastrados, mas o teclado não atinge o desconto mínimo.

Se o endpoint for chamado de novo com os mesmos preços, dentro de 24 horas, **nenhuma mensagem é enviada**: os preços são iguais aos publicados e o intervalo mínimo ainda não passou.

No perfil `test`, o banco é em memória. Reiniciar a aplicação apaga os dados e a próxima chamada volta a publicar as ofertas.

Exemplo do texto publicado para o mouse:

```text
🔥 PROMOÇÃO ENCONTRADA!

Produto: Mouse Gamer
Preço original: R$ 200.00
Preço atual: R$ 100.00
Desconto: 50.00%

Confira: https://example.com/mouse
```

A formatação atual utiliza a representação textual dos valores de `BigDecimal`, sem aplicar localização monetária brasileira.

## Configuração e execução

### Pré-requisitos

- JDK 21.
- Acesso à internet para resolver as dependências do build e, durante o uso, acessar o Telegram.
- Um bot no Telegram e seu token, e um chat de destino em que o bot tenha permissão para enviar mensagens. Necessários apenas para publicar ofertas.
- PostgreSQL, somente se for usar o perfil `dev`.

O Maven Wrapper está incluído no projeto. Não é necessário instalar Maven separadamente para utilizar os comandos abaixo.

### Perfis

A propriedade `spring.profiles.active=${APP_PROFILE:test}`, em `application.properties`, define o perfil ativo.

| Perfil | Banco de dados | Como ativar | Observações |
| --- | --- | --- | --- |
| `test` (padrão) | H2 em memória (`jdbc:h2:mem:testdb`) | Não é preciso definir nada, ou `APP_PROFILE=test`. | Console H2 em `/h2-console`. Os dados são perdidos ao reiniciar. Lê as variáveis do Telegram. |
| `dev` | PostgreSQL | `APP_PROFILE=dev` | `spring.jpa.hibernate.ddl-auto=update`. Não define as propriedades do Telegram (ver [Limitações atuais](#limitações-atuais)). |

### Variáveis de ambiente

| Variável | Perfil | Valor padrão | Uso |
| --- | --- | --- | --- |
| `APP_PROFILE` | Todos | `test` | Perfil ativo. |
| `TELEGRAM_BOT_TOKEN` | `test` | Sem padrão | Token do bot, usado em `telegram.bot.token`. |
| `TELEGRAM_CHAT_ID` | `test` | Sem padrão | Chat de destino, usado em `telegram.bot.chat-id`. |
| `DB_URL` | `dev` | `jdbc:postgresql://localhost:5432/amazon_promo_bot` | URL JDBC do PostgreSQL. |
| `DB_USERNAME` | `dev` | `postgres` | Usuário do banco. |
| `DB_PASSWORD` | `dev` | Sem padrão (obrigatória) | Senha do banco. |

As variáveis do Telegram são referenciadas somente em `application-test.properties`. Já as de banco são referenciadas somente em `application-dev.properties`. `application.properties` define apenas o nome da aplicação e o perfil ativo.

No PowerShell, na pasta raiz do projeto, para o perfil `test`:

```powershell
$env:TELEGRAM_BOT_TOKEN = "SEU_TOKEN_DO_BOT"
$env:TELEGRAM_CHAT_ID = "SEU_CHAT_ID"
```

No Linux/macOS, para o perfil `test`:

```bash
export TELEGRAM_BOT_TOKEN="SEU_TOKEN_DO_BOT"
export TELEGRAM_CHAT_ID="SEU_CHAT_ID"
```

Para o perfil `dev`, no PowerShell:

```powershell
$env:APP_PROFILE = "dev"
$env:DB_PASSWORD = "SUA_SENHA"
```

Para o perfil `dev`, no Linux/macOS:

```bash
export APP_PROFILE="dev"
export DB_PASSWORD="SUA_SENHA"
```

Esses comandos definem as variáveis para a sessão atual do terminal. Na IDE, configure as mesmas variáveis na configuração de execução.

O projeto não inclui carregamento automático de arquivo `.env`. Mantenha os valores reais fora do código e do controle de versão.

### Iniciar a aplicação

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Com a configuração padrão, o servidor fica disponível em `http://localhost:8080`.

A inicialização do servidor não publica ofertas: o processamento começa quando o endpoint `POST /promotions/test` é chamado.

### Gerar e executar o JAR

Windows PowerShell:

```powershell
.\mvnw.cmd clean package
java -jar target/amazon-promo-bot-0.0.1-SNAPSHOT.jar
```

Linux/macOS:

```bash
./mvnw clean package
java -jar target/amazon-promo-bot-0.0.1-SNAPSHOT.jar
```

O comando `package` também executa os testes.

## Endpoints disponíveis

Não há prefixo `/api`. Todos os caminhos são relativos a `http://localhost:8080`.

### Executar o processamento de promoções

```http
POST /promotions/test
```

| Item | Comportamento |
| --- | --- |
| Corpo da requisição | Não recebe corpo. |
| Origem dos produtos | Lista retornada por `ProductSource`, atualmente `FakeProductSource`. |
| Execução | Síncrona: a resposta é produzida após o processamento. |
| Resposta quando o fluxo termina normalmente | `204 No Content`, sem corpo. |
| Autenticação | Não implementada no MVP. |

PowerShell:

```powershell
Invoke-WebRequest -Method POST -Uri "http://localhost:8080/promotions/test"
```

Linux/macOS:

```bash
curl -i -X POST http://localhost:8080/promotions/test
```

No Postman, selecione `POST`, informe essa URL e envie a requisição sem body.

> O retorno `204` indica que o método concluiu o processamento, mas não comprova que todas as mensagens foram entregues. Falhas de validação e de integração tratadas por produto são registradas nos logs e não alteram essa resposta. Erros não tratados, como falha de banco ou da fonte, interrompem o processamento e resultam em erro HTTP.

### Cadastro de produtos

Os três endpoints usam o prefixo `/products` e o corpo no formato `ProductDTO`:

```json
{
  "externalId": "AMZ-100",
  "name": "Headset Gamer",
  "originalPrice": 300.00,
  "currentPrice": 200.00,
  "url": "https://example.com/headset"
}
```

#### Consultar produto

```http
GET /products/{externalId}
```

| Status | Quando ocorre |
| --- | --- |
| `200 OK` | O produto existe. O corpo traz o `ProductDTO`. |
| `404 Not Found` | Não há produto com o `externalId` informado. Sem corpo. |

#### Criar produto

```http
POST /products
```

| Status | Quando ocorre |
| --- | --- |
| `201 Created` | Produto criado. O cabeçalho `Location` aponta para `/products/{externalId}` e o corpo traz o `ProductDTO`. Um registro inicial é gravado em `price_history`. |
| `409 Conflict` | Já existe produto com o mesmo `externalId` (`Product Already Exists`) ou o corpo é inválido (`Invalid product`). |

#### Atualizar produto

```http
PUT /products/{externalId}
```

| Status | Quando ocorre |
| --- | --- |
| `200 OK` | Produto atualizado. O corpo traz o `ProductDTO` atualizado. Um registro é gravado em `price_history` somente se algum preço mudou. |
| `400 Bad Request` | O `externalId` do caminho diverge do `externalId` do corpo. Sem corpo. |
| `404 Not Found` | Não há produto com o `externalId` informado. |
| `409 Conflict` | O corpo é inválido. A validação ocorre antes da busca, então um corpo inválido retorna `409` mesmo quando o produto não existe. |

O `PUT` não dispara publicação e não grava `Promotion`.

#### Exemplos de requisição

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/products" -ContentType "application/json" -Body '{"externalId":"AMZ-100","name":"Headset Gamer","originalPrice":300.00,"currentPrice":200.00,"url":"https://example.com/headset"}'
```

Linux/macOS:

```bash
curl -i -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"externalId":"AMZ-100","name":"Headset Gamer","originalPrice":300.00,"currentPrice":200.00,"url":"https://example.com/headset"}'
```

Consulta:

```bash
curl -i http://localhost:8080/products/AMZ-100
```

### Formato dos erros

As respostas de erro dos endpoints de produto, tratadas por `ResourceExceptionHandler`, usam o corpo `StandardError`:

```json
{
  "timestamp": "2026-11-07T12:00:00Z",
  "status": 409,
  "error": "Invalid product",
  "message": "Product URL must be valid HTTPS URL",
  "path": "/products"
}
```

Os valores de `error` possíveis são `Resource not found` (404), `Product Already Exists` (409) e `Invalid product` (409).

## Integração com o Telegram

O `TelegramService` utiliza `RestClient.Builder` injetado para criar um cliente com a URL base `https://api.telegram.org`.

Cada mensagem gera uma requisição:

```http
POST https://api.telegram.org/bot{token}/sendMessage
```

O DTO `TelegramMessageRequest` é serializado no formato:

```json
{
  "chat_id": "SEU_CHAT_ID",
  "text": "Mensagem da promoção"
}
```

A anotação `@JsonProperty("chat_id")` define o nome do campo esperado no JSON.

O cliente processa o status HTTP e descarta o corpo da resposta por meio de `toBodilessEntity()`. Não há interpretação do JSON de retorno, armazenamento do identificador da mensagem ou confirmação posterior de entrega.

## Tratamento de falhas e logs

| Situação | Comportamento |
| --- | --- |
| Produto inválido no fluxo de publicação | `ProductValidator` lança `InvalidProductException`; `PromotionService` registra um aviso e continua. |
| Desconto insuficiente | O produto é sincronizado, mas ignorado sem tentativa de envio. |
| Publicação bloqueada pela deduplicação | O produto é ignorado sem tentativa de envio. |
| Falha do cliente HTTP | `TelegramService` captura `RestClientException`, registra o tipo da falha e lança `TelegramIntegrationException`, preservando a causa. |
| Falha ao publicar uma oferta | `PromotionService` captura `TelegramIntegrationException`, registra o `externalId` do produto e continua. Nenhuma `Promotion` é gravada, então uma chamada futura tenta de novo. |
| Lista vazia | O fluxo termina sem validar produtos ou publicar mensagens. |
| Falha ao consultar a fonte, erro de banco ou exceção inesperada | Não há tratamento específico no fluxo. A exceção interrompe o processamento do lote. |
| Erro de cadastro nos endpoints `/products` | `ResourceExceptionHandler` converte `ResourceNotFoundException` em `404` e `ProductExistsException` e `InvalidProductException` em `409`. |

Há logs em nível `INFO` para envios concluídos, `WARN` para produtos inválidos e falhas de publicação, e `ERROR` para falhas do cliente HTTP. Os perfis `dev` e `test` ativam `spring.jpa.show-sql`, portanto as instruções SQL também aparecem no log.

Não há nova tentativa automática de envio nem um resumo de sucesso ou falha no corpo da resposta do endpoint.

## Testes

A suíte separa as regras de negócio das dependências externas. Mockito simula colaboradores dos serviços; `MockRestServiceServer` intercepta as requisições do cliente HTTP, evitando envios reais nos testes de `TelegramService`.

| Classe | Foco |
| --- | --- |
| `ProductValidatorTest` | Regras de validação de produto: identificador, nome, preços, relação entre preços e URL. |
| `ProductServiceTest` | Consulta, criação e atualização de produtos, sincronização e registro de histórico de preços. |
| `PromotionEvaluatorTest` | Cálculo do desconto, limite inclusivo de 15%, valores próximos ao limite e arredondamento. |
| `PromotionServiceTest` | Fluxo de publicação, produtos inválidos e sem desconto, lista vazia e regras de deduplicação por preço e intervalo de 24 horas. |
| `PromotionPublisherTest` | Solicitação de envio, texto completo da mensagem e propagação de exceção do Telegram. |
| `TelegramServiceTest` | Requisição POST, `chat_id` e texto enviados, e conversão de resposta HTTP 400 em exceção de integração. |
| `AmazonPromoBotApplicationTests` | Carregamento do contexto Spring com `@SpringBootTest`. |

Não há testes dedicados para `PromotionController` nem para `ProductController`. A suíte também não contém teste específico para a continuidade do lote após `TelegramIntegrationException` em `PromotionServiceTest`, embora o código preveja esse comportamento. O projeto não configura JaCoCo ou outra ferramenta de cobertura.

### Executar a suíte

Para testar sem usar credenciais reais, podem ser usados valores fictícios. O teste de contexto não aciona a publicação e os testes do cliente HTTP simulam a comunicação.

Windows PowerShell:

```powershell
$env:TELEGRAM_BOT_TOKEN = "test-token"
$env:TELEGRAM_CHAT_ID = "-123"
.\mvnw.cmd test
```

Linux/macOS:

```bash
TELEGRAM_BOT_TOKEN="test-token" TELEGRAM_CHAT_ID="-123" ./mvnw test
```

### Executar uma classe específica

Windows PowerShell:

```powershell
.\mvnw.cmd "-Dtest=PromotionServiceTest" test
```

Linux/macOS:

```bash
./mvnw -Dtest=PromotionServiceTest test
```

A tabela acima descreve os testes presentes no código. Ela não representa um relatório de execução nem uma medição de cobertura.

## Limitações atuais

- **Fonte fictícia:** `FakeProductSource` retorna três produtos fixos. Não há integração com ofertas reais da Amazon.
- **Sem autenticação:** `POST /promotions/test`, `GET /products/{externalId}`, `POST /products` e `PUT /products/{externalId}` não exigem credenciais. No perfil `test`, o console H2 (`/h2-console`) também está habilitado.
- **Sem agendamento:** o processamento ocorre somente quando o endpoint é chamado.
- **Erro de banco ou da fonte interrompe o lote:** exceções não tratadas no fluxo, como falha de consulta à fonte ou de acesso ao banco, interrompem o processamento dos produtos restantes.
- **Envio antes de gravar a `Promotion`:** a mensagem é enviada antes da gravação do registro, e o lote não é executado em uma transação única. Se o `save` falhar depois do envio, a próxima chamada pode publicar a mesma oferta de novo.
- **Respostas 429 do Telegram não são tratadas:** não há leitura de `retry_after` nem nova tentativa específica para limitação de taxa. Qualquer falha HTTP é tratada como falha genérica de envio.
- **`InvalidProductException` retorna 409:** violações de validação nos endpoints de produto respondem `409 Conflict`, e não `400` ou `422`.
- **Valores em formato `BigDecimal`:** as mensagens exibem valores como `R$ 200.00`, com ponto decimal e sem formatação monetária brasileira (`R$ 200,00`).
- **Propriedades do Telegram ausentes no perfil `dev`:** `application.properties` e `application-dev.properties` não definem `telegram.bot.token` nem `telegram.bot.chat-id`. Ao usar o perfil `dev`, o `TelegramService` não recebe token nem chat de destino pela configuração. Para publicar nesse perfil, é preciso acrescentar essas propriedades.
- **Esquema gerenciado pelo Hibernate:** o perfil `dev` usa `spring.jpa.hibernate.ddl-auto=update`. Não há Flyway nem outra ferramenta de migração.
- **Limite de desconto fixo no código:** o mínimo de 15% exige alterar `MIN_DISCOUNT` e recompilar.

## Possíveis evoluções

- Implementar uma `ProductSource` que consulte ofertas reais, mantendo o contrato atual.
- Agendar o processamento, em vez de depender de chamadas manuais ao endpoint.
- Adicionar autenticação aos endpoints de processamento e de cadastro.
- Gravar a `Promotion` antes do envio, ou adotar um registro de envio pendente, para evitar duplicatas em caso de falha parcial.
- Tratar `429` do Telegram com espera e nova tentativa.
- Retornar `400` ou `422` para violações de validação, em vez de `409`.
- Formatar os valores monetários no padrão brasileiro.
- Adicionar migrações versionadas com Flyway no lugar de `ddl-auto=update`.
- Incluir testes para os controllers e para a continuidade do lote após falhas de envio.

## Autor

Desenvolvido por **Ryan de Souza**.

GitHub: [@ryansouzas](https://github.com/ryansouzas)
