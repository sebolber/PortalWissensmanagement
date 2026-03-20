-- Dieses Script wird beim Portal-Deployment ausgefuehrt, um die Datenbank anzulegen.
-- Es kann manuell oder als Teil des Deployments auf dem PostgreSQL-Container ausgefuehrt werden.
SELECT 'CREATE DATABASE wissensmanagement OWNER portal'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'wissensmanagement')\gexec
