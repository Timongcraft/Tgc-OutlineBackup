package de.timongcraft.outlinebackup.webhook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookReceiver.class);

    private final Gson gson = new Gson();
    private final Javalin app;

    private final String host;
    private final int port;
    private final String secret;
    private final String path;
    private final AsyncEventHandler asyncEventHandler;

    public WebhookReceiver(String host, int port, String path, String secret, AsyncEventHandler asyncEventHandler) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.secret = secret;
        this.asyncEventHandler = asyncEventHandler;
        app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.http.defaultContentType = "application/json";
        });
    }

    public void start() {
        app.post(path, ctx -> {
                    String outlineHeader = ctx.header("Outline-Signature");
                    if (outlineHeader == null) {
                        ctx.status(400).result("missing signature");
                        return;
                    }

                    String sig = null;
                    String timestampStr = null;
                    for (String part : outlineHeader.split(",")) {
                        if (part.startsWith("t=")) timestampStr = part.substring(2);
                        else if (part.startsWith("s=")) sig = part.substring(2);
                    }

                    if (sig == null || sig.isEmpty() || timestampStr == null || timestampStr.isEmpty()) {
                        ctx.status(400).result("missing signature");
                        return;
                    }

                    try {
                        long timeStamp = Long.parseLong(timestampStr);
                        if (Math.abs(System.currentTimeMillis() - timeStamp) > 5 * 60_000L) { // 5 minutes
                            ctx.status(400).result("timestamp too old");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        ctx.status(400).result("invalid timestamp");
                        return;
                    }

                    // 3. Read body and verify HMAC
                    String body = ctx.body();
                    if (!verifyHmacSha256(timestampStr + "." + body, sig, secret)) {
                        ctx.status(401).result("invalid signature");
                        return;
                    }

                    JsonObject json;
                    try {
                        json = gson.fromJson(body, JsonObject.class);
                    } catch (Exception e) {
                        ctx.status(400).result("bad json");
                        return;
                    }

                    CompletableFuture.runAsync(() -> asyncEventHandler.accept(json))
                            .exceptionally(t -> {
                                LOGGER.error("Event Handler errored", t);
                                return null;
                            });

                    ctx.status(200);
                }).get("/", ctx -> ctx.status(404))
                .start(host, port);
    }

    public void stop() {
        app.stop();
    }

    // verify HMAC-SHA256 where signature is hex-encoded
    private static boolean verifyHmacSha256(String payload, String signatureHex, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] calc = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = hexStringToByteArray(signatureHex);
            return MessageDigest.isEqual(calc, sigBytes); // constant-time compare
        } catch (Exception e) {
            return false;
        }
    }

    // helper: hex -> bytes
    private static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("^0x", ""); // tolerate leading 0x
        int len = s.length();
        if (len % 2 == 1) {
            // pad left with 0 if odd
            s = "0" + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    @FunctionalInterface
    public interface AsyncEventHandler {

        void accept(JsonObject eventBody);

    }

}