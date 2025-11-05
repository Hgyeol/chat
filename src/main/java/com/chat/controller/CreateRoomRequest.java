package com.chat.controller;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String roomId;
    private String name;

    public String getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }
}
