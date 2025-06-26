package net.mrcappy.corelib.nbt;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Represents NBT data for an item.
 * 
 * This is a simplified view of the actual NBT compound
 * because dealing with raw NBT is like performing brain
 * surgery with a rusty spoon.
 */
public class ItemNBT {
    
    private final Map<String, Object> data = new HashMap<>();
    
    /**
     * Get all keys in this NBT compound.
     */
    public Set<String> getKeys() {
        return data.keySet();
    }
    
    /**
     * Get a value by key.
     */
    public Object get(String key) {
        return data.get(key);
    }
    
    /**
     * Set a string value.
     */
    public void setString(String key, String value) {
        data.put(key, value);
    }
    
    /**
     * Set an integer value.
     */
    public void setInt(String key, int value) {
        data.put(key, value);
    }
    
    /**
     * Set a boolean value.
     */
    public void setBoolean(String key, boolean value) {
        data.put(key, value);
    }
    
    /**
     * Check if a key exists.
     */
    public boolean hasKey(String key) {
        return data.containsKey(key);
    }
}