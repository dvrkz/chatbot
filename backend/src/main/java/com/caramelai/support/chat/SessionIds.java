package com.caramelai.support.chat;

import com.caramelai.support.error.InvalidRequestException;
import java.util.regex.Pattern;

/** Session ID format rule shared by request bodies and path variables. */
public final class SessionIds {

    /** Canonical UUID text form (8-4-4-4-12 hexadecimal). */
    public static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private SessionIds() {
    }

    /** Throws InvalidRequestException (mapped to VALIDATION_FAILED) when the ID is not a UUID. */
    public static void require(String sessionId) {
        if (sessionId == null || !UUID_PATTERN.matcher(sessionId).matches()) {
            throw new InvalidRequestException("sessionId is not a UUID");
        }
    }
}
