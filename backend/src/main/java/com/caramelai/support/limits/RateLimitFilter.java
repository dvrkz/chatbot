package com.caramelai.support.limits;

import com.caramelai.support.config.ChatProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Per-client request limit on the chat and session-clear endpoints (FR-013, provisional).
 * Buckets live in a size-capped, expiring cache so client keys cannot grow without bound.
 * Forwarded headers are trusted only when caramel.limits.trust-forwarded-headers is true.
 */
@Component
public class RateLimitFilter implements WebFilter {

    static final String CHAT_PATH = "/api/chat/stream";
    static final String SESSION_PATH_PREFIX = "/api/chat/sessions/";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ChatProperties.Limits limits;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public RateLimitFilter(ChatProperties properties) {
        this.limits = properties.limits();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!isLimited(request)) {
            return chain.filter(exchange);
        }
        Bucket bucket = buckets.get(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return chain.filter(exchange);
        }
        return reject(exchange.getResponse(), probe);
    }

    static boolean isLimited(ServerHttpRequest request) {
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();
        return (method == HttpMethod.POST && CHAT_PATH.equals(path))
                || (method == HttpMethod.DELETE && path.startsWith(SESSION_PATH_PREFIX));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limits.burst())
                        .refillGreedy(limits.requestsPerMinute(), WINDOW)
                        .build())
                .build();
    }

    String clientKey(ServerHttpRequest request) {
        if (limits.trustForwardedHeaders()) {
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        InetSocketAddress address = request.getRemoteAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        return address.getAddress().getHostAddress();
    }

    private static Mono<Void> reject(ServerHttpResponse response, ConsumptionProbe probe) {
        long retryAfterSeconds = Math.max(1L,
                (long) Math.ceil(Duration.ofNanos(probe.getNanosToWaitForRefill()).toMillis() / 1000.0));
        response.setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        byte[] body = "{\"status\":429,\"title\":\"Too many requests\",\"code\":\"RATE_LIMITED\"}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
