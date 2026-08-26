package cn.jnoasa.creatorapplication;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;

/** Publishes a subject-scoped Halo notification to the applicant after review. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreatorApplicationReviewNotificationPublisher {
    static final String APPROVED_REASON_TYPE = "creator-application-approved";
    static final String REJECTED_REASON_TYPE = "creator-application-rejected";

    private static final DateTimeFormatter REVIEWED_AT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final NotificationCenter notificationCenter;
    private final NotificationReasonEmitter reasonEmitter;
    private final ExternalLinkProcessor externalLinkProcessor;

    public Mono<Void> publish(CreatorApplication application) {
        var spec = application.getSpec();
        if (spec.getStatus() == CreatorApplication.Status.PENDING) {
            return Mono.empty();
        }
        var reasonType = spec.getStatus() == CreatorApplication.Status.APPROVED
            ? APPROVED_REASON_TYPE : REJECTED_REASON_TYPE;
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(spec.getUsername());
        var interestReason = interestReason(reasonType, application);

        return notificationCenter.subscribe(subscriber, interestReason)
            .then(reasonEmitter.emit(reasonType, builder -> builder
                .author(UserIdentity.of(spec.getReviewer()))
                .subject(reasonSubject(application))
                .attributes(attributes(application))))
            .onErrorResume(error -> {
                log.warn("Failed to publish review notification for creator application {}",
                    application.getMetadata().getName(), error);
                return Mono.empty();
            });
    }

    private static Subscription.InterestReason interestReason(String reasonType,
                                                               CreatorApplication application) {
        var reason = new Subscription.InterestReason();
        reason.setReasonType(reasonType);
        reason.setSubject(Subscription.ReasonSubject.builder()
            .apiVersion(application.getApiVersion())
            .kind(application.getKind())
            .name(application.getMetadata().getName())
            .build());
        return reason;
    }

    private Reason.Subject reasonSubject(CreatorApplication application) {
        var spec = application.getSpec();
        return Reason.Subject.builder()
            .apiVersion(application.getApiVersion())
            .kind(application.getKind())
            .name(application.getMetadata().getName())
            .title(stageLabel(spec.getStage()) + "申请审核结果")
            .url(externalLinkProcessor.processLink("/console/uc"))
            .build();
    }

    private static Map<String, Object> attributes(CreatorApplication application) {
        var spec = application.getSpec();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("displayName", fallback(spec.getDisplayName(), spec.getUsername()));
        attributes.put("stageLabel", stageLabel(spec.getStage()));
        attributes.put("reviewedAt", spec.getReviewedAt() == null
            ? "" : REVIEWED_AT_FORMATTER.format(spec.getReviewedAt()));
        attributes.put("reviewer", fallback(spec.getReviewer(), "管理员"));
        if (spec.getStatus() == CreatorApplication.Status.REJECTED) {
            attributes.put("reviewMessage",
                fallback(spec.getReviewMessage(), "管理员未填写附加留言。"));
        }
        return attributes;
    }

    private static String stageLabel(CreatorApplication.Stage stage) {
        return stage == CreatorApplication.Stage.CONTRIBUTOR ? "贡献者" : "作者";
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
