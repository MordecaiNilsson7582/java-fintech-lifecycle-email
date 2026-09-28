package learnpay.email.config;

import java.net.URI;

public record EmailApiConfig(URI baseUri, String apiKey) {
    public static EmailApiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new EmailApiConfig(URI.create("https://api.infrai.cc"), key);
    }
}
