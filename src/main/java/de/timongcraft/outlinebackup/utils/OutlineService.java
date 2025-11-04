package de.timongcraft.outlinebackup.utils;

import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.Main;
import de.timongcraft.outlinebackup.api.ApiClient;
import de.timongcraft.outlinebackup.api.CollectionsApi;
import de.timongcraft.outlinebackup.api.FileOperationsApi;
import de.timongcraft.outlinebackup.api.model.FileOperation;
import de.timongcraft.outlinebackup.api.model.collections.ExportFormat;
import de.timongcraft.outlinebackup.config.PropertiesConfig;
import de.timongcraft.outlinebackup.webhook.WebhookReceiver;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class OutlineService {

    private static final Map<String, CompletableFuture<FileOperation.State>> PENDING = new ConcurrentHashMap<>();
    private static final WebhookReceiver WEBHOOK_RECEIVER = new WebhookReceiver(
            PropertiesConfig.WEBHOOK_HOST, PropertiesConfig.WEBHOOK_PORT, PropertiesConfig.WEBHOOK_PATH, PropertiesConfig.WEBHOOK_SECRET,
            OutlineService::handleWebhookEvent
    );

    public static void init() {
        Main.LOGGER.info("Starting webhook on {}:{} under '{}'", PropertiesConfig.WEBHOOK_HOST, PropertiesConfig.WEBHOOK_PORT, PropertiesConfig.WEBHOOK_PATH);
        WEBHOOK_RECEIVER.start();
        Runtime.getRuntime().addShutdownHook(new Thread(WEBHOOK_RECEIVER::stop));
    }

    public static CompletableFuture<Void> exportAllAsync(ApiClient client,
                                                         ExportFormat format,
                                                         boolean includeAttachments, boolean includePrivate,
                                                         Consumer<InputStream> downloadConsumer) {
        return CollectionsApi.exportAllAsync(client, format, includeAttachments, includePrivate)
                .thenCompose(exportOperation -> {
                    CompletableFuture<FileOperation.State> future = new CompletableFuture<>();
                    PENDING.put(exportOperation.id(), future);


                    return future.thenCompose(state -> {
                        if (state == FileOperation.State.COMPLETE) {
                            return FileOperationsApi.retrieveFileAsync(client, exportOperation)
                                    .thenAccept(is -> {
                                        try (InputStream in = is) {
                                            downloadConsumer.accept(in);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Failed processing downloaded file", e);
                                        }
                                    }).thenCompose(_v -> FileOperationsApi.deleteAsync(client, exportOperation));
                        } else if (state == FileOperation.State.FAILED) {
                            return CompletableFuture.failedFuture(new IllegalStateException("Export operation failed (reported by webhook) for " + exportOperation.id()));
                        } else {
                            return CompletableFuture.failedFuture(new IllegalStateException("Unexpected file operation state: " + state));
                        }
                    }).whenComplete((_, _) -> PENDING.remove(exportOperation.id()));
                });
    }

    private static void handleWebhookEvent(JsonObject eventBody) {
        try {
            String event = eventBody.has("event") && !eventBody.get("event").isJsonNull() ? eventBody.get("event").getAsString() : null;

            if (event == null || !event.contains("fileOperations")) return;

            JsonObject payload = eventBody.has("payload") && eventBody.get("payload").isJsonObject() ? eventBody.getAsJsonObject("payload") : null;
            if (payload == null) return;

            JsonObject model = payload.has("model") && payload.get("model").isJsonObject() ? payload.getAsJsonObject("model") : null;
            if (model == null) return;

            if (!model.has("id") || !model.has("state")) return;

            String modelId = model.get("id").getAsString();
            String modelStateStr = model.get("state").getAsString();

            FileOperation.State state;
            try {
                state = FileOperation.State.fromApiValue(modelStateStr);
            } catch (IllegalArgumentException e) {
                return;
            }

            if (state.isInProgress()) return;

            CompletableFuture<FileOperation.State> future = PENDING.remove(modelId);
            if (future != null) {
                future.complete(state);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to handle webhook event", e);
        }
    }

    private OutlineService() {}

}