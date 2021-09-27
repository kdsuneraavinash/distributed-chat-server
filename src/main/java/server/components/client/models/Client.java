package server.components.client.models;

import com.google.gson.Gson;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import server.core.ClientId;
import server.core.ParticipantId;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

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
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final ClientId clientId;
    @Getter(AccessLevel.PROTECTED)
    private final Socket socket;
    @Setter
    @Getter
    private ParticipantId participantId;
    private Thread thread;

    public Client(@NonNull Socket socket) {
        this.socket = socket;
        // TODO: Better unique ID?
        this.clientId = new ClientId(UUID.randomUUID().toString());
    }

    public void startListening(ClientListener.EventHandler eventHandler, Gson serializer) {
        ClientListener clientListener = new ClientListener(this, eventHandler, serializer);
        this.thread = new Thread(clientListener);
        this.thread.start();
    }

    public void sendMessage(String message) {
        try {
            OutputStream socketOutputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(socketOutputStream);
            printWriter.println(message);
            printWriter.flush();
            log.info("{} <- {}", clientId, message);
        } catch (IOException e) {
            e.printStackTrace();
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
