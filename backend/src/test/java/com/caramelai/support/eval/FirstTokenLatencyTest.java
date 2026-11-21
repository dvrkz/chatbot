package com.caramelai.support.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.chat.ChatService;
import com.caramelai.support.chat.dto.StreamEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * SC-001 and research R16: 20 concurrent conversations on one instance; 90% should show the first
 * streamed text within 3 seconds. Live (calls the real model), so it runs only in the live-eval profile.
 */
@Tag("live")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class FirstTokenLatencyTest {

    private static final int CONVERSATIONS = 20;

    @Autowired
    ChatService chatService;

    @Test
    void ninetyPercentShowFirstTextWithinThreeSeconds() {
        List<CompletableFuture<Long>> timings = new ArrayList<>();
        for (int i = 0; i < CONVERSATIONS; i++) {
            String session = UUID.randomUUID().toString();
            timings.add(CompletableFuture.supplyAsync(() -> {
                long start = System.nanoTime();
                chatService.stream(session, "Why am I getting HTTP 429 errors?")
                        .filter(e -> e instanceof StreamEvent.Token)
                        .next()
                        .block(Duration.ofSeconds(60));
                return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            }));
        }
        List<Long> millis = timings.stream().map(CompletableFuture::join).sorted().toList();
        long within = millis.stream().filter(ms -> ms <= 3000).count();
        System.out.println("LATENCY first-text ms: " + millis);
        System.out.println("LATENCY within 3s: " + within + "/" + CONVERSATIONS);
        assertThat(within * 100 / CONVERSATIONS).as("share of first-text times under 3 s").isGreaterThanOrEqualTo(90);
    }
}
