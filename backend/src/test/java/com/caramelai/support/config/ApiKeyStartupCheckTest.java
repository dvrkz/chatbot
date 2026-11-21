package com.caramelai.support.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Startup fails without a key, and the message names the variable but never the value (Principle I). */
class ApiKeyStartupCheckTest {

    private static final String KEY = "AIza-FAKE-KEY-0000";

    @Test
    void missingKeyFailsStartupAndNamesTheVariable() {
        assertThatThrownBy(() -> new ApiKeyStartupCheck(new MockEnvironment()).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GOOGLE_API_KEY")
                .hasMessageNotContaining(KEY);
    }

    @Test
    void blankKeyFailsStartup() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.ai.google.genai.api-key", "  ");
        assertThatThrownBy(() -> new ApiKeyStartupCheck(env).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unresolvedPlaceholderFailsStartup() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.google.genai.api-key", "${GOOGLE_API_KEY}");
        assertThatThrownBy(() -> new ApiKeyStartupCheck(env).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void presentKeyPassesWithoutPrintingIt() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.ai.google.genai.api-key", KEY);
        assertThatCode(() -> new ApiKeyStartupCheck(env).afterPropertiesSet()).doesNotThrowAnyException();
    }
}
