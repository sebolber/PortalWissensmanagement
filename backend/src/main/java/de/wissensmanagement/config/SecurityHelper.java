package de.wissensmanagement.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityHelper {

    public JwtAuthenticationFilter.AuthDetails getAuthDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthenticationFilter.AuthDetails details) {
            return details;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht authentifiziert");
    }

    public String getCurrentUserId() {
        return getAuthDetails().userId();
    }

    public String getCurrentTenantId() {
        return getAuthDetails().tenantId();
    }

    public String getCurrentEmail() {
        return getAuthDetails().email();
    }

    public void requireAuthentication() {
        getAuthDetails();
    }
}
