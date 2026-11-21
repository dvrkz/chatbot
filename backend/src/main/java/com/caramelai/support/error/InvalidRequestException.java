package com.caramelai.support.error;

/**
 * A request failed validation before the model is called. The message is for server logs only;
 * the response body never includes it.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String reason) {
        super(reason);
    }
}
