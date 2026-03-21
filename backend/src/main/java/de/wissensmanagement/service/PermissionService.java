package de.wissensmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prueft Benutzerberechtigungen fuer Wissensmanagement-Use-Cases
 * durch Abfrage der PortalCore-API.
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final String APP_ID = "portalwissensmanagement";

    // Use-Case-Konstanten passend zu portal-app.yaml
    public static final String UC_LESEN = "wissensmanagement-lesen";
    public static final String UC_SCHREIBEN = "wissensmanagement-schreiben";
    public static final String UC_VEROEFFENTLICHEN = "wissensmanagement-veroeffentlichen";
    public static final String UC_CHAT = "wissensmanagement-chat";
    public static final String UC_ADMIN = "wissensmanagement-admin";

    private final RestClient restClient;
    private final ConcurrentHashMap<String, CachedAppPermissions> cache = new ConcurrentHashMap<>();

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public PermissionService() {
        this.restClient = RestClient.create();
    }

    /**
     * Prueft ob der Benutzer eine bestimmte Berechtigung hat.
     */
    public boolean hatBerechtigung(String jwtToken, String useCase, String typ) {
        AppPermissions perms = getPermissions(jwtToken);
        if (perms == null) {
            return false;
        }
        if (perms.isSuperAdmin()) {
            return true;
        }
        return perms.hasPermission(useCase, typ);
    }

    /**
     * Prueft Leseberechtigung.
     */
    public boolean darfLesen(String jwtToken) {
        return hatBerechtigung(jwtToken, UC_LESEN, "lesen");
    }

    /**
     * Prueft Schreibberechtigung.
     */
    public boolean darfSchreiben(String jwtToken) {
        return hatBerechtigung(jwtToken, UC_SCHREIBEN, "schreiben");
    }

    /**
     * Prueft Veroeffentlichungsberechtigung.
     */
    public boolean darfVeroeffentlichen(String jwtToken) {
        return hatBerechtigung(jwtToken, UC_VEROEFFENTLICHEN, "schreiben");
    }

    /**
     * Prueft Chat-Berechtigung.
     */
    public boolean darfChatten(String jwtToken) {
        return hatBerechtigung(jwtToken, UC_CHAT, "lesen");
    }

    /**
     * Prueft Admin-Berechtigung.
     */
    public boolean darfAdministrieren(String jwtToken) {
        return hatBerechtigung(jwtToken, UC_ADMIN, "schreiben");
    }

    /**
     * Wirft 403 wenn Berechtigung fehlt.
     */
    public void requireBerechtigung(String jwtToken, String useCase, String typ) {
        if (!hatBerechtigung(jwtToken, useCase, typ)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Keine Berechtigung fuer diese Aktion (" + useCase + ":" + typ + ").");
        }
    }

    public void requireLesen(String jwtToken) {
        requireBerechtigung(jwtToken, UC_LESEN, "lesen");
    }

    public void requireSchreiben(String jwtToken) {
        requireBerechtigung(jwtToken, UC_SCHREIBEN, "schreiben");
    }

    public void requireVeroeffentlichen(String jwtToken) {
        requireBerechtigung(jwtToken, UC_VEROEFFENTLICHEN, "schreiben");
    }

    public void requireChat(String jwtToken) {
        requireBerechtigung(jwtToken, UC_CHAT, "lesen");
    }

    public void requireAdmin(String jwtToken) {
        requireBerechtigung(jwtToken, UC_ADMIN, "schreiben");
    }

    @SuppressWarnings("unchecked")
    private AppPermissions getPermissions(String jwtToken) {
        if (jwtToken == null) {
            return null;
        }

        CachedAppPermissions cached = cache.get(jwtToken);
        if (cached != null && !cached.isExpired()) {
            return cached.permissions;
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/apps/" + APP_ID + "/berechtigungen")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return null;
            }

            boolean superAdmin = Boolean.TRUE.equals(response.get("superAdmin"));
            List<Map<String, Object>> permissionsList =
                    (List<Map<String, Object>>) response.get("permissions");

            AppPermissions perms = new AppPermissions(superAdmin, permissionsList);
            cache.put(jwtToken, new CachedAppPermissions(perms));
            return perms;
        } catch (Exception e) {
            log.warn("Berechtigungsabfrage bei PortalCore fehlgeschlagen: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cache invalidieren (z.B. bei Rechteaenderungen).
     */
    public void invalidateCache() {
        cache.clear();
    }

    private static class AppPermissions {
        private final boolean superAdmin;
        private final Map<String, Map<String, Boolean>> permissions;

        @SuppressWarnings("unchecked")
        AppPermissions(boolean superAdmin, List<Map<String, Object>> permissionsList) {
            this.superAdmin = superAdmin;
            this.permissions = new ConcurrentHashMap<>();
            if (permissionsList != null) {
                for (Map<String, Object> perm : permissionsList) {
                    String useCase = (String) perm.get("useCase");
                    Map<String, Boolean> flags = new ConcurrentHashMap<>();
                    flags.put("anzeigen", Boolean.TRUE.equals(perm.get("anzeigen")));
                    flags.put("lesen", Boolean.TRUE.equals(perm.get("lesen")));
                    flags.put("schreiben", Boolean.TRUE.equals(perm.get("schreiben")));
                    flags.put("loeschen", Boolean.TRUE.equals(perm.get("loeschen")));
                    this.permissions.put(useCase, flags);
                }
            }
        }

        boolean isSuperAdmin() {
            return superAdmin;
        }

        boolean hasPermission(String useCase, String typ) {
            Map<String, Boolean> flags = permissions.get(useCase);
            if (flags == null) {
                return false;
            }
            return Boolean.TRUE.equals(flags.get(typ));
        }
    }

    private static class CachedAppPermissions {
        final AppPermissions permissions;
        final long createdAt;

        CachedAppPermissions(AppPermissions permissions) {
            this.permissions = permissions;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }
}
