package learnpay.email;

import learnpay.email.client.InfraiEmailClient;
import learnpay.email.config.EmailApiConfig;
import learnpay.email.domain.PaymentEvent;
import learnpay.email.service.LifecycleNotificationService;

public final class PaymentReceiptExample {
    private PaymentReceiptExample() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Pass the learner email as the only argument");
        var infrai = new InfraiEmailClient(EmailApiConfig.fromEnvironment());
        var service = new LifecycleNotificationService(infrai);
        var event = new PaymentEvent("pay_course_1042", args[0], "Avery", "Practical SQL",
                12900, "USD", PaymentEvent.RiskLevel.STANDARD);
        String messageId = service.notify(event);
        System.out.println("Receipt accepted with message_id=" + messageId);
    }
}
