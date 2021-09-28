package lk.ac.mrt.cse.cs4262.server.components.client.messages.responses;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lk.ac.mrt.cse.cs4262.server.core.RoomId;

@ToString
@AllArgsConstructor
public class CreateRoomClientResponse {
    public static final String TYPE = "createroom";

    private final String type;
    private final String approved;
    @SerializedName("roomid")
    private String roomId;

    public CreateRoomClientResponse(@NonNull RoomId roomId, boolean approved) {
        this.type = TYPE;
        this.roomId = roomId.getValue();
        this.approved = Boolean.toString(approved);
    }
}
