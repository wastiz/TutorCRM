package com.tutormgmt.email;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    List<EmailTemplate> findByUserIdOrderByNameAsc(UUID userId);

    Optional<EmailTemplate> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserId(UUID userId);
}
