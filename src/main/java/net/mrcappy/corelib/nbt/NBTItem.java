package net.mrcappy.corelib.nbt;

import org.bukkit.inventory.ItemStack;

/**
 * NBT Item wrapper.
 * 
 * This is what you actually use to modify item NBT.
 * It wraps an ItemStack and provides easy access to
 * its NBT data.
 * 
 * IMPORTANT: This creates a COPY of the ItemStack.
 * You need to get the modified item with getItem()
 * after making changes.
 */
public class NBTItem {
    
    private ItemStack item;
    private final NBTAdapter adapter;
    private NBTCompound compound;
    
    public NBTItem(ItemStack item, NBTAdapter adapter) {
        // Clone the item to avoid modifying the original
        this.item = item.clone();
        this.adapter = adapter;
        this.compound = adapter.getNBT(this.item);
    }
    
    /**
     * Get the modified ItemStack.
     * Call this after making all your NBT changes.
     */
    public ItemStack getItem() {
        // Apply the compound to the item
        item = adapter.applyNBT(item, compound);
        return item;
    }
    
    // Delegate all NBT operations to the compound
    // This is just convenience so you don't have to
    // call getCompound() every time
    
    public void setByte(String key, byte value) {
        compound.setByte(key, value);
    }
    
    public void setShort(String key, short value) {
        compound.setShort(key, value);
    }
    
    public void setInt(String key, int value) {
        compound.setInt(key, value);
    }
    
    public void setLong(String key, long value) {
        compound.setLong(key, value);
    }
    
    public void setFloat(String key, float value) {
        compound.setFloat(key, value);
    }
    
    public void setDouble(String key, double value) {
        compound.setDouble(key, value);
    }    
    public void setString(String key, String value) {
        compound.setString(key, value);
    }
    
    public void setBoolean(String key, boolean value) {
        compound.setBoolean(key, value);
    }
    
    public void setByteArray(String key, byte[] value) {
        compound.setByteArray(key, value);
    }
    
    public void setIntArray(String key, int[] value) {
        compound.setIntArray(key, value);
    }
    
    public byte getByte(String key) {
        return compound.getByte(key);
    }
    
    public short getShort(String key) {
        return compound.getShort(key);
    }
    
    public int getInt(String key) {
        return compound.getInt(key);
    }
    
    public long getLong(String key) {
        return compound.getLong(key);
    }
    
    public float getFloat(String key) {
        return compound.getFloat(key);
    }
    
    public double getDouble(String key) {
        return compound.getDouble(key);
    }
    
    public String getString(String key) {
        return compound.getString(key);
    }
    
    public boolean getBoolean(String key) {
        return compound.getBoolean(key);
    }
    
    public byte[] getByteArray(String key) {
        return compound.getByteArray(key);
    }
    
    public int[] getIntArray(String key) {
        return compound.getIntArray(key);
    }
    
    public boolean hasKey(String key) {
        return compound.hasKey(key);
    }
    
    public void removeKey(String key) {
        compound.removeKey(key);
    }
    
    /**
     * Get the underlying NBT compound.
     * For advanced operations.
     */
    public NBTCompound getCompound() {
        return compound;
    }
}