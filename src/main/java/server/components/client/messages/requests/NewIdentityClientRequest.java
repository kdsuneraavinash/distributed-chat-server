package server.components.client.messages.requests;

import lombok.Getter;

@Getter
public class NewIdentityClientRequest extends BaseClientRequest {
    public static final String TYPE = "newidentity";

    private String identity;
}
