package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.ChatHistoryPageResponse;
import pl.pbs.zwbackend.dto.ChatHistoryRequest;
import pl.pbs.zwbackend.service.ChatHistoryService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @GetMapping("/history")
    public ResponseEntity<ChatHistoryPageResponse> getChatHistory(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String senderEmail,
            @RequestParam(required = false) String searchKeyword) {

        ChatHistoryRequest request = ChatHistoryRequest.builder()
                .offset(offset)
                .limit(Math.min(limit, 100)) // Ensure limit doesn't exceed 100
                .fromDate(fromDate)
                .toDate(toDate)
                .senderEmail(senderEmail)
                .searchKeyword(searchKeyword)
                .build();

        ChatHistoryPageResponse response = chatHistoryService.getChatHistory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public ResponseEntity<ChatHistoryPageResponse> getChatHistoryWithBody(
            @Valid @RequestBody ChatHistoryRequest request) {
        ChatHistoryPageResponse response = chatHistoryService.getChatHistory(request);
        return ResponseEntity.ok(response);
    }
}
