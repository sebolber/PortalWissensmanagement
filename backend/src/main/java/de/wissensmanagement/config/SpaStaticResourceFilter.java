package de.wissensmanagement.config;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rewrites requests for static assets that are prefixed with an SPA route path.
 * When the Angular app is loaded at a sub-route like /artikel, the browser resolves
 * relative asset references (e.g. main-HASH.js) to /artikel/main-HASH.js.
 * This filter strips the SPA route prefix and forwards to the root-level resource.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaStaticResourceFilter extends OncePerRequestFilter {

    private static final Pattern STATIC_ASSET_EXT = Pattern.compile(".*\\.(js|css|ico|png|woff2|woff|ttf|svg|map|json)$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip API requests and single-segment paths (already at root level)
        if (!path.startsWith("/api/")) {
            // Strip leading slash, then check if there are multiple segments
            String withoutLeadingSlash = path.substring(1);
            int slashIndex = withoutLeadingSlash.indexOf('/');
            if (slashIndex > 0) {
                // Has at least 2 path segments: e.g. "artikel/main-HASH.js"
                String remaining = withoutLeadingSlash.substring(slashIndex); // "/main-HASH.js"
                if (STATIC_ASSET_EXT.matcher(remaining).matches()) {
                    request.getRequestDispatcher(remaining).forward(request, response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
