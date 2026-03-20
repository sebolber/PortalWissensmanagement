package de.wissensmanagement.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = super.getResource(resourcePath, location);
                        if (resource == null && resourcePath.contains("/")) {
                            // Strip SPA route prefix: /artikel/main-HASH.js -> main-HASH.js
                            String strippedPath = resourcePath.substring(resourcePath.indexOf('/') + 1);
                            resource = super.getResource(strippedPath, location);
                        }
                        return resource;
                    }
                });
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/{path1}/{path2:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/{path1}/{path2}/{path3:[^\\.]*}").setViewName("forward:/index.html");
    }
}
