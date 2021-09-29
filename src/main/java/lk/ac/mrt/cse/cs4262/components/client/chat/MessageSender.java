package lk.ac.mrt.cse.cs4262.components.client.chat;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;

/**
 * The interface of a class that manages client-server connection
 * on server side. Can send messages or disconnect.
 */
public interface MessageSender {
    /**
     * Sends a message to a single client.
     *
     * @param clientId ID of client.
     * @param message  Message to send.
     */
    void sendToClient(ClientId clientId, String message);

    /**
     * Sends a message to all clients in a room.
     *
     * @param roomId  ID of room.
     * @param message Message to send.
     */
    void sendToRoom(RoomId roomId, String message);

    /**
     * Sends a message to all clients in a room except for one.
     *
     * @param roomId          ID of room.
     * @param message         Message to send.
     * @param excludeClientId Client to exclude.
     */
    void sendToRoom(RoomId roomId, String message, ClientId excludeClientId);

    /**
     * Disconnects a client from server side.
     *
     * @param clientId ID of client.
     */
    void disconnect(ClientId clientId);
}
