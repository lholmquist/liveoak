/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.mongo.gridfs;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import io.liveoak.common.codec.DefaultResourceState;
import io.liveoak.mongo.gridfs.extension.GridFSExtension;
import io.liveoak.spi.state.ResourceState;
import io.liveoak.testtools.AbstractHTTPResourceTestCase;
import org.jboss.logging.Logger;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class AbstractGridFSTest extends AbstractHTTPResourceTestCase {

    protected static final String ALL = "*/*";
    protected static final String APPLICATION_JSON = "application/json";

    protected final Logger log = Logger.getLogger(getClass());

    protected String BASEPATH = "gridfs";

    protected DB db;
    private Mongo mongoClient;

    @Override
    public void loadExtensions() throws Exception {
        loadExtension("gridfs", new GridFSExtension());
        installResource("gridfs", "gridfs", createConfig());
    }

    public ResourceState createConfig() {
        String database = System.getProperty("mongo.db", "MongoControllerTest_" + UUID.randomUUID());
        Integer port = new Integer(System.getProperty("mongo.port", "27017"));
        String host = System.getProperty("mongo.host", "localhost");
        log.debug("Using Mongo on " + host + ":" + port + ", database: " + database);

        ResourceState config = new DefaultResourceState();
        config.putProperty("db", database);

        List<ResourceState> servers = new ArrayList<ResourceState>();
        ResourceState server = new DefaultResourceState();
        server.putProperty("port", port);
        server.putProperty("host", host);
        servers.add(server);
        config.putProperty("servers", servers);

        try {
            mongoClient = new MongoClient(host, port);
            db = mongoClient.getDB(database);
            db.dropDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println("TEST CONFIG: " + config);
        return config;
    }

    protected void assertLink(Object item, String rel, String href) {
        assertThat(item).isInstanceOf(JsonObject.class);
        JsonObject obj = (JsonObject) item;
        assertThat(obj.getString("rel")).isEqualTo(rel);
        assertThat(obj.getString("href")).isEqualTo(href);
    }

}
