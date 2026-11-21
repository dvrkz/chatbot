package com.caramelai.support.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.config.ChatProperties.Assistant;
import com.caramelai.support.config.ChatProperties.Cors;
import com.caramelai.support.config.ChatProperties.Limits;
import com.caramelai.support.config.ChatProperties.Memory;
import com.caramelai.support.config.ChatProperties.Support;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChatPropertiesValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private static ChatProperties valid() {
        return new ChatProperties(
                new Assistant("You are the Caramel AI tech support assistant."),
                new Support("support@example.com"),
                new Memory(20, Duration.ofMinutes(30), 10_000),
                new Limits(2000, Duration.ofSeconds(30), Duration.ofSeconds(15), 10, 5, false),
                new Cors("http://localhost:5173"));
    }

    private static Set<ConstraintViolation<ChatProperties>> violations(ChatProperties props) {
        return VALIDATOR.validate(props);
    }

    @Test
    void validDefaultsHaveNoViolations() {
        assertThat(violations(valid())).isEmpty();
    }

    @Test
    void windowSizeBelowMinimumIsRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), valid().support(),
                new Memory(1, Duration.ofMinutes(30), 10_000), valid().limits(), valid().cors());
        assertThat(violations(p)).isNotEmpty();
    }

    @Test
    void idleTimeoutLongerThanReplyTimeoutIsRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), valid().support(), valid().memory(),
                new Limits(2000, Duration.ofSeconds(30), Duration.ofSeconds(45), 10, 5, false),
                valid().cors());
        assertThat(violations(p)).extracting(ConstraintViolation::getMessage)
                .anyMatch(m -> m.contains("idle-timeout"));
    }

    @Test
    void replyTimeoutOutsideRangeIsRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), valid().support(), valid().memory(),
                new Limits(2000, Duration.ofSeconds(1), Duration.ofSeconds(1), 10, 5, false),
                valid().cors());
        assertThat(violations(p)).extracting(ConstraintViolation::getMessage)
                .anyMatch(m -> m.contains("reply-timeout"));
    }

    @Test
    void wildcardOriginIsRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), valid().support(), valid().memory(),
                valid().limits(), new Cors("*"));
        assertThat(violations(p)).isNotEmpty();
    }

    @Test
    void multipleOriginsAreRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), valid().support(), valid().memory(),
                valid().limits(), new Cors("http://a.example,http://b.example"));
        assertThat(violations(p)).isNotEmpty();
    }

    @Test
    void systemPromptWithBracesIsRejected() {
        ChatProperties p = new ChatProperties(new Assistant("Use {placeholder} here."),
                valid().support(), valid().memory(), valid().limits(), valid().cors());
        assertThat(violations(p)).extracting(ConstraintViolation::getMessage)
                .anyMatch(m -> m.contains("'{'"));
    }

    @Test
    void blankSupportContactIsRejected() {
        ChatProperties p = new ChatProperties(valid().assistant(), new Support(" "),
                valid().memory(), valid().limits(), valid().cors());
        assertThat(violations(p)).isNotEmpty();
    }
}
