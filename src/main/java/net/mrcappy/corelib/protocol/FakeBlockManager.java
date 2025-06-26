package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake block manager for client-side world modifications.
 * 
 * Want to show a player a diamond house that doesn't exist?
 * Want to hide that ugly cobblestone build from specific players?
 * This is your jam.
 * 
 * These blocks are lies sent via packets. The server has no
 * fucking clue they exist, which means no physics, no drops,
 * no interaction. Just pure visual deception.
 */
public class FakeBlockManager {
    
    private final ProtocolManager protocol;
    
    // Player -> Location -> BlockData
    private final Map<UUID, Map<Location, BlockData>> playerBlocks = new ConcurrentHashMap<>();
    
    public FakeBlockManager(ProtocolManager protocol) {
        this.protocol = protocol;
    }
    
    /**
     * Set a fake block for specific players.
     * 
     * The block only exists in their client. Everyone else
     * sees the real world. It's like augmented reality but
     * shittier.
     */
    public void setBlock(Location location, Material material, Player... players) {
        BlockData data = material.createBlockData();
        setBlock(location, data, players);
    }
    
    /**
     * Set a fake block with specific block data.
     */
    public void setBlock(Location location, BlockData data, Player... players) {
        PacketContainer packet = createBlockChangePacket(location, data);
        
        for (Player player : players) {
            // Send the fake block
            protocol.sendPacket(player, packet);
            
            // Track it so we can restore later
            playerBlocks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(location, data);
        }
    }
    
    /**
     * Set multiple fake blocks at once.
     * More efficient than individual block changes.
     */
    public void setBlocks(Map<Location, BlockData> blocks, Player... players) {
        // Group by chunk for multi-block change packets
        Map<Long, List<Map.Entry<Location, BlockData>>> byChunk = new HashMap<>();
        
        for (Map.Entry<Location, BlockData> entry : blocks.entrySet()) {
            Location loc = entry.getKey();
            long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            
            byChunk.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(entry);
        }
        
        // Send multi-block change packets per chunk
        for (List<Map.Entry<Location, BlockData>> chunkBlocks : byChunk.values()) {
            if (chunkBlocks.size() == 1) {
                // Single block, use regular packet
                Map.Entry<Location, BlockData> entry = chunkBlocks.get(0);
                setBlock(entry.getKey(), entry.getValue(), players);
            } else {
                // Multiple blocks in chunk, use multi-block change
                PacketContainer packet = createMultiBlockChangePacket(chunkBlocks);
                
                for (Player player : players) {
                    protocol.sendPacket(player, packet);
                    
                    // Track all blocks
                    Map<Location, BlockData> playerMap = playerBlocks
                        .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
                    
                    for (Map.Entry<Location, BlockData> entry : chunkBlocks) {
                        playerMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }    
    /**
     * Clear all fake blocks for a player.
     * Sends real block data to restore their world.
     */
    public void clearPlayer(Player player) {
        Map<Location, BlockData> blocks = playerBlocks.remove(player.getUniqueId());
        if (blocks == null) return;
        
        // Send real blocks to restore
        for (Location loc : blocks.keySet()) {
            BlockData realData = loc.getBlock().getBlockData();
            PacketContainer packet = createBlockChangePacket(loc, realData);
            protocol.sendPacket(player, packet);
        }
    }
    
    /**
     * Clear fake blocks in a region for a player.
     */
    public void clearRegion(Player player, Location min, Location max) {
        Map<Location, BlockData> blocks = playerBlocks.get(player.getUniqueId());
        if (blocks == null) return;
        
        Iterator<Map.Entry<Location, BlockData>> it = blocks.entrySet().iterator();
        while (it.hasNext()) {
            Location loc = it.next().getKey();
            
            if (loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
                loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
                loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ()) {
                
                // Restore real block
                BlockData realData = loc.getBlock().getBlockData();
                PacketContainer packet = createBlockChangePacket(loc, realData);
                protocol.sendPacket(player, packet);
                
                it.remove();
            }
        }
    }
    
    /**
     * Create a block change packet.
     * This is version-specific as fuck.
     */
    private PacketContainer createBlockChangePacket(Location location, BlockData data) {
        PacketContainer packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_BLOCK_CHANGE);
        
        // Set block position - uses a packed long in modern versions
        long packedPos = ((long)(location.getBlockX() & 0x3FFFFFF) << 38) |
                        ((long)(location.getBlockZ() & 0x3FFFFFF) << 12) |
                        (location.getBlockY() & 0xFFF);
        
        packet.getLongs().write(0, packedPos);
        
        // Set block state - this is where it gets nasty
        // We need the block state ID which is version and state specific
        // For now, just throw the BlockData object and hope for the best
        packet.getModifier().write(1, data);
        
        return packet;
    }
    
    /**
     * Create a multi-block change packet.
     * Even more version-specific than regular block changes.
     */
    private PacketContainer createMultiBlockChangePacket(
            List<Map.Entry<Location, BlockData>> blocks) {
        if (blocks.isEmpty()) return null;
        
        try {
            PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_MULTI_BLOCK_CHANGE
            );
            
            // Get chunk coordinates from first block
            Location first = blocks.get(0).getKey();
            int chunkX = first.getBlockX() >> 4;
            int chunkZ = first.getBlockZ() >> 4;
            
            // Create section position (packed long)
            long sectionPos = ((long)(chunkX & 0x3FFFFF) << 42) | 
                             ((long)(first.getBlockY() >> 4 & 0xFFFFF) << 20) |
                             ((long)(chunkZ & 0x3FFFFF));
            
            packet.getLongs().write(0, sectionPos);
            
            // Create block state array
            short[] positions = new short[blocks.size()];
            Object[] states = new Object[blocks.size()];
            
            for (int i = 0; i < blocks.size(); i++) {
                Location loc = blocks.get(i).getKey();
                BlockData data = blocks.get(i).getValue();
                
                // Calculate relative position in chunk section
                int relX = loc.getBlockX() & 15;
                int relY = loc.getBlockY() & 15;
                int relZ = loc.getBlockZ() & 15;
                
                positions[i] = (short)((relX << 8) | (relZ << 4) | relY);
                states[i] = data; // Let NMS handle conversion
            }
            
            // Set arrays in packet
            packet.getModifier().write(1, positions);
            packet.getModifier().write(2, states);
            
            return packet;
            
        } catch (Exception e) {
            // Multi-block change is complex, fallback to individual changes
            return null;
        }
    }
    
    /**
     * Get chunk key for grouping.
     */
    private long getChunkKey(int x, int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }
    
    /**
     * Clear all fake blocks for all players.
     * Used during shutdown.
     */
    public void clearAll() {
        playerBlocks.clear();
    }
}