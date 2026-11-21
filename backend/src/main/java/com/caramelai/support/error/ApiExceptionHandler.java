package com.caramelai.support.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

/**
 * Maps failures before streaming starts to RFC 9457 problem bodies. Bodies carry a fixed title
 * and an ErrorCode only. Exception messages, stack traces, and causes are never included.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({InvalidRequestException.class, WebExchangeBindException.class,
            ServerWebInputException.class})
    public ResponseEntity<ProblemDetail> validation(Exception ex) {
        // Log the category only; the request body is never logged.
        log.debug("request rejected category=VALIDATION_FAILED type={}", ex.getClass().getSimpleName());
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid request");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse framework) {
            HttpStatusCode status = framework.getStatusCode();
            if (status.is4xxClientError()) {
                return problem(HttpStatus.valueOf(status.value()), ErrorCode.VALIDATION_FAILED,
                        "Invalid request");
            }
        }
        log.error("unhandled failure category=INTERNAL_ERROR type={}", ex.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Something went wrong");
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode code, String title) {
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(title);
        detail.setProperty("code", code.name());
        return ResponseEntity.status(status).body(detail);
    }
}
