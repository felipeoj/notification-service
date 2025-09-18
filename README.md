# Notification Service

Microserviço responsável por processar eventos do ecossistema e enviar notificações por e-mail. Atua como consumidor de mensagens publicadas pelo serviço de usuários [`users-crud-clean-arch`](https://github.com/felipeoj/users-crud-clean-arch) via RabbitMQ.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-blue.svg)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Arquitetura

Este microserviço implementa **Clean Architecture** com as seguintes camadas:

- **Domain**: Entidades, enums e regras de negócio
- **Application**: DTOs, serviços de caso de uso e orquestração
- **Infrastructure**: Persistência, mensageria e integrações externas
- **Interfaces**: Controllers e APIs (futuro)

## Integração com o Ecossistema

### Repositório Principal
- **CRUD de Usuários**: [`users-crud-clean-arch`](https://github.com/felipeoj/users-crud-clean-arch)
- **Responsabilidade**: Publicar eventos de domínio via RabbitMQ
- **Eventos**: `USER_CREATED`, `LOGIN`

### Fluxo de Dados
```
[CRUD] → [RabbitMQ] → [Notification Service] → [Email Provider]
   ↓           ↓              ↓                    ↓
 Usuário   Eventos      Processamento        E-mail
```

## Tecnologias

- **Java 21** - Linguagem de programação
- **Spring Boot 3.5.5** - Framework principal
- **Spring AMQP** - Integração com RabbitMQ
- **Spring Mail** - Envio de e-mails
- **Spring Data JPA** - Persistência de dados
- **H2 Database** - Banco em memória (desenvolvimento)
- **Lombok** - Redução de boilerplate
- **Docker Compose** - Infraestrutura local

## Funcionalidades

### Eventos Suportados
- **USER_CREATED**: Notificação de boas-vindas para novos usuários
- **LOGIN**: Notificação de segurança para logins realizados

### Características
- Processamento assíncrono via RabbitMQ
- Persistência de histórico de notificações
- Tratamento de erros e retry automático
- Logs estruturados para observabilidade
- Configuração flexível por ambiente
- Idempotência por eventId

## Contrato de Mensagens

### Envelope de Evento
```json
{
  "eventType": "USER_CREATED | LOGIN",
  "occurredAt": "2025-09-15T13:45:20Z",
  "data": {
    "userName": "string",
    "userEmail": "string",
    "createdAt": "2025-09-15T13:45:20Z"
  }
}
```

### Headers Recomendados
- `eventId`: UUID único para rastreamento
- `sourceApp`: Identificação da aplicação origem
- `version`: Versão do contrato de evento

## Configuração

### application.properties
```properties
# Aplicação
spring.application.name=notification-service
server.port=8081

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Eventos
app.events.exchange=notifications.exchange
app.events.routing-key.user-created=user.created
app.events.routing-key.user-login=user.login
app.events.queue.user-created=notifications.email.usercreated.q
app.events.queue.user-login=notifications.email.login.q

# E-mail (MailHog para desenvolvimento)
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.from=noreply@localhost

# Banco de dados
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

## Executando Localmente

### Pré-requisitos
- Java 21+
- Maven 3.6+
- Docker e Docker Compose

### Subir Infraestrutura
```bash
# Subir RabbitMQ e MailHog
docker compose -f docker/docker-compose.yaml up -d

# Verificar serviços
# RabbitMQ Management: http://localhost:15672 (guest/guest)
# MailHog UI: http://localhost:8025
```

### Executar Aplicação
```bash
# Clonar e executar
git clone https://github.com/felipeoj/notification-service.git
cd notification-service
./mvnw spring-boot:run
```

### Testar Integração
```bash
# 1. Subir o CRUD principal
cd ../users-crud-clean-arch
./mvnw spring-boot:run

# 2. Criar usuário (gera evento USER_CREATED)
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teste",
    "email": "teste@example.com",
    "firstName": "Teste",
    "lastName": "Silva",
    "password": "123456"
  }'

# 3. Fazer login (gera evento LOGIN)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "teste",
    "password": "123456"
  }'
```

### Verificar Resultados
- **E-mails**: http://localhost:8025
- **RabbitMQ**: http://localhost:15672
- **H2 Console**: http://localhost:8081/h2-console

## Monitoramento

### Logs Estruturados
```json
{
  "level": "INFO",
  "message": "Email sent successfully for type=USER_CREATED, user=teste@example.com",
  "timestamp": "2025-09-18T15:30:00Z",
  "service": "notification-service"
}
```

### Métricas de Banco
- **Tabela**: `notifications`
- **Campos**: `sent`, `error_message`, `type`, `created_at`
- **Consultas**: Notificações por status, tipo, período

## Estrutura do Projeto

```
src/main/java/dev/felipeoj/notification_service/
├── domain/
│   ├── entity/
│   │   └── Notification.java
│   └── enums/
│       └── NotificationType.java
├── application/
│   ├── dto/
│   │   └── EmailNotificationDto.java
│   └── service/
│       └── NotificationService.java
├── infrastructure/
│   ├── config/
│   │   └── RabbitMQConfig.java
│   ├── messaging/
│   │   └── NotificationListeners.java
│   ├── repository/
│   │   └── NotificationRepository.java
│   └── service/
│       └── EmailService.java
└── interfaces/
    └── (futuro: controllers, APIs)
```

## Fluxo de Processamento

1. **Recepção**: Listener recebe evento do RabbitMQ
2. **Validação**: Verifica estrutura e campos obrigatórios
3. **Persistência**: Salva notificação como `PENDING`
4. **Envio**: Chama serviço de e-mail
5. **Atualização**: Marca como `SENT` ou `FAILED`
6. **Log**: Registra resultado para observabilidade


## Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.

## Links Relacionados

- **CRUD Principal**: [users-crud-clean-arch](https://github.com/felipeoj/users-crud-clean-arch)
- **Documentação Spring Boot**: [spring.io](https://spring.io/projects/spring-boot)
- **RabbitMQ Docs**: [rabbitmq.com](https://www.rabbitmq.com/documentation.html)

## Suporte

Para dúvidas ou sugestões, abra uma [issue](https://github.com/felipeoj/notification-service/issues) ou entre em contato.

---

**Desenvolvido com Spring Boot e Clean Architecture**
