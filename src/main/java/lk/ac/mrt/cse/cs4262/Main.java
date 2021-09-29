package lk.ac.mrt.cse.cs4262;

import lk.ac.mrt.cse.cs4262.common.utils.FileUtils;
import lombok.Cleanup;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "chatserver", description = "Run Chat Server Application")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, description = "Port to run", defaultValue = "4444")
    private int port;

    /**
     * Main entry point for the chat server application.
     *
     * @param args Command line args for the application.
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Start server on the specified configuration
        FileUtils.readResource("header.txt").ifPresent(System.out::println);
        @Cleanup ChatServer chatServer = new ChatServer(this.port);
        chatServer.startListening();
        return 0;
    }
}
