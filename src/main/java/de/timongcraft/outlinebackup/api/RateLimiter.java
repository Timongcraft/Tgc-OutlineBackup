package de.timongcraft.outlinebackup.api;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class RateLimiter {

    private final Map<String, AtomicLong> nextAvailable = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "outline-api-ratelimiter");
        thread.setDaemon(true);
        return thread;
    });

    public CompletableFuture<Void> acquire(String route) {
        long now = Instant.now().toEpochMilli();
        long routeAvailable = nextAvailable.computeIfAbsent(route, k -> new AtomicLong(0)).get();

        if (now >= routeAvailable) {
            return CompletableFuture.completedFuture(null);
        } else {
            long delay = routeAvailable - now;
            CompletableFuture<Void> promise = new CompletableFuture<>();
            scheduler.schedule(() -> promise.complete(null), delay, TimeUnit.MILLISECONDS);
            return promise;
        }
    }

    /**
     * Sets the next allowed time for a specific route
     * Uses max(existing, now + millis) to avoid reducing previously set delays.
     */
    public void setDelay(String route, long millisFromNow) {
        long newAllowed = Instant.now().toEpochMilli() + Math.max(0, millisFromNow);
        AtomicLong atom = nextAvailable.computeIfAbsent(route, k -> new AtomicLong(0));
        atom.updateAndGet(prev -> Math.max(prev, newAllowed));
    }

    public void setDelaySeconds(String route, long seconds) {
        setDelay(route, seconds * 1000L);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

}