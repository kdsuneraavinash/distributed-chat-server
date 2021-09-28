package lk.ac.mrt.cse.cs4262.common.symbols;

import lombok.NonNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID of a client.
 * Client is a TCP client application connected to the system.
 * Only needs to be unique for a particular server. (Not globally)
 */
public final class ClientId extends BaseId {
    private static final AtomicInteger NUMBER_OF_CLIENTS = new AtomicInteger(0);

    /**
     * Create a Client ID. See {@link ClientId}.
     *
     * @param value ID value.
     */
    private ClientId(@NonNull String value) {
        super(value);
    }

    /**
     * Create a unique Client ID. See {@link ClientId}.
     *
     * @return Created client id.
     */
    public static ClientId unique() {
        String clientIdValue = Integer.toString(NUMBER_OF_CLIENTS.getAndIncrement());
        return new ClientId(clientIdValue);
    }
}
