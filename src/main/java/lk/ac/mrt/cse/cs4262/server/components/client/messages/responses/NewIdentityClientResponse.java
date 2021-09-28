package lk.ac.mrt.cse.cs4262.server.components.client.messages.responses;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class NewIdentityClientResponse {
    public static final String TYPE = "newidentity";

    private final String type;
    private final String approved;

    public NewIdentityClientResponse(boolean approved) {
        this.type = TYPE;
        this.approved = Boolean.toString(approved);
    }
}
