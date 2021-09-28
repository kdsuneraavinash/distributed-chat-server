package lk.ac.mrt.cse.cs4262.components.client.messages.responses;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

@ToString
@Builder
public class DeleteRoomClientResponse {
    private final String type = "deleteroom";

    @NonNull
    @SerializedName("roomid")
    private final RoomId roomId;

    @NonNull
    @JsonAdapter(ToStringSerializer.class)
    private final Boolean approved;
}
