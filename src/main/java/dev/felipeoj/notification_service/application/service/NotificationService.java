package dev.felipeoj.notification_service.application.service;

import dev.felipeoj.notification_service.application.dto.EmailNotificationDto;
import dev.felipeoj.notification_service.domain.entity.Notification;
import dev.felipeoj.notification_service.infrastructure.repository.NotificationRepository;
import dev.felipeoj.notification_service.infrastructure.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Transactional
    public boolean process(EmailNotificationDto dto){
        if(dto == null || dto.getType() == null){
            log.error("Invalid DTO: null or missing type.");
            return false;
        }
        String to = (dto.getTo() != null && !dto.getTo().isBlank())
                ? dto.getTo()
                : dto.getUserEmail();

        if (to == null || to.isBlank()) {
            log.error("Recipient email (to) is missing");
            return false;
        }

        String userName = dto.getUserName();
        if (userName == null || userName.trim().isEmpty()) {
            log.error("Recipient username is missing. type={}", dto.getType());
            return false;
        }

        String subject = (dto.getSubject() != null && !dto.getSubject().isBlank())
                ? dto.getSubject()
                : "System notification";

        if (dto.getCreatedAt() == null) {
            dto.setCreatedAt(java.time.LocalDateTime.now());
        }


        Notification notification = new Notification();
        notification.setTo(to);
        notification.setSubject(subject);
        notification.setType(dto.getType());
        notification.setUserName(userName);
        notification.setUserEmail(dto.getUserEmail());
        notification.setCreatedAt(dto.getCreatedAt());
        notification.setSent(false);
        notification.setErrorMessage(null);

        Notification saved = notificationRepository.save(notification);

        boolean emailSent = false;
        try {
            emailSent = emailService.send(dto);
            if (emailSent){
                saved.setSent(true);
                log.info("Email sent successfully for type={}, user={}", dto.getType(), dto.getUserName());
            }else {
                saved.setSent(false);
                saved.setErrorMessage("Email service returned false");
                log.error("Email service failed for type={}, user={}", dto.getType(), dto.getUserName());
            }
        }catch (Exception e){
            saved.setSent(false);
            saved.setErrorMessage(e.getMessage());
            log.error("Exception sending email for type={}, user={}", dto.getType(), dto.getUserName(), e );
        }
        notificationRepository.save(saved);
        return emailSent;
    }
}
