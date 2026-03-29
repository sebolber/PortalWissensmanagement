# PortalWissensmanagement - Arc42 Architekturdokumentation

> Version 3.8.0 | Stand: 2026-03-29

## Inhaltsverzeichnis

1. [Einfuehrung und Ziele](#1-einfuehrung-und-ziele)
2. [Randbedingungen](#2-randbedingungen)
3. [Kontextabgrenzung](#3-kontextabgrenzung)
4. [Loesungsstrategie](#4-loesungsstrategie)
5. [Bausteinsicht](#5-bausteinsicht)
6. [Laufzeitsicht](#6-laufzeitsicht)
7. [Verteilungssicht](#7-verteilungssicht)
8. [Querschnittliche Konzepte](#8-querschnittliche-konzepte)
9. [Architekturentscheidungen](#9-architekturentscheidungen)
10. [Qualitaetsanforderungen](#10-qualitaetsanforderungen)
11. [Risiken und technische Schulden](#11-risiken-und-technische-schulden)
12. [Glossar](#12-glossar)

---

## 1. Einfuehrung und Ziele

### 1.1 Aufgabenstellung

Das PortalWissensmanagement ist eine **Portal-App** fuer hierarchische Wissensverwaltung mit KI-gestuetztem Chat. Kernfunktionen:

- **Hierarchische Artikelverwaltung** mit Baumstruktur (parent/children)
- **RAG-basierter Chat** mit Multi-Strategie-Retrieval (Volltext + Trigram Similarity)
- **Prompt-Template-Verwaltung** fuer wiederverwendbare KI-Prompts
- **Diktat-Funktion** (Rohtext via LLM in strukturierten Artikel umwandeln)
- **Versionierung** mit Aenderungshistorie
- **Feedback und Nutzungsanalyse**
- **Export/Import** fuer mandantenuebergreifende Datenmigration

### 1.2 Qualitaetsziele

| Prioritaet | Qualitaetsziel | Beschreibung |
|------------|----------------|--------------|
| 1 | **Wissensauffindbarkeit** | Hybrid-Suche (Volltext + Fuzzy) findet relevante Artikel zuverlaessig |
| 2 | **KI-Antwortqualitaet** | Multi-Strategie-Retrieval liefert praezisen Kontext fuer LLM |
| 3 | **Strukturierung** | Hierarchische Artikelstruktur mit Baum-Navigation |
| 4 | **Benutzerfreundlichkeit** | Chat-Interface, Diktat, Prompt-Templates |

### 1.3 Stakeholder

| Rolle | Erwartung |
|-------|-----------|
| **Wissensarbeiter** | Schnelles Finden und Erstellen von Wissensartikeln, KI-Chat |
| **Fachabteilungen** | Strukturiertes Wissen in Kategorien und Hierarchien |
| **Administratoren** | Kategorien, Gruppierungen, Prompts, Export/Import |
| **Betrieb** | Stabiler Container-Betrieb im PortalCore-Netzwerk |

---

## 2. Randbedingungen

### 2.1 Technische Randbedingungen

| Randbedingung | Beschreibung |
|---------------|--------------|
| Portal-App | Container im PortalCore-Oekosystem |
| Geteilte Datenbank | PostgreSQL mit Tabellen-Praefix `wm_` |
| JWT | Geteiltes Secret mit PortalCore |
| LLM-Proxy | LLM-Aufrufe ueber PortalCore-API |
| Backend | Java 21, Spring Boot 3.2.4 |
| Frontend | Angular 18, TypeScript 5.4 |
| Suche | PostgreSQL Volltext (German) + pg_trgm |

### 2.2 Konventionen

- Flyway-Migrationen mit `wm_`-Praefix, Historientabelle `wm_schema_history`
- UUID-Primaerschluessel
- REST-API unter `/api/`
- Multi-Tenancy via `tenant_id`

---

## 3. Kontextabgrenzung

### 3.1 Fachlicher Kontext

```
  Wissensarbeiter              Administrator
       |                            |
       v                            v
  +------------------------------------------+
  |         PortalWissensmanagement           |
  |                                          |
  |  - Artikel lesen/schreiben               |
  |  - Chat (RAG-basiert)                    |
  |  - Suche (Hybrid)                        |
  |  - Diktat (Text → Artikel)              |
  |  - Kategorien/Tags/Gruppierungen         |
  |  - Prompt-Templates                      |
  |  - Export/Import                          |
  +------------------+-----------------------+
                     |
        +------------+------------+
        |                         |
   +----v------+          +------v-------+
   | PortalCore|          | PostgreSQL   |
   | - Auth    |          | (geteilte DB)|
   | - LLM     |          | wm_*-Tabellen|
   | - Rechte  |          +--------------+
   | - Widgets |
   | - Tasks   |
   +-----------+
```

### 3.2 Technischer Kontext

| Schnittstelle | Richtung | Beschreibung |
|---------------|----------|-------------|
| PortalCore Auth | eingehend | JWT-Validierung |
| PortalCore Permissions | ausgehend | Berechtigungspruefung (gecacht) |
| PortalCore LLM | ausgehend | Chat-Proxy `/api/tenants/{id}/profile/llm/chat` |
| PortalCore LLM Models | ausgehend | Modellliste `/api/tenants/{id}/profile/llm` |
| PortalCore Tasks | ausgehend | Aufgaben-Integration |
| PortalCore Widgets | ausgehend | Widget-Registrierung |
| Browser | eingehend | Angular SPA (iframe) |
| PostgreSQL | ausgehend | JDBC (wm_*-Tabellen) |

---

## 4. Loesungsstrategie

| Entscheidung | Begruendung |
|--------------|-------------|
| **Multi-Strategie RAG** | Kombination aus Volltext-Suche, Trigram-Similarity und Article-Level-Fallback fuer beste Ergebnisse |
| **Chunk-basiertes Retrieval** | Artikel werden in Chunks aufgeteilt fuer praeziseres Matching |
| **Hierarchische Artikel** | Self-referential FK mit tree_path und depth fuer effiziente Baum-Queries |
| **Hybrid-Suche** | PostgreSQL tsvector (deutsches Stemming) + pg_trgm (Fuzzy-Matching) |
| **Prompt-Templates** | Wiederverwendbare Prompts fuer konsistente KI-Ergebnisse |
| **Diktat-Service** | Rohtext via LLM in strukturierten Artikel umwandeln |

---

## 5. Bausteinsicht

### 5.1 Ebene 1

```
+------------------------------------------------------------------+
|                    PortalWissensmanagement                         |
|                                                                    |
|  +-------------------+    REST API    +-------------------------+  |
|  |    Frontend       |<-------------->|      Backend            |  |
|  |    (Angular 18)   |                |   (Spring Boot 3.2)     |  |
|  +-------------------+                +----------+--------------+  |
|                                                  |                 |
|                                       +----------v--------------+  |
|                                       |    PostgreSQL (wm_*)    |  |
|                                       +-------------------------+  |
+------------------------------------------------------------------+
                    |
              +-----v------+
              |  PortalCore |
              +-------------+
```

### 5.2 Ebene 2 - Backend-Komponenten

| Komponente | Controller | Services | Beschreibung |
|------------|-----------|----------|--------------|
| **Artikel** | ArticleController | ArticleService, HierarchyService | CRUD, Hierarchie, Versionen, Statistik |
| **Chat** | ChatController | ChatService, RetrievalService, ChunkService | RAG-Chat mit Multi-Strategie-Retrieval |
| **Suche** | (in ArticleController) | SearchService | Hybrid-Suche (Volltext + Similarity) |
| **Kategorien** | CategoryController | CategoryService | Kategorieverwaltung |
| **Tags** | TagController | TagService | Tag-Verwaltung |
| **Gruppierungen** | GroupingController | GroupingService | Gruppierungsverwaltung |
| **Prompts** | PromptConfigController | PromptConfigService | Prompt-Template-Verwaltung |
| **Prompt-Kategorien** | PromptCategoryController | - | Prompt-Kategorisierung |
| **Diktat** | DictationController | DictationService | Rohtext → strukturierter Artikel |
| **Export/Import** | ExportImportController | ExportImportService | Mandantendaten Ex-/Import |
| **LLM** | - | LlmIntegrationService | PortalCore LLM-Proxy |
| **Feedback** | (in ArticleController) | FeedbackService | Bewertungssystem |
| **Analytics** | - | UsageTrackingService | Nutzungsverfolgung |
| **Sicherheit** | - | PermissionService | Berechtigungspruefung (gecacht) |
| **Tasks** | - | TaskIntegrationService | PortalCore-Aufgaben-Anbindung |
| **Widgets** | - | WidgetRegistrationService | Dashboard-Widget-Registrierung |

### 5.3 Ebene 2 - Frontend-Seiten

| Route | Komponente | Beschreibung |
|-------|-----------|-------------|
| `/` | StartseiteComponent | Startseite/Dashboard |
| `/artikel` | ArtikelListComponent | Artikelliste mit Filtern |
| `/artikel/neu` | ArtikelFormComponent | Artikel erstellen |
| `/artikel/:id` | ArtikelDetailComponent | Artikeldetail mit Hierarchie |
| `/artikel/:id/bearbeiten` | ArtikelFormComponent | Artikel bearbeiten |
| `/chat` | ChatComponent | RAG-Chat-Interface |
| `/suche` | SucheComponent | Volltextsuche |
| `/kategorien` | KategorienComponent | Kategorienverwaltung |
| `/konfiguration` | KonfigurationComponent | Einstellungen, Export/Import |

---

## 6. Laufzeitsicht

### 6.1 Chat-Nachricht senden (RAG-Pipeline)

```
Benutzer     Frontend    ChatCtrl    ChatService    RetrievalSvc    ChunkRepo     LlmIntegration
   |            |           |            |              |              |               |
   |-- Msg ---->|           |            |              |              |               |
   |            |-- POST -->|            |              |              |               |
   |            | /chat/send|-- send --->|              |              |               |
   |            |           |            |-- retrieve ->|              |               |
   |            |           |            |              |-- Fulltext ->|               |
   |            |           |            |              |<- Chunks ----|               |
   |            |           |            |              |-- Similarity>|               |
   |            |           |            |              |<- Chunks ----|               |
   |            |           |            |              |-- Fallback ->|               |
   |            |           |            |              |<- Articles --|               |
   |            |           |            |<- Context ---|              |               |
   |            |           |            |-- chat ------|--------------|-------------->|
   |            |           |            |              |              |   POST        |
   |            |           |            |              |              |   PortalCore  |
   |            |           |            |              |              |   /llm/chat   |
   |            |           |            |<- Response --|--------------|---------------|
   |            |           |            |-- save msg ->DB             |               |
   |            |           |<- Response |              |              |               |
   |            |<-- 200 ---|            |              |              |               |
   |<-- Antwort |           |            |              |              |               |
```

### 6.2 Diktat (Rohtext → Artikel)

```
Benutzer     Frontend    DictationCtrl   DictationSvc   LlmIntegration
   |            |             |                |               |
   |-- Text --->|             |                |               |
   |            |-- POST ---->|                |               |
   |            |/diktat/     |-- structure -->|               |
   |            |strukturieren|                |-- chat ------>|
   |            |             |                |  (System:     |
   |            |             |                |   "Strukturiere|
   |            |             |                |   als Artikel")|
   |            |             |                |<- Strukturiert|
   |            |             |<-- Artikel ----|               |
   |            |<-- 200 -----|                |               |
   |<-- Artikel |             |                |               |
```

### 6.3 Multi-Strategie-Retrieval

```
RetrievalService
   |
   |-- 1. Fulltext-Suche (tsvector, deutsches Stemming)
   |      → Exakte Schluesselwort-Treffer
   |
   |-- 2. Trigram-Similarity (pg_trgm)
   |      → Fuzzy-Matching bei Umformulierungen
   |
   |-- 3. Article-Level-Fallback
   |      → Komplette Artikel statt Chunks
   |
   |-- 4. Hybrid-Kombination
   |      → Gewichtete Zusammenfuehrung aller Strategien
   |
   └── Top-N Chunks als Kontext fuer LLM
```

---

## 7. Verteilungssicht

```
+---------------------------------------------------------------+
|                    Docker Host                                 |
|                                                                |
|  +------------------+    +-------------------------+           |
|  | PortalCore       |    | Wissensmanagement       |           |
|  | Backend :8080    |    | (Angular + Spring Boot) |           |
|  |                  |<---| :8081                   |           |
|  | - Auth/JWT       |    | - /api/artikel          |           |
|  | - LLM-Proxy      |    | - /api/chat            |           |
|  | - Permissions     |    | - /api/kategorien      |           |
|  | - Tasks           |    | - /api/prompts         |           |
|  | - Widgets         |    | - /api/diktat          |           |
|  +--------+---------+    +----------+--------------+           |
|           |                         |                          |
|           +------------+------------+                          |
|                        |                                       |
|              +---------v-----------+                           |
|              |   PostgreSQL 16     |                           |
|              |   :5432             |                           |
|              |   wm_*-Tabellen     |                           |
|              +---------------------+                           |
+---------------------------------------------------------------+
```

---

## 8. Querschnittliche Konzepte

### 8.1 Multi-Tenancy
- Alle Tabellen enthalten `tenant_id`
- SecurityHelper extrahiert Mandant aus JWT
- Organisationseinheit-Kontext fuer Artikel, Kategorien und Chat-Sessions

### 8.2 Berechtigungssystem

| Use-Case | Beschreibung |
|----------|-------------|
| `wissensmanagement-lesen` | Artikel lesen, Suche nutzen |
| `wissensmanagement-schreiben` | Artikel erstellen und bearbeiten |
| `wissensmanagement-veroeffentlichen` | Artikel publizieren und archivieren |
| `wissensmanagement-chat` | KI-Chat nutzen |
| `wissensmanagement-admin` | Kategorien, Prompts, Export/Import, Einstellungen |

### 8.3 Artikelhierarchie
- Self-referential FK (`parent_article_id`)
- `tree_path`: Pfad fuer effiziente Baum-Queries (z.B. `/root/sub1/sub2`)
- `depth`: Tiefe im Baum
- `sort_order`: Reihenfolge innerhalb einer Ebene
- Breadcrumb-Generierung ueber Pfad

### 8.4 Versionierung
- Automatische Version bei jeder Aktualisierung
- `wm_article_versions` speichert Titel, Inhalt, Summary, Aenderungsnotiz
- Aenderungsnachverfolgung (wer, wann, was)

### 8.5 Chunk-Management
- Artikel werden in Chunks aufgeteilt (ChunkService)
- Chunks enthalten: Inhalt, Heading, Artikeltitel, Token-Count
- `embedding_status`: Vorbereitung fuer kuenftige Vektor-Embeddings

---

## 9. Architekturentscheidungen

### ADR-1: Multi-Strategie-Retrieval statt Single-Search

**Kontext:** Benutzer formulieren Fragen unterschiedlich - exakt oder umschrieben.

**Entscheidung:** Vier Retrieval-Strategien parallel ausfuehren und kombinieren.

**Begruendung:**
- Volltext (tsvector): Praezise bei exakten Begriffen
- Trigram (pg_trgm): Robust bei Tippfehlern und Umformulierungen
- Article-Level: Fallback wenn Chunk-Suche zu wenig liefert
- Hybrid: Beste Ergebnisse durch gewichtete Kombination

### ADR-2: Hierarchische Artikel statt flache Struktur

**Kontext:** Wissen ist oft hierarchisch organisiert (Kapitel, Unterkapitel).

**Entscheidung:** Self-referential FK mit tree_path fuer Baumstruktur.

**Begruendung:**
- Natuerliche Wissensorganisation
- Effiziente Baum-Queries ueber tree_path
- Breadcrumb-Navigation
- Drag & Drop-Umordnung

### ADR-3: Prompt-Templates statt fest codierter Prompts

**Kontext:** KI-Prompts muessen anpassbar sein.

**Entscheidung:** Verwaltbare Prompt-Templates mit Kategorien.

**Begruendung:**
- Administratoren koennen Prompts ohne Code-Aenderung anpassen
- Prompt-Kategorien fuer Uebersichtlichkeit
- Aktivierung/Deaktivierung einzelner Prompts

---

## 10. Qualitaetsanforderungen

| ID | Szenario | Qualitaetsmerkmal |
|----|----------|-------------------|
| QS-1 | Suche findet relevante Artikel auch bei umformulierten Anfragen | Auffindbarkeit |
| QS-2 | Chat-Antworten basieren auf aktuellen Artikeln, nicht auf veraltetem Wissen | KI-Qualitaet |
| QS-3 | Artikel-Hierarchie laesst sich per Drag & Drop umorganisieren | Benutzbarkeit |
| QS-4 | Aenderungen an Artikeln erzeugen automatisch eine neue Version | Zuverlaessigkeit |
| QS-5 | Export/Import ermoeglicht Datenmigration zwischen Mandanten | Wartbarkeit |

---

## 11. Risiken und technische Schulden

| Risiko | Beschreibung | Massnahme |
|--------|--------------|-----------|
| **Embedding-Status** | `embedding_status`-Feld vorbereitet, aber Vektor-Embeddings noch nicht implementiert | pgvector-Integration planen |
| **LLM-Abhaengigkeit** | Chat-Funktion haengt von PortalCore-LLM-Proxy ab | Fehlerbehandlung, Offline-Hinweis |
| **Chunk-Qualitaet** | Chunk-Aufteilung beeinflusst RAG-Qualitaet | Chunk-Groesse und Ueberlappung optimieren |
| **8 Migrationen** | Wachsende Flyway-Migrationen | Bei Bedarf Baseline konsolidieren |

---

## 12. Glossar

| Begriff | Beschreibung |
|---------|--------------|
| **RAG** | Retrieval-Augmented Generation - Wissen aus DB als Kontext fuer LLM |
| **Chunk** | Textabschnitt eines Artikels fuer praezises Retrieval |
| **Trigram Similarity** | PostgreSQL pg_trgm-Erweiterung fuer Fuzzy-Text-Matching |
| **tsvector** | PostgreSQL Volltext-Suchvektor mit Stemming |
| **Hierarchie** | Baumstruktur der Artikel (parent/children) |
| **tree_path** | Pfad im Artikelbaum fuer effiziente Queries |
| **Gruppierung** | Organisatorische Zusammenfassung von Artikeln |
| **Prompt-Template** | Wiederverwendbare Vorlage fuer LLM-Anfragen |
| **Diktat** | Umwandlung von Rohtext in strukturierten Artikel via LLM |
| **Mandant (Tenant)** | Isolierte Organisationseinheit |
| **Portal-App** | Anwendung im PortalCore-Oekosystem |
