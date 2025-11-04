package de.timongcraft.outlinebackup.backup;

import de.timongcraft.outlinebackup.Main;
import de.timongcraft.outlinebackup.api.ApiClient;
import de.timongcraft.outlinebackup.config.PropertiesConfig;
import de.timongcraft.outlinebackup.utils.OutlineService;
import de.timongcraft.outlinebackup.utils.TimeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class BackupManager {

    private final ApiClient apiClient;
    private final Path backupDirectory = Path.of(PropertiesConfig.BACKUP_DIR);

    public BackupManager(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void runOnce() {
        Main.LOGGER.info("Starting backup...");
        try {
            Files.createDirectories(backupDirectory);

            OutlineService.exportAllAsync(apiClient,
                    PropertiesConfig.EXPORT_FORMAT,
                    PropertiesConfig.INCLUDE_ATTACHMENTS, PropertiesConfig.INCLUDE_PRIVATE,
                    PropertiesConfig.INITIAL_POLL_DELAY_MILLIS, PropertiesConfig.MAX_POLL_ATTEMPTS,
                    inputStream -> {
                        String timestamp = LocalDateTime.now().format(TimeUtils.FILE_TIMESTAMP_FORMAT);
                        String fileName = String.format("outline-export-%s.zip", timestamp);
                        Path path = backupDirectory.resolve(fileName);

                        writeBackupFile(inputStream, path);

                        Main.LOGGER.info("Saved backup export to: {}", path);
                    }
            ).join();
        } catch (Exception e) {
            Main.LOGGER.error("Backup failed", e);
        }
    }

    private void writeBackupFile(InputStream source, Path destination) {
        try (InputStream in = source;
             OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write backup file", e);
        }
    }

    public void enforceRetention() {
        try {
            Files.createDirectories(backupDirectory);
            Instant cutoff = Instant.now().minus(Duration.ofDays(PropertiesConfig.RETENTION_DAYS));

            try (Stream<Path> files = Files.list(backupDirectory)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        Instant lastModified = Files.getLastModifiedTime(file).toInstant();
                        if (lastModified.isBefore(cutoff)) {
                            Files.deleteIfExists(file);
                        }
                    } catch (IOException e) {
                        Main.LOGGER.warn("Could not delete old backup {}", file, e);
                    }
                });
            }
        } catch (IOException e) {
            Main.LOGGER.warn("Failed to enforce retention policy", e);
        }
    }

    public void scheduleDailyBackups() {
        //noinspection resource
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "outline-backup-scheduler");
            thread.setDaemon(false);
            return thread;
        });

        long initialDelayMillis = TimeUtils.computeInitialDelayMillis(PropertiesConfig.SCHEDULE_HOUR, ZoneId.systemDefault());

        Main.LOGGER.info("Next backup will run at {}",
                LocalDateTime.now().plus(initialDelayMillis, ChronoUnit.MILLIS).format(TimeUtils.LOG_TIMESTAMP_FORMAT));

        scheduler.scheduleAtFixedRate(() -> {
                    runOnce();
                    enforceRetention();
                }, initialDelayMillis,
                Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException _) {
                scheduler.shutdownNow();
            }
        }));
    }

}