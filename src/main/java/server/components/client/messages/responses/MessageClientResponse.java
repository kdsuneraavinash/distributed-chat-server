package server.components.client.messages.responses;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageClientResponse {
    private final String type = "message";
    private final String identity;
    private final String content;
}
