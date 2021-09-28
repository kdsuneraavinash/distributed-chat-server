package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collection;

@ToString
@Builder
public class WhoClientResponse {
    private final String type = "roomcontents";

    @NonNull
    @SerializedName("roomid")
    private final RoomId roomId;

    @NonNull
    @SerializedName("identities")
    private final Collection<ParticipantId> participantIds;

    @NonNull
    @SerializedName("owner")
    private final ParticipantId ownerId;
}
