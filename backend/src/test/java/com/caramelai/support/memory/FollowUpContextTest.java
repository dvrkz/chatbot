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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/** A follow-up sees earlier turns of its own conversation only (FR-004). */
class FollowUpContextTest {

    private static final String OTHER = "11111111-2222-4333-8444-555555555555";

    @Test
    void followUpIncludesTheEarlierTurnAndOtherSessionsSeeNothingOfIt() {
        ChatProperties props = TestChats.defaultProps();
        ChatModel model = TestChats.mockModel();
        List<Prompt> sent = new ArrayList<>();
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return Flux.just(TestChats.chunk("Answer one"));
        });
        ExpiringChatMemoryRepository repo = TestChats.repository(props);
        ChatService service = new ChatService(TestChats.client(model, TestChats.memory(repo, props), props),
                repo, props);

        service.stream(TestChats.SESSION, "Question about 429").blockLast();
        service.stream(TestChats.SESSION, "Tell me more").blockLast();
        service.stream(OTHER, "A fresh question").blockLast();

        List<Message> followUp = sent.get(1).getInstructions();
        assertThat(followUp.stream().anyMatch(m -> m instanceof UserMessage u
                && "Question about 429".equals(u.getText()))).isTrue();
        assertThat(followUp.stream().anyMatch(m -> m instanceof AssistantMessage a
                && "Answer one".equals(a.getText()))).isTrue();

        List<Message> otherSession = sent.get(2).getInstructions();
        assertThat(otherSession.stream().anyMatch(m -> m.getText() != null
                && m.getText().contains("Question about 429"))).isFalse();

        // Completed turns are stored; a stored assistant reply follows its user message.
        assertThat(repo.findByConversationId(TestChats.SESSION)).hasSize(4);
    }
}
