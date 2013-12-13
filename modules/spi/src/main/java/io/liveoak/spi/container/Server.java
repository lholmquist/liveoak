package io.liveoak.spi.container;

import java.net.InetAddress;

/**
 * @author Bob McWhirter
 */
public interface Server {
    void start() throws Exception;
    void stop() throws Exception;
}
