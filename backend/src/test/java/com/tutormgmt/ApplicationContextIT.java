package com.tutormgmt;

import com.tutormgmt.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;

class ApplicationContextIT extends AbstractPostgresIT {

    @Test
    void contextLoads() {
        // Verifies wiring + Liquibase migrations apply against a real PostgreSQL.
    }
}
