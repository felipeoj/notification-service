package dev.felipeoj.notification_service.infrastructure.service;

import dev.felipeoj.notification_service.application.dto.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);


    @Value("${spring.mail.from:noreply@localhost}")
    private String from;

    public boolean send(EmailNotificationDto dto){
        String to = (dto.getTo() != null && !dto.getTo().isBlank())
                ? dto.getTo()
                : dto.getUserEmail();

        if( to == null || to.isBlank()){
            log.error("Email destination is missing");
            return false;
        }

        String subject;
        switch (dto.getType()) {
            case LOGIN -> subject = "Login efetuado";
            case USER_CREATED -> subject = "Bem-Vindo!";
            default -> subject = "Notificação";
        }
        if (dto.getSubject() != null && !dto.getSubject().isBlank()){
            subject = dto.getSubject();
        }

        String when = dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "agora";
        String body;
        switch (dto.getType()){
            case LOGIN -> body = "Olá " + dto.getUserName() + ", detectamos um login na sua conta em " + when + ".";
            case USER_CREATED -> body = "Olá " + dto.getUserName() + ", seu cadastro foi criado em " + when + ".";
            default -> body = "Olá " + dto.getUserName() + ", você tem uma nova notificação.";
        }

        try{
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setFrom(from);
            msg.setText(body);

            javaMailSender.send(msg);
            log.info("Email sent to={}, type={}, subject={}", to, dto.getType(), subject);
            return true;
        }catch (Exception e) {
            log.error("Failed to send email to={}, type={}", to, dto.getType(), e);
            return false;
        }
    }
}
