package com.caramelai.support.chat.dto;

import com.caramelai.support.error.ErrorCode;

/**
 * Server-sent events for one reply (contracts/chat-stream-events.md). A successful stream ends with
 * exactly one Done; a failed stream ends with exactly one Error. Neither carries customer or
 * model text beyond the reply tokens themselves.
 */
public sealed interface StreamEvent permits StreamEvent.Token, StreamEvent.Done, StreamEvent.Error {

    String eventName();

    Object payload();

    /** A piece of reply text, in arrival order. Never empty. */
    record Token(String text) implements StreamEvent {
        @Override
        public String eventName() {
            return "token";
        }

        @Override
        public Object payload() {
            return this;
        }
    }

    /** The reply finished normally. Serializes as {}. */
    record Done() implements StreamEvent {
        @Override
        public String eventName() {
            return "done";
        }

        @Override
        public Object payload() {
            return this;
        }
    }

    /** The reply failed. The code is the only detail sent. */
    record Error(ErrorCode code) implements StreamEvent {
        @Override
        public String eventName() {
            return "error";
        }

        @Override
        public Object payload() {
            return this;
        }
    }
}
