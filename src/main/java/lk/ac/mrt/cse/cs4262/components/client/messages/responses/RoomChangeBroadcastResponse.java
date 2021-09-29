package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class RoomChangeBroadcastResponse {
    private final String type = "roomchange";

    @SerializedName("identity")
    private final ParticipantId participantId;

    @SerializedName("former")
    private final RoomId formerRoomId;

    @SerializedName("roomid")
    private final RoomId currentRoomId;
}
