package lk.ac.mrt.cse.cs4262.components.client.chat.events;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.RoomId;
import lk.ac.mrt.cse.cs4262.components.client.chat.MessageSender;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

@Log4j2
@AllArgsConstructor
public abstract class AbstractEventHandler {
    @Nullable
    private MessageSender messageSender;

    /**
     * Attach a message sender to this event handler.
     *
     * @param newMessageSender Message Sender.
     */
    public void attachMessageSender(MessageSender newMessageSender) {
        this.messageSender = newMessageSender;
    }

    protected void sendToClient(ClientId clientId, String message) {
        if (messageSender != null) {
            messageSender.sendToClient(clientId, message);
        }
    }

    protected void sendToRoom(RoomId roomId, String message) {
        log.info("Server >> Room({}): {}", roomId, message);
        if (messageSender != null) {
            messageSender.sendToRoom(roomId, message);
        }
    }

    protected void sendToRoom(RoomId roomId, String message, ClientId excludeClientId) {
        log.info("Server >> Room({}) exc Client({}): {}", roomId, excludeClientId, message);
        if (messageSender != null) {
            messageSender.sendToRoom(roomId, message, excludeClientId);
        }
    }

    protected void disconnectClient(ClientId clientId) {
        if (messageSender != null) {
            messageSender.disconnect(clientId);
        }
    }
}