package com.chat.controller;

import com.chat.model.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
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
                .map(msg -> new ChatMessageDto(msg.getType(), msg.getContent(), msg.getSender()))
                .collect(Collectors.toList());
    }

    @MessageMapping("/chat/{roomId}/sendMessage")
    @SendTo("/topic/rooms/{roomId}")
    @Transactional
    public ChatMessageDto sendMessage(@DestinationVariable String roomId, @Payload ChatMessageDto chatMessageDto, SimpMessageHeaderAccessor headerAccessor) {
        if (chatMessageDto.getType() == ChatMessage.MessageType.CHAT) {
            ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            String username = (String) headerAccessor.getSessionAttributes().get("username");

            ChatMessage chatMessage = ChatMessage.builder()
                    .type(chatMessageDto.getType())
                    .content(chatMessageDto.getContent())
                    .sender(username)
                    .chatRoom(chatRoom)
                    .build();

            chatMessageRepository.save(chatMessage);
            chatMessageDto.setSender(username);
        }
        return chatMessageDto;
    }

    @MessageMapping("/chat/{roomId}/addUser")
    @SendTo("/topic/rooms/{roomId}")
    @Transactional
    public ChatMessageDto addUser(@DestinationVariable String roomId, @Payload ChatMessageDto chatMessageDto,
                                  SimpMessageHeaderAccessor headerAccessor, Authentication authentication) {
        chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        String username = authentication.getName();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        chatMessageDto.setSender(username);
        return chatMessageDto;
    }
}