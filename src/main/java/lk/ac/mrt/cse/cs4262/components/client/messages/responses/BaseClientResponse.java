package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import lombok.Getter;

@Getter
public abstract class BaseClientResponse {
    private final String type;

    protected BaseClientResponse(String type) {
        this.type = type;
    }
}
