package server.component.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
public class ClientComponent implements Runnable {
    private final int port;
    private final ClientManager clientManager;

    public ClientComponent(int port, ClientManager clientManager) {
        this.port = port;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        // Listen on client port and connect each new client to the manager.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                clientManager.connect(new Client(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
