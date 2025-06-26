package net.mrcappy.corelib.nbt;

import net.mrcappy.corelib.version.MinecraftVersion;
import net.mrcappy.corelib.version.ReflectionUtil;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Modern NBT adapter for Minecraft 1.19-1.21.
 * 
 * This is where the reflection nightmare lives.
 * If you're debugging NBT issues, start here and
 * prepare for pain.
 * 
 * This adapter uses the Mojang-mapped NMS classes
 * which are somewhat consistent across versions,
 * but method names still change because fuck you.
 */
public class ModernNBTAdapter implements NBTAdapter {
    
    private final MinecraftVersion version;
    
    // Cached classes - these fuckers are expensive to look up
    private final Class<?> nbtTagCompoundClass;
    private final Class<?> craftItemStackClass;
    private final Class<?> nmsItemStackClass;
    
    // Cached methods - same deal
    private final Method asNMSCopy;
    private final Method asBukkitCopy;
    private final Method getOrCreateTag;
    private final Method setTag;
    
    public ModernNBTAdapter(MinecraftVersion version) {
        this.version = version;
        
        try {
            // Load classes
            nbtTagCompoundClass = ReflectionUtil.getNMSClass("nbt.CompoundTag");
            craftItemStackClass = ReflectionUtil.getCraftBukkitClass("inventory.CraftItemStack");
            nmsItemStackClass = ReflectionUtil.getNMSClass("world.item.ItemStack");
            
            // Load methods
            asNMSCopy = ReflectionUtil.getMethod(craftItemStackClass, "asNMSCopy", ItemStack.class);
            asBukkitCopy = ReflectionUtil.getMethod(craftItemStackClass, "asBukkitCopy", nmsItemStackClass);
            
            // These method names vary by version, god damn it
            if (version.isAtLeast(MinecraftVersion.v1_20_R3)) {
                getOrCreateTag = ReflectionUtil.getMethod(nmsItemStackClass, "getOrCreateTag");
                setTag = ReflectionUtil.getMethod(nmsItemStackClass, "setTag", nbtTagCompoundClass);
            } else {
                // Older versions have slightly different names
                getOrCreateTag = ReflectionUtil.getMethod(nmsItemStackClass, "getOrCreateTag");
                setTag = ReflectionUtil.getMethod(nmsItemStackClass, "setTag", nbtTagCompoundClass);
            }
            
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to initialize NBT adapter for version " + version, e
            );
        }
    }    
    @Override
    public NBTCompound createCompound() {
        try {
            Constructor<?> constructor = ReflectionUtil.getConstructor(nbtTagCompoundClass);
            Object nmsCompound = constructor.newInstance();
            return new ModernNBTCompound(nmsCompound);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create NBT compound", e);
        }
    }
    
    @Override
    public NBTCompound getNBT(ItemStack item) {
        try {
            // Convert to NMS ItemStack
            Object nmsItem = ReflectionUtil.invoke(asNMSCopy, null, item);
            
            // Get or create the tag
            Object nmsCompound = ReflectionUtil.invoke(getOrCreateTag, nmsItem);
            
            return new ModernNBTCompound(nmsCompound);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NBT from item", e);
        }
    }
    
    @Override
    public ItemStack applyNBT(ItemStack item, NBTCompound compound) {
        try {
            // Convert to NMS ItemStack
            Object nmsItem = ReflectionUtil.invoke(asNMSCopy, null, item);
            
            // Set the tag
            Object nmsCompound = ((ModernNBTCompound) compound).getHandle();
            ReflectionUtil.invoke(setTag, nmsItem, nmsCompound);
            
            // Convert back to Bukkit ItemStack
            return ReflectionUtil.invoke(asBukkitCopy, null, nmsItem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply NBT to item", e);
        }
    }
    
    @Override
    public String toString(NBTCompound compound) {
        try {
            Object nmsCompound = ((ModernNBTCompound) compound).getHandle();
            return nmsCompound.toString();
        } catch (Exception e) {
            return "NBTCompound[ERROR: " + e.getMessage() + "]";
        }
    }    
    /**
     * Modern NBT compound implementation.
     * This wraps the NMS CompoundTag class.
     * 
     * Every method here involves reflection.
     * It's slow. Deal with it.
     */
    private class ModernNBTCompound implements NBTCompound {
        
        private final Object nmsCompound;
        
        ModernNBTCompound(Object nmsCompound) {
            this.nmsCompound = nmsCompound;
        }
        
        @Override
        public void setByte(String key, byte value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putByte", String.class, byte.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set byte: " + key, e);
            }
        }
        
        @Override
        public void setShort(String key, short value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putShort", String.class, short.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set short: " + key, e);
            }
        }
        
        @Override
        public void setInt(String key, int value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putInt", String.class, int.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set int: " + key, e);
            }
        }        
        @Override
        public void setLong(String key, long value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putLong", String.class, long.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set long: " + key, e);
            }
        }
        
        @Override
        public void setFloat(String key, float value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putFloat", String.class, float.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set float: " + key, e);
            }
        }
        
        @Override
        public void setDouble(String key, double value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putDouble", String.class, double.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set double: " + key, e);
            }
        }        
        @Override
        public void setString(String key, String value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putString", String.class, String.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set string: " + key, e);
            }
        }
        
        @Override
        public void setBoolean(String key, boolean value) {
            // NBT doesn't have native boolean, use byte
            setByte(key, (byte)(value ? 1 : 0));
        }
        
        @Override
        public void setByteArray(String key, byte[] value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putByteArray", String.class, byte[].class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set byte array: " + key, e);
            }
        }
        
        @Override
        public void setIntArray(String key, int[] value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "putIntArray", String.class, int[].class
                );
                ReflectionUtil.invoke(method, nmsCompound, key, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set int array: " + key, e);
            }
        }
        
        @Override
        public void setCompound(String key, NBTCompound value) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "put", String.class, 
                    ReflectionUtil.getNMSClass("nbt.Tag")
                );
                Object nmsValue = ((ModernNBTCompound) value).getHandle();
                ReflectionUtil.invoke(method, nmsCompound, key, nmsValue);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set compound: " + key, e);
            }
        }        
        // Getters - even more reflection hell
        
        @Override
        public byte getByte(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getByte", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public short getShort(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getShort", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public int getInt(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getInt", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public long getLong(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getLong", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0L;
            }
        }        
        @Override
        public float getFloat(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getFloat", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0f;
            }
        }
        
        @Override
        public double getDouble(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getDouble", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return 0d;
            }
        }
        
        @Override
        public String getString(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getString", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return "";
            }
        }
        
        @Override
        public boolean getBoolean(String key) {
            return getByte(key) != 0;
        }
        
        @Override
        public byte[] getByteArray(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getByteArray", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return new byte[0];
            }
        }        
        @Override
        public int[] getIntArray(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getIntArray", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return new int[0];
            }
        }
        
        @Override
        public NBTCompound getCompound(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getCompound", String.class
                );
                Object compound = ReflectionUtil.invoke(method, nmsCompound, key);
                return compound != null ? new ModernNBTCompound(compound) : null;
            } catch (Exception e) {
                return null;
            }
        }
        
        @Override
        public boolean hasKey(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "contains", String.class
                );
                return ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        public void removeKey(String key) {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "remove", String.class
                );
                ReflectionUtil.invoke(method, nmsCompound, key);
            } catch (Exception e) {
                // Silently fail, what else can we do?
            }
        }        
        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getKeys() {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "getAllKeys"
                );
                return ReflectionUtil.invoke(method, nmsCompound);
            } catch (Exception e) {
                return Set.of();
            }
        }
        
        @Override
        public boolean isEmpty() {
            try {
                Method method = ReflectionUtil.getMethod(
                    nbtTagCompoundClass, "isEmpty"
                );
                return ReflectionUtil.invoke(method, nmsCompound);
            } catch (Exception e) {
                return true;
            }
        }
        
        @Override
        public Object getHandle() {
            return nmsCompound;
        }
    }
}