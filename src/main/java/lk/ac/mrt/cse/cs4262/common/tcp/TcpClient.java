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
public class TcpClient {
    public static String request(String ipAddress, int port, String payload) throws IOException {
        @Cleanup Socket socket = new Socket(ipAddress, port);
        @Cleanup PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), false);
        printWriter.write(payload);
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
