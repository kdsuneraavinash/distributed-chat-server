package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
public class MoveJoinValidateResponse {
    private final boolean validated;
}
