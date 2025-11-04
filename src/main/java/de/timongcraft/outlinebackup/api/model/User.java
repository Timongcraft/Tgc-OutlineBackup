package de.timongcraft.outlinebackup.api.model;

import com.google.gson.JsonObject;
import de.timongcraft.outlinebackup.utils.JsonUtils;
import java.time.Instant;
import org.jetbrains.annotations.Nullable;

public record User(String id, String name, @Nullable String email, @Nullable String avatarUrl, User.Role role, boolean isSuspended,
                   Instant createdAt, Instant lastActiveAt) {

    public static User fromJson(JsonObject json) {
        return new User(
                JsonUtils.getRequiredString(json, "id"),
                JsonUtils.getRequiredString(json, "name"),
                JsonUtils.getNullableString(json, "email"),
                JsonUtils.getNullableString(json, "avatarUrl"),
                Role.fromApiValue(JsonUtils.getRequiredString(json, "role")),
                JsonUtils.getRequiredBoolean(json, "isSuspended"),
                JsonUtils.getRequiredInstant(json, "createdAt"),
                JsonUtils.getRequiredInstant(json, "lastActiveAt")
        );
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", role=" + role +
                ", isSuspended=" + isSuspended +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                '}';
    }

    public enum Role {

        ADMIN("admin"),
        MEMBER("member"),
        VIEWER("viewer"),
        GUEST("guest");

        private final String apiValue;

        Role(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }

        public static Role fromApiValue(String apiValue) {
            for (Role role : values()) {
                if (role.apiValue.equals(apiValue)) return role;
            }
            throw new IllegalArgumentException("Unknown role: " + apiValue);
        }

        @Override
        public String toString() {
            return "Role{" +
                    "apiValue='" + apiValue + '\'' +
                    '}';
        }
    }

}