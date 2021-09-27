package server.component.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListener implements Runnable {
    private final Client client;

    public ClientListener(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            try (
                    Socket socket = client.getSocket();
                    InputStream inputStream = socket.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                String inputLine;
                while ((inputLine = bufferedReader.readLine()) != null) {
                    handleRawInput(inputLine);
                }
            }
            System.err.println("Disconnected client: " + client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRawInput(String input) {
        // TODO: Process input and call client methods
        System.out.println(client.toString() + input);
    }
}
