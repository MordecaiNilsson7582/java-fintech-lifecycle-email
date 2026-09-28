package learnpay.email.domain;

public sealed interface NotificationDecision {
    record Ready(String auditReference, String subject, String html) implements NotificationDecision {}
    record Held(String auditReference, String reason) implements NotificationDecision {}
}
