package lk.ac.mrt.cse.cs4262.common.symbols;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID of a client.
 * Client is a TCP client application connected to the system.
 * Only needs to be unique for a particular server. (Not globally)
 */
public class ClientId extends BaseId {
    private static final AtomicInteger NUMBER_OF_CLIENTS = new AtomicInteger(0);

    /**
     * Create a unique Client ID. See {@link ClientId}.
     */
    public ClientId() {
        super(Integer.toString(NUMBER_OF_CLIENTS.getAndIncrement()));
    }
}
