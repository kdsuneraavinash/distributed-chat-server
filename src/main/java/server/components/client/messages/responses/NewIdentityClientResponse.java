package server.components.client.messages.responses;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NewIdentityClientResponse {
    private final String type = "newidentity";
    private final String approved;
}
