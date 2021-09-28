package lk.ac.mrt.cse.cs4262.components.client.models;

import com.google.gson.Gson;
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

/**
 * Client model class encapsulating client connection information,
 * thread, identity and other state information.
 * Each client will get a unique client ID which will be used
 * for equivalence checks.
 */
@Log4j2
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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

    /**
     * Participant ID if the client is a participant.
     */
    @Setter
    @Getter
    private ParticipantId participantId;

    /**
     * Thread that is handling client requests.
     */
    private Thread thread;

    /**
     * Creates a new client. See {@link Client}.
     *
     * @param socket Socket connection of the client.
     */
    public Client(@NonNull Socket socket) {
        this.socket = socket;
        this.clientId = new ClientId();
    }

    public void startListening(ClientListener.EventHandler eventHandler, Gson serializer) {
        ClientListener clientListener = new ClientListener(this, eventHandler, serializer);
        this.thread = new Thread(clientListener);
        this.thread.start();
    }

    public void sendMessage(String message) {
        if (socket.isClosed()) {
            log.error("{} |<- {}", this, message);
            return;
        }
        try {
            OutputStream socketOutputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(socketOutputStream);
            printWriter.println(message);
            printWriter.flush();
            log.info("{} <- {}", this, message);
        } catch (IOException e) {
            log.error("{} X<- {}", this, message);
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread.join();
        }
        if (!this.socket.isClosed()) {
            this.socket.close();
        }
    }
}
