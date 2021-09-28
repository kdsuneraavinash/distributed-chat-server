package lk.ac.mrt.cse.cs4262.common.symbols;


import lombok.NonNull;

/**
 * The ID of a participant in the system.
 * Participants are the clients that are can chat in the system.
 * Participant ID must be unique globally across the system.
 */
public class ParticipantId extends BaseId {
    /**
     * Create a Participant ID. See {@link ParticipantId}.
     *
     * @param value ID value.
     */
    public ParticipantId(@NonNull String value) {
        super(value);
    }
}
