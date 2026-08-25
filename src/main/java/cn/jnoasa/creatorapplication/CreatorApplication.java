package cn.jnoasa.creatorapplication;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "creator.jnoasa.cn", version = "v1alpha1", kind = "CreatorApplication",
    plural = "creatorapplications", singular = "creatorapplication")
public class CreatorApplication extends AbstractExtension {
    private Spec spec;

    @Data
    public static class Spec {
        private String username;
        private String displayName;
        private Stage stage;
        private Status status;
        private String reason;
        /** Private data URL; never returned by the userspace summary endpoint. */
        private String qqScreenshot;
        private String articleTitle;
        private String articleUrl;
        private String reviewMessage;
        private String reviewer;
        private Instant submittedAt;
        private Instant reviewedAt;
    }

    public enum Stage { CONTRIBUTOR, AUTHOR }
    public enum Status { PENDING, APPROVED, REJECTED }
}
