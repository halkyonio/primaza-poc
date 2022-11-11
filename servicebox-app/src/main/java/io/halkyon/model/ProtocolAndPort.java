package io.halkyon.model;

/**
 * Structure representing a protocol:port endpoint.
 */
public class ProtocolAndPort {
    /**
     * The protocol.
     */
    public final String protocol;
    /**
     * The port.
     */
    public final int port;

    /**
     * Creates a new HostAndPort
     *
     * @param protocol the host
     * @param port the port
     */
    public ProtocolAndPort(String protocol, int port) {
        this.protocol = protocol;
        this.port = port;
    }
}
