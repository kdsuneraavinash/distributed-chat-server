package lk.ac.mrt.cse.cs4262.components.client.connector;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lombok.AllArgsConstructor;
import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Listens on a client for any inputs.
 * Will use {@link Reporter} to delegate any messages.
 * Closing the socket would be done when thread exits.
 */
@AllArgsConstructor
public class ClientSocketListener implements Runnable {
    private final ClientId clientId;
    private final Socket socket;
    private final Reporter reporter;

    @Override
    public void run() {
        reporter.clientConnected(clientId);
        try {
            @Cleanup InputStream inputStream = socket.getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                reporter.clientRequestReceived(clientId, inputLine);
            }
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            reporter.clientDisconnected(clientId);
        }
    }

    /**
     * Reporter interface for handling any inputs/events from the client.
     * Note that, these events are fired only on actions from client side.
     * For example, disconnecting on server side will not fire this.
     */
    public interface Reporter {
        /**
         * Report that a client connected.
         *
         * @param clientId ID of the connected client.
         */
        void clientConnected(ClientId clientId);

        /**
         * Report that a client sent a message.
         *
         * @param clientId   ID of the Client.
         * @param rawRequest Raw message string.
         */
        void clientRequestReceived(ClientId clientId, String rawRequest);

        /**
         * Report that a client disconnected.
         *
         * @param clientId ID of the Client.
         */
        void clientDisconnected(ClientId clientId);
    }
}
