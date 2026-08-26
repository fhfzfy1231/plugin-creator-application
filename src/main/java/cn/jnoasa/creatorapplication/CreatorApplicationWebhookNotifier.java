package cn.jnoasa.creatorapplication;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import run.halo.app.plugin.ReactiveSettingFetcher;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatorApplicationWebhookNotifier {
    private static final String SETTINGS_GROUP = "webhook";
    private static final JsonMapper JSON_MAPPER = JsonMapper.shared();
    private static final WebClient WEB_CLIENT = WebClient.builder()
        .defaultHeader("User-Agent", "Halo-Creator-Application/0.4.0")
        .build();

    private final ReactiveSettingFetcher settingFetcher;

    public Mono<Void> notifySubmitted(CreatorApplication application) {
        return settingFetcher.fetch(SETTINGS_GROUP, WebhookSettings.class)
            .defaultIfEmpty(WebhookSettings.disabled())
            .flatMap(settings -> {
                if (!settings.isUsable()) {
                    return Mono.empty();
                }
                return send(application, settings);
            })
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(5)))
            .onErrorResume(error -> {
                log.warn("Failed to send creator application webhook for {}",
                    application.getMetadata().getName(), error);
                return Mono.empty();
            });
    }

    private Mono<Void> send(CreatorApplication application, WebhookSettings settings) {
        return Mono.fromCallable(() -> createRequest(application, settings))
            .flatMap(request -> WEB_CLIENT.post()
                .uri(settings.endpoint().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Creator-Timestamp", request.timestamp())
                .header("X-Creator-Signature", request.signature())
                .bodyValue(request.body())
                .retrieve()
                .toBodilessEntity())
            .then();
    }

    private WebhookRequest createRequest(CreatorApplication application,
                                         WebhookSettings settings) throws Exception {
        var spec = application.getSpec();
        Map<String, Object> applicationPayload = new LinkedHashMap<>();
        applicationPayload.put("name", application.getMetadata().getName());
        applicationPayload.put("username", spec.getUsername());
        applicationPayload.put("displayName", spec.getDisplayName());
        applicationPayload.put("stage", spec.getStage().name());
        applicationPayload.put("reason", spec.getReason());
        applicationPayload.put("articleTitle", spec.getArticleTitle());
        applicationPayload.put("submittedAt", spec.getSubmittedAt() == null
            ? null : spec.getSubmittedAt().toString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "application.submitted");
        payload.put("eventId", "application.submitted:" + application.getMetadata().getName());
        payload.put("occurredAt", Instant.now().toString());
        payload.put("application", applicationPayload);
        if (settings.reviewUrl() != null && !settings.reviewUrl().isBlank()) {
            payload.put("reviewUrl", settings.reviewUrl().trim());
        }

        var body = JSON_MAPPER.writeValueAsString(payload);
        var timestamp = Long.toString(Instant.now().getEpochSecond());
        var signature = "sha256=" + sign(settings.secret(), timestamp, body);
        return new WebhookRequest(timestamp, signature, body);
    }

    private static String sign(String secret, String timestamp, String body)
        throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var message = (timestamp + "." + body).getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(mac.doFinal(message));
    }

    private record WebhookRequest(String timestamp, String signature, String body) {
    }

    public record WebhookSettings(Boolean enabled, String endpoint, String secret,
                                  String reviewUrl) {
        static WebhookSettings disabled() {
            return new WebhookSettings(false, null, null, null);
        }

        boolean isUsable() {
            return Boolean.TRUE.equals(enabled)
                && endpoint != null && !endpoint.isBlank()
                && secret != null && secret.length() >= 16;
        }
    }
}
