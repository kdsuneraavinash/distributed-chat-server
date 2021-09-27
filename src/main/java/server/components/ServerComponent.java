package server.components;

import lombok.Getter;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
public abstract class ServerComponent implements Runnable, AutoCloseable {
    @Getter
    @ToString.Include
    protected final int port;

    protected ServerComponent(int port) {
        this.port = port;
    }

    @Override
    public void close() throws Exception {
        // TODO: Remove later
    }
}
