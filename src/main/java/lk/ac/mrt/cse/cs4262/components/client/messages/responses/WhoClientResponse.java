package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.ToString;

import java.util.Collection;

@ToString
@Builder
public class WhoClientResponse {
    private final String type = "roomcontents";

    @SerializedName("roomid")
    private final RoomId roomId;

    @SerializedName("identities")
    private final Collection<ParticipantId> participantIds;

    @SerializedName("owner")
    private final ParticipantId ownerId;
}
