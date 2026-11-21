package com.caramelai.support.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.caramelai.support.chat.ChatController;
import com.caramelai.support.chat.ChatService;
import com.caramelai.support.chat.TestChats;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.limits.RateLimitFilter;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * HTTP-level checks that error bodies never carry stack traces, upstream text, or key-like strings
 * (FR-011, FR-017, constitution Principle I).
 */
class ErrorResponseLeakTest {

    private static final String FAKE_KEY = "AIza-FAKE-KEY-0000";
    private static final String BODY_400 = "{\"sessionId\":\"not-a-uuid\",\"message\":\"hi\"}";

    private static String body(String sessionId) {
        return "{\"sessionId\":\"" + sessionId + "\",\"message\":\"hi\"}";
    }

    private static void assertNoLeak(String responseBody) {
        assertThat(responseBody).doesNotContain(FAKE_KEY).doesNotContain("Exception")
                .doesNotContain("at com.caramelai").doesNotContain("stack");
    }

    @Test
    void badRequestBodyHasOnlyTheCode() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        WebTestClient client = WebTestClient.bindToController(new ChatController(
                        new ChatService(TestChats.client(model, TestChats.memory(repo, props), props), repo, props)))
                .controllerAdvice(new ApiExceptionHandler()).build();

        String response = client.post().uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(BODY_400)
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).returnResult().getResponseBody();

        assertNoLeak(response);
        assertThat(response).contains("VALIDATION_FAILED");
    }

    @Test
    void internalErrorBodyNeverEchoesTheUpstreamMessage() {
        ChatService failing = mock(ChatService.class);
        when(failing.stream(any(), any())).thenThrow(new RuntimeException("provider said " + FAKE_KEY));
        WebTestClient client = WebTestClient.bindToController(new ChatController(failing))
                .controllerAdvice(new ApiExceptionHandler()).build();

        String response = client.post().uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body(TestChats.SESSION))
                .exchange().expectStatus().isEqualTo(500)
                .expectBody(String.class).returnResult().getResponseBody();

        assertNoLeak(response);
        assertThat(response).contains("INTERNAL_ERROR");
    }

    @Test
    void rateLimitedBodyCarriesOnlyTheCode() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(Flux.empty());
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        WebTestClient client = WebTestClient.bindToController(new ChatController(
                        new ChatService(TestChats.client(model, TestChats.memory(repo, props), props), repo, props)))
                .webFilter(new RateLimitFilter(props))
                .controllerAdvice(new ApiExceptionHandler()).build();

        String last = null;
        for (int i = 0; i < 7; i++) {
            last = client.post().uri("/api/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(body(TestChats.SESSION))
                    .exchange().expectBody(String.class).returnResult().getResponseBody();
        }
        assertNoLeak(last);
        assertThat(last).contains("RATE_LIMITED");
    }
}
