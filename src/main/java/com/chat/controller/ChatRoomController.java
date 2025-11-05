package com.chat.controller;

import com.chat.model.ChatRoom;
import com.chat.model.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    @PostMapping
    public ResponseEntity<Void> createRoom(@RequestBody CreateRoomRequest request) {
        if (chatRoomRepository.findByRoomId(request.getRoomId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Room already exists
        }
        ChatRoom newRoom = new ChatRoom(request.getRoomId(), request.getName());
        chatRoomRepository.save(newRoom);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomDto>> getAllRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        List<ChatRoomDto> roomDtos = rooms.stream()
                .map(room -> new ChatRoomDto(room.getId(), room.getRoomId(), room.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomDtos);
    }
}
