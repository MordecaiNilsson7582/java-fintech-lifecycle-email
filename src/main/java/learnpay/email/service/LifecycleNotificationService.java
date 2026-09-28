package learnpay.email.service;

import learnpay.email.client.InfraiEmailClient;
import learnpay.email.domain.NotificationDecision;
import learnpay.email.domain.PaymentEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class LifecycleNotificationService {
    private final InfraiEmailClient email;

    public LifecycleNotificationService(InfraiEmailClient email) {
        this.email = email;
    }

    public NotificationDecision decide(PaymentEvent event) {
        String auditReference = "payment:" + event.eventId();
        if (event.riskLevel() == PaymentEvent.RiskLevel.REVIEW) {
            return new NotificationDecision.Held(auditReference, "manual payment review");
        }
        String amount = BigDecimal.valueOf(event.amountMinor(), 2)
                .setScale(2, RoundingMode.UNNECESSARY) + " " + event.currency();
        String subject = "Payment received for " + event.courseName();
        String html = "<h1>Payment received</h1><p>" + escape(event.learnerName())
                + ", your " + escape(event.courseName()) + " enrollment payment of "
                + escape(amount) + " is recorded.</p><p>Reference: " + escape(auditReference) + "</p>";
        return new NotificationDecision.Ready(auditReference, subject, html);
    }

    public String notify(PaymentEvent event) {
        NotificationDecision decision = decide(event);
        if (decision instanceof NotificationDecision.Held held) {
            return held.auditReference();
        }
        NotificationDecision.Ready ready = (NotificationDecision.Ready) decision;
        return email.send(event.learnerEmail(), ready.subject(), ready.html(), event.eventId());
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
