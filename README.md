# Tgc-OutlineBackup

A simple tool to back up [Outline](https://www.getoutline.com/) via its export feature.

## Features

- Export all collections via the Outline API and download the resulting archive.
- Daily scheduled backups or single-run mode (`--once`).
- Automatic cleanup of old backups based on retention policy.

## Requirements

- Java 25 or later.
- A valid Outline API key (set via system property `outline.api.key`).

## Building

On Unix / macOS:

```bash
    ./mvnw clean package
```

On Windows (PowerShell / CMD):

```bat
    mvnw.cmd clean package
```

This produces a `-jar-with-dependencies` in `target/`.

### Run the application

One-time (single backup) mode:

    java -Doutline.api.key=<YOUR_API_KEY> -jar target/Tgc-OutlineBackup-<CURRENT_VERSION>-jar-with-dependencies.jar --once

Scheduled daily backups (runs at configured schedule hour, default 02:00):

    java -Doutline.api.key=<YOUR_API_KEY> -jar target/Tgc-OutlineBackup-<CURRENT_VERSION>-jar-with-dependencies.jar

## Obtaining an API Key

To use Tgc-OutlineBackup, you need an API key from your Outline account:

1. Log in to your Outline instance
2. Navigate to **Account -> API & Apps**
3. Generate a new API key

> Keep your API key secret

### Configuration (via system properties)

| Property                          | Default           | Description                                         |
|-----------------------------------|-------------------|-----------------------------------------------------|
| `outline.api.key`                 | _none (required)_ | Outline API key                                     |
| `backup.dir`                      | `./backups`       | Backups dir                                         |
| `export.format`                   | `json`            | Export format: `json` / `html` / `outline-markdown` |
| `export.include.attachments`      | `true`            | Include attachments in the export                   |
| `export.include.private`          | `true`            | Include private collections in the export           |
| `retention.days`                  | `30`              | Days to retain backup exports                       |
| `schedule.hour`                   | `2`               | Hour of day (0–23) to run daily backup              |
| `poll.initial.delay.milliseconds` | `2000`            | Initial poll delay when checking operation status   |
| `poll.max.attempts`               | `10`              | Max attempts to poll operation completion           |
| `ratelimit.max.attempts`          | `10`              | Max retry attempts on rate-limit                    |