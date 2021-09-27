package server.components.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import server.components.ServerComponent;
import server.components.client.messages.ClientMessageSerializer;
import server.components.client.messages.requests.BaseClientRequest;
import server.components.client.messages.requests.ListClientRequest;
import server.components.client.messages.requests.MessageClientRequest;
import server.components.client.messages.requests.NewIdentityClientRequest;
import server.components.client.messages.responses.MessageClientResponse;
import server.components.client.messages.responses.NewIdentityClientResponse;
import server.components.client.messages.responses.RoomChangeClientResponse;
import server.components.client.models.Client;
import server.components.client.models.ClientListener;
import server.state.ServerState;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * Server component that handles client requests.
 * Manages the chat messages etc... among clients.
 * Command messages will be proxied to other servers.
 */
@Log4j2
public class ClientComponent extends ServerComponent {
    private final ServerState serverState;
    private final HashSet<Client> clients;
    private final Gson serializer;

    public ClientComponent(int port) {
        super(port);
        this.clients = new HashSet<>();
        this.serverState = new ServerState();
        this.serializer = new GsonBuilder()
                .registerTypeAdapter(BaseClientRequest.class, new ClientMessageSerializer())
                .create();
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
                client.startListening(new ClientSideEventHandler(client), this.serializer);
                clients.add(client);
            }
        } catch (IOException e) {
            log.error("Server socket opening failed on port {}.", getPort());
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        for (Client client : clients) {
            client.close();
        }
    }

    private void sendMessage(Client client, Object message) {
        client.sendMessage(serializer.toJson(message));
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
            log.info("Connected {}. Total {} clients connected.", client, clients.size());
        }

        @Override
        public void disconnect() {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            clients.remove(client);
            log.info("Disconnected {}. Total {} clients connected.", client, clients.size());
        }

        @Override
        public void receiveMessage(NewIdentityClientRequest request) {
            boolean isApproved = serverState.createIdentity(request.getIdentity());
            NewIdentityClientResponse newIdentityClientResponse = new NewIdentityClientResponse(Boolean.toString(isApproved));
            sendMessage(client, newIdentityClientResponse);
            if (isApproved) {
                client.setIdentity(request.getIdentity());
                for (Client otherClient : clients) {
                    RoomChangeClientResponse roomChangeClientResponse = new RoomChangeClientResponse(request.getIdentity(), "", "MainHall-s1");
                    sendMessage(otherClient, roomChangeClientResponse);
                }
            }
        }

        @Override
        public void receiveMessage(ListClientRequest request) {
        }

        @Override
        public void receiveMessage(MessageClientRequest request) {
            MessageClientResponse response = new MessageClientResponse(client.getIdentity(), request.getContent());
            for (Client otherClient : clients) {
                if (otherClient != client) {
                    sendMessage(otherClient, response);
                }
            }
        }
    }
}
