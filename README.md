# PortalWissensmanagement

> Wissensmanagement mit KI-gestuetztem RAG-Chat und hierarchischer Artikelverwaltung

![Java 21](https://img.shields.io/badge/Java-21-orange) ![Spring Boot 3.2.4](https://img.shields.io/badge/Spring%20Boot-3.2.4-green) ![Angular 18](https://img.shields.io/badge/Angular-18-red) ![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue) ![Docker](https://img.shields.io/badge/Docker-ready-blue)

## Inhaltsverzeichnis

- [Ueber das Projekt](#ueber-das-projekt)
- [Technologie-Stack](#technologie-stack)
- [Architektur](#architektur)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Installation und Setup](#installation-und-setup)
- [Konfiguration](#konfiguration)
- [API-Dokumentation](#api-dokumentation)
- [RAG-Chat](#rag-chat)
- [Artikelhierarchie](#artikelhierarchie)
- [Datenbankschema](#datenbankschema)
- [Berechtigungen](#berechtigungen)
- [Integration mit PortalCore](#integration-mit-portalcore)
- [Weiterfuehrende Dokumentation](#weiterfuehrende-dokumentation)

---

## Ueber das Projekt

Das PortalWissensmanagement ist eine **Portal-App** fuer strukturierte Wissensverwaltung mit KI-Unterstuetzung. Es wird als Docker-Container im PortalCore-Netzwerk betrieben.

### Hauptfunktionen

- **Hierarchische Artikelverwaltung** mit Baumstruktur, Drag & Drop, Breadcrumbs
- **RAG-basierter Chat** mit Multi-Strategie-Retrieval (Volltext + Trigram Similarity)
- **Hybrid-Suche** (PostgreSQL Volltext + Fuzzy-Matching)
- **Prompt-Template-Verwaltung** fuer wiederverwendbare KI-Prompts
- **Diktat-Funktion** - Rohtext via LLM in strukturierten Artikel umwandeln
- **Versionierung** mit vollstaendiger Aenderungshistorie
- **Feedback-System** mit Bewertungen (1-5 Sterne)
- **Nutzungsanalyse** (Views, Usage, zuletzt genutzt)
- **Export/Import** fuer mandantenuebergreifende Datenmigration
- **Dashboard-Widgets** (integriert in PortalCore)
- **Aufgaben-Verknuepfung** (Artikel ↔ PortalCore-Aufgaben)

---

## Technologie-Stack

| Schicht | Technologie |
|---------|-------------|
| **Backend** | Spring Boot 3.2.4, Java 21, Spring Data JPA, Spring Security |
| **Frontend** | Angular 18, TypeScript 5.4, Marked 17 (Markdown), Standalone Components |
| **Datenbank** | PostgreSQL 16 (geteilt mit PortalCore, Praefix `wm_`) |
| **Migrationen** | Flyway (8 Migrationen, Tabelle `wm_schema_history`) |
| **Suche** | PostgreSQL Volltext (German Stemming) + pg_trgm (Trigram Similarity) |
| **Authentifizierung** | JWT (geteiltes Secret mit PortalCore) |
| **Build** | Maven 3.9 (Backend), Angular CLI 18 (Frontend) |
| **Deployment** | Docker (Multi-Stage Build) |

---

## Architektur

**Schichtenarchitektur** mit RAG-Integration:

```
Frontend (Angular 18)
    ↕ REST API
Controller → Service → Repository → Entity
    ↕            ↕
PortalCore   PostgreSQL (wm_*)
(Auth, LLM)    ↕
            RetrievalService → ChunkService
            (Volltext + Trigram + Fallback)
```

Detaillierte Dokumentation: [docs/arc42-architecture.md](docs/arc42-architecture.md)

---

## Projektstruktur

```
backend/
  src/main/java/de/wissensmanagement/
    controller/       9 REST-Controller
    service/          16 Business-Services
    entity/           13 JPA-Entities
    repository/       13 Spring Data Repositories
    dto/              9 Data Transfer Objects
    enums/            3 Enumerationen (ArticleStatus, ChatRole, PromptType)
    config/           Security, JWT, CORS, SPA
  src/main/resources/
    db/migration/     8 Flyway-Migrationen (V1-V8)
    application.yml

frontend/
  src/app/
    pages/
      startseite/     Startseite
      artikel/        Liste, Detail, Formular
      chat/           RAG-Chat-Interface
      kategorien/     Kategorienverwaltung
      konfiguration/  Einstellungen, Export/Import
      suche/          Volltextsuche
    components/       ArticleTreeComponent
    services/         ArtikelService, ChatService
    models/           TypeScript Interfaces
    interceptors/     Auth-Interceptor

portal-app.yaml       App-Manifest fuer PortalCore
```

---

## Voraussetzungen

- Java 21+
- Maven 3.9+
- Node.js 18+
- Angular CLI 18
- PostgreSQL 16 (mit pg_trgm-Erweiterung)
- Docker (fuer Deployment)
- Laufende PortalCore-Instanz

---

## Installation und Setup

### 1. Backend starten

```bash
cd backend
mvn spring-boot:run
```

API verfuegbar unter `http://localhost:8080/api`.

### 2. Frontend starten (Entwicklung)

```bash
cd frontend
npm install
ng serve
```

Frontend verfuegbar unter `http://localhost:4200`.

### 3. Docker Build & Run

```bash
docker build -t wissensmanagement .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://portal-db:5432/portal \
  -e DB_USER=portal \
  -e DB_PASS=portal \
  -e JWT_SECRET=<shared-secret> \
  -e PORTAL_CORE_BASE_URL=http://portal-backend:8080 \
  wissensmanagement
```

---

## Konfiguration

### Umgebungsvariablen

| Variable | Beschreibung | Standard |
|----------|-------------|---------|
| `DB_URL` | JDBC-URL der PostgreSQL-Datenbank | `jdbc:postgresql://portal-db:5432/portal` |
| `DB_USER` | Datenbank-Benutzer | `portal` |
| `DB_PASS` | Datenbank-Passwort | `portal` |
| `JWT_SECRET` | JWT-Signaturschluessel (geteilt mit PortalCore) | - |
| `PORTAL_CORE_BASE_URL` | URL des PortalCore-Backends | `http://portal-backend:8080` |
| `CORS_ALLOWED_ORIGINS` | Erlaubte CORS-Origins | `*` |

---

## API-Dokumentation

Basis-URL: `http://<host>:8080/api`

### Artikel (`/api/artikel`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/artikel` | Artikel auflisten (paginiert, filterbar) |
| GET | `/artikel/{id}` | Einzelnen Artikel abrufen (inkl. View-Tracking) |
| POST | `/artikel` | Artikel erstellen |
| PUT | `/artikel/{id}` | Artikel aktualisieren |
| PUT | `/artikel/{id}/publish` | Artikel publizieren |
| PUT | `/artikel/{id}/archive` | Artikel archivieren |
| DELETE | `/artikel/{id}` | Artikel loeschen |
| GET | `/artikel/{id}/versionen` | Versionshistorie |
| POST | `/artikel/{id}/feedback` | Bewertung abgeben |
| GET | `/artikel/statistik` | Statistik (Admin) |
| GET | `/artikel/neueste` | Neueste Artikel |
| GET | `/artikel/beliebt` | Meistgelesene Artikel |
| GET | `/artikel/zuletzt-genutzt` | Zuletzt genutzte Artikel |
| GET | `/artikel/aufgabe/{taskId}` | Artikel zu einer Aufgabe |
| POST | `/artikel/{id}/aufgabe` | Aufgabe aus Artikel erstellen |
| POST | `/artikel/generate-summary` | Zusammenfassung via LLM |
| GET | `/artikel/baum` | Artikelbaum (Hierarchie) |
| GET | `/artikel/{id}/kinder` | Direkte Kinder-Artikel |
| GET | `/artikel/{id}/breadcrumb` | Breadcrumb-Pfad |
| PUT | `/artikel/{id}/verschieben` | Artikel verschieben |
| PUT | `/artikel/sortierung` | Reihenfolge aendern |
| GET | `/artikel/suche` | Hybrid-Suche |

### Chat (`/api/chat`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/chat/sessions` | Chat-Sessions des Benutzers |
| POST | `/chat/sessions` | Neue Session erstellen |
| GET | `/chat/sessions/{id}` | Session-Details |
| DELETE | `/chat/sessions/{id}` | Session loeschen |
| GET | `/chat/sessions/{id}/messages` | Nachrichten einer Session |
| POST | `/chat/send` | Nachricht senden (RAG + LLM) |
| GET | `/chat/llm-models` | Verfuegbare LLM-Modelle |

### Kategorien (`/api/kategorien`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/kategorien` | lesen | Alle Kategorien |
| GET | `/kategorien/{id}` | lesen | Kategorie-Detail |
| POST | `/kategorien` | admin | Kategorie erstellen |
| PUT | `/kategorien/{id}` | admin | Kategorie aktualisieren |
| DELETE | `/kategorien/{id}` | admin | Kategorie loeschen |

### Tags (`/api/tags`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/tags` | lesen | Alle Tags |
| POST | `/tags` | admin | Tag erstellen |
| DELETE | `/tags/{id}` | admin | Tag loeschen |

### Gruppierungen (`/api/gruppierungen`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/gruppierungen` | lesen | Alle Gruppierungen |
| POST | `/gruppierungen` | admin | Gruppierung erstellen |
| DELETE | `/gruppierungen/{id}` | admin | Gruppierung loeschen |

### Prompts (`/api/prompts`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/prompts` | Prompts auflisten (filterbar nach Typ, aktiv) |
| GET | `/prompts/{id}` | Prompt-Detail |
| POST | `/prompts` | Prompt erstellen |
| PUT | `/prompts/{id}` | Prompt aktualisieren |
| DELETE | `/prompts/{id}` | Prompt loeschen |
| POST | `/prompts/{id}/anwenden` | Prompt auf Inhalt anwenden |

### Prompt-Kategorien (`/api/prompt-kategorien`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/prompt-kategorien` | Alle Prompt-Kategorien |
| POST | `/prompt-kategorien` | Kategorie erstellen |
| PUT | `/prompt-kategorien/{id}` | Kategorie aktualisieren |
| DELETE | `/prompt-kategorien/{id}` | Kategorie loeschen |

### Export/Import (`/api/konfiguration`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/konfiguration/export` | Alle Mandantendaten exportieren (JSON) |
| POST | `/konfiguration/import` | Mandantendaten importieren |

### Diktat (`/api/diktat`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| POST | `/diktat/strukturieren` | Rohtext via LLM strukturieren |

### Health Check (`/api/health`)

| Methode | Pfad | Auth | Beschreibung |
|---------|------|------|-------------|
| GET | `/health` | Nein | Health Check fuer Docker |

---

## RAG-Chat

Der Chat verwendet **Multi-Strategie-Retrieval** fuer optimale Ergebnisse:

### Retrieval-Strategien

1. **Volltext-Suche** (PostgreSQL tsvector, deutsches Stemming)
   - Findet exakte Schluesselwort-Treffer
   - Beruecksichtigt Wortvarianten (z.B. "Behandlung" → "behandl")

2. **Trigram Similarity** (PostgreSQL pg_trgm)
   - Fuzzy-Matching bei Tippfehlern und Umformulierungen
   - Robust gegen unterschiedliche Formulierungen

3. **Article-Level Fallback**
   - Greift auf komplette Artikel zurueck wenn Chunk-Suche zu wenig liefert

4. **Hybrid-Kombination**
   - Gewichtete Zusammenfuehrung aller Strategien
   - Boosting fuer besonders relevante Treffer

### Ablauf

```
Benutzerfrage → Retrieval (4 Strategien) → Top-N Chunks → Prompt + Kontext → LLM → Antwort mit Quellen
```

---

## Artikelhierarchie

Artikel koennen in einer Baumstruktur organisiert werden:

- **parent_article_id**: Verweis auf uebergeordneten Artikel
- **tree_path**: Pfad fuer effiziente Baum-Queries (z.B. `/root-id/sub1-id/sub2-id`)
- **depth**: Tiefe im Baum (0 = Root)
- **sort_order**: Reihenfolge innerhalb einer Ebene

### API-Operationen

- `GET /api/artikel/baum` - Gesamten Baum abrufen
- `GET /api/artikel/{id}/kinder` - Direkte Kinder
- `GET /api/artikel/{id}/breadcrumb` - Breadcrumb-Pfad
- `PUT /api/artikel/{id}/verschieben` - Artikel verschieben (neuer Parent)
- `PUT /api/artikel/sortierung` - Reihenfolge innerhalb einer Ebene

---

## Datenbankschema

13 Entitaeten mit `wm_`-Praefix:

| Tabelle | Beschreibung |
|---------|-------------|
| `wm_articles` | Artikel mit Hierarchie, Versionierung, Statistik |
| `wm_article_versions` | Versionshistorie |
| `wm_article_tags` | Artikel-Tag-Zuordnung (n:m) |
| `wm_categories` | Hierarchische Kategorien |
| `wm_tags` | Tags |
| `wm_groupings` | Gruppierungen |
| `wm_chunks` | RAG-Chunks (Textabschnitte fuer Retrieval) |
| `wm_feedback` | Bewertungen (1-5 Sterne) |
| `wm_usage` | Nutzungstracking |
| `wm_suggestions` | KI-Vorschlaege |
| `wm_chat_sessions` | Chat-Sessions |
| `wm_chat_messages` | Chat-Nachrichten |
| `wm_prompt_configs` | Prompt-Templates |
| `wm_prompt_categories` | Prompt-Kategorien |

Vollstaendiges ER-Diagramm: [docs/er-diagram.md](docs/er-diagram.md)

---

## Berechtigungen

| Use-Case | Beschreibung |
|----------|-------------|
| `wissensmanagement-lesen` | Artikel lesen, Suche nutzen |
| `wissensmanagement-schreiben` | Artikel erstellen und bearbeiten |
| `wissensmanagement-veroeffentlichen` | Artikel publizieren und archivieren |
| `wissensmanagement-chat` | KI-Chat nutzen |
| `wissensmanagement-admin` | Kategorien, Prompts, Export/Import, Einstellungen |

Berechtigungen werden ueber PortalCore geprueft (5-Minuten-Cache).

---

## Integration mit PortalCore

| Integration | Endpunkt | Beschreibung |
|------------|----------|-------------|
| **Authentifizierung** | JWT-Token | Geteiltes Secret, Token via Header oder Cookie |
| **Berechtigungen** | PortalCore API | Use-Case-Pruefung mit Caching |
| **LLM Chat-Proxy** | `POST /api/tenants/{id}/profile/llm/chat` | KI-Aufrufe |
| **LLM Modelle** | `GET /api/tenants/{id}/profile/llm` | Verfuegbare Modelle |
| **Aufgaben** | PortalCore Task-API | Artikel ↔ Aufgaben verknuepfen |
| **Dashboard-Widgets** | Widget-Registrierung | Bei App-Start |

---

## Datenbankmigrationen

| Version | Beschreibung |
|---------|-------------|
| V1 | Kernschema: Kategorien, Tags, Artikel, Versionen, Feedback |
| V3 | Gruppierungen |
| V4 | Hierarchie, Suche, Embedding-Status |
| V5 | Prompt-Konfigurationen |
| V6 | Erweiterte Chunk-Felder |
| V7 | Prompt-Kategorien |
| V8 | Organisationseinheit-Kontext |

---

## Weiterfuehrende Dokumentation

| Datei | Beschreibung |
|-------|-------------|
| [docs/arc42-architecture.md](docs/arc42-architecture.md) | Arc42 Architekturdokumentation |
| [docs/er-diagram.md](docs/er-diagram.md) | Vollstaendiges ER-Diagramm (Mermaid) |
| [portal-app.yaml](portal-app.yaml) | App-Manifest fuer PortalCore |
