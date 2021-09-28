package lk.ac.mrt.cse.cs4262.components.client.connector;

import lk.ac.mrt.cse.cs4262.common.symbols.ClientId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A client that is connected to the current system.
 */
@Log4j2
@ToString
@AllArgsConstructor
public class ChatClientImpl implements ChatClient {
    @NonNull
    @Getter
    private final ClientId clientId;

    /**
     * Socket connection of the client.May close if client disconnects.
     * The lifetime of thread depends on the socket connection.
     * If socket closes, thread will also exit.
     */
    @NonNull
    @Getter(AccessLevel.PROTECTED)
    private final Socket socket;

    @NonNull
    private final Thread thread;

    @Override
    public void sendMessage(@NonNull String message) {
        // No messages sent if disconnected
        // and disconnect if socket already closed.
        if (socket.isClosed()) {
            return;
        }
        try {
            // Get output stream of socket. (Don't close afterwards)
            OutputStream socketOutputStream = getSocket().getOutputStream();
            PrintWriter printWriter = new PrintWriter(socketOutputStream, false, StandardCharsets.UTF_8);
            printWriter.println(message);
            printWriter.flush();
            log.info("Server -> {}: {}", this, message);
        } catch (IOException e) {
            // Sending failed. Disconnect if socket closed. Otherwise ignore.
            log.error("Server -X {}: {}", this, message);
            log.throwing(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (!socket.isClosed()) {
            socket.close();
        }
        thread.interrupt();
        thread.join();
    }
}