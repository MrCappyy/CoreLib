package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.CoreLibPlugin;
import net.mrcappy.corelib.protocol.export.PCAPExporter;
import net.mrcappy.corelib.protocol.injector.PlayerInjector;
import net.mrcappy.corelib.protocol.listener.AnnotationProcessor;
import net.mrcappy.corelib.protocol.listener.PacketListener;
import net.mrcappy.corelib.protocol.listener.PacketListenerManager;
import net.mrcappy.corelib.protocol.logging.WebhookLogger;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.util.PacketEffects;
import net.mrcappy.corelib.protocol.packet.PacketType;
import net.mrcappy.corelib.version.MinecraftVersion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Protocol Manager - The heart of packet manipulation.
 * 
 * This is where we shove our grubby hands into Minecraft's
 * network intestines and start playing puppet master with
 * every byte that flows through.
 * 
 * We inject into Netty's pipeline like a parasite because
 * that's where packets go to die (or get molested by us).
 * Every single packet has to pass through our checkpoint,
 * and we decide if it lives, dies, or gets mutated into
 * something completely different.
 * 
 * WARNING: This uses more reflection than a hall of mirrors.
 * If you don't understand Netty pipelines, go watch a YouTube
 * tutorial and come back in 3 hours when you're even more confused.
 */
public class ProtocolManager implements Listener {
    
    private static ProtocolManager instance;
    private final Plugin plugin;
    private final MinecraftVersion version;
    
    // Player injectors - one per player
    private final Map<UUID, PlayerInjector> injectors = new ConcurrentHashMap<>();
    
    // Listener management
    private final PacketListenerManager listenerManager;
    
    // Packet history tracking
    private final Map<UUID, PacketHistory> histories = new ConcurrentHashMap<>();
    
    // Rate limiting
    private final RateLimiter rateLimiter;
    
    // JavaScript engine for filters
    private final ScriptEngine scriptEngine;
    
    // Fake entity manager
    private final FakeEntityManager fakeEntityManager;
    
    // Fake block manager  
    private final FakeBlockManager fakeBlockManager;
    
    // Packet scheduler
    private final PacketScheduler packetScheduler;
    
    // Packet effects utility
    private final PacketEffects effects;
    
    // Client capability detection
    private final ClientCapabilityDetector capabilityDetector;
    
    // Annotation processor
    private final AnnotationProcessor annotationProcessor;
    
    // Optional components - package private for injector access
    WebhookLogger webhookLogger;
    PCAPExporter pcapExporter;
    
    // Debug mode
    private boolean debugMode = false;
    
    public ProtocolManager(Plugin plugin) {
        this.plugin = plugin;
        this.version = MinecraftVersion.getCurrent();
        instance = this;
        
        // Initialize subsystems
        this.listenerManager = new PacketListenerManager(this);
        this.rateLimiter = new RateLimiter();
        this.scriptEngine = new ScriptEngine(plugin);
        this.fakeEntityManager = new FakeEntityManager(this);
        this.fakeBlockManager = new FakeBlockManager(this);
        this.packetScheduler = new PacketScheduler(this);
        this.effects = new PacketEffects(this);
        this.capabilityDetector = new ClientCapabilityDetector();
        this.annotationProcessor = new AnnotationProcessor(this);
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Inject into all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            injectPlayer(player);
        }
        
