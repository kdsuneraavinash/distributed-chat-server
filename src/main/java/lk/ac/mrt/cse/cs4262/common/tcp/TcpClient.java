package lk.ac.mrt.cse.cs4262.common.tcp;


import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Simple TCP client that sends a request and accepts the response.
 * Socket is closed after responding.
 */
public final class TcpClient {
    private TcpClient() {
    }

    /**
     * Send a request to a server using TCP protocol.
     * No timeout will be in effect.
     *
     * @param ipAddress IP address of server.
     * @param port      Port of server.
     * @param payload   Payload to send.
     * @return Response from server.
     * @throws IOException If connection failed.
     */
    public static String request(String ipAddress, int port, String payload) throws IOException {
        return TcpClient.request(ipAddress, port, payload, 0);
    }

    /**
     * Send a request to a server using TCP protocol.
     *
     * @param ipAddress IP address of server.
     * @param port      Port of server.
     * @param payload   Payload to send.
     * @param timeout   Payload to send. (milliseconds)
     * @return Response from server.
     * @throws IOException If connection failed.
     */
    public static String request(String ipAddress, int port, String payload, int timeout) throws IOException {
        @Cleanup Socket socket = new Socket(ipAddress, port);
        socket.setSoTimeout(timeout);
        @Cleanup PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.println(payload);
        printWriter.flush();
        @Cleanup InputStreamReader reader = new InputStreamReader(socket.getInputStream());
        @Cleanup BufferedReader bufferedReader = new BufferedReader(reader);
        String response = bufferedReader.readLine();
        if (response == null) {
            throw new IOException("connection closed");
        }
        return response;
    }
}
