package pl.pbs.zwbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.MessageRequest;
import pl.pbs.zwbackend.dto.MessageResponse;
import pl.pbs.zwbackend.dto.MessageSendResponse;
import pl.pbs.zwbackend.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    
    private final ChatService chatService;
    
    @PostMapping("/send")
    public ResponseEntity<MessageSendResponse> sendMessage(
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        MessageSendResponse response = chatService.sendMessage(request, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MessageResponse> messages = chatService.getMessages(page, size);
        return ResponseEntity.ok(messages);
    }
}
