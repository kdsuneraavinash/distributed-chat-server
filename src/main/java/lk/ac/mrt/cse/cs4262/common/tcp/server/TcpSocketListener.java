package lk.ac.mrt.cse.cs4262.common.tcp.server;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A TCP socket listener that listens for a socket.
 * The first input is taken as the input.
 * After sending the output, socket is closed.
 */
@Log4j2
@AllArgsConstructor
public class TcpSocketListener implements Runnable {
    private final Socket socket;
    private final TcpRequestHandler eventHandler;

    @Override
    public void run() {
        String inetAddress = String.valueOf(socket.getInetAddress());
        try {
            @Cleanup InputStream inputStream = socket.getInputStream();
            @Cleanup InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String request = bufferedReader.readLine();
            if (request != null) {
                log.trace("request <- {}: {}", inetAddress, request);
                String response = eventHandler.handleRequest(request);
                @Cleanup OutputStream socketOutputStream = socket.getOutputStream();
                @Cleanup PrintWriter printWriter = new PrintWriter(socketOutputStream, false);
                printWriter.println(response);
                printWriter.flush();
                log.trace("response -> {}: {}", inetAddress, response);
            }
        } catch (IOException e) {
            log.error("failed -X {}", inetAddress);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
