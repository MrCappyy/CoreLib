package net.mrcappy.corelib.nbt;

import org.bukkit.inventory.ItemStack;

/**
 * NBT adapter interface.
 * 
 * Different Minecraft versions have different NBT implementations.
 * This interface abstracts that away so we can swap implementations
 * based on the server version.
 * 
 * If you're implementing a new adapter, good fucking luck.
 * The reflection code is a nightmare.
 */
public interface NBTAdapter {
    
    /**
     * Create a new NBT compound.
     */
    NBTCompound createCompound();
    
    /**
     * Get NBT compound from an ItemStack.
     * Creates one if it doesn't exist.
     */
    NBTCompound getNBT(ItemStack item);
    
    /**
     * Apply NBT compound to an ItemStack.
     * Returns a new ItemStack with the NBT applied.
     */
    ItemStack applyNBT(ItemStack item, NBTCompound compound);
    
    /**
     * Convert NBT to string for debugging.
     * Because sometimes you need to see what the fuck is going on.
     */
    String toString(NBTCompound compound);
}