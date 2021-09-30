package lk.ac.mrt.cse.cs4262;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.utils.FileUtils;
import lombok.Data;
import lombok.ToString;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A configuration file containing all other servers and connection info.
 * WIll be created from a TSV file.
 */
@ToString
public final class ServerConfiguration {
    private static final int NUMBER_OF_COLUMNS = 4;
    private static final int SERVER_NAME_COL_INDEX = 0;
    private static final int SERVER_ADDRESS_COL_INDEX = 1;
    private static final int CLIENT_PORT_COL_INDEX = 2;
    private static final int COORD_PORT_COL_INDEX = 3;

    private final Map<ServerId, Info> configuration;

    private ServerConfiguration() {
        this.configuration = new HashMap<>();
    }

    /**
     * Create a configuration map. See {@link ServerConfiguration}.
     * The provided file should be a TSV file with 4 columns.
     * namely serverid, server_address, clients_port, coordination_port.
     * clients_port and coordination_port should be numbers.
     *
     * @param path TSV file path.
     * @return Created configuration.
     * @throws IOException File reading failed.
     */
    public static ServerConfiguration fromPath(Path path) throws IOException {
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        List<List<String>> rawConfiguration = FileUtils.readTsv(path, NUMBER_OF_COLUMNS);
        for (List<String> row : rawConfiguration) {
            ServerId serverId = new ServerId(row.get(SERVER_NAME_COL_INDEX));
            String serverAddress = row.get(SERVER_ADDRESS_COL_INDEX);
            int clientPort = Integer.parseInt(row.get(CLIENT_PORT_COL_INDEX));
            int coordinationPort = Integer.parseInt(row.get(COORD_PORT_COL_INDEX));
            Info info = new Info(serverAddress, clientPort, coordinationPort);
            serverConfiguration.configuration.put(serverId, info);
        }
        return serverConfiguration;
    }

    /**
     * @return All known servers.
     */
    public Collection<ServerId> allServerIds() {
        return Collections.unmodifiableCollection(configuration.keySet());
    }

    /**
     * @param serverId ID of server.
     * @return Server address. Empty if server not known.
     */
    public Optional<String> getServerAddress(ServerId serverId) {
        return Optional.ofNullable(configuration.get(serverId)).map(Info::getServerAddress);
    }

    /**
     * @param serverId ID of server.
     * @return Client port. Empty if server not known.
     */
    public Optional<Integer> getClientPort(ServerId serverId) {
        return Optional.ofNullable(configuration.get(serverId)).map(Info::getClientPort);
    }

    /**
     * @param serverId ID of server.
     * @return Coordination port. Empty if server not known.
     */
    public Optional<Integer> getCoordinationPort(ServerId serverId) {
        return Optional.ofNullable(configuration.get(serverId)).map(Info::getCoordinationPort);
    }

    @Data
    private static class Info {
        private final String serverAddress;
        private final int clientPort;
        private final int coordinationPort;
    }
}
