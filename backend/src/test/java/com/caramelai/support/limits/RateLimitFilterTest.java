package com.caramelai.support.limits;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.config.ChatProperties.Assistant;
import com.caramelai.support.config.ChatProperties.Cors;
import com.caramelai.support.config.ChatProperties.Limits;
import com.caramelai.support.config.ChatProperties.Memory;
import com.caramelai.support.config.ChatProperties.Support;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class RateLimitFilterTest {

    private static RateLimitFilter filter(boolean trustForwarded) {
        ChatProperties props = new ChatProperties(
                new Assistant("You are the Caramel AI tech support assistant."),
                new Support("support@example.com"),
                new Memory(20, Duration.ofMinutes(30), 10_000),
                // 10 per minute with a burst of 5, as in application.yml
                new Limits(2000, Duration.ofSeconds(30), Duration.ofSeconds(15), 10, 5, trustForwarded),
                new Cors("http://localhost:5173"));
        return new RateLimitFilter(props);
    }

    private static MockServerWebExchange chat(String remoteIp) {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/chat/stream")
                .remoteAddress(new InetSocketAddress(remoteIp, 50000)));
    }

    private static int run(RateLimitFilter filter, MockServerWebExchange exchange, AtomicInteger passed) {
        WebFilterChain chain = ex -> {
            passed.incrementAndGet();
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        return exchange.getResponse().getStatusCode() == null
                ? 200
                : exchange.getResponse().getStatusCode().value();
    }

    @Test
    void sixthRapidRequestFromOneClientGets429WithRetryAfter() {
        RateLimitFilter filter = filter(false);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            assertThat(run(filter, chat("10.0.0.1"), passed)).isEqualTo(200);
        }
        MockServerWebExchange sixth = chat("10.0.0.1");
        assertThat(run(filter, sixth, passed)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(sixth.getResponse().getHeaders().getFirst("Retry-After")).isNotNull();
        assertThat(passed.get()).isEqualTo(5);
    }

    @Test
    void limitsAreSeparatePerClient() {
        RateLimitFilter filter = filter(false);
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            run(filter, chat("10.0.0.1"), passed);
        }
        assertThat(run(filter, chat("10.0.0.2"), passed)).isEqualTo(200);
    }

    @Test
    void healthCheckIsNotLimited() {
        RateLimitFilter filter = filter(false);
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            MockServerWebExchange health = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health"));
            assertThat(run(filter, health, passed)).isEqualTo(200);
        }
        assertThat(passed.get()).isEqualTo(20);
    }

    @Test
    void forwardedHeaderIgnoredUnlessTrusted() {
        RateLimitFilter untrusted = filter(false);
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.post("/api/chat/stream")
                    .header(HttpHeaders.HOST, "x").header("X-Forwarded-For", "203.0.113." + i)
                    .remoteAddress(new InetSocketAddress("10.0.0.9", 1)));
            run(untrusted, ex, passed);
        }
        // All five share the real remote address, so a sixth from it is limited.
        assertThat(run(untrusted, chat("10.0.0.9"), passed)).isEqualTo(429);
    }
}
