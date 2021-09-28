package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import lombok.Getter;

@Getter
public class NewIdentityClientRequest extends BaseClientRequest {
    public static final String TYPE = "newidentity";

    private String identity;
}
