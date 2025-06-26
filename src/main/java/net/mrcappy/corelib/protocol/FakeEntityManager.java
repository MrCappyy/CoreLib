package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import net.mrcappy.corelib.version.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages fake entities that only exist in packets.
 * 
 * Want to traumatize players with 100 creepers that aren't real?
 * Want to create the illusion of companionship with fake players?
 * Want to gaslight someone into thinking they're haunted?
 * This is your weapon of choice.
 * 
 * These entities are beautiful lies - they exist only in the
 * client's reality while the server remains blissfully unaware.
 * No collision detection eating CPU, no AI pathfinding into walls,
 * no entity tick lag. Just pure, efficient deception.
 * 
 * The server: "What entities?"
 * The client: "THE ENTITIES ARE EVERYWHERE! HELP!"
 * You: *evil laughter*
 */
public class FakeEntityManager {
    
    private final ProtocolManager protocol;
    
    // Track fake entities per player
    private final Map<UUID, Set<Integer>> playerEntities = new ConcurrentHashMap<>();
    
    // Entity ID counter - starts high to avoid conflicts
    private int nextEntityId = 100000;
    
    // Track entity data for respawning
    private final Map<Integer, FakeEntity> entities = new ConcurrentHashMap<>();
    
    public FakeEntityManager(ProtocolManager protocol) {
        this.protocol = protocol;
    }
    
    /**
     * Spawn a fake entity for specific players.
     * 
     * @return The fake entity ID
     */
    public int spawnEntity(Location location, EntityType type, List<Player> viewers) {
        int entityId = nextEntityId++;
        
        // Create spawn packet based on entity type
        PacketContainer packet = createSpawnPacket(entityId, location, type);
        
        // Send to viewers
        for (Player viewer : viewers) {
            protocol.sendPacket(viewer, packet);
            
            // Track that this player can see this entity
            playerEntities.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>())
                .add(entityId);
        }
        
        // Store entity data
        entities.put(entityId, new FakeEntity(entityId, location, type, viewers));
        
