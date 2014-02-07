package com.github.nhuray.dropwizard.spring;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.cli.EnvironmentCommand;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.ServerFactory;
import com.yammer.dropwizard.jersey.JacksonMessageBodyProvider;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.servlet.ServletContextListener;
import java.io.IOException;

public class WebAppCommand<T extends Configuration> extends EnvironmentCommand<T> {
    public static final String AUTHORIZE_TAG_CLASS_NAME = "org.springframework.security.taglibs.authz.AbstractAuthorizeTag";

    private static final Logger log= LoggerFactory.getLogger(WebAppCommand.class);


    private final Class<T> configurationClass;
    private ConfigurableApplicationContext applicationContext;
    private String dropWizardContextPath;
    private ServletContextListener webAppInitializer;

    public WebAppCommand(Service<T> service, ConfigurableApplicationContext applicationContext, ServletContextListener webAppInitializer, String dropWizardContextPath) {
        super(service, "webapp", "Runs the Dropwizard service as an Web Application HTTP server");
        this.webAppInitializer = webAppInitializer;
        this.applicationContext = applicationContext;
        this.dropWizardContextPath = dropWizardContextPath;
        this.configurationClass = service.getConfigurationClass();
    }

    /*
     * Since we don't subclass ServerCommand, we need a concrete reference to the configuration
     * class.
     */
    @Override
    protected Class<T> getConfigurationClass() {
        return configurationClass;
    }

    @Override
    protected void run(Environment environment, Namespace namespace, T configuration) throws Exception {

        final ServletContainer jerseyContainer = environment.getJerseyServletContainer();
        environment.addProvider(new JacksonMessageBodyProvider(environment.getObjectMapperFactory().build(),
                environment.getValidator()));
        environment.addServlet(jerseyContainer, dropWizardContextPath + "/*");
        environment.setJerseyServletContainer(null);

        final Server server = new ServerFactory(configuration.getHttpConfiguration(),
                environment.getName()).buildServer(environment);

        logBanner(environment.getName(), log);
        try {
            // run Spring main app
            if (!applicationContext.isActive()) applicationContext.refresh();

            // run the web app with parent application context
            HandlerCollection handlerCollection = (HandlerCollection) server.getHandler();
            handlerCollection.addHandler(jettyWebAppContext());

            // start jetty server
            server.start();
            for (ServerLifecycleListener listener : environment.getServerListeners()) {
                listener.serverStarted(server);
            }
        } catch (Exception e) {
            log.error("Unable to start server, shutting down", e);
            server.stop();
        }
    }



    private void logBanner(String name, Logger logger) {
        try {
            final String banner = Resources.toString(Resources.getResource("banner.txt"),
                    Charsets.UTF_8);
            logger.info("Starting {}\n{}", name, banner);
        } catch (IllegalArgumentException ignored) {
            // don't display the banner if there isn't one
            logger.info("Starting {}", name);
        } catch (IOException ignored) {
            logger.info("Starting {}", name);
        }
    }

    private Handler jettyWebAppContext() throws IOException {
        WebAppContext webApp;
        webApp = new WebAppContext();

        webApp.setContextPath("/");
        String war = new ClassPathResource("/webapp").getURI().toString();
        webApp.setWar(war);

        /* Disable directory listings if no index.html is found. */
        webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                "false");

        /* Create the root web application applicationContext and set it as a servlet
         * attribute so the dispatcher servlet can find it. */
        GenericWebApplicationContext webApplicationContext = new GenericWebApplicationContext();
        webApplicationContext.setParent(applicationContext);

        webApp.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                webApplicationContext);

        /*
         * Set the attributes that the Metrics servlets require.  The Metrics
         * servlet is added in the WebAppInitializer.
         */
        webApp.addEventListener(webAppInitializer);

        if (ClassUtils.isPresent(AUTHORIZE_TAG_CLASS_NAME, this.getClass().getClassLoader())){
            webApp.addEventListener(new SecurityExpressionHandlerFix());
        }

        // start web application context
        webApplicationContext.refresh();

        return webApp;
    }
}
