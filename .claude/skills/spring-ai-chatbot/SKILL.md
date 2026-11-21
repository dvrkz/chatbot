---
name: spring-ai-chatbot
description: Build and modify Java chatbots with Spring AI 2.0.x. Use when implementing AI chat endpoints, calling or streaming LLM responses with ChatModel / StreamingChatModel / ChatClient, building Prompts from System/User/Assistant messages, configuring LLM providers (Google AI Studio, OpenAI, Anthropic, Ollama, Bedrock, OpenRouter, Mistral) in application.yml, overriding ChatOptions per request, adding conversation memory, or handling streaming (Flux/SSE) responses in Java/Spring Boot.
---

# Spring AI Chatbot (v2.0.x)

Guidance for building chat features with Spring AI **2.0.2**, based on the official Chat Model API reference (https://docs.spring.io/spring-ai/reference/api/chatmodel.html). Spring AI 2.0.x supports **Spring Boot 4.0.x and 4.1.x**.

If something here conflicts with the reference docs for the installed version, the docs win. Provider pages live under `https://docs.spring.io/spring-ai/reference/api/chat/`.

## 1. Dependencies

Import the BOM, then add one provider starter. Do not set versions on Spring AI starters.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>2.0.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Google AI Studio (Gemini Developer API) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-google-genai</artifactId>
</dependency>
```

Streaming endpoints that return `Flux` need the reactive stack on the classpath (`spring-boot-starter-webflux`). The ChatClient docs state that streaming requires it and non-streaming requires the Servlet stack, so decide this once and keep the app consistent.

## 2. Core API

```java
public interface ChatModel extends Model<Prompt, ChatResponse>, StreamingChatModel {
    default String call(String message) {...}
    ChatResponse call(Prompt prompt);
}

public interface StreamingChatModel extends StreamingModel<Prompt, ChatResponse> {
    default Flux<String> stream(String message) {...}
    Flux<ChatResponse> stream(Prompt prompt);
}
```

- `call(String)` is a convenience for simple cases. Real apps normally use `call(Prompt)` and a `ChatResponse`.
- `stream(String)` returns `Flux<String>` (just the text). `stream(Prompt)` returns `Flux<ChatResponse>` (text plus metadata per chunk).

### Synchronous completion

```java
ChatResponse response = chatModel.call(new Prompt(messages));

String text = response.getResult().getOutput().getText();   // first Generation -> AssistantMessage -> text
ChatResponseMetadata meta = response.getMetadata();         // usage, model, etc.
```

`ChatResponse` holds `List<Generation>` (`getResults()`) plus `ChatResponseMetadata`. Each `Generation` wraps an `AssistantMessage` (`getOutput()`) and `ChatGenerationMetadata` (`getMetadata()`). Use `getResult()` for the first generation, which is what a chatbot almost always wants.

### Streaming

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<String> stream(@RequestParam String message) {
    return chatModel.stream(message);                       // Flux<String>
}

Flux<ChatResponse> chunks = chatModel.stream(prompt);       // when you need metadata
```

Never call `.block()` or `.blockLast()` on a streaming Flux inside a request handler. Return the `Flux` and let the framework subscribe.

### Prompt and messages

A `Prompt` holds a `List<Message>` plus optional `ChatOptions`. `Message` implementations carry the conversational role (`MessageType`):

```java
List<Message> messages = List.of(
    new SystemMessage("You are a tech support assistant for Caramel AI, an LLM inferencing company."),
    new UserMessage("Why am I getting HTTP 429?"),
    new AssistantMessage("You've hit a rate limit. Which model are you calling?"),
    new UserMessage("gemma on the chat completions endpoint"));

ChatResponse response = chatModel.call(new Prompt(messages));
```

- `SystemMessage`: instructions and persona. Put it first.
- `UserMessage`: the end user's input.
- `AssistantMessage`: prior model replies. Replay history to give the model conversational context. The model is stateless; you resend history on every call (or use chat memory, section 5).

## 3. Configuration and options

### Startup defaults (application.yml)

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GOOGLE_API_KEY}
        chat:
          model: gemma-4-26b-a4b-it
          temperature: 0.3
          max-output-tokens: 1024
