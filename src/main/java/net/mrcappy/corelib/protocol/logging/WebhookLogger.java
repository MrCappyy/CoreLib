package net.mrcappy.corelib.protocol.logging;

import com.google.gson.Gson;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Webhook logger for packet events.
 * 
 * Send your packet logs to Discord/Slack/Whatever because
 * staring at console logs is for peasants. Now you can get
 * pinged at 3 AM when someone's using a hacked client!
 * 
 * "Why would I want packet logs in Discord?"
 * I don't know, why are you using this feature?
 * 
 * Uses async HTTP because blocking the packet thread for
 * network I/O is how you turn your server into PowerPoint.
 */
public class WebhookLogger {
    
    private final String webhookUrl;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Webhook-Logger");
        t.setDaemon(true);
        return t;
    });
    private final Gson gson = new Gson();
    
    public WebhookLogger(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    /**
     * Log a packet event to webhook.
     * Fire and forget because we don't care if it fails.
     */
    public void logPacket(Player player, PacketContainer packet, boolean outgoing) {
        executor.submit(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("player", player.getName());
                payload.put("uuid", player.getUniqueId().toString());
                payload.put("packet_type", packet.getType().name());
                payload.put("direction", outgoing ? "OUTGOING" : "INCOMING");
                payload.put("timestamp", System.currentTimeMillis());
                
                // Add some packet data
                Map<String, Object> packetData = new HashMap<>();
                if (packet.getIntegers().size() > 0) {
                    packetData.put("integers", packet.getIntegers().getValues());
                }
                if (packet.getStrings().size() > 0) {
                    packetData.put("strings", packet.getStrings().getValues());
                }
                payload.put("data", packetData);
                
                sendWebhook(payload);
            } catch (Exception e) {
                // Webhook failed, oh no! Anyway...
            }
        });
    }
    
    /**
     * Actually send the HTTP request.
     * This is why we use async - HTTP is slow as balls.
     */
    private void sendWebhook(Map<String, Object> payload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "CoreLib-PacketLogger/1.0");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        String json = gson.toJson(payload);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        
        // We don't care about the response
        conn.getResponseCode();
        conn.disconnect();
    }
    
    /**
     * Shutdown the webhook logger.
     * Waits for pending webhooks because we're polite.
     */
    public void shutdown() {
        executor.shutdown();
    }
}