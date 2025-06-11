package pl.pbs.zwbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WebSocket connection and usage information for chat functionality")
public class WebSocketInfoResponse {
    
    @Schema(description = "WebSocket connection URL", example = "/ws")
    private String connectionUrl;
    
    @Schema(description = "WebSocket connection URL with SockJS fallback", example = "/ws")
    private String connectionUrlWithSockJS;
    
    @Schema(description = "Description of the WebSocket functionality")
    private String description;
    
    @Schema(description = "Available topics to subscribe to")
    private List<TopicInfo> topics;
    
    @Schema(description = "Available message destinations to send to")
    private List<MessageDestination> messageDestinations;
    
    @Schema(description = "Usage instructions")
    private List<String> usage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about WebSocket topics")
    public static class TopicInfo {
        
        @Schema(description = "Topic name to subscribe to", example = "/topic/public")
        private String topic;
        
        @Schema(description = "Description of what this topic provides")
        private String description;
        
        @Schema(description = "Example of the topic usage", example = "/topic/project/1")
        private String example;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about WebSocket message destinations")
    public static class MessageDestination {
        
        @Schema(description = "Destination to send messages to", example = "/app/chat.sendMessage")
        private String destination;
        
        @Schema(description = "Description of what this destination does")
        private String description;
        
        @Schema(description = "Example payload to send to this destination")
        private String payloadExample;
    }
}
