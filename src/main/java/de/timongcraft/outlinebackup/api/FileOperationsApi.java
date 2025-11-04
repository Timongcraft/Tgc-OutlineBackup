package de.timongcraft.outlinebackup.api;

import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.api.model.FileOperation;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public final class FileOperationsApi {

    public static CompletableFuture<FileOperation> infoAsync(ApiClient client, FileOperation operation) {
        return infoAsync(client, operation.id());
    }

    public static CompletableFuture<FileOperation> infoAsync(ApiClient client, String id) {
        return client.postJsonAsync("fileOperations.info", idBody(id))
                .thenApply(response -> FileOperation.fromJson(response.getAsJsonObject("data")));
    }

    public static CompletableFuture<InputStream> retrieveFileAsync(ApiClient client, FileOperation operation) {
        return retrieveFileAsync(client, operation.id());
    }

    public static CompletableFuture<InputStream> retrieveFileAsync(ApiClient client, String id) {
        return client.postInputStreamAsync("fileOperations.redirect", idBody(id));
    }

    public static CompletableFuture<Void> deleteAsync(ApiClient client, FileOperation operation) {
        return deleteAsync(client, operation.id());
    }

    public static CompletableFuture<Void> deleteAsync(ApiClient client, String id) {
        return client.postJsonAsync("fileOperations.delete", idBody(id)).thenApply(_ -> null);
    }

    private static JsonObject idBody(String id) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        return json;
    }

    private FileOperationsApi() {}

}