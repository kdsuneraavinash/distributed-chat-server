package lk.ac.mrt.cse.cs4262.components;

/**
 * Interface of a server component.
 * Server component is an independent entity listening and
 * responding to external requests.
 */
public interface ServerComponent extends AutoCloseable {
    /**
     * Connect all the sub-components together.
     * Must be called after initialization.
     */
    void connect();
}
