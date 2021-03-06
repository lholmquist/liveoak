package io.liveoak.keycloak;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DefaultServletConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.tmp.TmpAdminRedirectServlet;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.undertow.servlet.Servlets.servlet;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakServer {

    private final UndertowServer undertow;

    private KeycloakSessionFactory factory;

    public KeycloakServer(UndertowServer undertow) {
        this.undertow = undertow;
    }

    public KeycloakSessionFactory getKeycloakSessionFactory() {
        return factory;
    }

    protected void setupDefaultRealm() {
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();

        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(Constants.ADMIN_REALM);

            // No need to require admin to change password as this server is for dev/test
            adminRealm.getUser("admin").removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

            if (adminRealm.getApplicationByName("console") == null) {
                // Create Application in realm for console and initialize it
                ApplicationModel consoleApp = new ApplicationManager(manager).createApplication(adminRealm, "console");
                consoleApp.addDefaultRole("user");
                consoleApp.addRole("admin");

                UserModel consoleAppClient = consoleApp.getApplicationUser();
                consoleAppClient.addRedirectUri("http://localhost:8080/admin");
                consoleAppClient.addWebOrigin("http://localhost:8080");

                // Likely a bug in ApplianceBootstrap as currently it requires 'password' TODO: Remove
                adminRealm.updateRequiredApplicationCredentials(Collections.EMPTY_SET);
                adminRealm.addRequiredResourceCredential(CredentialRepresentation.SECRET);

                // Just for development to use hardcoded client_secret instead of generated TODO: Remove and find better solution
                UserCredentialModel secret = UserCredentialModel.secret("password");
                adminRealm.updateCredential(consoleAppClient, secret);
            }

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    public void start() throws Exception {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(KeycloakApplication.class.getName());

        DeploymentInfo deploymentInfo = new DeploymentInfo();

        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setContextPath("/auth" );
        deploymentInfo.setDeploymentName("Keycloak");
        deploymentInfo.setResourceManager(new KeycloakResourceManager());

        deploymentInfo.setDefaultServletConfig(new DefaultServletConfig(true));
        deploymentInfo.addWelcomePage("index.html");

        ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/rest/*");
        resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", "/rest" );

        deploymentInfo.addServletContextAttribute(ResteasyDeployment.class.getName(), deployment);
        deploymentInfo.addServlet(resteasyServlet);

        FilterInfo filter = Servlets.filter("SessionFilter", KeycloakSessionServletFilter.class);
        deploymentInfo.addFilter(filter);
        deploymentInfo.addFilterUrlMapping("SessionFilter", "/rest/*", DispatcherType.REQUEST);

        ServletInfo tmpAdminRedirectServlet = Servlets.servlet("TmpAdminRedirectServlet", TmpAdminRedirectServlet.class);
        tmpAdminRedirectServlet.addMappings("/admin", "/admin/");
        deploymentInfo.addServlet(tmpAdminRedirectServlet);

        undertow.deploy(deploymentInfo);

        factory = ((KeycloakApplication)deployment.getApplication()).getFactory();

        setupDefaultRealm();
    }

    public void stop() {
        undertow.undeploy("Keycloak");
        factory.close();
    }

    public static class KeycloakResourceManager implements ResourceManager {
        @Override
        public Resource getResource(String path) throws IOException {
            String realPath = "META-INF/resources" + path;
            URL url = getClass().getClassLoader().getResource(realPath);
            if ( url == null ) {
                return null;
            }
            return new URLResource(url, url.openConnection(), path);
        }

        @Override
        public boolean isResourceChangeListenerSupported() {
            return false;
        }

        @Override
        public void registerResourceChangeListener(ResourceChangeListener listener) {
        }

        @Override
        public void removeResourceChangeListener(ResourceChangeListener listener) {
        }

        @Override
        public void close() throws IOException {
        }
    }

}
