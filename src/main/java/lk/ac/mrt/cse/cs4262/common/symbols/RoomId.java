package lk.ac.mrt.cse.cs4262.common.symbols;


import lombok.NonNull;

/**
 * The ID of a room in the system.
 * Rooms are the chat rooms that users can chat in the system.
 * Room ID must be unique globally across the system.
 */
public class RoomId extends BaseId {
    /**
     * Create a Room ID. See {@link RoomId}.
     *
     * @param value ID value.
     */
    public RoomId(@NonNull String value) {
        super(value);
    }
}
