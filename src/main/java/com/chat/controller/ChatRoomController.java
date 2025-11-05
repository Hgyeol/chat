package com.chat.controller;

import com.chat.model.ChatRoom;
import com.chat.model.ChatRoomRepository;
import com.chat.user.User;
import com.chat.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Void> createRoom(@RequestBody CreateRoomRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        if (chatRoomRepository.findByRoomId(request.getRoomId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Room already exists
        }
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
        ChatRoom newRoom = new ChatRoom(request.getRoomId(), request.getName(), currentUser);
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
