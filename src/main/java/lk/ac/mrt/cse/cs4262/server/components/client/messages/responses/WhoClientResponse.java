package lk.ac.mrt.cse.cs4262.server.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import lombok.ToString;
import lk.ac.mrt.cse.cs4262.server.core.ParticipantId;
import lk.ac.mrt.cse.cs4262.server.core.RoomId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ToString
public class WhoClientResponse {
    public static final String TYPE = "roomcontents";

    private final String type;
    @SerializedName("roomid")
    private final String roomId;
    @SerializedName("identities")
    private final List<String> participantIds;
    @SerializedName("owner")
    private final String ownerId;

    public WhoClientResponse(@NonNull RoomId roomId, @NonNull Collection<ParticipantId> participantIds, ParticipantId ownerId) {
        this.type = TYPE;
        this.roomId = roomId.getValue();
        this.participantIds = participantIds.stream().map(ParticipantId::getValue).collect(Collectors.toList());
        this.ownerId = (ownerId == null) ? "" : ownerId.getValue();
    }
}
