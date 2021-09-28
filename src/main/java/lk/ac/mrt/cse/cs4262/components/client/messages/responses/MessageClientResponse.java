package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class MessageClientResponse {
    private final String type = "message";

    @NonNull
    @SerializedName("identity")
    private final ParticipantId participantId;

    @NonNull
    private final String content;
}
