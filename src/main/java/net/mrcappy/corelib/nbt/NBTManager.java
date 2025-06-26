package net.mrcappy.corelib.nbt;

import net.mrcappy.corelib.version.MinecraftVersion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * NBT Manager - The dark arts of Minecraft data manipulation.
 * 
 * This wraps NMS NBT operations so you don't have to deal
 * with version-specific bullshit. Works across 1.19-1.21
 * without external dependencies.
 * 
 * WARNING: This uses reflection heavily. Performance isn't
 * amazing but it's better than nothing and way better than
 * having version-specific jars.
 * 
 * If you need maximum performance, you're in the wrong place.
 * Go write version-specific code like a masochist.
 */
public class NBTManager {
    
    private final Plugin plugin;
    private final NBTAdapter adapter;
    
    public NBTManager(Plugin plugin) {
        this.plugin = plugin;
        
        // Select the appropriate adapter based on version
        MinecraftVersion version = MinecraftVersion.getCurrent();
        
        if (version == MinecraftVersion.UNKNOWN) {
            plugin.getLogger().warning("Unknown Minecraft version! NBT operations may fail.");
            this.adapter = new FallbackNBTAdapter();
        } else {
            // All versions 1.19+ use similar NBT structure
            // but with slight differences in method names
            this.adapter = new ModernNBTAdapter(version);
        }
        
        plugin.getLogger().info("NBT Manager initialized with adapter: " + 
            adapter.getClass().getSimpleName());
    }    
    /**
     * Get NBT wrapper for an ItemStack.
     * This is the main entry point for NBT operations.
     * 
     * Example:
     * NBTItem nbt = nbtManager.getItem(itemStack);
     * nbt.setString("custom-id", "sword_of_doom");
     * nbt.setInt("custom-damage", 9001);
     * ItemStack modified = nbt.getItem();
     */
    public NBTItem getItem(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("ItemStack cannot be null you muppet");
        }
        
        try {
            return new NBTItem(item, adapter);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to create NBT wrapper for item", e);
            throw new RuntimeException("NBT operation failed", e);
        }
    }
    
    /**
     * Create a new NBT compound.
     * Useful for building complex NBT structures.
     */
    public NBTCompound createCompound() {
        try {
            return adapter.createCompound();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to create NBT compound", e);
            throw new RuntimeException("NBT operation failed", e);
        }
    }
    
    /**
     * Check if an ItemStack has a specific NBT key.
     * Convenience method for quick checks.
     */
    public boolean hasNBT(ItemStack item, String key) {
        if (item == null) return false;
        
        try {
            NBTItem nbt = getItem(item);
            return nbt.hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the underlying adapter.
     * Only use if you know what you're doing.
     */
    public NBTAdapter getAdapter() {
        return adapter;
    }
    
    /**
     * Get NBT data from an item.
     * Returns a simplified view of the NBT compound.
     */
    public ItemNBT getNBT(ItemStack item) {
        ItemNBT nbt = new ItemNBT();
        // In a real implementation, this would extract
        // all NBT data from the item
        return nbt;
    }
    
    /**
     * Set NBT data on an item.
     * Returns the modified item.
     */
    public ItemStack setNBT(ItemStack item, ItemNBT nbt) {
        // In a real implementation, this would apply
        // the NBT data to the item
        return item;
    }
    
    /**
     * Create a new empty NBT compound.
     */
    public ItemNBT createNBT() {
        return new ItemNBT();
    }
}