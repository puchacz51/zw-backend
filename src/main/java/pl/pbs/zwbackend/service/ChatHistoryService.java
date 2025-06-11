package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.ChatHistoryPageResponse;
import pl.pbs.zwbackend.dto.ChatHistoryRequest;
import pl.pbs.zwbackend.dto.ChatHistoryResponse;
import pl.pbs.zwbackend.model.Message;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.MessageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public ChatHistoryPageResponse getChatHistory(ChatHistoryRequest request) {
        int page = request.getOffset() / request.getLimit();
        Pageable pageable = PageRequest.of(page, request.getLimit());

        Page<Message> messagePage;
        
        if (hasFilters(request)) {
            messagePage = messageRepository.findMessagesWithFilters(
                    request.getFromDate(),
                    request.getToDate(),
                    request.getSenderEmail(),
                    request.getSearchKeyword(),
                    pageable
            );
        } else {
            messagePage = messageRepository.findAllWithSenderOrderByTimestampDesc(pageable);
        }

        List<ChatHistoryResponse> messages = messagePage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ChatHistoryPageResponse.builder()
                .messages(messages)
                .totalElements((int) messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .currentPage(messagePage.getNumber())
                .pageSize(messagePage.getSize())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .build();
    }

    private boolean hasFilters(ChatHistoryRequest request) {
        return request.getFromDate() != null || 
               request.getToDate() != null || 
               request.getSenderEmail() != null || 
               request.getSearchKeyword() != null;
    }

    private ChatHistoryResponse convertToResponse(Message message) {
        User sender = message.getSender();
        return ChatHistoryResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderFirstName(sender.getFirstName())
                .senderLastName(sender.getLastName())
                .senderEmail(sender.getEmail())
                .timestamp(message.getTimestamp())
                .build();
    }
}
