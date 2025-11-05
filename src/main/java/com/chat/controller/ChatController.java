package com.chat.controller;

import com.chat.model.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatController(RedisTemplate<String, Object> redisTemplate, ChatMessageRepository chatMessageRepository, ChatRoomRepository chatRoomRepository) {
        this.redisTemplate = redisTemplate;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    @GetMapping("/api/chat/{roomId}/messages")
    public List<ChatMessageDto> getChatHistory(@PathVariable String roomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomRoomId(roomId);
        return messages.stream()
                .map(msg -> new ChatMessageDto (msg.getType(), msg.getContent(), msg.getSender()))
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat/{roomId}/sendMessage")
    @SendTo("/topic/rooms/{roomId}")
    @Transactional
    public ChatMessageDto sendMessage(@DestinationVariable String roomId, @Payload ChatMessageDto chatMessageDto) {
        if (chatMessageDto.getType() == ChatMessage.MessageType.CHAT) {
            ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            ChatMessage chatMessage = ChatMessage.builder()
                    .type(chatMessageDto.getType())
                    .content(chatMessageDto.getContent())
                    .sender(chatMessageDto.getSender())
                    .chatRoom(chatRoom)
                    .build();

            chatMessageRepository.save(chatMessage);
        }
        return chatMessageDto;
    }

    @MessageMapping("/chat/{roomId}/addUser")
    @SendTo("/topic/rooms/{roomId}")
    @Transactional
    public ChatMessageDto addUser(@DestinationVariable String roomId, @Payload ChatMessageDto chatMessageDto,
                                  SimpMessageHeaderAccessor headerAccessor) {
        // Find the chat room or throw an exception if not found
        chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        // Store session information in Redis
        String sessionId = headerAccessor.getSessionId();
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("username", chatMessageDto.getSender());
        sessionData.put("roomId", roomId);
        redisTemplate.opsForHash().putAll(sessionId, sessionData);

        return chatMessageDto;
    }
}