package de.timongcraft.outlinebackup.api;

import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.api.model.FileOperation;
import de.timongcraft.outlinebackup.api.model.collections.ExportFormat;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class CollectionsApi {

    public static CompletableFuture<FileOperation> exportAllAsync(ApiClient client,
                                                                  ExportFormat format,
                                                                  boolean includeAttachments,
                                                                  boolean includePrivate) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("format", format.getApiValue());
        requestBody.addProperty("includeAttachments", includeAttachments);
        requestBody.addProperty("includePrivate", includePrivate);

        return client.postJsonAsync("collections.export_all", requestBody)
                .thenApply(response -> {
                    JsonObject data = response.getAsJsonObject("data");
                    if (data.has("fileOperation")) {
                        return FileOperation.fromJson(data.getAsJsonObject("fileOperation"));
                    } else {
                        throw new IllegalStateException("No fileOperation found in response: " + response);
                    }
                });
    }

    private CollectionsApi() {}

}