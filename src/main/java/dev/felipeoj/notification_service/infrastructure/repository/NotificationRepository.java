package dev.felipeoj.notification_service.infrastructure.repository;

import dev.felipeoj.notification_service.domain.entity.Notification;
import dev.felipeoj.notification_service.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findBySentFalse();
    List<Notification> findByType(NotificationType type);
    List<Notification> findByUserEmail(String userEmail);
}
