package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.ChatMessageRequest;
import pl.pbs.zwbackend.dto.ChatMessageResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.model.ChatMessage;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.ChatMessageRepository;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        User sender = userRepository.findByEmail(request.getSenderEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getSenderEmail()));

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .content(request.getContent())
                .type(request.getType())
                .sender(sender)
                .project(project)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return convertToResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getProjectMessages(Long projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messages = chatMessageRepository.findByProjectIdOrderByTimestampDesc(projectId, pageable);
        return messages.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getGlobalMessages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messages = chatMessageRepository.findGlobalMessagesOrderByTimestampDesc(pageable);
        return messages.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentProjectMessages(Long projectId, LocalDateTime since) {
        List<ChatMessage> messages = chatMessageRepository.findRecentProjectMessages(projectId, since);
        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentGlobalMessages(LocalDateTime since) {
        List<ChatMessage> messages = chatMessageRepository.findRecentGlobalMessages(since);
        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ChatMessageResponse convertToResponse(ChatMessage message) {
        User sender = message.getSender();
        UserSummaryResponse senderSummary = UserSummaryResponse.builder()
                .id(sender.getId())
                .firstName(sender.getFirstName())
                .lastName(sender.getLastName())
                .email(sender.getEmail())
                .build();

        return ChatMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .sender(senderSummary)
                .projectId(message.getProject() != null ? message.getProject().getId() : null)
                .projectName(message.getProject() != null ? message.getProject().getName() : null)
                .timestamp(message.getTimestamp())
                .build();
    }
}
