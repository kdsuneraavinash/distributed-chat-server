package server.components.client.messages.requests;

import lombok.Getter;

@Getter
public class WhoClientRequest extends BaseClientRequest {
    public static final String TYPE = "who";
}
