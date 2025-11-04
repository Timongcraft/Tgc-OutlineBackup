package de.timongcraft.outlinebackup;

import de.timongcraft.outlinebackup.api.ApiClient;
import de.timongcraft.outlinebackup.backup.BackupManager;
import de.timongcraft.outlinebackup.config.PropertiesConfig;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) {
        LOGGER.info("Initializing {} v{}...",
                Main.class.getPackage().getImplementationVendor(), Main.class.getPackage().getImplementationVersion());

        if (PropertiesConfig.API_KEY.isEmpty()) {
            LOGGER.error("No Outline API key provided. Set system property 'outline.api.key'");
            System.exit(1);
        }

        ApiClient client = new ApiClient(PropertiesConfig.BASE_URL, PropertiesConfig.API_KEY);
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));
        BackupManager manager = new BackupManager(client);

        boolean runOnce = false;
        for (String arg : args) {
            if ("--once".equalsIgnoreCase(arg)) {
                runOnce = true;
                break;
            }
        }

        LOGGER.info("Initialized");

        if (runOnce) {
            LOGGER.info("Running in single backup mode");
            manager.runOnce();
            LOGGER.info("Shutting down...");
            System.exit(0);
        } else {
            LOGGER.info("Running in scheduled daily backup mode (backups start at {}:00 '{}')",
                    String.format("%02d", PropertiesConfig.SCHEDULE_HOUR), ZoneId.systemDefault());
            manager.scheduleDailyBackups();
        }
    }

}