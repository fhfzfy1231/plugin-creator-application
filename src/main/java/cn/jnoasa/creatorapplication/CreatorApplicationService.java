package cn.jnoasa.creatorapplication;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Component
@RequiredArgsConstructor
public class CreatorApplicationService {
    private final ReactiveExtensionClient client;
    private final CreatorApplicationWebhookNotifier webhookNotifier;
    private final CreatorApplicationReviewNotificationPublisher reviewNotificationPublisher;

    public Mono<CreatorApplication> submit(String username, SubmitCommand command) {
        return client.fetch(User.class, username)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
            .flatMap(user -> validateNoPending(username, command.stage()).then(Mono.defer(() -> {
                validate(command);
                var app = new CreatorApplication();
                var metadata = new Metadata();
                metadata.setGenerateName("creator-application-");
                app.setMetadata(metadata);
                var spec = new CreatorApplication.Spec();
                spec.setUsername(username);
                spec.setDisplayName(user.getSpec().getDisplayName());
                spec.setStage(command.stage());
                spec.setStatus(CreatorApplication.Status.PENDING);
                spec.setReason(trim(command.reason()));
                spec.setQqScreenshot(trim(command.qqScreenshot()));
                spec.setArticleTitle(trim(command.articleTitle()));
                spec.setArticleUrl(null);
                spec.setSubmittedAt(Instant.now());
                app.setSpec(spec);
                return client.create(app)
                    .flatMap(created -> webhookNotifier.notifySubmitted(created).thenReturn(created));
            })));
    }

    public Mono<CreatorApplication> review(String name, String reviewer, ReviewCommand command) {
        return client.fetch(CreatorApplication.class, name)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
            .flatMap(app -> {
                if (app.getSpec().getStatus() != CreatorApplication.Status.PENDING) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "申请已审核"));
                }
                app.getSpec().setStatus(command.approved()
                    ? CreatorApplication.Status.APPROVED : CreatorApplication.Status.REJECTED);
                app.getSpec().setReviewMessage(trim(command.message()));
                app.getSpec().setReviewer(reviewer);
                app.getSpec().setReviewedAt(Instant.now());
                return client.update(app)
                    .flatMap(updated -> reviewNotificationPublisher.publish(updated)
                        .thenReturn(updated));
            });
    }

    private Mono<Void> validateNoPending(String username, CreatorApplication.Stage stage) {
        return client.listAll(CreatorApplication.class, ListOptions.builder().build(), Sort.unsorted())
            .filter(app -> username.equals(app.getSpec().getUsername()))
            .filter(app -> stage == app.getSpec().getStage())
            .filter(app -> app.getSpec().getStatus() == CreatorApplication.Status.PENDING)
            .hasElements().flatMap(exists -> exists
                ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "已有待审核申请")) : Mono.empty());
    }

    private static void validate(SubmitCommand c) {
        if (c.stage() == null) throw bad("必须选择申请阶段");
        if (c.stage() == CreatorApplication.Stage.CONTRIBUTOR) {
            if (trim(c.reason()) == null) throw bad("申请理由不能为空");
            var image = trim(c.qqScreenshot());
            if (image == null || !image.matches("^data:image/(png|jpeg|webp);base64,.+")) throw bad("请上传 PNG、JPEG 或 WebP 截图");
            if (image.length() > 2_800_000) throw bad("截图不能超过约 2 MB");
        } else {
            if (trim(c.articleTitle()) == null) throw bad("文章名称不能为空");
        }
    }

    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record SubmitCommand(CreatorApplication.Stage stage, String reason, String qqScreenshot,
                                String articleTitle) {}
    public record ReviewCommand(boolean approved, String message) {}
}
