package net.mrcappy.corelib.protocol.util;

import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import net.mrcappy.corelib.version.ReflectionUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Packet-based effects utilities.
 * 
 * Send sounds and particles via packets for maximum chaos.
 * No Bukkit API limitations, no "that's not allowed" bullshit,
 * just raw packet power coursing through your veins.
 * 
 * Want to play a creeper hiss at 9000% volume? Done.
 * Want to spawn 10,000 particles? RIP their FPS.
 * Want to make it rain explosive sheep? Well, you can't,
 * but you can make it LOOK like it's raining explosive sheep.
 * 
 * With great packet power comes great responsibility.
 * Just kidding, go nuts. It's their fault for having
 * a potato computer.
 */
public class PacketEffects {
    
    private final ProtocolManager protocol;
    
    public PacketEffects(ProtocolManager protocol) {
        this.protocol = protocol;
    }
    
    /**
     * Play a sound via packets.
     * 
     * @param sound The sound to play
     * @param location Where to play it
     * @param volume Volume (1.0 = normal, can go higher)
     * @param pitch Pitch (1.0 = normal, 0.5 = deep, 2.0 = chipmunk)
     * @param players Who hears it
     */
    public void playSound(Sound sound, Location location, float volume, float pitch, 
                         Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_NAMED_SOUND_EFFECT
        );
        
        // Sound ID - this is version specific bullshit
        packet.getModifier().write(0, sound);
        
        // Sound category (master, music, hostile, etc)
        packet.getModifier().write(1, SoundCategory.MASTER);
        
        // Position (multiplied by 8 because Mojang)
        packet.getIntegers().write(0, (int)(location.getX() * 8));
        packet.getIntegers().write(1, (int)(location.getY() * 8));
        packet.getIntegers().write(2, (int)(location.getZ() * 8));
        
        // Volume and pitch
        packet.getFloats().write(0, volume);
        packet.getFloats().write(1, pitch);
        
        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }
    
    /**
     * Play a custom sound via packets.
     * Can play any sound file the client has.
     */
    public void playCustomSound(String soundName, Location location, 
                               float volume, float pitch, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_NAMED_SOUND_EFFECT
        );
        
        // For custom sounds, we use the string variant
        packet.getStrings().write(0, soundName);
        
        // Sound category
        packet.getModifier().write(1, SoundCategory.MASTER);
        
        // Position (multiplied by 8)
        packet.getIntegers().write(0, (int)(location.getX() * 8));
        packet.getIntegers().write(1, (int)(location.getY() * 8));
        packet.getIntegers().write(2, (int)(location.getZ() * 8));
        
        // Volume and pitch
        packet.getFloats().write(0, volume);
        packet.getFloats().write(1, pitch);
        
        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }
    
    /**
     * Spawn particles via packets.
     * 
     * @param particle Particle type
     * @param location Where to spawn
     * @param count How many particles (warning: don't crash clients)
     * @param offsetX Random offset range
     * @param offsetY Random offset range  
     * @param offsetZ Random offset range
     * @param speed Particle speed/spread
     * @param data Extra data (for colored particles)
     */
    public void spawnParticle(Particle particle, Location location, int count,
                             double offsetX, double offsetY, double offsetZ,
                             double speed, Object data, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_WORLD_PARTICLES
        );
        
        // Particle type
        packet.getModifier().write(0, particle);
        
        // Long distance flag (render from far away)
        packet.getBooleans().write(0, true);
        
        // Position
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        
        // Offsets
        packet.getFloats().write(0, (float)offsetX);
        packet.getFloats().write(1, (float)offsetY);
        packet.getFloats().write(2, (float)offsetZ);
        
        // Speed
        packet.getFloats().write(3, (float)speed);
        
        // Count
        packet.getIntegers().write(0, count);
        
        // Particle data (color, block type, etc)
        if (data != null) {
            packet.getModifier().write(10, data);
        }
        
        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }    
    /**
     * Create a particle explosion effect.
     * Because everyone loves explosions.
     */
    public void createExplosion(Location center, float radius, int particleCount,
                               Player... players) {
        // Explosion particles in a sphere
        for (int i = 0; i < particleCount; i++) {
            double angle1 = Math.random() * Math.PI * 2;
            double angle2 = Math.random() * Math.PI;
            
            double x = center.getX() + radius * Math.sin(angle2) * Math.cos(angle1);
            double y = center.getY() + radius * Math.sin(angle2) * Math.sin(angle1);
            double z = center.getZ() + radius * Math.cos(angle2);
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            
            spawnParticle(Particle.EXPLOSION_NORMAL, particleLoc, 1,
                0, 0, 0, 0.1, null, players);
        }
        
        // Explosion sound
        playSound(Sound.ENTITY_GENERIC_EXPLODE, center, 2.0f, 1.0f, players);
    }
    
    /**
     * Make an entity glow for specific players.
     * No teams needed, just packets.
     */
    public void setGlowing(int entityId, boolean glowing, Player... players) {
        PacketContainer packet = PacketContainer.createPacket(
            PacketType.PLAY_SERVER_ENTITY_METADATA
        );
        
        packet.getIntegers().write(0, entityId);
        
        // This is where metadata gets nasty
        // The glowing flag is in byte 0, bit 6
        byte flags = glowing ? (byte)0x40 : (byte)0x00;
        
        // Create metadata list
        List<Object> metadataList = new ArrayList<>();
        
        try {
            // Get metadata classes
            Class<?> dataWatcherClass = ReflectionUtil.getNMSClass("network.syncher.SynchedEntityData");
            Class<?> dataWatcherItemClass = ReflectionUtil.getNMSClass("network.syncher.SynchedEntityData$DataValue");
            Class<?> dataWatcherObjectClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataAccessor");
            Class<?> dataWatcherSerializerClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataSerializer");
            Class<?> dataWatcherRegistryClass = ReflectionUtil.getNMSClass("network.syncher.EntityDataSerializers");
            
            // Get the BYTE serializer
            Field byteSerializerField = ReflectionUtil.getField(dataWatcherRegistryClass, "BYTE");
            Object byteSerializer = byteSerializerField.get(null);
            
            // Create EntityDataAccessor for index 0 (flags)
            Constructor<?> accessorConstructor = ReflectionUtil.getConstructor(
                dataWatcherObjectClass, int.class, dataWatcherSerializerClass
            );
            Object accessor = ReflectionUtil.newInstance(accessorConstructor, 0, byteSerializer);
            
            // Create DataValue
            Constructor<?> itemConstructor = ReflectionUtil.getConstructor(
                dataWatcherItemClass, dataWatcherObjectClass, Object.class
            );
            Object dataValue = ReflectionUtil.newInstance(itemConstructor, accessor, flags);
            
            metadataList.add(dataValue);
            
            // Set the metadata list in packet
            packet.getModifier().write(1, metadataList);
        } catch (Exception e) {
            // Metadata is version-specific cancer, log and continue
            protocol.getPlugin().getLogger().warning(
                "Failed to set glowing effect - version incompatibility: " + e.getMessage()
            );
        }
        
        for (Player player : players) {
            protocol.sendPacket(player, packet);
        }
    }
}