package learnpay.email.client;

import learnpay.email.config.EmailApiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiEmailClient {
    private final EmailApiConfig config;
    private final HttpClient http;

    public InfraiEmailClient(EmailApiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiEmailClient(EmailApiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public void updateTemplate(String templateId, String subject, String html) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", subject);
        body.put("html", html);
        request("PATCH", "/v1/email/template/update/" + pathSegment(templateId), body, "template-" + templateId);
    }

    public String previewTemplate(String templateId, Map<String, String> templateVars) {
        return request("POST", "/v1/email/template/preview/" + pathSegment(templateId),
                Map.of("vars", templateVars), null);
    }

    public String send(String to, String subject, String html, String eventId) {
        // Canonical operation: infrai.email.send
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", to);
        body.put("subject", subject);
        body.put("html", html);
        String envelope = request("POST", "/v1/email/send", body, eventId);
        return Json.stringField(envelope, "message_id");
    }

    private String request(String method, String path, Map<String, ?> body, String idempotencyKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUri().resolve(path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json");
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);

        String payload = Json.object(body);
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(payload)).build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                boolean ok = Json.booleanField(envelope, "ok");
                if (response.statusCode() == 429) {
                    pause(retryDelayMillis(response, attempt));
                    continue;
                }
                if (response.statusCode() >= 500) {
                    throw new IllegalStateException("Email service transport response: " + response.statusCode());
                }
                if (!ok) {
                    throw new InfraiApiException(Json.stringField(envelope, "code"), response.statusCode());
                }
                return envelope;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not reach email service", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Email request interrupted", exception);
            }
        }
        throw new IllegalStateException("Email request retry budget exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1_000L)
                .orElse(250L * (1L << attempt));
    }

    private static void pause(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static String pathSegment(String value) {
        if (!value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid template id");
        return value;
    }
}
