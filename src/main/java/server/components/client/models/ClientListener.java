package server.components.client.models;

import com.google.gson.Gson;
import lombok.Cleanup;
import lombok.NonNull;
import server.components.client.messages.requests.BaseClientRequest;
import server.components.client.messages.requests.ListClientRequest;
import server.components.client.messages.requests.MessageClientRequest;
import server.components.client.messages.requests.NewIdentityClientRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Listens on a client for any inputs.
 * Will use {@link EventHandler} to delegate any messages.
 */
public class ClientListener implements Runnable {
    private final EventHandler eventHandler;
    private final Socket socket;
    private final Gson serializer;

    public ClientListener(@NonNull Socket socket, @NonNull EventHandler eventHandler, @NonNull Gson serializer) {
        this.socket = socket;
        this.eventHandler = eventHandler;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        eventHandler.connect();
        try {
            @Cleanup InputStream inputStream = socket.getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                // TODO: Process messages on a new thread?
                receiveMessage(inputLine);
            }
        } catch (IOException ignored) {
        } finally {
            eventHandler.disconnect();
        }
    }

    void receiveMessage(String message) {
        BaseClientRequest request = serializer.fromJson(message, BaseClientRequest.class);
        if (request instanceof NewIdentityClientRequest) {
            eventHandler.receiveMessage((NewIdentityClientRequest) request);
        } else if (request instanceof ListClientRequest) {
            eventHandler.receiveMessage((ListClientRequest) request);
        } else if (request instanceof MessageClientRequest) {
            eventHandler.receiveMessage((MessageClientRequest) request);
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
        void connect();

        void disconnect();

        void receiveMessage(NewIdentityClientRequest request);

        void receiveMessage(ListClientRequest request);

        void receiveMessage(MessageClientRequest request);
    }
}
