package com.pragma.openfinance.notification.domain.port;

import com.pragma.openfinance.notification.domain.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByTppId(String tppId);

    List<Webhook> findByTppIdAndStatus(String tppId, Webhook.WebhookStatus status);
}
