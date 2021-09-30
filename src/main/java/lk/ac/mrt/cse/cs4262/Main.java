package lk.ac.mrt.cse.cs4262;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.utils.FileUtils;
import lombok.Cleanup;
import org.checkerframework.checker.nullness.qual.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "chatserver", description = "Run Chat Server Application")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-s", "--serverid"}, description = "Name of the server", required = true)
    @Nullable
    private String serverId;

    @CommandLine.Option(names = {"-f", "--servers_conf"}, description = "Server configuration file", required = true)
    @Nullable
    private File serversConf;

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
        @SuppressWarnings("nullness")
        ServerId currentServerId = new ServerId(serverId);
        @SuppressWarnings("nullness")
        Path configurationFilePath = Objects.requireNonNull(serversConf).toPath();

        // Start server on the specified configuration
        FileUtils.readResource("header.txt").ifPresent(System.out::println);
        ServerConfiguration serverConfiguration = ServerConfiguration.fromPath(configurationFilePath);
        @Cleanup ChatServer chatServer = new ChatServer(currentServerId, serverConfiguration);
        chatServer.startListening();
        return 0;
    }
}
