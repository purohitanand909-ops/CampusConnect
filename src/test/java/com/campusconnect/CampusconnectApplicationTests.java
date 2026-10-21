package com.campusconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full Spring context integration test.
 * Uses the 'local' profile so application-local.properties is loaded
 * (which provides the DB password), allowing Flyway to run migrations
 * and Hibernate to validate the schema against a real MySQL database.
 */
@SpringBootTest
@ActiveProfiles("local")
class CampusconnectApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the Spring context starts without errors.
        // This verifies: DB connection, Flyway migrations, JPA entity validation.
    }

}
