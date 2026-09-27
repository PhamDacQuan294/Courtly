package com.courtly.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SepayWebhookLogRepository extends JpaRepository<SepayWebhookLog, UUID> {
}
