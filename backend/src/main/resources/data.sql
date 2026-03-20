-- Beispieldaten (nur einfuegen wenn Tabelle leer)
MERGE INTO artikel (id, titel, inhalt, kategorie, autor, erstellt_am, aktualisiert_am) KEY(id) VALUES
('a1', 'Willkommen im Wissensmanagement',
 'Das Wissensmanagement ist die zentrale Plattform fuer Dokumentationen, Anleitungen und Fachwissen Ihrer Organisation. Hier koennen Sie Wissensbeitraege erstellen, bearbeiten und mit Ihren Kolleginnen und Kollegen teilen.',
 'Allgemein', 'System', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a2', 'Erste Schritte',
 'So nutzen Sie das Wissensmanagement optimal: 1. Durchsuchen Sie vorhandene Artikel nach Kategorien. 2. Nutzen Sie die Volltextsuche fuer gezielte Recherche. 3. Erstellen Sie neue Artikel, um Ihr Wissen zu teilen. 4. Halten Sie Artikel aktuell, indem Sie sie regelmaessig ueberpruefen.',
 'Anleitungen', 'System', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a3', 'Datenschutz im Gesundheitswesen',
 'Der Umgang mit personenbezogenen Daten im Gesundheitswesen unterliegt besonderen Anforderungen. Die DSGVO und das Bundesdatenschutzgesetz (BDSG) definieren strenge Vorgaben fuer die Verarbeitung von Gesundheitsdaten.',
 'Compliance', 'System', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a4', 'IT-Sicherheitsrichtlinien',
 'Unsere IT-Sicherheitsrichtlinien umfassen: Passwortkomplexitaet, Zwei-Faktor-Authentifizierung fuer alle kritischen Systeme, regelmaessige Sicherheitsupdates und Schulungen fuer alle Mitarbeitenden.',
 'IT-Sicherheit', 'System', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
