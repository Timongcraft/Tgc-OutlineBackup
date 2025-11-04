package de.timongcraft.outlinebackup.config;

import de.timongcraft.outlinebackup.api.model.collections.ExportFormat;

public final class PropertiesConfig {

    public static final String BASE_URL =
            System.getProperty("outline.base.url", "https://app.getoutline.com/");

    public static final String API_KEY =
            System.getProperty("outline.api.key", "");

    public static final String BACKUP_DIR =
            System.getProperty("backup.dir", "./backups");

    public static final ExportFormat EXPORT_FORMAT =
            ExportFormat.fromApiValue(System.getProperty("export.format", "json"));

    public static final boolean INCLUDE_ATTACHMENTS =
            Boolean.parseBoolean(System.getProperty("export.include.attachments", "true"));

    public static final boolean INCLUDE_PRIVATE =
            Boolean.parseBoolean(System.getProperty("export.include.private", "true"));

    public static final int RETENTION_DAYS =
            Integer.parseInt(System.getProperty("retention.days", "30"));

    public static final int SCHEDULE_HOUR =
            Integer.parseInt(System.getProperty("schedule.hour", "2"));

    public static final String WEBHOOK_HOST = System.getProperty("webhook.host", "0.0.0.0");

    public static final int WEBHOOK_PORT =
            Integer.parseInt(System.getProperty("webhook.port", "3000"));

    public static final String WEBHOOK_PATH = System.getProperty("webhook.path", "/webhook");

    public static final String WEBHOOK_SECRET = System.getProperty("webhook.secret", "");

    private PropertiesConfig() {}

}