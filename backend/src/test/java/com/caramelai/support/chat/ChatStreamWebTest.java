package com.caramelai.support.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.ApiExceptionHandler;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * HTTP-level tests of POST /api/chat/stream. Uses the real service over a faked model, bound to the
 * controller directly so no Spring context or provider key is needed.
 */
class ChatStreamWebTest {

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE =
            new ParameterizedTypeReference<>() {
            };

    private static WebTestClient client(ChatModel model) {
        ChatProperties props = TestChats.defaultProps();
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(
                TestChats.client(model, TestChats.memory(repo, props), props), repo, props);
        return WebTestClient.bindToController(new ChatController(service))
                .controllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static String body(String sessionId, String message) {
        return "{\"sessionId\":\"" + sessionId + "\",\"message\":\"" + message + "\"}";
    }

    private static List<ServerSentEvent<String>> stream(WebTestClient client, String message) {
        return client.post().uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body(TestChats.SESSION, message))
                .exchange()
                .expectStatus().isOk()
                .returnResult(SSE).getResponseBody()
                .collectList().block();
    }

    @Test
    void successfulReplyStreamsTokensThenDoneAsTheLastEvent() {
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(
                TestChats.chunk("A 429 means "), TestChats.chunk("rate limited.")));

        List<ServerSentEvent<String>> events = stream(client(model), "Why 429?");

        assertThat(events).extracting(e -> e.event()).containsExactly("token", "token", "done");
        assertThat(events.get(0).data()).isEqualTo("{\"text\":\"A 429 means \"}");
    }

    @Test
    void modelFailureEndsWithASingleErrorEventAndNoDone() {
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("down")));

        List<ServerSentEvent<String>> events = stream(client(model), "Why 401?");

        assertThat(events).extracting(e -> e.event()).containsExactly("error");
        assertThat(events.get(0).data()).isEqualTo("{\"code\":\"UPSTREAM_UNAVAILABLE\"}");
    }

    @Test
    void invalidSessionIdReturnsBadRequestWithoutCallingTheModel() {
        ChatModel model = mock(ChatModel.class);

        client(model).post().uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body("not-a-uuid", "Hello"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED");

        verifyNoInteractions(model);
    }
}
