package com.acme.salary.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the Angular build and lets Angular own its routes: any GET that is not an API call and
 * does not look like a file (no "." in the last segment) gets index.html, so refreshing or
 * bookmarking /employees/123 works. Controllers are matched first, so this never hides an endpoint.
 */
@Configuration
class SpaFallbackConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return isAppRoute(path) ? location.createRelative("index.html") : null;
                    }
                });
    }

    private static boolean isAppRoute(String path) {
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !path.startsWith("api/") && !lastSegment.contains(".");
    }
}
