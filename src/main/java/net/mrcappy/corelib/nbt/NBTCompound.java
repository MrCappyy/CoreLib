package net.mrcappy.corelib.nbt;

import java.util.Set;

/**
 * NBT Compound wrapper.
 * 
 * Represents an NBT compound tag that can hold
 * various data types. Think of it as a Map<String, Object>
 * but with more restrictions and worse performance.
 * 
 * Supported types:
 * - Primitives (byte, short, int, long, float, double)
 * - String
 * - byte[] and int[] arrays
 * - Lists and nested compounds (if you're brave)
 */
public interface NBTCompound {
    
    // Setters
    void setByte(String key, byte value);
    void setShort(String key, short value);
    void setInt(String key, int value);
    void setLong(String key, long value);
    void setFloat(String key, float value);
    void setDouble(String key, double value);
    void setString(String key, String value);
    void setBoolean(String key, boolean value); // Stored as byte 0/1
    void setByteArray(String key, byte[] value);
    void setIntArray(String key, int[] value);
    void setCompound(String key, NBTCompound value);
    
    // Getters
    byte getByte(String key);
    short getShort(String key);
    int getInt(String key);
    long getLong(String key);
    float getFloat(String key);
    double getDouble(String key);
    String getString(String key);
    boolean getBoolean(String key);
    byte[] getByteArray(String key);
    int[] getIntArray(String key);
    NBTCompound getCompound(String key);
    
    // Utility methods
    boolean hasKey(String key);
    void removeKey(String key);
    Set<String> getKeys();
    boolean isEmpty();
    
    /**
     * Get the underlying NMS object.
     * Only use if you really know what you're doing.
     */
    Object getHandle();
}