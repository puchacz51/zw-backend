package pl.pbs.zwbackend.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of chat message")
public enum MessageType {
    @Schema(description = "Regular chat message")
    CHAT,

    @Schema(description = "User joined the chat")
    JOIN,

    @Schema(description = "User left the chat")
    LEAVE
}
