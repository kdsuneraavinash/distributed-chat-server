package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class CreateRoomClientResponse {
    private final String type = "createroom";

    @NonNull
    @SerializedName("roomid")
    private final RoomId roomId;

    @NonNull
    @JsonAdapter(ToStringSerializer.class)
    private final Boolean approved;
}
