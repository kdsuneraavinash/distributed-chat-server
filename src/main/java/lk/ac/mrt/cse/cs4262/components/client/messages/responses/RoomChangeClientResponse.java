package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class RoomChangeClientResponse {
    private final String type = "roomchange";

    @NonNull
    @SerializedName("identity")
    private final ParticipantId participantId;

    @NonNull
    @SerializedName("former")
    private final RoomId formerRoomId;

    @NonNull
    @SerializedName("roomid")
    private final RoomId currentRoomId;
}
