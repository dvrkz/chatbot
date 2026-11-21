package com.caramelai.support.chat;

import com.caramelai.support.chat.dto.StreamEvent;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.ErrorCode;
import com.caramelai.support.error.InvalidRequestException;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Streams one reply for one customer message. Enforces the idle and overall reply limits, maps
 * every failure to a fixed ErrorCode, and removes the failed turn's customer message from memory.
 * Never logs message or reply text (FR-020, constitution Principle V).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ExpiringChatMemoryRepository memory;
    private final ChatProperties properties;

    public ChatService(ChatClient chatClient, ExpiringChatMemoryRepository memory, ChatProperties properties) {
        this.chatClient = chatClient;
        this.memory = memory;
        this.properties = properties;
    }

    /**
     * Starts one reply. Input-size violations throw synchronously, before any event is produced,
     * so the controller returns an HTTP 400 without calling the model.
     */
    public Flux<StreamEvent> stream(String sessionId, String message) {
        SessionIds.require(sessionId);
        if (message == null || message.isEmpty()) {
            throw new InvalidRequestException("message is required");
        }
        if (message.length() > properties.limits().maxMessageChars()) {
            throw new InvalidRequestException("message exceeds the configured length");
        }

        String requestId = UUID.randomUUID().toString();
        Instant started = Instant.now();
        AtomicReference<String> outcome = new AtomicReference<>("CANCELLED");

        Flux<String> chunks = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content();

        Duration idle = properties.limits().idleTimeout();
        Duration overall = properties.limits().replyTimeout();

        // The overall deadline stops the token stream and sets a flag. The terminal signal then
        // reports REPLY_TIMEOUT instead of Done, so a deadline can never look like a clean finish.
        AtomicBoolean deadlineReached = new AtomicBoolean(false);
        Flux<StreamEvent> tokens = chunks
                .filter(text -> text != null && !text.isEmpty())
                .<StreamEvent>map(StreamEvent.Token::new)
                .timeout(idle)
                .takeUntilOther(Mono.delay(overall).doOnNext(tick -> deadlineReached.set(true)));

        Mono<StreamEvent> terminal = Mono.defer(() -> {
            if (deadlineReached.get()) {
                return Mono.error(new ReplyTimeoutException());
            }
            outcome.set("COMPLETE");
            return Mono.just(new StreamEvent.Done());
        });

        return tokens
                .concatWith(terminal)
                .onErrorResume(error -> {
                    ErrorCode code = classify(error);
                    outcome.set(code.name());
                    removeFailedTurn(sessionId, message);
                    log.warn("chat turn failed requestId={} category={}", requestId, code);
                    return Flux.just(new StreamEvent.Error(code));
                })
                .doOnCancel(() -> removeFailedTurn(sessionId, message))
                .doFinally(signal -> log.info("chat turn finished requestId={} outcome={} durationMs={}",
                        requestId, outcome.get(), Duration.between(started, Instant.now()).toMillis()));
    }

    /** Removes stored context for a session. Unknown or expired sessions are a no-op. */
    public void clear(String sessionId) {
        SessionIds.require(sessionId);
        memory.deleteByConversationId(sessionId);
    }

    /** Thrown when the overall reply limit is reached. */
    static final class ReplyTimeoutException extends RuntimeException {
        ReplyTimeoutException() {
            super("reply exceeded the overall limit");
        }
    }

    static ErrorCode classify(Throwable error) {
        if (error instanceof ReplyTimeoutException || error instanceof TimeoutException) {
            return ErrorCode.REPLY_TIMEOUT;
        }
        return ErrorCode.UPSTREAM_UNAVAILABLE;
    }

    /**
     * Removes the customer message of a failed turn so a retry starts clean (data-model.md, failed
     * turn handling). Assistant text is never stored for a failed turn.
     */
    void removeFailedTurn(String sessionId, String message) {
        List<Message> stored = new ArrayList<>(memory.findByConversationId(sessionId));
        for (int i = stored.size() - 1; i >= 0; i--) {
            if (stored.get(i) instanceof UserMessage user) {
                if (message.equals(user.getText())) {
                    stored.remove(i);
                    memory.saveAll(sessionId, stored);
                }
                return;
            }
        }
    }
}
