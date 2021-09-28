package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;

@ToString
@AllArgsConstructor
public class MessageClientResponse {
    public static final String TYPE = "message";

    private final String type;
    @SerializedName("identity")
    private final String participantId;
    private final String content;

    public MessageClientResponse(ParticipantId participantId, String content) {
        this.type = TYPE;
        this.participantId = participantId.getValue();
        this.content = content;
    }
}
