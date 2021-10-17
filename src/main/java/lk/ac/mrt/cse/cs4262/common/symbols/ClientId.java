package lk.ac.mrt.cse.cs4262.common.symbols;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID of a client.
 * Client is a TCP client application connected to the system.
 * Only needs to be unique for a particular server. (Not globally)
 */
public final class ClientId extends BaseId {
    private static final AtomicInteger NUMBER_OF_CLIENTS = new AtomicInteger(0);

    @Getter
    private final boolean isFakeClient;

    /**
     * Create a Client ID. See {@link ClientId}.
     *
     * @param value        ID value.
     * @param isFakeClient Whether the client is fake. (Mock)
     */
    private ClientId(String value, boolean isFakeClient) {
        super(value);
        this.isFakeClient = isFakeClient;
    }

    /**
     * Create a unique real Client ID. See {@link ClientId}.
     *
     * @return Created client id.
     */
    public static ClientId real() {
        String clientIdValue = Integer.toString(NUMBER_OF_CLIENTS.getAndIncrement());
        return new ClientId(clientIdValue, false);
    }

    /**
     * Create a unique fake Client ID. See {@link ClientId}.
     *
     * @return Created client id.
     */
    public static ClientId fake() {
        String clientIdValue = Integer.toString(NUMBER_OF_CLIENTS.getAndIncrement());
        return new ClientId(clientIdValue, true);
    }
}
