package learnpay.email.service;

import learnpay.email.domain.NotificationDecision;
import learnpay.email.domain.PaymentEvent;

public final class LifecycleNotificationServiceTest {
    public static void main(String[] args) {
        var service = new LifecycleNotificationService(null);
        var event = new PaymentEvent("pay_review_27", "learner@example.com", "Morgan", "Fractions Lab",
                4500, "USD", PaymentEvent.RiskLevel.REVIEW);

        NotificationDecision result = service.decide(event);
        if (!(result instanceof NotificationDecision.Held held)) {
            throw new AssertionError("A review-risk payment must be held");
        }
        if (!held.auditReference().equals("payment:pay_review_27")) {
            throw new AssertionError("The audit reference must retain the payment event id");
        }
        System.out.println("PASS review-risk payment is held with an audit reference");
    }
}