```

Property prefixes: `spring.ai.google.genai` for connection/auth, `spring.ai.google.genai.chat` for chat options. `spring.ai.model.chat=none` disables the chat model auto-configuration.

### Portable ChatOptions

`ChatOptions` exposes provider-neutral settings: `model`, `temperature`, `maxTokens`, `topP`, `topK`, `frequencyPenalty`, `presencePenalty`, `stopSequences`. Each provider also has its own options class (e.g. `GoogleGenAiChatOptions`, `OpenAiChatOptions`) with extras. Prefer the portable `ChatOptions` unless you need a provider-specific setting; it keeps code portable (section 4).

### Per-request overrides: two different behaviors

This is the most common source of surprises:

| Entry point | Behavior |
|---|---|
| `ChatModel.call/stream` with `Prompt` options | **Full override.** The `ChatOptions` on the `Prompt` take complete precedence over startup defaults. Set every option you care about. Pass `null` options (or omit them) to use the defaults. |
| `ChatClient` | **Incremental "delta" override.** Only the options you set on the request replace the defaults; everything else keeps its startup/default value. |

```java
// ChatModel: full override. Fields you leave unset are NOT inherited from application.yml.
ChatResponse r = chatModel.call(new Prompt(
    "Summarize this error log.",
    ChatOptions.builder().model("gemma-4-26b-a4b-it").temperature(0.1).maxTokens(512).build()));

// ChatClient: delta. Only temperature changes; model/max tokens stay as configured.
String answer = chatClient.prompt()
    .user("Summarize this error log.")
    .options(ChatOptions.builder().temperature(0.1))
    .call()
    .content();
```

`ChatClient` builder-level defaults (`defaultOptions`, `defaultSystem`, `defaultAdvisors`, `defaultTools`, ...) have per-request counterparts without the `default` prefix (`options`, `system`, `advisors`, `tools`, ...). The reference page gives only the signature `options(ChatOptions.Builder)` with no example, so confirm the call shape against the installed version when you use it.

## 4. Multi-provider portability

Business logic should depend on the `ChatModel` / `StreamingChatModel` / `ChatClient` abstractions, never on a provider class. Switching providers is then a dependency + properties change.

| Provider | Starter | Notes |
|---|---|---|
| Google AI Studio | `spring-ai-starter-model-google-genai` | Set `spring.ai.google.genai.api-key` to use the Gemini Developer API. Omitting it and setting `project-id` + `location` selects Vertex AI instead. |
| OpenAI | `spring-ai-starter-model-openai` | `spring.ai.openai.api-key`, `spring.ai.openai.chat.model`. |
| OpenRouter | `spring-ai-starter-model-openai` | Not a listed provider on the Chat Model page. Reuse the OpenAI starter and override `spring.ai.openai.base-url` plus the API key and model. Take the exact base URL and model naming from OpenRouter's docs. |
| Anthropic, Ollama, Amazon Bedrock Converse, Mistral AI | follow `spring-ai-starter-model-<provider>` | The pattern is expected but not confirmed on the pages I read. Check the artifact name on the provider's page (`chat/anthropic-chat.html`, `chat/ollama-chat.html`, `chat/bedrock-converse.html`, `chat/mistralai-chat.html`) before adding it. |

Swap checklist:
1. Replace the starter dependency.
2. Replace the `spring.ai.<provider>.*` properties and the model name.
3. Keep controllers and services unchanged. Only inject `ChatModel` or `ChatClient`.
4. Re-check any provider-specific options you set (they will not carry over), and prefer portable `ChatOptions`.

OpenAI-compatible servers use `spring.ai.openai.base-url`, `.api-key`, and `.chat.model` with the model name passed through as-is. Do not use `extra-body` against the official OpenAI API (it returns HTTP 400 for unknown parameters).

Watch out when multiple provider starters are on the classpath: with more than one `ChatModel` bean, injection is ambiguous. Keep one starter, or use `spring.ai.model.chat` to select one and qualify beans explicitly.

## 5. ChatClient and conversation memory

`ChatClient` is the higher-level fluent API on top of `ChatModel`. Prefer it for chatbots that need a default system prompt, memory, or advisors.

```java
@Configuration
class ChatConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
            .defaultSystem("You are a tech support assistant for Caramel AI ...")
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    }
}

// non-streaming
String reply = chatClient.prompt()
    .user(userText)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
    .call()
    .content();

// streaming
Flux<String> stream = chatClient.prompt()
    .user(userText)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
    .stream()
    .content();
