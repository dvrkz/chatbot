package com.caramelai.support.memory;

import com.caramelai.support.config.ChatProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/**
 * In-process conversation store. Sessions expire after inactivity and the store is size-capped,
 * so abandoned conversations cannot grow without bound (research R5, data-model lifecycle).
 * Single instance only: state is not shared across nodes.
 */
@Component
public class ExpiringChatMemoryRepository implements ChatMemoryRepository {

    private final Cache<String, List<Message>> sessions;

    public ExpiringChatMemoryRepository(ChatProperties properties) {
        this.sessions = Caffeine.newBuilder()
                .expireAfterAccess(properties.memory().sessionTtl())
                .maximumSize(properties.memory().maxSessions())
                .build();
    }

    @Override
    public List<String> findConversationIds() {
        return new ArrayList<>(sessions.asMap().keySet());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Message> messages = sessions.getIfPresent(conversationId);
        return messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        sessions.put(conversationId, List.copyOf(messages));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        sessions.invalidate(conversationId);
    }
}
