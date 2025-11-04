package de.timongcraft.outlinebackup.utils;

import de.timongcraft.outlinebackup.api.ApiClient;
import de.timongcraft.outlinebackup.api.CollectionsApi;
import de.timongcraft.outlinebackup.api.FileOperationsApi;
import de.timongcraft.outlinebackup.api.model.FileOperation;
import de.timongcraft.outlinebackup.api.model.collections.ExportFormat;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class OutlineService {

    public static CompletableFuture<Void> exportAllAsync(ApiClient client,
                                                         ExportFormat format,
                                                         boolean includeAttachments, boolean includePrivate,
                                                         int initialDelayMillis,
                                                         int maxAttempts,
                                                         Consumer<InputStream> downloadConsumer) {
        return CollectionsApi.exportAllAsync(client, format, includeAttachments, includePrivate)
                .thenCompose(exportOperation -> pollForCompletion(client, exportOperation, initialDelayMillis, maxAttempts, downloadConsumer));
    }

    private static CompletableFuture<Void> pollForCompletion(ApiClient client,
                                                             FileOperation exportOperation,
                                                             int initialDelayMillis,
                                                             int maxAttempts,
                                                             Consumer<InputStream> downloadConsumer) {
        return pollAttempt(client, exportOperation.id(), 1, initialDelayMillis, maxAttempts, downloadConsumer);
    }

    private static CompletableFuture<Void> pollAttempt(ApiClient client,
                                                       String operationId,
                                                       int attempt,
                                                       int delayMillis,
                                                       int maxAttempts,
                                                       Consumer<InputStream> downloadConsumer) {
        if (attempt > maxAttempts) {
            return CompletableFuture.failedFuture(new IllegalStateException("Timed out waiting for operation " + operationId + " to complete"));
        }

        Executor delayedExecutor = CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS);

        return CompletableFuture.supplyAsync(() -> null, delayedExecutor)
                .thenCompose(_ -> FileOperationsApi.infoAsync(client, operationId))
                .thenCompose(infoOperation -> {
                    if (infoOperation.state() == FileOperation.State.COMPLETE) {
                        return FileOperationsApi.retrieveFileAsync(client, infoOperation)
                                .thenCompose(is -> CompletableFuture.runAsync(() -> {
                                    try (InputStream in = is) {
                                        downloadConsumer.accept(in);
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed processing downloaded file", e);
                                    }
                                })).thenCompose(_ -> FileOperationsApi.deleteAsync(client, infoOperation));
                    } else if (infoOperation.state() == FileOperation.State.FAILED) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Export operation failed"));
                    } else {
                        int nextDelay = Math.min(60_000, (int) Math.ceil(delayMillis * 1.5));
                        return pollAttempt(client, operationId, attempt + 1, nextDelay, maxAttempts, downloadConsumer);
                    }
                });
    }

    private OutlineService() {}

}