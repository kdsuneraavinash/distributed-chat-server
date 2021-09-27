package server.components.client.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.java.Log;

import java.net.Socket;
import java.util.UUID;

/**
 * Client model class encapsulating client connection information,
 * thread, identity and other state information.
 * Each client will get a unique client ID which will be used
 * for equivalence checks.
 */
@Log
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Client implements AutoCloseable {
    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    private final String clientId;
    private final Socket socket;
    private Thread thread;

    public Client(@NonNull Socket socket) {
        this.socket = socket;
        // TODO: Better unique ID?
        this.clientId = UUID.randomUUID().toString();
    }

    public void startListening(ClientListener.EventHandler eventHandler) {
        ClientListener clientListener = new ClientListener(socket, eventHandler);
        this.thread = new Thread(clientListener);
        this.thread.start();
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
