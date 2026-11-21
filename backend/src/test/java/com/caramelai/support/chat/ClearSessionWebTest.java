package com.caramelai.support.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.ApiExceptionHandler;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/** DELETE /api/chat/sessions/{id} (User Story 5). */
class ClearSessionWebTest {

    private static final ChatProperties PROPS = TestChats.defaultProps();

    private final ChatModel model = TestChats.mockModel();
    private final ExpiringChatMemoryRepository repo = TestChats.repository(PROPS);
    private final WebTestClient client = WebTestClient.bindToController(new ChatController(
                    new ChatService(TestChats.client(model, TestChats.memory(repo, PROPS), PROPS), repo, PROPS)))
            .controllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void existingUnknownAndExpiredSessionsAllReturn204() {
        // An unknown session is the same as an expired one: nothing is stored under the ID.
        client.delete().uri("/api/chat/sessions/" + TestChats.SESSION).exchange().expectStatus().isNoContent();
        client.delete().uri("/api/chat/sessions/" + UUID.randomUUID()).exchange().expectStatus().isNoContent();
    }

    @Test
    void nonUuidPathReturns400() {
        client.delete().uri("/api/chat/sessions/not-a-uuid").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void laterMessageAfterClearStartsWithNoPriorContext() {
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(TestChats.chunk("ok")));
        client.post().uri("/api/chat/stream").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sessionId\":\"" + TestChats.SESSION + "\",\"message\":\"first\"}")
                .exchange().expectStatus().isOk()
                .returnResult(String.class).getResponseBody().blockLast();
        assertThat(repo.findByConversationId(TestChats.SESSION)).isNotEmpty();

        client.delete().uri("/api/chat/sessions/" + TestChats.SESSION).exchange().expectStatus().isNoContent();

        assertThat(repo.findByConversationId(TestChats.SESSION)).isEmpty();
    }
}
