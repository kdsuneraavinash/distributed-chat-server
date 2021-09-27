package server.components.client.models;

import lombok.Cleanup;
import lombok.NonNull;

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

    public ClientListener(@NonNull Socket socket, @NonNull EventHandler eventHandler) {
        this.socket = socket;
        this.eventHandler = eventHandler;
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
                eventHandler.receiveMessage(inputLine);
            }
        } catch (IOException ignored) {
        } finally {
            eventHandler.disconnect();
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

        void receiveMessage(String message);
    }
}