        plugin.getLogger().info("Protocol Manager initialized for " + version);
    }    
    /**
     * Inject into a player's network channel.
     * This is where we hook into their packet stream.
     */
    public void injectPlayer(Player player) {
        try {
            PlayerInjector injector = new PlayerInjector(this, player);
            injector.inject();
            injectors.put(player.getUniqueId(), injector);
            
            if (debugMode) {
                plugin.getLogger().info("Injected into " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to inject into player: " + player.getName(), e);
        }
    }
    
    /**
     * Remove injection from a player.
     * Important to clean up or we'll leak memory.
     */
    public void uninjectPlayer(Player player) {
        PlayerInjector injector = injectors.remove(player.getUniqueId());
        if (injector != null) {
            injector.uninject();
        }
        
        // Clean up history
        histories.remove(player.getUniqueId());
        
        // Clean up client info
        capabilityDetector.removePlayer(player.getUniqueId());
        
        // Clean up fake entities/blocks
        fakeEntityManager.clearPlayer(player);
        fakeBlockManager.clearPlayer(player);
        
        // Clean up rate limits
        rateLimiter.clearPlayer(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Inject with a slight delay to ensure the channel is ready
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            injectPlayer(event.getPlayer());
        }, 1L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        uninjectPlayer(event.getPlayer());
    }
    
    /**
     * Send a packet to a player.
     * This bypasses normal sending and injects directly.
     */
    public void sendPacket(Player player, PacketContainer packet) {
        PlayerInjector injector = injectors.get(player.getUniqueId());
        if (injector != null) {
            injector.sendPacket(packet);
        } else {
            throw new IllegalStateException(
                "Player not injected: " + player.getName()
            );
        }
    }
    
    /**
     * Send a packet to multiple players.
     */
    public void sendPacket(Collection<? extends Player> players, PacketContainer packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }
    
    /**
     * Broadcast a packet to all online players.
     */
    public void broadcastPacket(PacketContainer packet) {
        sendPacket(plugin.getServer().getOnlinePlayers(), packet);
    }    
    /**
     * Register a packet listener.
     */
    public void registerListener(PacketListener listener) {
        listenerManager.register(listener);
    }
    
    /**
     * Unregister a packet listener.
     */
    public void unregisterListener(PacketListener listener) {
        listenerManager.unregister(listener);
    }
    
    /**
     * Get packet history for a player.
     */
    public PacketHistory getHistory(Player player) {
        return histories.computeIfAbsent(
            player.getUniqueId(), 
            uuid -> new PacketHistory(100) // Keep last 100 packets
        );
    }
    
    /**
     * Check if a packet should be rate limited.
     */
    public boolean shouldRateLimit(Player player, PacketType type) {
        return rateLimiter.shouldLimit(player.getUniqueId(), type);
    }
    
    /**
     * Get the JavaScript engine for filters.
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }
    
    /**
     * Get the fake entity manager.
     */
    public FakeEntityManager getFakeEntityManager() {
        return fakeEntityManager;
    }
    
    /**
     * Get the fake block manager.
     */
    public FakeBlockManager getFakeBlockManager() {
        return fakeBlockManager;
    }
    
    /**
     * Get the packet scheduler.
     */
    public PacketScheduler getPacketScheduler() {
        return packetScheduler;
    }
    
    /**
     * Get the listener manager.
     */
    public PacketListenerManager getListenerManager() {
        return listenerManager;
    }
    
    /**
     * Get client capability detector.
     */
    public ClientCapabilityDetector getCapabilityDetector() {
        return capabilityDetector;
    }
    
    /**
     * Get annotation processor.
     */
    public AnnotationProcessor getAnnotationProcessor() {
        return annotationProcessor;
    }
    
    /**
     * Get the rate limiter.
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    /**
     * Get the packet effects utility.
     */
    public PacketEffects getEffects() {
        return effects;
    }
    
    /**
     * Set webhook logger URL.
     * Pass null to disable webhook logging.
     */
    public void setWebhookLogger(String webhookUrl) {
        if (webhookLogger != null) {
            webhookLogger.shutdown();
            webhookLogger = null;
        }
        
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            webhookLogger = new WebhookLogger(webhookUrl);
        }
    }
    
    /**
     * Start PCAP export to file.
     */
    public void startPCAPExport(java.io.File file) throws java.io.IOException {
        if (pcapExporter != null) {
            pcapExporter.close();
        }
        pcapExporter = new PCAPExporter(file);
    }
    
    /**
     * Stop PCAP export.
     */
    public void stopPCAPExport() {
        if (pcapExporter != null) {
            pcapExporter.close();
            pcapExporter = null;
        }
    }
    
    /**
     * Get packet effects utility.
     */
    public net.mrcappy.corelib.protocol.util.PacketEffects getPacketEffects() {
        return new net.mrcappy.corelib.protocol.util.PacketEffects(this);
    }
    
    /**
     * Enable or disable debug mode.
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
    
    public static ProtocolManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "ProtocolManager not initialized! Did CoreLib load properly?"
            );
        }
        return instance;
    }
    
    /**
     * Shutdown the protocol manager.
     * Uninjects all players and cleans up.
     */
    public void shutdown() {
        // Uninject all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            uninjectPlayer(player);
        }
        
        // Clear all data
        injectors.clear();
        histories.clear();
        listenerManager.clear();
        scriptEngine.shutdown();
        
        // Shutdown optional components
        if (webhookLogger != null) {
            webhookLogger.shutdown();
        }
        if (pcapExporter != null) {
            pcapExporter.close();
        }
        
        // Clear packet scheduler
        packetScheduler.clear();
        
        // Clear fake entities and blocks
        fakeEntityManager.clearAll();
        fakeBlockManager.clearAll();
    }
}