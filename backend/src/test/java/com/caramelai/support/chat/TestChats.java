package com.caramelai.support.chat;

import com.caramelai.support.config.ChatProperties;
import com.caramelai.support.config.ChatProperties.Assistant;
import com.caramelai.support.config.ChatProperties.Cors;
import com.caramelai.support.config.ChatProperties.Limits;
import com.caramelai.support.config.ChatProperties.Memory;
import com.caramelai.support.config.ChatProperties.Support;
import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.model.Generation;

/** Test wiring: a real ChatClient and memory over a mocked model, so the model seam is faked. */
public final class TestChats {

    public static final String SESSION = "3f9b6c1e-5a2d-4e7b-9c1a-0d8e2f4a6b7c";

    private TestChats() {
    }

    public static ChatProperties props(int windowSize, Duration idle, Duration overall, int maxChars) {
        return new ChatProperties(
                new Assistant("You are the Caramel AI tech support assistant."),
                new Support("support@example.com"),
                new Memory(windowSize, Duration.ofMinutes(30), 10_000),
                new Limits(maxChars, overall, idle, 10, 5, false),
                new Cors("http://localhost:5173"));
    }

    public static ChatProperties defaultProps() {
        return props(20, Duration.ofSeconds(15), Duration.ofSeconds(30), 2000);
    }

    public static ExpiringChatMemoryRepository repository(ChatProperties props) {
        return new ExpiringChatMemoryRepository(props);
    }

    public static ChatMemory memory(ExpiringChatMemoryRepository repository, ChatProperties props) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(props.memory().windowSize())
                .build();
    }

    public static ChatClient client(ChatModel model, ChatMemory memory, ChatProperties props) {
        return ChatClient.builder(model)
                .defaultSystem(props.assistant().systemPrompt())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    /** A mocked model whose default options are non-null, as ChatClient requires. */
    public static ChatModel mockModel() {
        ChatModel model = org.mockito.Mockito.mock(ChatModel.class);
        org.mockito.Mockito.when(model.getOptions()).thenReturn(ChatOptions.builder().build());
        return model;
    }

    public static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
