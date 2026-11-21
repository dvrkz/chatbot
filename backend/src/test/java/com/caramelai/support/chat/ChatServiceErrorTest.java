package com.caramelai.support.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.caramelai.support.chat.dto.StreamEvent;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.ErrorCode;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ChatServiceErrorTest {

    private static final String FAKE_KEY = "AIza-FAKE-KEY-0000";

    private static ChatService service(ChatModel model, ExpiringChatMemoryRepository repo, ChatProperties props) {
        return new ChatService(TestChats.client(model, TestChats.memory(repo, props), props), repo, props);
    }

    @Test
    void modelFailureEmitsUpstreamUnavailableWithoutLeakingDetail() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.error(new RuntimeException("provider said " + FAKE_KEY)));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);

        StepVerifier.create(service(model, repo, props).stream(TestChats.SESSION, "Why 401?"))
                .expectNext(new StreamEvent.Error(ErrorCode.UPSTREAM_UNAVAILABLE))
                .verifyComplete();
    }

    @Test
    void partialTextFollowedByFailureKeepsTokensAndEndsWithError() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.concat(Flux.just(TestChats.chunk("You are seeing a 429 because ")),
                        Flux.error(new RuntimeException("connection reset"))));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);

        StepVerifier.create(service(model, repo, props).stream(TestChats.SESSION, "Why 429?"))
                .expectNext(new StreamEvent.Token("You are seeing a 429 because "))
                .expectNext(new StreamEvent.Error(ErrorCode.UPSTREAM_UNAVAILABLE))
                .verifyComplete();
    }

    @Test
    void failedTurnRemovesItsCustomerMessageFromMemory() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("boom")));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);

        StepVerifier.create(service(model, repo, props).stream(TestChats.SESSION, "Retry me"))
                .expectNextCount(1)
                .verifyComplete();

        boolean anyMatch = repo.findByConversationId(TestChats.SESSION).stream()
                .anyMatch(m -> m instanceof UserMessage u && "Retry me".equals(u.getText()));
        assertThat(anyMatch).isFalse();
    }
}
