package net.mrcappy.corelib.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mrcappy.corelib.scheduler.CoreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Player data cache because Bukkit's OfflinePlayer is slower
 * than a sloth on Xanax. This caches player data and provides
 * fast lookups for common operations.
 * 
 * Features:
 * - UUID to name mapping
 * - Custom player data storage
 * - Persistent across restarts
 * - Actually fucking fast
 * 
 * Data is stored in JSON because YAML parsing is slow
 * and NBT is overkill for this.
 */
public class PlayerCache implements Listener {
    
    private final Plugin plugin;
    private final File dataFolder;
    private final Map<UUID, CachedPlayer> cache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Stats for monitoring
    private long cacheHits = 0;
    private long cacheMisses = 0;
    
    // Auto-save interval (5 minutes)
    private static final long SAVE_INTERVAL = 20L * 60 * 5;    
    public PlayerCache(Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        // Create data folder
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load existing data
        loadAllPlayers();
        
        // Schedule auto-save
        CoreScheduler.runTimer(this::save, SAVE_INTERVAL, SAVE_INTERVAL);
    }
    
    /**
     * Get cached player data.
     * Creates new entry if not exists.
     */
    public CachedPlayer getPlayer(UUID uuid) {
        CachedPlayer player = cache.get(uuid);
        if (player != null) {
            cacheHits++;
            return player;
        }
        
        cacheMisses++;
        return cache.computeIfAbsent(uuid, this::loadOrCreatePlayer);
    }
    
    /**
     * Get player by name.
     * Case-insensitive but returns null if not cached.
     */
    public CachedPlayer getPlayer(String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        return uuid != null ? getPlayer(uuid) : null;
    }
    
    /**
     * Get player UUID by name.
     * Faster than Bukkit.getOfflinePlayer().
     */
    public UUID getUUID(String name) {
        return nameToUuid.get(name.toLowerCase());
    }
    
    /**
     * Check if player has joined before.
     * Faster than OfflinePlayer.hasPlayedBefore().
     */
    public boolean hasPlayedBefore(UUID uuid) {
        return cache.containsKey(uuid) || new File(dataFolder, uuid + ".json").exists();
    }    
    /**
     * Save all cached data to disk.
     * Called periodically and on shutdown.
     */
    public void save() {
        int saved = 0;
        for (CachedPlayer player : cache.values()) {
            if (player.isDirty()) {
                savePlayer(player);
                saved++;
            }
        }
        
        if (saved > 0) {
            plugin.getLogger().info("Saved " + saved + " player data files.");
        }
    }
    
    /**
     * Force save a specific player.
     */
    public void savePlayer(CachedPlayer player) {
        File file = new File(dataFolder, player.getUuid() + ".json");
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(player, writer);
            player.setDirty(false);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to save player data: " + player.getUuid(), e);
        }
    }
    
    /**
     * Load all player data files.
     * Called on startup.
     */
    private void loadAllPlayers() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        
        int loaded = 0;
        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);
                
                CachedPlayer player = loadPlayer(uuid);
                if (player != null) {
                    cache.put(uuid, player);
                    nameToUuid.put(player.getLastName().toLowerCase(), uuid);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data: " + file.getName());
            }
        }
        
        plugin.getLogger().info("Loaded " + loaded + " player data files.");
    }    
    /**
     * Load player data from disk.
     */
    private CachedPlayer loadPlayer(UUID uuid) {
        File file = new File(dataFolder, uuid + ".json");
        if (!file.exists()) return null;
        
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, CachedPlayer.class);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to load player data: " + uuid, e);
            return null;
        }
    }
    
    /**
     * Load or create player data.
     */
    private CachedPlayer loadOrCreatePlayer(UUID uuid) {
        // Try to load from disk
        CachedPlayer player = loadPlayer(uuid);
        if (player != null) return player;
        
        // Create new player data
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        player = new CachedPlayer(uuid);
        player.setLastName(offlinePlayer.getName() != null ? 
            offlinePlayer.getName() : uuid.toString());
        
        return player;
    }
    
    // Event handlers
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CachedPlayer cached = getPlayer(player.getUniqueId());
        
        // Update data
        cached.setLastName(player.getName());
        cached.setLastLogin(System.currentTimeMillis());
        cached.setOnline(true);
        cached.setDirty(true);
        
        // Update name mapping
        nameToUuid.put(player.getName().toLowerCase(), player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CachedPlayer cached = getPlayer(player.getUniqueId());
        
        // Update data
        cached.setLastSeen(System.currentTimeMillis());
        cached.setOnline(false);
        cached.setDirty(true);
        
        // Save immediately on quit
        savePlayer(cached);
    }
    
    // Stats and management methods
    
    /**
     * Get the number of cached players.
     */
    public int getCachedPlayerCount() {
        return cache.size();
    }
    
    /**
     * Get cache hit count.
     */
    public long getCacheHits() {
        return cacheHits;
    }
    
    /**
     * Get cache miss count.
     */
    public long getCacheMisses() {
        return cacheMisses;
    }
    
    /**
     * Clear all cached data.
     * USE WITH CAUTION!
     */
    public void clearCache() {
        cache.clear();
        nameToUuid.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }
}