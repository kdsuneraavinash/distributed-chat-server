package lk.ac.mrt.cse.cs4262.common.symbols;


import lombok.NonNull;

/**
 * The ID of a server in the system.
 * Servers are the system connected servers.
 * Server ID must be unique globally across the system.
 * Each server has a unique ID and all the servers are known at the start.
 */
public class ServerId extends BaseId {
    /**
     * Create a Server ID. See {@link ServerId}.
     *
     * @param value ID value.
     */
    public ServerId(@NonNull String value) {
        super(value);
    }
}
