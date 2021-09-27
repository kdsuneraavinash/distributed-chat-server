package server.components.client.models;

import com.google.gson.Gson;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import server.components.client.messages.requests.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Listens on a client for any inputs.
 * Will use {@link EventHandler} to delegate any messages.
 */
@Log4j2
public class ClientListener implements Runnable {
    private final EventHandler eventHandler;
    private final Client client;
    private final Gson serializer;

    public ClientListener(@NonNull Client client, @NonNull EventHandler eventHandler, @NonNull Gson serializer) {
        this.client = client;
        this.eventHandler = eventHandler;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        eventHandler.connectClient(client);
        try {
            @Cleanup InputStream inputStream = client.getSocket().getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                // TODO: Process messages on a new thread?
                receiveMessage(inputLine);
            }
        } catch (IOException ignored) {
        } finally {
            eventHandler.disconnectClient(client);
        }
    }

    void receiveMessage(String message) {
        log.info("{} -> {}", client.getClientId(), message);
        BaseClientRequest request = serializer.fromJson(message, BaseClientRequest.class);
        if (request instanceof NewIdentityClientRequest) {
            NewIdentityClientRequest castedRequest = (NewIdentityClientRequest) request;
            eventHandler.newIdentityRequest(client, castedRequest.getIdentity());
        } else if (request instanceof ListClientRequest) {
            eventHandler.chatRoomListRequest(client);
        } else if (request instanceof MessageClientRequest) {
            MessageClientRequest castedRequest = (MessageClientRequest) request;
            eventHandler.messageRequest(client, castedRequest.getContent());
        } else if (request instanceof WhoClientRequest) {
            eventHandler.whoRequest(client);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Event handler interface for handling any inputs/events from the client.
     * Note that, these events are fired only on actions from client side.
     * For example, disconnecting on server side will not fire this.
     */
    public interface EventHandler {
        void connectClient(Client client);

        void disconnectClient(Client client);

        void newIdentityRequest(Client client, String identity);

        void chatRoomListRequest(Client client);

        void messageRequest(Client client, String content);

        void whoRequest(Client client);
    }
}
