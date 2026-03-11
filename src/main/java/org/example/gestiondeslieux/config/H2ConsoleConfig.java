package org.example.gestiondeslieux.config;

import jakarta.servlet.Servlet;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
    public ServletRegistrationBean<Servlet> h2ConsoleServlet(
            @Value("${spring.h2.console.path:/h2-console}") String consolePath) {
        try {
            Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();

            ServletRegistrationBean<Servlet> registration = new ServletRegistrationBean<>(
                    servlet,
                    normalizePath(consolePath) + "/*");
            registration.setName("H2Console");
            registration.addInitParameter("-trace", "false");
            registration.addInitParameter("-webAllowOthers", "false");
            return registration;
        } catch (ReflectiveOperationException ex) {
            throw new BeanCreationException("Unable to register H2 console servlet", ex);
        }
    }

    private String normalizePath(String consolePath) {
        if (consolePath == null || consolePath.isBlank()) {
            return "/h2-console";
        }
        return consolePath.startsWith("/") ? consolePath : "/" + consolePath;
    }
}
