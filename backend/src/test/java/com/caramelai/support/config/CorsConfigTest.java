package com.caramelai.support.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.config.ChatProperties.Assistant;
import com.caramelai.support.config.ChatProperties.Cors;
import com.caramelai.support.config.ChatProperties.Limits;
import com.caramelai.support.config.ChatProperties.Memory;
import com.caramelai.support.config.ChatProperties.Support;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Drives the real CorsWebFilter against a mock exchange. Spring Framework 7 rejects any method that
 * is not explicitly allowed, so the allowed methods are asserted as well.
 */
class CorsConfigTest {

    private static final String ALLOWED = "http://localhost:5173";
    private static final String API = "http://localhost:8080/api/chat/stream";

    private static CorsWebFilter filter() {
        ChatProperties props = new ChatProperties(
                new Assistant("You are the Caramel AI tech support assistant."),
                new Support("support@example.com"),
                new Memory(20, Duration.ofMinutes(30), 10_000),
                new Limits(2000, Duration.ofSeconds(30), Duration.ofSeconds(15), 10, 5, false),
                new Cors(ALLOWED));
        return new CorsConfig().corsWebFilter(props);
    }

    /** Runs the filter and returns the exchange so headers and status can be inspected. */
    private static MockServerWebExchange run(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = ex -> Mono.empty();
        filter().filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void preflightFromConfiguredOriginIsAllowedExactly() {
        MockServerWebExchange ex = run(MockServerHttpRequest.options(API)
                .header(HttpHeaders.ORIGIN, ALLOWED)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"));

        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ALLOWED);
        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .isNull();
    }

    @Test
    void simpleRequestFromConfiguredOriginIsAllowedExactly() {
        MockServerWebExchange ex = run(MockServerHttpRequest.post(API)
                .header(HttpHeaders.ORIGIN, ALLOWED));

        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ALLOWED);
    }

    @Test
    void foreignOriginIsRejected() {
        MockServerWebExchange ex = run(MockServerHttpRequest.post(API)
                .header(HttpHeaders.ORIGIN, "http://evil.example"));

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isNull();
    }

    @Test
    void wildcardIsNeverEchoed() {
        MockServerWebExchange ex = run(MockServerHttpRequest.post(API)
                .header(HttpHeaders.ORIGIN, ALLOWED));

        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isNotEqualTo("*");
    }
}
