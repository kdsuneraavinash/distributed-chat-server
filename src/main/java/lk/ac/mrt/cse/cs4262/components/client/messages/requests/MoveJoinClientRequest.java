package lk.ac.mrt.cse.cs4262.components.client.messages.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public class MoveJoinClientRequest extends BaseClientRequest {
    @Nullable
    private String former;

    @SerializedName("roomid")
    @Nullable
    private String roomId;

    @Nullable
    private String identity;

}
