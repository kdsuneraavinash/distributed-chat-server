package server.components.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.java.Log;
import server.components.ServerComponent;
import server.components.client.messages.BaseMessage;
import server.components.client.messages.ListMessage;
import server.components.client.messages.MessageSerializer;
import server.components.client.messages.NewIdentityMessage;
import server.components.client.models.Client;
import server.components.client.models.ClientListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log
public class ClientComponent extends ServerComponent {
    private final HashSet<Client> clients;
    private final Gson gson;

    public ClientComponent(int port) {
        super(port);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(BaseMessage.class, new MessageSerializer());
        this.gson = gsonBuilder.create();
        this.clients = new HashSet<>();
    }

    @Override
    public void run() {
        try {
            // Listen on client port and connect each new client to the manager.
            @Cleanup ServerSocket serverSocket = new ServerSocket(port);
            while (!Thread.currentThread().isInterrupted()) {
                // Create a new client from each socket connection.
                Socket socket = serverSocket.accept();
                Client client = new Client(socket);
                client.startListening(new ClientSideEventHandler(client));
                clients.add(client);
            }
        } catch (IOException e) {
            log.severe("Server socket opening failed on port " + port);
            log.throwing("ServerSocket", "<init>", e);
        }
    }

    @Override
    public void close() throws Exception {
        for (Client client : clients) {
            client.close();
        }
    }

    private BaseMessage parseMessage(String message) {
        return gson.fromJson(message, BaseMessage.class);
    }

    /**
     * A class for event handling for events from client side.
     * This includes client disconnecting or client sending messages.
     */
    private class ClientSideEventHandler implements ClientListener.EventHandler {
        private final Client client;

        ClientSideEventHandler(@NonNull Client client) {
            this.client = client;
        }

        @Override
        public void connect() {
            clients.add(client);
            log.info("Connected " + client);
            log.info("Number of clients " + clients.size());
        }

        @Override
        public void disconnect() {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            clients.remove(client);
            log.info("Disconnected " + client);
            log.info("Number of clients " + clients.size());
        }

        @Override
        public void receiveMessage(String message) {
            BaseMessage baseMessage = parseMessage(message);
            if (baseMessage instanceof NewIdentityMessage) {
                NewIdentityMessage newIdentityMessage = (NewIdentityMessage) baseMessage;
                System.out.println(newIdentityMessage.getIdentity());
            } else if (baseMessage instanceof ListMessage) {
                ListMessage listMessage = (ListMessage) baseMessage;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}
