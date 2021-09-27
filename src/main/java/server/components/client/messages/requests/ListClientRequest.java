package server.components.client.messages.requests;

import lombok.Getter;

@Getter
public class ListClientRequest extends BaseClientRequest {
    public static final String TYPE = "list";
}
