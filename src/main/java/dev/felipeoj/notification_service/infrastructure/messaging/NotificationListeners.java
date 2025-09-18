package dev.felipeoj.notification_service.infrastructure.messaging;

import ch.qos.logback.core.encoder.EchoEncoder;
import dev.felipeoj.notification_service.application.dto.EmailNotificationDto;
import dev.felipeoj.notification_service.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class NotificationListeners {
    private static final Logger log = LoggerFactory.getLogger(NotificationListeners.class);
    private final NotificationService notificationService;

    private LocalDateTime toLocalDateTimeUtc(Object timestamp) {
        if (timestamp == null) return null;

        try {
            if (timestamp instanceof String) {
                Instant i = Instant.parse(timestamp.toString());
                return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
            }

            if (timestamp instanceof Number) {
                long epochSeconds = ((Number) timestamp).longValue();
                Instant i = Instant.ofEpochSecond(epochSeconds);
                return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
            }

            String str = timestamp.toString();
            if (str.matches("\\d+(\\.\\d+)?")) {
                double epochSeconds = Double.parseDouble(str);
                Instant i = Instant.ofEpochSecond((long) epochSeconds);
                return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
            }

            Instant i = Instant.parse(str);
            return LocalDateTime.ofInstant(i, ZoneOffset.UTC);

        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using now()", timestamp);
            return LocalDateTime.now();
        }
    }

    @RabbitListener(queues = "${app.events.queue.user-created}")
    public void onUserCreated(@Payload Map<String, Object> payload){
        try {
            Object eventType = payload.get("eventType");
            Object occurredAt = payload.get("occurredAt");
            Object dataObj = payload.get("data");

            if (!(dataObj instanceof Map<?, ?> data)){
                log.error("Invalid payload. payload={}", payload);
                return;
            }

            Object userName = data.get("userName");
            Object userEmail = data.get("userEmail");
            Object createdAt = data.get("createdAt");

            if(userName == null || userEmail == null){
                log.error("Missing username/userEmail in event. data={}", data);
                return;
            }

            LocalDateTime createdAtLdt = toLocalDateTimeUtc(createdAt);
            if (createdAtLdt == null){
                createdAtLdt = toLocalDateTimeUtc(occurredAt);
            }

            EmailNotificationDto dto = new EmailNotificationDto();
            dto.setType(dev.felipeoj.notification_service.domain.enums.NotificationType.USER_CREATED);
            dto.setTo(userEmail.toString());
            dto.setSubject(null);
            dto.setUserName(userName.toString());
            dto.setUserEmail(userEmail.toString());
            dto.setCreatedAt(createdAtLdt);

            boolean sent = notificationService.process(dto);
            if (sent) {
                log.info("Processed USER_CREATED notification for email={}", dto.getUserEmail());
            } else {
                log.warn("Failed to process USER_CREATED notification for email={}", dto.getUserEmail());
            }

        }catch (Exception e){
            log.error("Error processing USER_CREATED event", e);
        }
    }


    @RabbitListener(queues = "${app.events.queue.user-login}")
    public void onUserLogin(@Payload Map<String, Object> payload) {
        try {
            Object eventType = payload.get("eventType");
            Object occurredAt = payload.get("occurredAt");
            Object dataObj = payload.get("data");

            if (!(dataObj instanceof Map<?, ?> data)) {
                log.error("Invalid payload. payload={}", payload);
                return;
            }

            Object userName = data.get("userName");
            Object userEmail = data.get("userEmail");
            Object timestamp = data.get("timestamp");

            if (userName == null || userEmail == null) {
                log.error("Missing username/userEmail in event. data={}", data);
                return;
            }

            LocalDateTime when = toLocalDateTimeUtc(timestamp);
            if (when == null) {
                when = toLocalDateTimeUtc(occurredAt);
            }

            EmailNotificationDto dto = new EmailNotificationDto();
            dto.setType(dev.felipeoj.notification_service.domain.enums.NotificationType.LOGIN);
            dto.setTo(userEmail.toString());
            dto.setSubject(null);
            dto.setUserName(userName.toString());
            dto.setUserEmail(userEmail.toString());
            dto.setCreatedAt(when);

            boolean sent = notificationService.process(dto);
            if (sent) {
                log.info("Processed LOGIN notification for email={}", dto.getUserEmail());
            } else {
                log.warn("Failed to process LOGIN notification for email={}", dto.getUserEmail());
            }
        } catch (Exception e) {
            log.error("Error processing LOGIN event", e);
        }
    }
}
