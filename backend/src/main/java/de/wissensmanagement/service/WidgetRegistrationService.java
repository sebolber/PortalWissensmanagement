package de.wissensmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Registers dashboard widgets in PortalCore on application startup.
 */
@Service
public class WidgetRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(WidgetRegistrationService.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    @Value("${portal.widget.registration-enabled:false}")
    private boolean registrationEnabled;

    public WidgetRegistrationService() {
        this.restClient = RestClient.create();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWidgets() {
        if (!registrationEnabled) {
            log.info("Widget registration disabled, skipping.");
            return;
        }

        registerWidget("wm-neueste-artikel", "Neueste Artikel",
                "Zeigt die neuesten Wissensartikel an",
                "Wissensmanagement", "/api/artikel/neueste?limit=5",
                "/artikel", 2, 2);

        registerWidget("wm-beliebte-artikel", "Beliebte Artikel",
                "Zeigt die beliebtesten Wissensartikel an",
                "Wissensmanagement", "/api/artikel/beliebt?limit=5",
                "/artikel", 2, 2);

        registerWidget("wm-statistik", "Wissensmanagement Statistik",
                "Zeigt Statistiken zur Wissensdatenbank",
                "Wissensmanagement", "/api/artikel/statistik",
                "/artikel", 2, 1);

        log.info("Widget registration completed.");
    }

    private void registerWidget(String widgetKey, String titel, String beschreibung,
                                 String kategorie, String datenEndpunkt,
                                 String linkZiel, int breite, int hoehe) {
        try {
            Map<String, Object> widget = Map.ofEntries(
                    Map.entry("id", UUID.randomUUID().toString()),
                    Map.entry("widgetKey", widgetKey),
                    Map.entry("titel", titel),
                    Map.entry("beschreibung", beschreibung),
                    Map.entry("kategorie", kategorie),
                    Map.entry("widgetTyp", "DATA_LIST"),
                    Map.entry("appId", "wissensmanagement"),
                    Map.entry("appName", "Wissensmanagement"),
                    Map.entry("standardBreite", breite),
                    Map.entry("standardHoehe", hoehe),
                    Map.entry("minBreite", 1),
                    Map.entry("minHoehe", 1),
                    Map.entry("maxBreite", 4),
                    Map.entry("maxHoehe", 4),
                    Map.entry("datenEndpunkt", datenEndpunkt),
                    Map.entry("linkZiel", linkZiel),
                    Map.entry("aktiv", true),
                    Map.entry("konfigurierbar", false)
            );

            restClient.post()
                    .uri(portalCoreBaseUrl + "/api/dashboard/widgets/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(widget)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Registered widget: {}", widgetKey);
        } catch (Exception e) {
            log.warn("Failed to register widget '{}': {}", widgetKey, e.getMessage());
        }
    }
}
