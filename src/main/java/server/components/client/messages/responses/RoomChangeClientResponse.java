package server.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import lombok.ToString;
import server.core.ParticipantId;
import server.core.RoomId;

@ToString
public class RoomChangeClientResponse {
    public static final String TYPE = "roomchange";

    private final String type;
    @SerializedName("identity")
    private final String participantId;
    @SerializedName("former")
    private final String formerRoomId;
    @SerializedName("roomid")
    private final String currentRoomId;

    public RoomChangeClientResponse(@NonNull ParticipantId participantId, @NonNull RoomId formerRoomId, @NonNull RoomId currentRoomId) {
        this.type = TYPE;
        this.participantId = participantId.getValue();
        this.formerRoomId = formerRoomId.getValue();
        this.currentRoomId = currentRoomId.getValue();
    }

    public RoomChangeClientResponse(@NonNull ParticipantId participantId, @NonNull RoomId currentRoomId) {
        this.type = TYPE;
        this.participantId = participantId.getValue();
        this.formerRoomId = "";
        this.currentRoomId = currentRoomId.getValue();
    }
}
