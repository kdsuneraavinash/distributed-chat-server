package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MoveJoinValidateRequest {

    @SerializedName("participantid")
    private String participantId;

    @SerializedName("roomid")
    private String roomid;
}
