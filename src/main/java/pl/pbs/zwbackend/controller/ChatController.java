package pl.pbs.zwbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pl.pbs.zwbackend.dto.ChatMessageRequest;
import pl.pbs.zwbackend.dto.ChatMessageResponse;
import pl.pbs.zwbackend.service.ChatService;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage) {
        ChatMessageResponse savedMessage = chatService.saveMessage(chatMessage);
        
        if (chatMessage.getProjectId() != null) {
            messagingTemplate.convertAndSend("/topic/project/" + chatMessage.getProjectId(), savedMessage);
        } else {
            messagingTemplate.convertAndSend("/topic/public", savedMessage);
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageRequest chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderEmail());
        
        ChatMessageResponse savedMessage = chatService.saveMessage(chatMessage);
        
        if (chatMessage.getProjectId() != null) {
            messagingTemplate.convertAndSend("/topic/project/" + chatMessage.getProjectId(), savedMessage);
        } else {
            messagingTemplate.convertAndSend("/topic/public", savedMessage);
        }
    }
}
