package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class MessageBroadcastResponse {
    private final String type = "message";

    @SerializedName("identity")
    private final ParticipantId participantId;

    private final String content;
}
