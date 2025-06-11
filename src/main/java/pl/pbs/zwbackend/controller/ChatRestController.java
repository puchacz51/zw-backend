package pl.pbs.zwbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.ChatMessageResponse;
import pl.pbs.zwbackend.dto.WebSocketInfoResponse;
import pl.pbs.zwbackend.service.ChatService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat management endpoints including WebSocket information")
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/websocket-info")
    @Operation(summary = "Get WebSocket connection information", 
               description = "Provides information about WebSocket endpoints, topics, and usage examples for real-time chat")
    @ApiResponse(responseCode = "200", description = "WebSocket information retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = WebSocketInfoResponse.class)))
    public ResponseEntity<WebSocketInfoResponse> getWebSocketInfo() {
        WebSocketInfoResponse info = WebSocketInfoResponse.builder()
                .connectionUrl("/ws")
                .connectionUrlWithSockJS("/ws")
                .description("WebSocket endpoint for real-time chat functionality")
                .topics(List.of(
                    WebSocketInfoResponse.TopicInfo.builder()
                        .topic("/topic/public")
                        .description("Global chat messages for all users")
                        .build(),
                    WebSocketInfoResponse.TopicInfo.builder()
                        .topic("/topic/project/{projectId}")
                        .description("Project-specific chat messages")
                        .example("/topic/project/1")
                        .build()
                ))
                .messageDestinations(List.of(
                    WebSocketInfoResponse.MessageDestination.builder()
                        .destination("/app/chat.sendMessage")
                        .description("Send a chat message")
                        .payloadExample("{\n  \"content\": \"Hello everyone!\",\n  \"projectId\": 1,\n  \"type\": \"CHAT\",\n  \"senderEmail\": \"user@example.com\"\n}")
                        .build(),
                    WebSocketInfoResponse.MessageDestination.builder()
                        .destination("/app/chat.addUser")
                        .description("Add user to chat (join notification)")
                        .payloadExample("{\n  \"content\": \"User joined the chat\",\n  \"projectId\": 1,\n  \"type\": \"JOIN\",\n  \"senderEmail\": \"user@example.com\"\n}")
                        .build()
                ))
                .usage(List.of(
                    "1. Connect to WebSocket at /ws endpoint",
                    "2. Subscribe to /topic/public for global chat or /topic/project/{projectId} for project chat",
                    "3. Send messages to /app/chat.sendMessage",
                    "4. Send join notifications to /app/chat.addUser",
                    "5. All subscribers will receive real-time message updates"
                ))
                .build();
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project chat messages", description = "Retrieve paginated chat messages for a specific project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved messages",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ChatMessageResponse>> getProjectMessages(
            @Parameter(description = "ID of the project", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of messages per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatMessageResponse> messages = chatService.getProjectMessages(projectId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/global")
    @Operation(summary = "Get global chat messages", description = "Retrieve paginated global chat messages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved messages",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ChatMessageResponse>> getGlobalMessages(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of messages per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<ChatMessageResponse> messages = chatService.getGlobalMessages(page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/project/{projectId}/recent")
    @Operation(summary = "Get recent project messages", description = "Retrieve recent chat messages for a project since a specific timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent messages",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "400", description = "Invalid timestamp format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChatMessageResponse>> getRecentProjectMessages(
            @Parameter(description = "ID of the project", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "ISO datetime string (e.g., 2023-12-01T10:00:00)", required = true, example = "2023-12-01T10:00:00")
            @RequestParam String since) {
        LocalDateTime sinceDateTime = LocalDateTime.parse(since);
        List<ChatMessageResponse> messages = chatService.getRecentProjectMessages(projectId, sinceDateTime);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/global/recent")
    @Operation(summary = "Get recent global messages", description = "Retrieve recent global chat messages since a specific timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent messages",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Invalid timestamp format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChatMessageResponse>> getRecentGlobalMessages(
            @Parameter(description = "ISO datetime string (e.g., 2023-12-01T10:00:00)", required = true, example = "2023-12-01T10:00:00")
            @RequestParam String since) {
        LocalDateTime sinceDateTime = LocalDateTime.parse(since);
        List<ChatMessageResponse> messages = chatService.getRecentGlobalMessages(sinceDateTime);
        return ResponseEntity.ok(messages);
    }
}
