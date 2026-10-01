package org.demo.whs.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionAuthorizationMigrationTest {

    @Test
    void should_PreservePrivilegeMappingsAndBeIdempotent_When_MigrationRuns() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:permission_migration;MODE=MySQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE permissions (id VARCHAR(36) PRIMARY KEY, code VARCHAR(50) UNIQUE, "
                        + "name VARCHAR(100) UNIQUE, resource VARCHAR(100), action VARCHAR(50), description TEXT)");
                statement.execute("CREATE TABLE roles (id VARCHAR(36) PRIMARY KEY, code VARCHAR(50), name VARCHAR(50))");
                statement.execute("CREATE TABLE role_permissions (role_id VARCHAR(36), permission_id VARCHAR(36), "
                        + "PRIMARY KEY (role_id, permission_id))");
                statement.execute("INSERT INTO roles VALUES ('admin', 'ROLE_ADMIN', 'ADMIN'), "
                        + "('manager', 'ROLE_MANAGER', 'MANAGER'), ('custom', 'ROLE_CUSTOM', 'CUSTOM')");
            }
            var migration = new ClassPathResource(
                    "db/migration/V20261001_01__replace_role_authorization_with_permissions.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT COUNT(*) FROM permissions")) {
                    result.next();
                    assertEquals(7, result.getInt(1));
                }
                try (var result = statement.executeQuery(
                        "SELECT role_id, COUNT(*) FROM role_permissions GROUP BY role_id ORDER BY role_id")) {
                    result.next();
                    assertEquals("admin", result.getString(1));
                    assertEquals(7, result.getInt(2));
                    result.next();
                    assertEquals("manager", result.getString(1));
                    assertEquals(1, result.getInt(2));
                }
            }
        }
    }
}
