package dev.felipeoj.notification_service.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    @Value("${app.events.exchange}")
    private String exchangeName;

    @Value("${app.events.routing-key.user-created}")
    private String userCreatedRoutingKey;

    @Value("${app.events.routing-key.user-login}")
    private String userLoginRoutingKey;

    @Value("${app.events.queue.user-created}")
    private String userCreatedQueue;

    @Value("${app.events.queue.user-login}")
    private String userLoginQueue;

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(exchangeName);
     }

    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(userCreatedQueue).build();
    }

    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable(userLoginQueue).build();
    }

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(notificationsExchange())
                .with(userCreatedRoutingKey);
    }

    @Bean
    public Binding userLoginBinding() {
        return BindingBuilder
                .bind(userLoginQueue())
                .to(notificationsExchange())
                .with(userLoginRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
