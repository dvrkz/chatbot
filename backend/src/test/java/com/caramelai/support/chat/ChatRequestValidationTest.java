package com.caramelai.support.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.caramelai.support.chat.dto.ChatRequest;
import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.error.InvalidRequestException;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

class ChatRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private static Set<ConstraintViolation<ChatRequest>> violations(String session, String message) {
        return VALIDATOR.validate(new ChatRequest(session, message));
    }

    @Test
    void blankMessageIsRejectedBeforeAnyModelCall() {
        assertThat(violations(TestChats.SESSION, "   ")).isNotEmpty();
        assertThat(violations(TestChats.SESSION, "")).isNotEmpty();
    }

    @Test
    void nonUuidSessionIsRejected() {
        assertThat(violations("not-a-uuid", "How do I get an API key?")).isNotEmpty();
    }

    @Test
    void surroundingWhitespaceIsTrimmed() {
        assertThat(new ChatRequest(TestChats.SESSION, "  hi there  ").message()).isEqualTo("hi there");
    }

    @Test
    void overLengthMessageIsRefusedWithoutCallingTheModel() {
        ChatProperties props = TestChats.defaultProps(); // max 2000 characters
        ChatModel model = TestChats.mockModel();
        ExpiringChatMemoryRepository repository = TestChats.repository(props);
        ChatService service = new ChatService(
                TestChats.client(model, TestChats.memory(repository, props), props), repository, props);

        String tooLong = "x".repeat(2001);
        assertThatThrownBy(() -> service.stream(TestChats.SESSION, tooLong))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(model);
    }

    @Test
    void messageAtTheLimitIsAccepted() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        org.mockito.Mockito.when(model.stream(any(Prompt.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());
        ExpiringChatMemoryRepository repository = TestChats.repository(props);
        ChatService service = new ChatService(
                TestChats.client(model, TestChats.memory(repository, props), props), repository, props);

        // Subscribing is not needed: the length check runs synchronously when stream() is called.
        service.stream(TestChats.SESSION, "x".repeat(2000));
    }
}
