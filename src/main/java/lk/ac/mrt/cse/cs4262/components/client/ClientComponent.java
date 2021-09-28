package lk.ac.mrt.cse.cs4262.components.client;

import lk.ac.mrt.cse.cs4262.common.state.SystemState;
import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lk.ac.mrt.cse.cs4262.components.ServerComponent;
import lk.ac.mrt.cse.cs4262.components.client.connector.ChatClient;
import lk.ac.mrt.cse.cs4262.components.client.connector.ChatClientImpl;
import lk.ac.mrt.cse.cs4262.components.client.connector.ClientSocketListener;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent implements ServerComponent {
    private final int port;
    private final ChatConnector chatConnector;

    /**
     * Create a client connector. See {@link ClientComponent}.
     *
     * @param port        Port to listen.
     * @param systemState System read only view.
     */
    public ClientComponent(int port, SystemState systemState) {
        this.port = port;
        this.chatConnector = new ChatConnector(systemState);
        systemState.attachListener(chatConnector);
    }

    @Override
    public void run() {
        try {
            // Listen on client port and connect each new client to the manager.
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                ClientId clientId = ClientId.unique();
                Thread thread = new Thread(new ClientSocketListener(clientId, socket, chatConnector));
                ChatClient client = new ChatClientImpl(clientId, socket, thread);
                chatConnector.addClient(client);
                thread.start();
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", port);
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        chatConnector.close();
    }
}
