package com.tutormgmt.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

/**
 * Auditing clock for {@code @CreatedDate} / {@code @LastModifiedDate}.
 * All timestamps are stored in UTC.
 */
@Configuration
class JpaConfig {

    @Component("auditingDateTimeProvider")
    static class AuditingDateTimeProvider implements DateTimeProvider {
        @Override
        public Optional<java.time.temporal.TemporalAccessor> getNow() {
            return Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
        }
    }
}
