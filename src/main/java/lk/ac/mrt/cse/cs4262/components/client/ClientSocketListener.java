package lk.ac.mrt.cse.cs4262.components.client;

import lombok.AllArgsConstructor;
import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Listens on a client for any inputs.
 * Will use {@link Reporter} to delegate any messages.
 */
@AllArgsConstructor
public class ClientSocketListener implements Runnable {
    private final Client client;
    private final Reporter reporter;

    @Override
    public void run() {
        reporter.connectClient(client);
        try {
            @Cleanup InputStream inputStream = client.getSocket().getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                // TODO: Process messages on a new thread?
                reporter.receiveRequest(client, inputLine);
            }
        } catch (IOException ignored) {
        } finally {
            reporter.disconnectClient(client);
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
         * @param client Connected client.
         */
        void connectClient(Client client);

        /**
         * Report that a client sent a message.
         *
         * @param client     Client.
         * @param rawRequest Raw message string.
         */
        void receiveRequest(Client client, String rawRequest);

        /**
         * Report that a client disconnected.
         *
         * @param client Client.
         */
        void disconnectClient(Client client);
    }
}
