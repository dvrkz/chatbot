package com.caramelai.support.chat.dto;

import com.caramelai.support.chat.SessionIds;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for POST /api/chat/stream.
 *
 * sessionId: "Required. UUID format." (data-model.md ChatRequest)
 * message:   "Required. After trimming, 1 to `max-message-chars` characters." The upper bound
 *            comes from configuration and is checked in ChatService, not here.
 */
public record ChatRequest(
        @NotBlank
        @Pattern(regexp = SessionIds.UUID_REGEX, message = "sessionId must be a UUID")
        String sessionId,

        @NotBlank
        String message) {

    public ChatRequest {
        if (message != null) {
            message = message.strip();
        }
    }
}
