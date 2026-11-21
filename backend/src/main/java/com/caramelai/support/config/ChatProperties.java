package com.caramelai.support.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Operator-controlled settings bound from {@code caramel.*} in application.yml.
 * Constraints follow data-model.md (OperatorSettings). Invalid values fail startup.
 */
@Validated
@ConfigurationProperties(prefix = "caramel")
public record ChatProperties(
        @Valid @NotNull Assistant assistant,
        @Valid @NotNull Support support,
        @Valid @NotNull Memory memory,
        @Valid @NotNull Limits limits,
        @Valid @NotNull Cors cors) {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration ONE_DAY = Duration.ofHours(24);
    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    private static final Duration TWO_SECONDS = Duration.ofSeconds(2);
    private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);

    public record Assistant(
            // "Not blank. No `{` or `}` characters." (checked after placeholder resolution)
            @NotBlank
            @Pattern(regexp = "^[^{}]*$", message = "must not contain '{' or '}'")
            String systemPrompt) {
    }

    public record Support(
            // "Not blank. Release gate: must not equal the placeholder." (gate enforced in release checklist)
            @NotBlank String contact) {
    }

    public record Memory(
            // "2 to 100"
            @Min(2) @Max(100) int windowSize,
            // "1m to 24h"
            @NotNull Duration sessionTtl,
            // "100 to 1,000,000"
            @Min(100) @Max(1_000_000) int maxSessions) {

        @AssertTrue(message = "session-ttl must be between 1m and 24h")
        public boolean isSessionTtlInRange() {
            return sessionTtl != null
                    && sessionTtl.compareTo(ONE_MINUTE) >= 0
                    && sessionTtl.compareTo(ONE_DAY) <= 0;
        }
    }

    public record Limits(
            // "1 to 20,000"
            @Min(1) @Max(20_000) int maxMessageChars,
            // "5s to 5m"
            @NotNull Duration replyTimeout,
            // "2s to `reply-timeout`"
            @NotNull Duration idleTimeout,
            // "1 to 600"
            @Min(1) @Max(600) int requestsPerMinute,
            // "1 to 100"
            @Min(1) @Max(100) int burst,
            // Not in data-model.md yet; add it there. Default false; only enable behind a trusted proxy.
            boolean trustForwardedHeaders) {

        @AssertTrue(message = "reply-timeout must be between 5s and 5m")
        public boolean isReplyTimeoutInRange() {
            return replyTimeout != null
                    && replyTimeout.compareTo(FIVE_SECONDS) >= 0
                    && replyTimeout.compareTo(FIVE_MINUTES) <= 0;
        }

        @AssertTrue(message = "idle-timeout must be between 2s and reply-timeout")
        public boolean isIdleTimeoutInRange() {
            return idleTimeout != null
                    && replyTimeout != null
                    && idleTimeout.compareTo(TWO_SECONDS) >= 0
                    && idleTimeout.compareTo(replyTimeout) <= 0;
        }
    }

    public record Cors(
            // "Exactly one origin, no `*`"
            @NotBlank
            @Pattern(regexp = "^https?://[^*,\\s/]+(:\\d+)?$",
                    message = "must be exactly one origin with no wildcard")
            String allowedOrigin) {
    }
}
