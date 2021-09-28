package lk.ac.mrt.cse.cs4262.components.client.connector;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

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
@Log4j2
@AllArgsConstructor
public class ClientSocketListener implements Runnable {
    private final ClientId clientId;
    private final Socket socket;
    private final Reporter reporter;

    @Override
    public void run() {
        try {
            @Cleanup InputStream inputStream = socket.getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            boolean exitByServer = false;
            while (!exitByServer) {
                String inputLine = bufferedReader.readLine();
                if (inputLine == null) {
                    throw new IOException("Closed connection");
                }
                log.info("Client({}) -> Server: {}", clientId, inputLine);
                exitByServer = reporter.processClientRequest(clientId, inputLine);
            }
        } catch (IOException e) {
            log.error("Client({}) exit unexpectedly.", clientId);
            reporter.clientSideDisconnect(clientId);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Reporter interface for handling any inputs/events from the client.
     * Note that, these events are fired only on actions from client side.
     * For example, disconnecting on server side will not fire this.
     */
    public interface Reporter {
        /**
         * Report that a client sent a message.
         *
         * @param clientId   ID of the Client.
         * @param rawRequest Raw message string.
         * @return Whether to exit reporting. (Closes connection if true)
         */
        boolean processClientRequest(ClientId clientId, String rawRequest);

        /**
         * Report that client exited unexpectedly.
         *
         * @param clientId Exited client.
         */
        void clientSideDisconnect(ClientId clientId);
    }
}
