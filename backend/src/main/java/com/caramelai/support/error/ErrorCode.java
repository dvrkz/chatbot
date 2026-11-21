package com.caramelai.support.error;

/** Closed set of failure codes. Carries no free text (data-model.md, ErrorCode). */
public enum ErrorCode {
    VALIDATION_FAILED,
    RATE_LIMITED,
    REPLY_TIMEOUT,
    UPSTREAM_UNAVAILABLE,
    INTERNAL_ERROR
}