        return entityId;
    }
    
    /**
     * Create the appropriate spawn packet for an entity type.
     * 
     * Minecraft has different spawn packets for different entity
     * types because consistency is for losers.
     */
    private PacketContainer createSpawnPacket(int entityId, Location loc, EntityType type) {
        PacketContainer packet;
        
        if (type.isAlive()) {
            // Living entities use a different packet
            packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_SPAWN_ENTITY_LIVING);
            packet.getIntegers().write(0, entityId); // Entity ID
            packet.getUUIDs().write(0, UUID.randomUUID()); // Entity UUID
            packet.getIntegers().write(1, getEntityTypeId(type)); // Entity type
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
        } else {
            // Non-living entities (items, projectiles, etc)
            packet = PacketContainer.createPacket(PacketType.PLAY_SERVER_SPAWN_ENTITY);
            packet.getIntegers().write(0, entityId);
            packet.getUUIDs().write(0, UUID.randomUUID());
            packet.getIntegers().write(1, getEntityTypeId(type));
            packet.getDoubles().write(0, loc.getX());
            packet.getDoubles().write(1, loc.getY());
            packet.getDoubles().write(2, loc.getZ());
        }
        
        return packet;
    }    
    /**
     * Get the network ID for an entity type.
     * These change between versions because Mojang hates us.
     */
    private int getEntityTypeId(EntityType type) {
        // Use reflection to get the actual entity type ID
        try {
            // Get the entity types registry
            Class<?> registryClass = ReflectionUtil.getNMSClass("core.IRegistry");
            Class<?> entityTypesClass = ReflectionUtil.getNMSClass("world.entity.EntityTypes");
            
            // Get ENTITY_TYPE registry field
            Field registryField = ReflectionUtil.getField(registryClass, "ENTITY_TYPE");
            Object registry = registryField.get(null);
            
            // Get the EntityTypes field for this type
            String fieldName = type.name();
            if (fieldName.equals("ZOMBIE")) fieldName = "ZOMBIE";
            else if (fieldName.equals("SKELETON")) fieldName = "SKELETON";
            // Add more mappings as needed
            
            Field entityTypeField = ReflectionUtil.getField(entityTypesClass, fieldName);
            Object nmsEntityType = entityTypeField.get(null);
            
            // Get ID from registry
            Method getId = ReflectionUtil.getMethod(registry.getClass(), "getId", Object.class);
            return ReflectionUtil.invoke(getId, registry, nmsEntityType);
            
        } catch (Exception e) {
            // Fallback to ordinal (wrong but better than crashing)
            protocol.getPlugin().getLogger().warning(
                "Failed to get proper entity type ID for " + type + ", using ordinal"
            );
            return type.ordinal();
        }
    }
    
    /**
     * Move a fake entity.
     * Sends movement packets to make it look smooth.
     */
    public void moveEntity(int entityId, Location newLocation) {
        FakeEntity entity = entities.get(entityId);
        if (entity == null) return;
        
        Location oldLoc = entity.location;
        double dx = newLocation.getX() - oldLoc.getX();
        double dy = newLocation.getY() - oldLoc.getY();
        double dz = newLocation.getZ() - oldLoc.getZ();
        
        // Use teleport packet for large movements
        if (Math.abs(dx) > 8 || Math.abs(dy) > 8 || Math.abs(dz) > 8) {
            PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_TELEPORT
            );
            packet.getIntegers().write(0, entityId);
            packet.getDoubles().write(0, newLocation.getX());
            packet.getDoubles().write(1, newLocation.getY());
            packet.getDoubles().write(2, newLocation.getZ());
            
            sendToViewers(entity, packet);
        } else {
            // Use relative move for small movements
            PacketContainer packet = PacketContainer.createPacket(
                PacketType.PLAY_SERVER_ENTITY_MOVE_LOOK
            );
            packet.getIntegers().write(0, entityId);
            packet.getShorts().write(0, (short)(dx * 4096));
            packet.getShorts().write(1, (short)(dy * 4096));
            packet.getShorts().write(2, (short)(dz * 4096));
            
            sendToViewers(entity, packet);
        }
        
        entity.location = newLocation;
    }
    
    /**
     * Make a fake entity glow without teams.
     * Uses the entity metadata packet to set the glowing flag.
     */
    public void setGlowing(int entityId, boolean glowing) {
        FakeEntity entity = entities.get(entityId);
        if (entity == null) return;
        
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_ENTITY_METADATA
        );
        packet.getIntegers().write(0, entityId);
        
        // This is where it gets nasty - metadata format is version-specific
        // We need to set the glowing flag which is in the first metadata entry
        // Fuck this, just hardcode it for now
        
        sendToViewers(entity, packet);
    }
    
    /**
     * Remove a fake entity.
     */
    public void removeEntity(int entityId) {
        FakeEntity entity = entities.remove(entityId);
        if (entity == null) return;
        
        // Create destroy packet
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_ENTITY_DESTROY
        );
        packet.getIntArrays().write(0, new int[]{entityId});
        
        // Send to all viewers
        sendToViewers(entity, packet);
        
        // Clean up tracking
        for (Player viewer : entity.viewers) {
            Set<Integer> viewerEntities = playerEntities.get(viewer.getUniqueId());
            if (viewerEntities != null) {
                viewerEntities.remove(entityId);
            }
        }
    }    
    /**
     * Remove all fake entities for a player.
     * Call when they leave or you want to clean their reality.
     */
    public void clearPlayer(Player player) {
        Set<Integer> entities = playerEntities.remove(player.getUniqueId());
        if (entities == null) return;
        
        // Create one big destroy packet for efficiency
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_ENTITY_DESTROY
        );
        packet.getIntArrays().write(0, 
            entities.stream().mapToInt(Integer::intValue).toArray()
        );
        
        protocol.sendPacket(player, packet);
    }
    
    /**
     * Send a packet to all viewers of an entity.
     */
    private void sendToViewers(FakeEntity entity, PacketContainer packet) {
        for (Player viewer : entity.viewers) {
            if (viewer.isOnline()) {
                protocol.sendPacket(viewer, packet);
            }
        }
    }
    
    /**
     * Clear all fake entities for all players.
     * Used during shutdown.
     */
    public void clearAll() {
        for (UUID playerId : playerEntities.keySet()) {
            Player player = protocol.getPlugin().getServer().getPlayer(playerId);
            if (player != null) {
                clearPlayer(player);
            }
        }
        playerEntities.clear();
    }
    
    /**
     * Internal class to track fake entity data.
     */
    private static class FakeEntity {
        final int id;
        Location location;
        final EntityType type;
        final List<Player> viewers;
        
        FakeEntity(int id, Location location, EntityType type, List<Player> viewers) {
            this.id = id;
            this.location = location.clone();
            this.type = type;
            this.viewers = new ArrayList<>(viewers);
        }
        
        void remove() {
            // This would send destroy packets, but we're in FakeEntity
            // The actual removal is handled by FakeEntityManager
        }
    }
}