package lk.ac.mrt.cse.cs4262;

import lombok.Cleanup;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import lk.ac.mrt.cse.cs4262.server.ChatServer;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "chatserver", description = "Run Chat Server Application")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, description = "Port to run", defaultValue = "4444")
    private int port;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Start lk.ac.mrt.cse.cs4262.server on the specified configuration
        Configurator.setRootLevel(Level.ALL);
        @Cleanup ChatServer chatServer = new ChatServer(this.port);
        chatServer.startListening();
        return 0;
    }
}
