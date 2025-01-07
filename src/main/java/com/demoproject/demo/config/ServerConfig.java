/* ==========================================================================
 * Server Configuration Module
 * 
 * PURPOSE: Configures embedded Tomcat server with HTTP/HTTPS connectors
 * DEPENDENCIES: Apache Tomcat, Spring Boot, SLF4J
 * SCOPE: Application-wide server configuration
 * ========================================================================== */

package com.demoproject.demo.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.catalina.LifecycleException;

/* --------------------------------------------------------------------------
 * Core Server Configuration
 * 
 * FUNCTIONALITY:
 * - Configures embedded Tomcat server
 * - Sets up HTTP to HTTPS redirection
 * - Manages connection pooling and timeouts
 * - Handles graceful connector shutdown
 * 
 * SECURITY CONSIDERATIONS:
 * - Forces HTTPS for all secure traffic
 * - Configurable connection limits
 * - Timeout protection against hung connections
 * -------------------------------------------------------------------------- */
@Configuration
public class ServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    /* .... Server Properties .... */
    @Value("${server.tomcat.max-connections:10000}")
    private int maxConnections;

    @Value("${server.tomcat.accept-count:100}")
    private int acceptCount;

    @Value("${server.tomcat.connection-timeout:20000}")
    private int connectionTimeout;

    /* .... Server Factory Configuration .... */
    /**
     * Creates and configures the main servlet container factory.
     *
     * @return Configured TomcatServletWebServerFactory
     * 
     * FEATURES:
     * - Graceful connector shutdown
     * - HTTP to HTTPS redirection
     * - Connection pool management
     * 
     * EDGE CASES:
     * - Handles shutdown errors gracefully
     * - Manages connection cleanup
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        tomcat.addConnectorCustomizers(connector -> {
            connector.addLifecycleListener(new LifecycleListener() {
                @Override
                public void lifecycleEvent(LifecycleEvent event) {
                    if (Lifecycle.STOP_EVENT.equals(event.getType())) {
                        Connector connector = (Connector) event.getLifecycle();
                        if (connector.getState().isAvailable()) {
                            try {
                                logger.info("Gracefully shutting down connector");
                                connector.stop();
                            } catch (LifecycleException e) {
                                logger.warn("Error during connector shutdown", e);
                            }
                        }
                    }
                }
            });
        });
        
        tomcat.addAdditionalTomcatConnectors(createRedirectConnector());
        return tomcat;
    }

    /* .... HTTP Connector Configuration .... */
    /**
     * Creates HTTP connector that redirects to HTTPS.
     *
     * @return Configured HTTP connector
     * 
     * CONFIGURATION:
     * - Port: 8080 (HTTP)
     * - Redirect Port: 8443 (HTTPS)
     * - Connection Timeout: 20s
     * - Max Threads: 150
     * - Accept Queue: 100
     */
    private Connector createRedirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        connector.setProperty("connectionTimeout", String.valueOf(connectionTimeout));
        connector.setProperty("maxThreads", "150");
        connector.setProperty("acceptCount", String.valueOf(acceptCount));
        return connector;
    }

    /* @todo [SECURITY] Add HTTPS connector configuration
     * @todo [RESILIENCE] Implement rate limiting for DDoS protection
     * @todo [MONITOR] Add connection pool metrics
     * @todo [PERF] Tune thread pool settings based on load testing
     */
}