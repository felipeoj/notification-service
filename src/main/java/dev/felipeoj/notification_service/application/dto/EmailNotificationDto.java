package dev.felipeoj.notification_service.application.dto;

import dev.felipeoj.notification_service.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDto {
    private String to;
    private String subject;
    private NotificationType type;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;
}
