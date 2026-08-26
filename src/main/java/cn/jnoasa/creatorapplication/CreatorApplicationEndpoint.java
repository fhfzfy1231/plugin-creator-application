package cn.jnoasa.creatorapplication;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;

@Component
@RequiredArgsConstructor
public class CreatorApplicationEndpoint implements CustomEndpoint {
    private final ReactiveExtensionClient client;
    private final CreatorApplicationService service;

    @Override public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.creator.jnoasa.cn/v1alpha1");
    }

    @Override public RouterFunction<ServerResponse> endpoint() {
        return route()
            .GET("me", this::me)
            .POST("applications", this::submit)
            .GET("applications", this::list)
            .GET("applications/paged", this::listPaged)
            .POST("applications/{name}/review", this::review)
            .build();
    }

    private Mono<ServerResponse> me(ServerRequest request) {
        return request.principal().flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("username", p.getName())));
    }

    private Mono<ServerResponse> submit(ServerRequest request) {
        return request.principal().flatMap(p -> request.bodyToMono(SubmitBody.class)
            .flatMap(body -> service.submit(p.getName(), new CreatorApplicationService.SubmitCommand(
                body.stage, body.reason, body.qqScreenshot, body.articleTitle, body.articleUrl))))
            .flatMap(value -> ServerResponse.ok().bodyValue(value));
    }

    private Mono<ServerResponse> list(ServerRequest request) {
        return client.listAll(CreatorApplication.class, ListOptions.builder().build(),
                Sort.by(Sort.Direction.DESC, "metadata.creationTimestamp"))
            .collectList().flatMap(items -> ServerResponse.ok().bodyValue(items));
    }

    private Mono<ServerResponse> listPaged(ServerRequest request) {
        var scope = request.queryParam("scope").orElse("pending").toLowerCase();
        var requestedPage = positiveInt(request.queryParam("page").orElse(null), 1);
        var size = Math.min(100,
            positiveInt(request.queryParam("size").orElse(null), 20));

        return client.listAll(CreatorApplication.class, ListOptions.builder().build(),
                Sort.by(Sort.Direction.DESC, "metadata.creationTimestamp"))
            .collectList()
            .flatMap(allItems -> {
                long pendingTotal = allItems.stream()
                    .filter(item -> item.getSpec().getStatus()
                        == CreatorApplication.Status.PENDING)
                    .count();
                long reviewedTotal = allItems.size() - pendingTotal;
                List<CreatorApplication> scopedItems = allItems.stream()
                    .filter(item -> matchesScope(item, scope))
                    .toList();
                int totalPages = Math.max(1,
                    (int) Math.ceil((double) scopedItems.size() / size));
                int page = Math.min(requestedPage, totalPages);
                int fromIndex = Math.min((page - 1) * size, scopedItems.size());
                int toIndex = Math.min(fromIndex + size, scopedItems.size());
                var response = new PagedApplications(
                    scopedItems.subList(fromIndex, toIndex),
                    scopedItems.size(),
                    pendingTotal,
                    reviewedTotal,
                    page,
                    size,
                    totalPages
                );
                return ServerResponse.ok().bodyValue(response);
            });
    }

    private static boolean matchesScope(CreatorApplication item, String scope) {
        boolean pending = item.getSpec().getStatus() == CreatorApplication.Status.PENDING;
        return switch (scope) {
            case "reviewed" -> !pending;
            case "all" -> true;
            default -> pending;
        };
    }

    private static int positiveInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Mono<ServerResponse> review(ServerRequest request) {
        return request.principal().flatMap(p -> request.bodyToMono(ReviewBody.class)
            .flatMap(body -> service.review(request.pathVariable("name"), p.getName(),
                new CreatorApplicationService.ReviewCommand(body.approved, body.message))))
            .flatMap(value -> ServerResponse.ok().bodyValue(value));
    }

    public static class SubmitBody {
        public CreatorApplication.Stage stage;
        public String reason;
        public String qqScreenshot;
        public String articleTitle;
        public String articleUrl;
    }
    public static class ReviewBody { public boolean approved; public String message; }

    public record PagedApplications(List<CreatorApplication> items, long total,
                                    long pendingTotal, long reviewedTotal,
                                    int page, int size, int totalPages) {
    }
}
