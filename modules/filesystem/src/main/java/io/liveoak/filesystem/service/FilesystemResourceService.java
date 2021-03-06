package io.liveoak.filesystem.service;

import io.liveoak.filesystem.FileSystemAdminResource;
import io.liveoak.filesystem.FilesystemResource;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.vertx.java.core.Vertx;

import java.io.File;

/**
 * @author Bob McWhirter
 */
public class FilesystemResourceService implements Service<FilesystemResource> {

    public FilesystemResourceService(String id) {
        this.id = id;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.resource = new FilesystemResource(
                this.adminResourceInjector.getValue(),
                this.id,
                this.vertxInjector.getValue() );
    }

    @Override
    public void stop(StopContext context) {
        this.resource = null;
    }

    @Override
    public FilesystemResource getValue() throws IllegalStateException, IllegalArgumentException {
        return this.resource;
    }

    public Injector<Vertx> vertxInjector() {
        return this.vertxInjector;
    }

    public Injector<FileSystemAdminResource> adminResourceInjector() {
        return this.adminResourceInjector;

    }

    protected String id;

    protected InjectedValue<Vertx> vertxInjector = new InjectedValue<>();
    protected InjectedValue<FileSystemAdminResource> adminResourceInjector = new InjectedValue<>();
    protected FilesystemResource resource;
}
