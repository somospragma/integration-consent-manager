package com.pragma.openfinance.notification.domain.port;

import com.pragma.openfinance.notification.domain.model.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(UUID webhookId, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(
            WebhookDelivery.DeliveryStatus status, Instant now);
}
