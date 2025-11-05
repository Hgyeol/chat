package com.chat.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private ChatMessage.MessageType type;
    private String content;
    private String sender;
}
