package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.MessageRequest;
import pl.pbs.zwbackend.dto.MessageResponse;
import pl.pbs.zwbackend.dto.MessageSendResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.model.Message;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.MessageRepository;
import pl.pbs.zwbackend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    
    @Transactional
    public MessageSendResponse sendMessage(MessageRequest request, String userEmail) {
        try {
            User sender = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
            
            Message message = Message.builder()
                    .content(request.getContent())
                    .sender(sender)
                    .build();
            
            Message savedMessage = messageRepository.save(message);
            MessageResponse messageResponse = convertToResponse(savedMessage);
            
            return MessageSendResponse.builder()
                    .success(true)
                    .message("Message sent successfully")
                    .data(messageResponse)
                    .build();
            
        } catch (Exception e) {
            return MessageSendResponse.builder()
                    .success(false)
                    .message("Failed to send message: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
      @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findAllMessagesWithSenderOrderByTimestampDesc(pageable);
        return messages.map(this::convertToResponse);
    }
    
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserByEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return userService.convertToUserSummaryResponse(user);
    }
    
    private MessageResponse convertToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .sender(userService.convertToUserSummaryResponse(message.getSender()))
                .build();
    }
}
