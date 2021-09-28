package lk.ac.mrt.cse.cs4262.components.client;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.common.symbols.ParticipantId;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

@Log4j2
@ToString
public class Client implements AutoCloseable {
    /**
     * ID of the client.
     * This is a unique id for each client session.
     */
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final ClientId clientId;

    /**
     * Socket connection of the client.
     * May close if client disconnects.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Socket socket;

    private Thread thread;

    /**
     * Participant ID if the client is a participant.
     */
    @Setter
    @Getter
    private ParticipantId participantId;

    /**
     * Creates a new client. See {@link Client}.
     *
     * @param socket Socket connection of the client.
     */
    public Client(@NonNull Socket socket) {
        this.socket = socket;
        this.clientId = new ClientId();
    }

    /**
     * Sends a message to the connected client.
     *
     * @param message Message to send.
     */
    public void sendMessage(String message) {
        // No messages sent if disconnected
        // and disconnect if socket already closed.
        if (!isConnected()) {
            return;
        }
        try {
            // Get output stream of socket. (Don't close afterwards)
            OutputStream socketOutputStream = getSocket().getOutputStream();
            PrintWriter printWriter = new PrintWriter(socketOutputStream);
            printWriter.println(message);
            printWriter.flush();
            log.info("Server -> {}: {}", this, message);
        } catch (IOException e) {
            // Sending failed. Disconnect if socket closed. Otherwise ignore.
            log.error("Server -X {}: {}", this, message);
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread.join();
            this.thread = null;
        }
        if (!this.socket.isClosed()) {
            this.socket.close();
        }
        log.info("{} disconnected.", this);
    }

    /**
     * @return Whether the client is still connected. (Socket open)
     */
    public boolean isConnected() {
        return !socket.isClosed();
    }

    /**
     * @return Whether the client is still connected and has acquired an id. (can chat)
     */
    public boolean isParticipating() {
        return !socket.isClosed() && participantId != null;
    }
}
