package com.caramelai.support.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.caramelai.support.chat.dto.StreamEvent;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.ErrorCode;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Timeout paths (constitution Principle V). These use real short limits rather than virtual time:
 * StepVerifier.withVirtualTime swaps Reactor's global schedulers, and that broke the streaming
 * pipeline in later tests. The limits are the smallest valid values: idle 2 s, reply 5 s.
 */
@Timeout(60)
class ChatServiceTimeoutTest {

    private static final Duration IDLE = Duration.ofSeconds(2);
    private static final Duration OVERALL = Duration.ofSeconds(5);

    @Test
    void silenceLongerThanIdleLimitEmitsReplyTimeout() {
        ChatProperties props = TestChats.props(20, IDLE, OVERALL, 2000);
        ChatModel model = TestChats.mockModel();
        // One chunk, then the model stalls forever.
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.concat(Flux.just(TestChats.chunk("A")), Flux.never()));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);

        StepVerifier.create(new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props).stream(TestChats.SESSION, "Why 429?"))
                .expectNext(new StreamEvent.Token("A"))
                .expectNext(new StreamEvent.Error(ErrorCode.REPLY_TIMEOUT))
                .expectComplete()
                .verify(Duration.ofSeconds(15));
    }

    @Test
    void replyLongerThanOverallLimitEmitsReplyTimeoutEvenWithSteadyChunks() {
        // Chunks every 1.5 s stay inside the 2 s idle limit, but the reply passes the 5 s overall limit.
        ChatProperties props = TestChats.props(20, IDLE, OVERALL, 2000);
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.interval(Duration.ofMillis(1500)).map(i -> TestChats.chunk("x")));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);

        StepVerifier.create(new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props).stream(TestChats.SESSION, "Long question"))
                // Chunks arrive at about 1.5, 3.0, and 4.5 seconds. The 5 second deadline then fires.
                .expectNextCount(3)
                .expectNext(new StreamEvent.Error(ErrorCode.REPLY_TIMEOUT))
                .expectComplete()
                .verify(Duration.ofSeconds(20));
    }

    @Test
    void fastStreamCompletesWithDoneAndNoError() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(
                Flux.just(TestChats.chunk("Hello "), TestChats.chunk("there")));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props);

        StepVerifier.create(service.stream(TestChats.SESSION, "Hi"))
                .expectNext(new StreamEvent.Token("Hello "))
                .expectNext(new StreamEvent.Token("there"))
                .expectNext(new StreamEvent.Done())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }
}
