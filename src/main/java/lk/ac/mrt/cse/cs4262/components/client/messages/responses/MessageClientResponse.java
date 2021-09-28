package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

@ToString
@Builder
public class MessageClientResponse {
    private final String type = "message";

    @NonNull
    @SerializedName("identity")
    private final ParticipantId participantId;

    @NonNull
    private final String content;
}
