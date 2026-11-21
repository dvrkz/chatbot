package com.caramelai.support.config;

import com.caramelai.support.memory.ExpiringChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single LLM seam (constitution Principle III). Model name, temperature, and token limits
 * come from spring.ai.google.genai.* in application.yml, not from code (Principle II).
 *
 * The system prompt uses the system role. If the live spike (GemmaSpikeTest, research R1) shows
 * the model ignores system instructions, switch to the fallback described there.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatMemory chatMemory(ExpiringChatMemoryRepository repository, ChatProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.memory().windowSize())
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, ChatProperties properties) {
        return builder
                .defaultSystem(properties.assistant().systemPrompt())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
