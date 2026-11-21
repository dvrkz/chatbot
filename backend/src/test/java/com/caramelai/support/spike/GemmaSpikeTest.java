package com.caramelai.support.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * GATE (plan.md, research R1 and R4). Makes live calls to Google AI Studio, so it runs only when
 * GOOGLE_API_KEY is set and is tagged "live" (excluded from the default test run).
 *
 * Records three facts: (a) the model ID resolves, (b) the system instruction is honored,
 * (c) time to first streamed chunk. Results are written into research.md R1 and R4.
 */
@Tag("live")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class GemmaSpikeTest {

    @Autowired
    ChatModel chatModel;

    @Test
    void modelIdResolvesAndAnswers() {
        String reply = chatModel.call(new Prompt(List.of(new UserMessage("Reply with the word OK."))))
                .getResult().getOutput().getText();
        assertThat(reply).isNotBlank();
        System.out.println("SPIKE (a) model reply: " + reply);
    }

    @Test
    void systemInstructionIsHonored() {
        List<Message> messages = List.of(
                new SystemMessage("Reply only with the single word PONG, no other text."),
                new UserMessage("What is the capital of France?"));
        String reply = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
        System.out.println("SPIKE (b) system-instruction reply: " + reply);
        // Record only. The result decides whether ChatClientConfig needs the R1 fallback.
    }

    @Test
    void timeToFirstChunk() {
        AtomicLong start = new AtomicLong(System.nanoTime());
        Long firstChunkMs = chatModel.stream(new Prompt(new UserMessage("Name one LLM inference provider.")))
                .next()
                .map(r -> Duration.ofNanos(System.nanoTime() - start.get()).toMillis())
                .block(Duration.ofSeconds(60));
        System.out.println("SPIKE (c) time to first chunk ms: " + firstChunkMs);
        assertThat(firstChunkMs).isNotNull();
    }
}
