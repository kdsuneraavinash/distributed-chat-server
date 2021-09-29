package lk.ac.mrt.cse.cs4262.common.symbols;


import com.google.gson.annotations.JsonAdapter;
import lk.ac.mrt.cse.cs4262.common.utils.ToStringSerializer;

/**
 * The ID of a room in the system.
 * Rooms are the chat rooms that users can chat in the system.
 * Room ID must be unique globally across the system.
 */
@JsonAdapter(ToStringSerializer.class)
public class RoomId extends BaseId {
    /**
     * Room ID reserved for null responses.
     */
    public static final RoomId NULL = new RoomId("");

    /**
     * Create a Room ID. See {@link RoomId}.
     *
     * @param value ID value.
     */
    public RoomId(String value) {
        super(value);
    }
}
