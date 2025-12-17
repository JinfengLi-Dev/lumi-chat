package com.lumichat.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat configuration to handle Unicode characters in requests and error responses.
 * This is necessary because HTTP headers can only contain ASCII characters (0-255),
 * and Tomcat's default error reporting tries to include request data in headers.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addConnectorCustomizers(this::customizeConnector);
    }

    private void customizeConnector(Connector connector) {
        // Allow special characters in query strings
        connector.setProperty("relaxedQueryChars", "[]{}|");
        connector.setProperty("relaxedPathChars", "[]{}|");

        // Set URI encoding to UTF-8
        connector.setURIEncoding("UTF-8");
    }
}
