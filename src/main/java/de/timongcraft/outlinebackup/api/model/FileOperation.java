package de.timongcraft.outlinebackup.api.model;

import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.utils.JsonUtils;
import java.time.Instant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public record FileOperation(String id, Type type, State state,
                            JsonObject collectionJson, User user,
                            @Nullable Long size, Instant createdAt) {

    public static FileOperation fromJson(JsonObject json) {
        return new FileOperation(
                JsonUtils.getRequiredString(json, "id"),
                Type.fromApiValue(JsonUtils.getRequiredString(json, "type")),
                State.fromApiValue(JsonUtils.getRequiredString(json, "state")),
                JsonUtils.getNullableObject(json, "collection"),
                User.fromJson(JsonUtils.getRequiredObject(json, "user")),
                JsonUtils.getNullableLong(json, "size"),
                JsonUtils.getRequiredInstant(json, "createdAt")
        );
    }

    public enum State {

        CREATING("creating"),
        UPLOADING("uploading"),
        COMPLETE("complete"),
        FAILED("error"),
        EXPIRED("expired");

        private final String apiValue;

        State(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }

        /**
         * Determines if the operation is still in progress
         */
        public boolean isInProgress() {
            return switch (this) {
                case CREATING, UPLOADING -> true;
                default -> false;
            };
        }

        public static State fromApiValue(String apiValue) {
            for (State state : values()) {
                if (state.apiValue.equals(apiValue)) return state;
            }
            throw new IllegalArgumentException("Unknown state: " + apiValue);
        }

        @Override
        public String toString() {
            return "State{" +
                    "apiValue='" + apiValue + '\'' +
                    '}';
        }
    }

    public enum Type {

        IMPORT("import"),
        EXPORT("export");

        private final String apiValue;

        Type(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }

        public static Type fromApiValue(String apiValue) {
            for (Type type : values()) {
                if (type.apiValue.equals(apiValue)) return type;
            }
            throw new IllegalArgumentException("Unknown type: " + apiValue);
        }

        @Override
        public String toString() {
            return "Type{" +
                    "apiValue='" + apiValue + '\'' +
                    '}';
        }
    }

}