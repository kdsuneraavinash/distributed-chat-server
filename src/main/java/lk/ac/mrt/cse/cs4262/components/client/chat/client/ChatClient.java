package lk.ac.mrt.cse.cs4262.components.client.chat.client;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;

/**
 * Client base interface.
 */
public interface ChatClient extends AutoCloseable {
    /**
     * @return Get the client ID. This is unique for each client.
     */
    ClientId getClientId();

    /**
     * Sends a message to this client.
     * Message will not be sent if the socket is closed.
     *
     * @param message The string message to send to client.
     */
    void sendMessage(String message);
}
