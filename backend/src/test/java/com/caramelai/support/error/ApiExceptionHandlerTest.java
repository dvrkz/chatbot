package com.caramelai.support.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {

    private static final String FAKE_KEY = "AIza-FAKE-KEY-0000";

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void invalidRequestMapsToBadRequestWithoutReason() {
        ResponseEntity<ProblemDetail> response =
                handler.validation(new InvalidRequestException("sessionId is not a UUID"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Invalid request");
        assertThat(body.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(body.getDetail()).isNull();
    }

    @Test
    void unexpectedFailureMapsToInternalErrorAndLeaksNothing() {
        ResponseEntity<ProblemDetail> response =
                handler.unexpected(new RuntimeException("upstream said key " + FAKE_KEY));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Something went wrong");
        assertThat(body.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(body.toString()).doesNotContain(FAKE_KEY).doesNotContain("RuntimeException");
    }

    @Test
    void frameworkClientErrorKeepsItsStatusAndMapsToValidationFailed() {
        // Unknown routes keep 404; the code stays inside the closed ErrorCode set.
        ResponseEntity<ProblemDetail> response =
                handler.unexpected(new ResponseStatusException(HttpStatus.NOT_FOUND, "no route"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getProperties()).containsEntry("code", "VALIDATION_FAILED");
    }
}
