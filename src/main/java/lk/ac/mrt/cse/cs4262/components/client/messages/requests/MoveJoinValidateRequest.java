package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MoveJoinValidateRequest {

    @SerializedName("participantid")
    private String participantId;

    @SerializedName("roomid")
    private String formerRoomId;
}
