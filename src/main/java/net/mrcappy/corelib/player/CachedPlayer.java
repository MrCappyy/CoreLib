package net.mrcappy.corelib.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cached player data object.
 * 
 * This stores commonly accessed player information
 * to avoid expensive OfflinePlayer lookups.
 * 
 * You can extend this with custom data by using
 * the metadata map. It's serialized to JSON so
 * keep it simple (strings, numbers, booleans).
 */
public class CachedPlayer {
    
    private final UUID uuid;
    private String lastName;
    private long firstLogin;
    private long lastLogin;
    private long lastSeen;
    private boolean online;
    private final Map<String, Object> metadata = new HashMap<>();
    
    // Transient fields (not saved)
    private transient boolean dirty = false;
    
    public CachedPlayer(UUID uuid) {
        this.uuid = uuid;
        this.firstLogin = System.currentTimeMillis();
        this.lastLogin = firstLogin;
        this.lastSeen = firstLogin;
    }
    
    // Getters and setters
    // Yeah, it's boilerplate, but we're not using Lombok
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.dirty = true;
    }
    
    public long getFirstLogin() {
        return firstLogin;
    }
    
    public long getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
        this.dirty = true;
    }    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        this.dirty = true;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
        this.dirty = true;
    }
    
    /**
     * Get custom metadata value.
     * Returns null if not set.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }
    
    /**
     * Set custom metadata value.
     * Value must be JSON-serializable.
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
        this.dirty = true;
    }
    
    /**
     * Remove metadata value.
     */
    public void removeMetadata(String key) {
        if (metadata.remove(key) != null) {
            this.dirty = true;
        }
    }
    
    /**
     * Check if metadata key exists.
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Get all metadata.
     * Don't modify this directly!
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    // Dirty flag management
    
    public boolean isDirty() {
        return dirty;
    }
    
    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}