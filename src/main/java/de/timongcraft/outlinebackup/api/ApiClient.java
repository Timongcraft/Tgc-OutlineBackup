package de.timongcraft.outlinebackup.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.Main;
import de.timongcraft.outlinebackup.config.PropertiesConfig;
import de.timongcraft.outlinebackup.utils.TimeUtils;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ApiClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Gson gson = new Gson();
    private final String baseApiUrl;
    private final String authHeader;
    private final RateLimiter rateLimiter = new RateLimiter();

    public ApiClient(String baseUrl, String apiKey) {
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        this.baseApiUrl = baseUrl.endsWith("api/") ? baseUrl : baseUrl + "api/";
        this.authHeader = "Bearer " + apiKey;
    }

    public <T> CompletableFuture<HttpResponse<T>> postAsync(String methodPath, JsonObject requestBody,
                                                            HttpResponse.BodyHandler<T> responseBodyHandler) {
        return attemptSend(methodPath, requestBody, responseBodyHandler, 0);
    }

    public CompletableFuture<JsonObject> postJsonAsync(String methodPath, JsonObject requestBody) {
        return postAsync(methodPath, requestBody, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), JsonObject.class));
    }

    /**
     * The InputStream must be consumed/closed by the caller.
     */
    public CompletableFuture<InputStream> postInputStreamAsync(String methodPath, JsonObject requestBody) {
        return postAsync(methodPath, requestBody, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body);
    }

    private <T> CompletableFuture<HttpResponse<T>> attemptSend(String methodPath, JsonObject requestBody,
                                                               HttpResponse.BodyHandler<T> responseBodyHandler, int attempt) {
        final int nextAttempt = attempt + 1;

        return rateLimiter.acquire(methodPath).thenCompose(v -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseApiUrl + methodPath))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            return httpClient.sendAsync(request, responseBodyHandler)
                    .thenCompose(response -> {
                        int status = response.statusCode();

                        if (status == 429 /*rate-limited*/) {
                            Optional<String> retryAfterHeader = response.headers().firstValue("Retry-After");
                            long retryMillis = parseRetryAfterMillis(retryAfterHeader.orElse(null));;
                            Main.LOGGER.warn("Rate limited on '{}'. Retry-After: {} (attempt {}/{})",
                                    methodPath, TimeUtils.formatMillis(retryMillis), nextAttempt, PropertiesConfig.MAX_RETRY_ATTEMPTS);

                            rateLimiter.setDelay(methodPath, retryMillis);

                            if (nextAttempt > PropertiesConfig.MAX_RETRY_ATTEMPTS) {
                                return CompletableFuture.failedFuture(new IllegalStateException("Exceeded max rate-limit retries for '" + methodPath + "'"));
                            }

                            return rateLimiter.acquire(methodPath).thenCompose(_ -> attemptSend(methodPath, requestBody, responseBodyHandler, nextAttempt));
                        }

                        if (status < 200 || status >= 300) {
                            String responseBody;
                            try {
                                responseBody = response.body() == null ? "<null>" : response.body().toString();
                            } catch (Exception ex) {
                                responseBody = "<threw>";
                            }
                            return CompletableFuture.failedFuture(new IllegalStateException("HTTP " + status + " for '" + methodPath + "'\n" + responseBody));
                        }

                        return CompletableFuture.completedFuture(response);
                    });
        });
    }

    /**
     * Parse Retry-After: supports numeric seconds or RFC-1123 date header.
     * falls back to 1 second.
     */
    private long parseRetryAfterMillis(String header) {
        if (header == null || header.isBlank()) return 1000L;
        try {
            header = header.trim();
            // numeric seconds
            if (header.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.')) {
                double seconds = Double.parseDouble(header);
                return (long) Math.ceil(seconds * 1000L);
            }

            // try RFC-1123 date
            Instant instant = DateTimeFormatter.RFC_1123_DATE_TIME.parse(header, Instant::from);
            return Math.max(1000, instant.toEpochMilli() - Instant.now().toEpochMilli());
        } catch (Exception e) {
            Main.LOGGER.warn("Could not parse Retry-After header '{}', defaulting to 1s", header, e);
            return 1000L;
        }
    }

    public void shutdown() {
        rateLimiter.shutdown();
    }

}