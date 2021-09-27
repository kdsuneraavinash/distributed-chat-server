package server.components.client.messages.requests;

import lombok.Getter;

@Getter
public class MessageClientRequest extends BaseClientRequest {
    public static final String TYPE = "message";

    private String content;
}
