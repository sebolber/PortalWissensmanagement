CREATE TABLE artikel (
    id VARCHAR(36) PRIMARY KEY,
    titel VARCHAR(500) NOT NULL,
    inhalt TEXT,
    kategorie VARCHAR(200),
    autor VARCHAR(200),
    erstellt_am TIMESTAMP DEFAULT NOW(),
    aktualisiert_am TIMESTAMP DEFAULT NOW()
);

-- Beispieldaten
INSERT INTO artikel (id, titel, inhalt, kategorie, autor) VALUES
('a1', 'Willkommen im Wissensmanagement',
 'Das Wissensmanagement ist die zentrale Plattform fuer Dokumentationen, Anleitungen und Fachwissen Ihrer Organisation. Hier koennen Sie Wissensbeitraege erstellen, bearbeiten und mit Ihren Kolleginnen und Kollegen teilen.',
 'Allgemein', 'System'),
('a2', 'Erste Schritte',
 'So nutzen Sie das Wissensmanagement optimal: 1. Durchsuchen Sie vorhandene Artikel nach Kategorien. 2. Nutzen Sie die Volltextsuche fuer gezielte Recherche. 3. Erstellen Sie neue Artikel, um Ihr Wissen zu teilen. 4. Halten Sie Artikel aktuell, indem Sie sie regelmaessig ueberpruefen.',
 'Anleitungen', 'System'),
('a3', 'Datenschutz im Gesundheitswesen',
 'Der Umgang mit personenbezogenen Daten im Gesundheitswesen unterliegt besonderen Anforderungen. Die DSGVO und das Bundesdatenschutzgesetz (BDSG) definieren strenge Vorgaben fuer die Verarbeitung von Gesundheitsdaten. Beachten Sie insbesondere: Datensparsamkeit, Zweckbindung, Einwilligung der Betroffenen und technisch-organisatorische Massnahmen.',
 'Compliance', 'System'),
('a4', 'IT-Sicherheitsrichtlinien',
 'Unsere IT-Sicherheitsrichtlinien umfassen: Passwortkomplexitaet (mind. 12 Zeichen, Gross-/Kleinbuchstaben, Zahlen, Sonderzeichen), Zwei-Faktor-Authentifizierung fuer alle kritischen Systeme, regelmaessige Sicherheitsupdates und Schulungen fuer alle Mitarbeitenden.',
 'IT-Sicherheit', 'System');