```

- `ChatClient.Builder` is an auto-configured, **prototype-scoped** bean. Inject it and call `.build()`; each injection gets a fresh builder.
- `call()` alone does not invoke the model. Invocation happens when `content()`, `chatResponse()`, or `responseEntity()` is called.
- `ChatMemory.CONVERSATION_ID` is **required** on every call that uses a memory advisor. Omitting it throws `IllegalArgumentException`; there is no default. Use the chat session ID.
- `MessageWindowChatMemory` keeps the last 20 messages by default, evicting the oldest and preserving system messages. It is backed by a `ChatMemoryRepository` (in-memory by default; JDBC, Redis, Mongo, Cassandra, Neo4j are available). If the `ChatMemory` bean is not auto-configured in your setup, build one explicitly with `MessageWindowChatMemory`.
- If you create clients with `ChatClient.create(chatModel)` or `ChatClient.builder(chatModel)` for several model beans, you bypass the auto-configured builder, so observability and `ChatClientBuilderCustomizer` beans are ignored. Inject `ChatClientBuilderConfigurer` and call `configurer.configure(builder).build()` to keep them. Mark one client `@Primary` to avoid ambiguity.
- Structured output via `entity(...)` is not available with `stream()`. Aggregate the output and convert with `BeanOutputConverter`.
- Advisor order matters: advisors run in the order they are registered. Put `SimpleLoggerAdvisor` near the end and enable it with `logging.level.org.springframework.ai.chat.client.advisor=DEBUG` only in non-production, since it logs prompts.

## 6. Best practices and guardrails

**Dependency injection**
- Use constructor injection. Inject `ChatModel`, `StreamingChatModel`, or `ChatClient.Builder` (or a configured `ChatClient` bean). Don't use field `@Autowired`.
- Inject the interface, not a provider class like `GoogleGenAiChatModel`, so providers stay swappable.
- Keep prompt construction and model calls in a service class. Controllers should only map HTTP to that service.

**Secrets**
- Read API keys from environment variables: `api-key: ${GOOGLE_API_KEY}`. Never hardcode a key in `application.yml`, source, tests, or the frontend, and never log it.
- Spring Boot does not read a `.env` file by default. Load it with a dotenv mechanism (for example `spring.config.import=optional:file:.env[.properties]`, so a `GOOGLE_API_KEY=...` line in `.env` resolves the `${GOOGLE_API_KEY}` placeholder) or export the variable in the shell or run configuration. Keep `.env` in `.gitignore`.
- Never put the key in a `Prompt`, system message, or log line.

**Prompts and safety**
- Keep the system prompt in configuration, not scattered across code. Never concatenate untrusted user text into the system message.
- Bound conversations: use a windowed memory and set `max-output-tokens` / `maxTokens` so a session cannot grow unbounded.
- Set client timeouts and map provider failures to a clean error response. Don't expose raw provider errors to end users.

**Avoid deprecated or stale patterns**
- The old `spring-ai-vertex-ai-gemini` module is removed. Use `spring-ai-starter-model-google-genai`. Properties moved from `spring.ai.vertex.ai.gemini.*` to `spring.ai.google.genai.*`, and the package from `org.springframework.ai.vertexai.gemini` to `org.springframework.ai.google.genai`.
- Don't copy old-style `spring-ai-<provider>-spring-boot-starter` artifact names or pre-1.0 snippets from blog posts. Starters follow `spring-ai-starter-model-<provider>`.
- `AdvisedRequest` appears in stale wording of the logger-advisor docs. Current advisors work with `ChatClientRequest`.
- Don't use `extra-body` with the official OpenAI API, and don't set `temperature` on GPT-5 models (it errors).
- Google thinking options: `thinkingLevel` (Gemini 3.x) and `thinkingBudget` (Gemini 2.5) are mutually exclusive. Setting both causes an API error.

## 7. Project notes (Caramel AI chatbot)

- Provider: Google AI Studio via `spring-ai-starter-model-google-genai`, API key from `GOOGLE_API_KEY`.
- Target model per `prd.md`: `gemma-4-26b-a4b-it`. The Google GenAI docs list Gemini model names as examples. Confirm that this exact model ID is served by the Gemini Developer API for your key before relying on it, and fail fast at startup if it is not.
- Stack requirements, scope, and streaming expectations are in `prd.md`.
