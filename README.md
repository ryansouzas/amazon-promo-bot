# Amazon Promo Bot

Aplicação backend em **Java 21 e Spring Boot** que valida produtos, identifica descontos e publica ofertas em um chat do Telegram.

O MVP atual permite executar todo o fluxo de publicação a partir de uma requisição HTTP: obter os produtos, validar os dados, verificar o desconto mínimo, formatar a mensagem e enviá-la ao Telegram.

> **Estágio atual:** protótipo com produtos fictícios e integração HTTP com o Telegram. A consulta de ofertas reais da Amazon ainda não está implementada. Os links usados na demonstração pertencem a `example.com`.

## Sumário

- [Objetivo](#objetivo)
- [Funcionalidades implementadas](#funcionalidades-implementadas)
- [Tecnologias](#tecnologias)
- [Arquitetura e responsabilidades](#arquitetura-e-responsabilidades)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Modelo de produto](#modelo-de-produto)
- [Regras de negócio](#regras-de-negócio)
- [Demonstração do MVP](#demonstração-do-mvp)
- [Configuração e execução](#configuração-e-execução)
- [Endpoint disponível](#endpoint-disponível)
- [Integração com o Telegram](#integração-com-o-telegram)
- [Tratamento de falhas e logs](#tratamento-de-falhas-e-logs)
- [Testes](#testes)
- [Limitações atuais](#limitações-atuais)
- [Possíveis evoluções](#possíveis-evoluções)
- [Autor](#autor)

## Objetivo

Construir uma base para divulgação de promoções no Telegram, com responsabilidades separadas e regras de negócio testáveis.

Nesta etapa, a aplicação demonstra o processamento de ofertas com dados controlados. A interface `ProductSource` permite introduzir outra implementação de busca de produtos em uma evolução do projeto.

## Funcionalidades implementadas

| Funcionalidade | Comportamento atual |
| --- | --- |
| Acionamento por HTTP | Executa o processamento por meio de `POST /api/promotions/test`. |
| Fonte de produtos | Carrega uma lista fixa de três produtos por meio de `FakeProductSource`. |
| Validação de produtos | Verifica identificação, nome, preços e URL antes de avaliar a oferta. |
| Cálculo de desconto | Calcula o percentual com `BigDecimal`, duas casas decimais e `RoundingMode.HALF_UP`. |
| Filtro de ofertas | Seleciona produtos com desconto calculado maior ou igual a 15%. |
| Formatação de mensagens | Inclui nome, preço original, preço atual, desconto e link do produto. |
| Publicação no Telegram | Envia uma mensagem de texto por oferta elegível utilizando `RestClient`. |
| Tratamento de produto inválido | Registra um aviso, ignora o produto e continua o processamento. |
| Tratamento de falha de integração | Converte falhas do cliente HTTP em exceção própria e continua para os próximos produtos. |
| Configuração externa | Obtém o token do bot e o chat de destino por variáveis de ambiente. |
| Logs | Registra envios concluídos, produtos inválidos e falhas no envio. |
| Testes automatizados | Inclui testes de validação, cálculo, publicação, orquestração, cliente HTTP e carregamento do contexto. |

## Tecnologias

| Tecnologia | Uso no projeto |
| --- | --- |
| Java 21 | Linguagem e versão definida no build. |
| Spring Boot 4.1.1 | Inicialização, configuração e gerenciamento de dependências. |
| Spring Web MVC | Exposição do endpoint REST. |
| Spring RestClient | Requisições HTTP para o Telegram. |
| Maven e Maven Wrapper | Gerenciamento de dependências, testes e empacotamento. |
| Lombok | Geração de construtores e suporte a logs. |
| SLF4J | Registro de eventos da aplicação. |
| JUnit Jupiter | Testes e testes parametrizados. |
| Mockito | Simulação de dependências e verificação de interações. |
| MockRestServiceServer | Simulação das respostas HTTP nos testes de `TelegramService`. |

As versões de Java e Spring Boot acima correspondem ao `pom.xml` fornecido. A versão Maven do artefato é `0.0.1-SNAPSHOT`.

## Arquitetura e responsabilidades

A aplicação é um monólito organizado em pacotes por responsabilidade. Todos os componentes executam no mesmo processo.

O `PromotionController` recebe a requisição e chama `PromotionService`. O serviço consulta `ProductSource` e, para cada produto, coordena a validação, a avaliação do desconto e a publicação. O `PromotionPublisher` prepara o texto e delega o envio HTTP a `TelegramService`.

| Componente | Responsabilidade |
| --- | --- |
| `PromotionController` | Disponibilizar o endpoint que inicia o processamento. |
| `ProductSource` | Definir o contrato de obtenção da lista de produtos. |
| `FakeProductSource` | Fornecer os produtos fictícios da demonstração. |
| `ProductValidator` | Garantir que o produto atenda às regras de validade. |
| `PromotionEvaluator` | Calcular o desconto e verificar o mínimo exigido. |
| `PromotionService` | Coordenar o fluxo e tratar as falhas previstas por produto. |
| `PromotionPublisher` | Formatar a mensagem de oferta e solicitar seu envio. |
| `TelegramService` | Montar e executar a requisição HTTP para o Telegram. |
| `TelegramProperties` | Representar as propriedades `telegram.bot.token` e `telegram.bot.chat-id`. |
| `TelegramConfig` | Habilitar o registro de `TelegramProperties`. |
| `TelegramMessageRequest` | Representar o JSON enviado ao Telegram. |
| `InvalidProductException` | Sinalizar violação das regras de validação. |
| `TelegramIntegrationException` | Representar uma falha na comunicação com o Telegram. |

## Estrutura do projeto

Os pacotes Java ficam sob `com.ryansouzas.amazon_promo_bot`.

| Caminho | Conteúdo |
| --- | --- |
| `src/main/java/.../AmazonPromoBotApplication.java` | Ponto de entrada da aplicação. |
| `src/main/java/.../config/` | `TelegramConfig` e `TelegramProperties`. |
| `src/main/java/.../controller/` | `PromotionController`. |
| `src/main/java/.../dto/` | `TelegramMessageRequest`. |
| `src/main/java/.../exceptions/` | Exceções de validação e integração. |
| `src/main/java/.../model/` | Record `ProductDTO`. |
| `src/main/java/.../service/` | Avaliação, orquestração, formatação e envio. |
| `src/main/java/.../source/` | Contrato e implementação da fonte de produtos. |
| `src/main/java/.../validation/` | `ProductValidator`. |
| `src/main/resources/application.properties` | Nome da aplicação e propriedades do Telegram. |
| `src/test/java/.../service/` | Testes dos serviços. |
| `src/test/java/.../validation/` | Testes de validação. |
| `src/test/java/.../AmazonPromoBotApplicationTests.java` | Teste de carregamento do contexto Spring. |
| `pom.xml` | Dependências e configuração do build. |
| `mvnw` e `mvnw.cmd` | Maven Wrapper para Linux/macOS e Windows. |

## Modelo de produto

O produto é representado por um record Java:

```java
public record Product(
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

Os preços utilizam `BigDecimal` para os cálculos decimais. O record não possui mapeamento JPA; os dados são obtidos da fonte em memória, sem persistência.

## Regras de negócio

### Validação do produto

Antes da avaliação do desconto, `ProductValidator` aplica as seguintes regras:

| Dado | Regra |
| --- | --- |
| Produto | Não pode ser nulo. |
| Identificador externo | Não pode ser nulo, vazio ou composto apenas por espaços. |
| Nome | Não pode ser nulo, vazio ou composto apenas por espaços. |
| Preço original e preço atual | Não podem ser nulos. |
| Valores dos preços | Devem ser estritamente maiores que zero. |
| Relação entre preços | O preço atual deve ser menor ou igual ao preço original. |
| URL | Deve estar preenchida, ser aceita por `URI.create`, utilizar HTTPS e possuir host. |

Uma violação gera `InvalidProductException`. O serviço registra a falha e passa para o próximo produto.

Preços iguais são válidos, mas resultam em desconto de 0% e não geram publicação. A validação de URL não consulta o endereço nem exige domínio da Amazon.

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

## Demonstração do MVP

A implementação `FakeProductSource` retorna os seguintes produtos:

| Identificador | Produto | Preço original | Preço atual | Desconto | Resultado |
| --- | --- | --- | --- | --- | --- |
| `AMZ-001` | Mouse Gamer | R$ 200.00 | R$ 100.00 | 50.00% | Enviar |
| `AMZ-002` | Teclado Mecânico | R$ 300.00 | R$ 270.00 | 10.00% | Ignorar |
| `AMZ-003` | Monitor Gamer | R$ 1500.00 | R$ 900.00 | 40.00% | Enviar |

Com a integração funcionando, cada chamada ao endpoint envia **duas mensagens**, uma para o mouse e outra para o monitor.

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
- Um bot no Telegram e seu token.
- Um chat de destino em que o bot tenha permissão para enviar mensagens.

O Maven Wrapper está incluído no projeto. Não é necessário instalar Maven separadamente para utilizar os comandos abaixo.

### Preparar o bot

Tenha em mãos o token do bot e o identificador do chat de destino. Adicione o bot ao grupo ou canal escolhido e conceda as permissões necessárias para publicar.

O MVP utiliza um único destino por execução da aplicação, definido em `TELEGRAM_CHAT_ID`.

### Configurar as variáveis de ambiente

O arquivo `src/main/resources/application.properties` contém:

```properties
spring.application.name=amazon-promo-bot

telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.bot.chat-id=${TELEGRAM_CHAT_ID}
```

No PowerShell, na pasta raiz do projeto:

```powershell
$env:TELEGRAM_BOT_TOKEN = "SEU_TOKEN_DO_BOT"
$env:TELEGRAM_CHAT_ID = "SEU_CHAT_ID"
```

No Linux/macOS:

```bash
export TELEGRAM_BOT_TOKEN="SEU_TOKEN_DO_BOT"
export TELEGRAM_CHAT_ID="SEU_CHAT_ID"
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

A inicialização do servidor não publica ofertas: o processamento começa quando o endpoint é chamado.

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

O comando `package` também executa os testes. As variáveis do Telegram devem estar definidas no ambiente, inclusive para o teste de contexto.

## Endpoint disponível

### Executar o processamento de promoções

```http
POST /api/promotions/test
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
Invoke-WebRequest -Method POST -Uri "http://localhost:8080/api/promotions/test"
```

Linux/macOS:

```bash
curl -i -X POST http://localhost:8080/api/promotions/test
```

No Postman, selecione `POST`, informe essa URL e envie a requisição sem body.

> O retorno `204` indica que o método concluiu o processamento, mas não comprova que todas as mensagens foram entregues. Falhas de validação e de integração tratadas por produto são registradas nos logs e não alteram essa resposta.

Não existe controle de duplicidade: chamadas repetidas tentam publicar novamente as mesmas ofertas elegíveis.

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
| Produto inválido | `ProductValidator` lança `InvalidProductException`; `PromotionService` registra um aviso e continua. |
| Desconto insuficiente | O produto é ignorado sem tentativa de envio. |
| Falha do cliente HTTP | `TelegramService` captura `RestClientException`, registra o tipo da falha e lança `TelegramIntegrationException`, preservando a causa. |
| Falha ao publicar uma oferta | `PromotionService` captura `TelegramIntegrationException`, registra o identificador do produto e continua. |
| Lista vazia | O fluxo termina sem validar produtos ou publicar mensagens. |
| Falha ao consultar a fonte ou exceção inesperada | Não possui tratamento específico no fluxo atual e pode interromper a execução. |

Há logs em nível `INFO` para envios concluídos, `WARN` para produtos inválidos e falhas de publicação, e `ERROR` para falhas do cliente HTTP.

Não há nova tentativa automática de envio nem um resumo de sucesso ou falha no corpo da resposta do endpoint.

## Testes

A suíte separa as regras de negócio das dependências externas. Mockito simula colaboradores dos serviços; `MockRestServiceServer` intercepta as requisições do cliente HTTP, evitando envios reais nos testes de `TelegramService`.

| Classe | Cenários existentes |
| --- | --- |
| `ProductValidatorTest` | Produto válido e nulo; identificador e nome ausentes; preços nulos, zero ou negativos; preço atual maior que o original; preços iguais; URL ausente, HTTPS válida, HTTP, malformada e sem host. |
| `PromotionEvaluatorTest` | Descontos acima, abaixo e exatamente no limite; valores próximos ao limite; ausência de desconto; cálculo com preços decimais; arredondamento. |
| `PromotionPublisherTest` | Solicitação de envio; conferência do texto completo; propagação de exceção do Telegram. |
| `PromotionServiceTest` | Publicação de oferta válida; produto sem desconto suficiente; produto inválido; continuidade após produto inválido; lista vazia. |
| `TelegramServiceTest` | Requisição POST bem-sucedida; envio de `chat_id` e texto corretos; conversão de resposta HTTP 400 em exceção de integração. |
| `AmazonPromoBotApplicationTests` | Carregamento do contexto Spring com `@SpringBootTest`. |

O código continua o processamento após `TelegramIntegrationException`, mas a versão atual de `PromotionServiceTest` ainda não contém um teste dedicado a essa continuidade. Também não há teste dedicado do controller ou percentual de cobertura configurado com JaCoCo.

### Executar a suíte

Defina as variáveis de ambiente descritas anteriormente. Para testar sem usar credenciais reais, podem ser usados valores fictícios: o teste de contexto não aciona a publicação e os testes do cliente HTTP simulam a comunicação.

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

Os relatórios de execução são gerados em `target/surefire-reports/`.

### Executar uma classe específica

Windows PowerShell:

```powershell
.\mvnw.cmd "-Dtest=PromotionServiceTest" test
```

Linux/macOS:

```bash
./mvnw -Dtest=PromotionServiceTest test
```

A relação de cenários acima descreve os testes presentes no código; não representa um relatório de execução ou medição de cobertura.





## Autor

Desenvolvido por **Ryan de Souza**.

GitHub: [@ryansouzas](https://github.com/ryansouzas)

