package com.caramelai.support.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.caramelai.support.chat.ChatService;
import com.caramelai.support.chat.TestChats;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/** Conversation content and secrets never reach the logs (FR-020, constitution Principle V). */
class NoContentLoggingTest {

    private static final String MARKER = "MARKER-CUSTOMER-TEXT-7731";
    private static final String FAKE_KEY = "AIza-FAKE-KEY-0000";

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void attach() {
        logger = (Logger) LoggerFactory.getLogger("com.caramelai.support");
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void conversationTextAndSecretsAreNotLogged() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(TestChats.chunk("reply " + MARKER)));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props);

        service.stream(TestChats.SESSION, "My question " + MARKER).blockLast();

        String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(logged).isNotBlank();
        assertThat(logged).doesNotContain(MARKER).doesNotContain(FAKE_KEY);
    }

    @Test
    void failuresLogOnlyTheErrorCategory() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        when(model.stream(any(Prompt.class)))
                .thenReturn(Flux.error(new RuntimeException("provider echoed " + FAKE_KEY + " and " + MARKER)));
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props);

        service.stream(TestChats.SESSION, "Question " + MARKER).blockLast();

        String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(logged).contains("UPSTREAM_UNAVAILABLE");
        assertThat(logged).doesNotContain(MARKER).doesNotContain(FAKE_KEY);
    }
}
