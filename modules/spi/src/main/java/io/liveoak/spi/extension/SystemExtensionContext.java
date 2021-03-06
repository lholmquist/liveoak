package io.liveoak.spi.extension;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Bob McWhirter
 */
public interface SystemExtensionContext {

    String id();
    ServiceTarget target();
    void mountPrivate(ServiceName adminResourceName);

    //void mount(ServiceName name);
}
