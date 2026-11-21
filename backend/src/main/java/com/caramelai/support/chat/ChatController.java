package com.caramelai.support.chat;

import com.caramelai.support.chat.dto.ChatRequest;
import com.caramelai.support.chat.dto.StreamEvent;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Streams one reply as Server-Sent Events. Validation failures return HTTP 400 before any event.
     * Model and timeout failures arrive as a final `error` event.
     */
    @PostMapping(path = "/api/chat/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(request.sessionId(), request.message())
                .map(ChatController::toSse);
    }

    /** Discards a conversation's stored context. Idempotent: 204 whether or not one existed. */
    @DeleteMapping("/api/chat/sessions/{sessionId}")
    public ResponseEntity<Void> clear(@PathVariable String sessionId) {
        chatService.clear(sessionId);
        return ResponseEntity.noContent().build();
    }

    static ServerSentEvent<Object> toSse(StreamEvent event) {
        return ServerSentEvent.<Object>builder(event.payload())
                .event(event.eventName())
                .build();
    }
}
