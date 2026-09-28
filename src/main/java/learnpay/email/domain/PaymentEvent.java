package learnpay.email.domain;

import java.util.Objects;

public record PaymentEvent(
        String eventId,
        String learnerEmail,
        String learnerName,
        String courseName,
        long amountMinor,
        String currency,
        RiskLevel riskLevel
) {
    public PaymentEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(learnerEmail);
        Objects.requireNonNull(learnerName);
        Objects.requireNonNull(courseName);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(riskLevel);
        if (amountMinor <= 0) throw new IllegalArgumentException("amountMinor must be positive");
    }

    public enum RiskLevel { STANDARD, REVIEW }
}
