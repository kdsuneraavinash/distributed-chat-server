package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class CreateRoomClientRequest extends BaseClientRequest {
    public static final String TYPE = "createroom";

    @SerializedName("roomid")
    private String roomId;
}
