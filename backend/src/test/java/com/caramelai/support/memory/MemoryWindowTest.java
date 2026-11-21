package com.caramelai.support.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.caramelai.support.chat.ChatService;
import com.caramelai.support.chat.TestChats;
import com.caramelai.support.config.ChatProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/** Context is bounded by the configured window (FR-005, constitution Principle III). */
class MemoryWindowTest {

    @Test
    void oldestMessagesAreDroppedAndTheSystemPromptIsNeverStored() {
        int window = 20;
        ChatProperties props = TestChats.props(window, Duration.ofSeconds(2), Duration.ofSeconds(5), 2000);
        ChatModel model = TestChats.mockModel();
        List<Prompt> sent = new ArrayList<>();
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return Flux.just(TestChats.chunk("reply"));
        });
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props);

        for (int turn = 0; turn < 25; turn++) {
            service.stream(TestChats.SESSION, "turn " + turn).blockLast();
        }

        Prompt lastPrompt = sent.get(sent.size() - 1);
        List<Message> nonSystem = lastPrompt.getInstructions().stream()
                .filter(m -> !(m instanceof SystemMessage))
                .toList();
        // The window holds at most `window` prior messages; the current customer message is added on top.
        assertThat(nonSystem.size()).isLessThanOrEqualTo(window + 1);
        assertThat(lastPrompt.getInstructions().stream().filter(m -> m instanceof SystemMessage).count())
                .isEqualTo(1);
        // The earliest turn has fallen out of the window.
        assertThat(nonSystem.stream().anyMatch(m -> m instanceof UserMessage u && "turn 0".equals(u.getText())))
                .isFalse();

        List<Message> stored = repo.findByConversationId(TestChats.SESSION);
        assertThat(stored.stream().noneMatch(m -> m instanceof SystemMessage)).isTrue();
    }
}
