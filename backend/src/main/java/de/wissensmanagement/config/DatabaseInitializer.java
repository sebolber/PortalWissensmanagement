package de.wissensmanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Erstellt die Datenbank 'wissensmanagement' automatisch, falls sie noch nicht existiert.
 * Verbindet sich dazu mit der Standard-Datenbank 'postgres' auf demselben Server.
 */
@Configuration
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Bean(initMethod = "run")
    public DatabaseCreator databaseCreator() {
        return new DatabaseCreator();
    }

    @Bean
    @DependsOn("databaseCreator")
    public FlywayMigrationInitializer flywayInitializer(org.flywaydb.core.Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    public static class DatabaseCreator {
        public void run() {
            String dbHost = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
            String dbPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432";
            String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "wissensmanagement";
            String dbUser = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "portal";
            String dbPass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "portal";

            String adminUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres";

            try (Connection conn = DriverManager.getConnection(adminUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery(
                        "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
                if (!rs.next()) {
                    stmt.executeUpdate("CREATE DATABASE " + dbName);
                    log.info("Datenbank '{}' erstellt", dbName);
                } else {
                    log.info("Datenbank '{}' existiert bereits", dbName);
                }
            } catch (Exception e) {
                log.warn("Datenbank-Erstellung fehlgeschlagen (evtl. existiert sie bereits): {}", e.getMessage());
            }
        }
    }
}
