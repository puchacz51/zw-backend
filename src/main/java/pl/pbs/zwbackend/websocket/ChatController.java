package pl.pbs.zwbackend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import pl.pbs.zwbackend.dto.ChatMessagePayload;
import pl.pbs.zwbackend.dto.MessageRequest;
import pl.pbs.zwbackend.dto.MessageSendResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.service.ChatService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessagePayload sendMessage(@Payload ChatMessagePayload chatMessage, 
                                         SimpMessageHeaderAccessor headerAccessor,
                                         Principal principal) {
        
        // Get authenticated user
        if (principal == null) {
            return ChatMessagePayload.builder()
                    .type("ERROR")
                    .content("User not authenticated")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
        
        String userEmail = principal.getName();
        
        // Save message to database
        MessageRequest messageRequest = MessageRequest.builder()
                .content(chatMessage.getContent())
                .build();
        
        MessageSendResponse response = chatService.sendMessage(messageRequest, userEmail);
          if (response.isSuccess()) {
            UserSummaryResponse sender = response.getData().getSender();
            
            // Return the message with complete sender information
            return ChatMessagePayload.builder()
                    .type("CHAT")
                    .content(chatMessage.getContent())
                    .senderEmail(sender.getEmail())
                    .senderName(sender.getFirstName() + " " + sender.getLastName())
                    .senderId(sender.getId())
                    .senderFirstName(sender.getFirstName())
                    .senderLastName(sender.getLastName())
                    .senderAvatarUrl(sender.getAvatarUrl())
                    .timestamp(response.getData().getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        } else {
            // Return error message
            return ChatMessagePayload.builder()
                    .type("ERROR")
                    .content("Failed to send message")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }
    
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessagePayload addUser(@Payload ChatMessagePayload chatMessage,
                                     SimpMessageHeaderAccessor headerAccessor,
                                     Principal principal) {
          if (principal != null) {
            String userEmail = principal.getName();
            
            try {
                UserSummaryResponse user = chatService.getUserByEmail(userEmail);
                
                return ChatMessagePayload.builder()
                        .type("JOIN")
                        .content(user.getFirstName() + " " + user.getLastName() + " joined the chat!")
                        .senderEmail(userEmail)
                        .senderName(user.getFirstName() + " " + user.getLastName())
                        .senderId(user.getId())
                        .senderFirstName(user.getFirstName())
                        .senderLastName(user.getLastName())
                        .senderAvatarUrl(user.getAvatarUrl())
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build();
            } catch (Exception e) {
                return ChatMessagePayload.builder()
                        .type("ERROR")
                        .content("Failed to get user information")
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build();
            }
        }
        
        return ChatMessagePayload.builder()
                .type("ERROR")
                .content("User not authenticated")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
