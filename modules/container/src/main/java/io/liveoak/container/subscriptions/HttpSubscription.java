package io.liveoak.container.subscriptions;

import io.liveoak.common.codec.ResourceCodec;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ResourceResponse;
import io.liveoak.spi.container.Subscription;
import io.liveoak.spi.resource.async.PropertySink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.netty.buffer.ByteBuf;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;

import java.net.URI;
import java.util.UUID;

/**
 * @author Bob McWhirter
 */
public class HttpSubscription implements Subscription {

    public HttpSubscription(DefaultSubscriptionManager subscriptionManager, HttpClient httpClient, String path, URI destination, ResourceCodec codec) {
        this.subscriptionManager = subscriptionManager;
        this.id = UUID.randomUUID().toString();
        this.httpClient = httpClient;
        this.resourcePath = new ResourcePath(path);
        this.destination = destination;
        this.codec = codec;
    }

    @Override
    public Resource parent() {
        return this.subscriptionManager;
    }

    @Override
    public String id() {
        return this.id;
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    @Override
    public void readProperties(RequestContext ctx, PropertySink sink) throws Exception {
        sink.accept("type", "http");
        sink.accept("path", this.resourcePath.toString());
        sink.accept("destination", this.destination.toString());
        sink.close();
    }

    @Override
    public void delete(RequestContext ctx, Responder responder) {
        this.subscriptionManager.removeSubscription(this);
        responder.resourceDeleted(this);
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------

    @Override
    public ResourcePath resourcePath() {
        return this.resourcePath;
    }

    @Override
    public void resourceCreated(ResourceResponse resourceResponse) throws Exception {
        resourceUpdated(resourceResponse);
    }

    @Override
    public void resourceUpdated(ResourceResponse resourceResponse) throws Exception {
        URI uri = destinationUri(resourceResponse.resource());
        HttpClientRequest request = this.httpClient.put(uri.getPath(), (response) -> {
        });

        request.setChunked(true);

        RequestContext requestContext = new RequestContext.Builder().build();
        ByteBuf encoded = codec.encode(requestContext, resourceResponse.state());
        request.write(new Buffer(encoded));
        request.end();
    }

    @Override
    public void resourceDeleted(ResourceResponse resourceResponse) throws Exception {
        URI uri = destinationUri(resourceResponse.resource());
        HttpClientRequest request = this.httpClient.delete(uri.getPath(), (response) -> {
        });

        request.setChunked(true);

        RequestContext requestContext = new RequestContext.Builder().build();
        ByteBuf encoded = codec.encode(requestContext, resourceResponse.state());
        request.write(new Buffer(encoded));
        request.end();
    }

    protected URI destinationUri(Resource resource) {
        URI uri = this.destination.resolve(resource.id());
        return uri;
    }

    private DefaultSubscriptionManager subscriptionManager;
    private String id;
    private HttpClient httpClient;
    private ResourcePath resourcePath;
    private final URI destination;
    private ResourceCodec codec;

}
