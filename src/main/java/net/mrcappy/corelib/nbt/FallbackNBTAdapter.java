package net.mrcappy.corelib.nbt;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fallback NBT adapter for unsupported versions.
 * 
 * This doesn't actually do NBT operations.
 * It just stores data in memory and warns the user
 * that their shit isn't going to work properly.
 * 
 * Better than crashing, I guess?
 */
public class FallbackNBTAdapter implements NBTAdapter {
    
    private static boolean warnedOnce = false;
    
    private void warn() {
        if (!warnedOnce) {
            System.err.println("╔════════════════════════════════════════╗");
            System.err.println("║         NBT OPERATIONS DISABLED!       ║");
            System.err.println("║                                        ║");
            System.err.println("║  Your Minecraft version is not         ║");
            System.err.println("║  supported. NBT operations will        ║");
            System.err.println("║  not persist!                          ║");
            System.err.println("╚════════════════════════════════════════╝");
            warnedOnce = true;
        }
    }
    
    @Override
    public NBTCompound createCompound() {
        warn();
        return new FallbackCompound();
    }
    
    @Override
    public NBTCompound getNBT(ItemStack item) {
        warn();
        return new FallbackCompound();
    }
    
    @Override
    public ItemStack applyNBT(ItemStack item, NBTCompound compound) {
        warn();
        // Can't actually apply NBT, just return the item unchanged
        return item;
    }
    
    @Override
    public String toString(NBTCompound compound) {
        return "FallbackNBT" + ((FallbackCompound)compound).data.toString();
    }    
    /**
     * Fake NBT compound that just stores shit in a HashMap.
     * Doesn't actually do anything useful.
     */
    private static class FallbackCompound implements NBTCompound {
        
        private final Map<String, Object> data = new HashMap<>();
        
        @Override
        public void setByte(String key, byte value) {
            data.put(key, value);
        }
        
        @Override
        public void setShort(String key, short value) {
            data.put(key, value);
        }
        
        @Override
        public void setInt(String key, int value) {
            data.put(key, value);
        }
        
        @Override
        public void setLong(String key, long value) {
            data.put(key, value);
        }
        
        @Override
        public void setFloat(String key, float value) {
            data.put(key, value);
        }
        
        @Override
        public void setDouble(String key, double value) {
            data.put(key, value);
        }
        
        @Override
        public void setString(String key, String value) {
            data.put(key, value);
        }
        
        @Override
        public void setBoolean(String key, boolean value) {
            data.put(key, value);
        }
        
        @Override
        public void setByteArray(String key, byte[] value) {
            data.put(key, value);
        }
        
        @Override
        public void setIntArray(String key, int[] value) {
            data.put(key, value);
        }
        
        @Override
        public void setCompound(String key, NBTCompound value) {
            data.put(key, value);
        }        
        @Override
        public byte getByte(String key) {
            return data.containsKey(key) ? (byte) data.get(key) : 0;
        }
        
        @Override
        public short getShort(String key) {
            return data.containsKey(key) ? (short) data.get(key) : 0;
        }
        
        @Override
        public int getInt(String key) {
            return data.containsKey(key) ? (int) data.get(key) : 0;
        }
        
        @Override
        public long getLong(String key) {
            return data.containsKey(key) ? (long) data.get(key) : 0L;
        }
        
        @Override
        public float getFloat(String key) {
            return data.containsKey(key) ? (float) data.get(key) : 0f;
        }
        
        @Override
        public double getDouble(String key) {
            return data.containsKey(key) ? (double) data.get(key) : 0d;
        }
        
        @Override
        public String getString(String key) {
            return data.containsKey(key) ? (String) data.get(key) : "";
        }
        
        @Override
        public boolean getBoolean(String key) {
            return data.containsKey(key) ? (boolean) data.get(key) : false;
        }
        
        @Override
        public byte[] getByteArray(String key) {
            return data.containsKey(key) ? (byte[]) data.get(key) : new byte[0];
        }
        
        @Override
        public int[] getIntArray(String key) {
            return data.containsKey(key) ? (int[]) data.get(key) : new int[0];
        }
        
        @Override
        public NBTCompound getCompound(String key) {
            return data.containsKey(key) ? (NBTCompound) data.get(key) : null;
        }        
        @Override
        public boolean hasKey(String key) {
            return data.containsKey(key);
        }
        
        @Override
        public void removeKey(String key) {
            data.remove(key);
        }
        
        @Override
        public Set<String> getKeys() {
            return data.keySet();
        }
        
        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }
        
        @Override
        public Object getHandle() {
            // No handle in fallback mode
            return null;
        }
    }
}