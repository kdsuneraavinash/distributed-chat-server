package lk.ac.mrt.cse.cs4262.components.client.connector;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lombok.NonNull;

/**
 * Client base interface.
 */
public interface ChatClient extends AutoCloseable {
    /**
     * @return Get the client ID. This is unique for each client.
     */
    @NonNull ClientId getClientId();

    /**
     * Sends a message to this client.
     * Message will not be sent if the socket is closed.
     *
     * @param message The string message to send to client.
     */
    void sendMessage(@NonNull String message);
}
