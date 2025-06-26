package net.mrcappy.corelib.protocol.packet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet types registry.
 * 
 * This maps packet classes to their types because Minecraft
 * has approximately 47 billion different packet types and they
 * multiply like rabbits every update.
 * 
 * Mojang: "Let's add 17 new packets for... bamboo rendering!"
 * Me: *screaming internally while updating this enum*
 * 
 * Half these packets do the same shit but exist separately
 * because consistency is for people who don't hate their users.
 * 
 * If you're adding support for a new version, grab a bottle
 * of your strongest liquor. You'll need it.
 */
public enum PacketType {
    
    // Handshake packets
    HANDSHAKE_CLIENT_SET_PROTOCOL("PacketHandshakingInSetProtocol", Protocol.HANDSHAKE, Direction.SERVERBOUND),
    
    // Status packets  
    STATUS_CLIENT_START("PacketStatusInStart", Protocol.STATUS, Direction.SERVERBOUND),
    STATUS_CLIENT_PING("PacketStatusInPing", Protocol.STATUS, Direction.SERVERBOUND),
    STATUS_SERVER_INFO("PacketStatusOutServerInfo", Protocol.STATUS, Direction.CLIENTBOUND),
    STATUS_SERVER_PONG("PacketStatusOutPong", Protocol.STATUS, Direction.CLIENTBOUND),
    
    // Login packets
    LOGIN_CLIENT_START("PacketLoginInStart", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_CLIENT_ENCRYPTION_BEGIN("PacketLoginInEncryptionBegin", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_CLIENT_CUSTOM_PAYLOAD("PacketLoginInCustomPayload", Protocol.LOGIN, Direction.SERVERBOUND),
    LOGIN_SERVER_DISCONNECT("PacketLoginOutDisconnect", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_ENCRYPTION_BEGIN("PacketLoginOutEncryptionBegin", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_SUCCESS("PacketLoginOutSuccess", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_SET_COMPRESSION("PacketLoginOutSetCompression", Protocol.LOGIN, Direction.CLIENTBOUND),
    LOGIN_SERVER_CUSTOM_PAYLOAD("PacketLoginOutCustomPayload", Protocol.LOGIN, Direction.CLIENTBOUND),
    
    // Play packets - Dear god there are so many
    // Client -> Server
    PLAY_CLIENT_KEEP_ALIVE("ServerboundKeepAlivePacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CHAT("ServerboundChatPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CHAT_COMMAND("ServerboundChatCommandPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_USE_ENTITY("ServerboundInteractPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_POSITION("ServerboundMovePlayerPosPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_POSITION_LOOK("ServerboundMovePlayerPosRotPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_LOOK("ServerboundMovePlayerRotPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_FLYING("ServerboundMovePlayerStatusOnlyPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_BLOCK_DIG("ServerboundPlayerActionPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_BLOCK_PLACE("ServerboundUseItemOnPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_HELD_ITEM_SLOT("ServerboundSetCarriedItemPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_ARM_ANIMATION("ServerboundSwingPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_ENTITY_ACTION("ServerboundPlayerCommandPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CLOSE_WINDOW("ServerboundContainerClosePacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_WINDOW_CLICK("ServerboundContainerClickPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_CUSTOM_PAYLOAD("ServerboundCustomPayloadPacket", Protocol.PLAY, Direction.SERVERBOUND),
    PLAY_CLIENT_USE_ITEM("ServerboundUseItemPacket", Protocol.PLAY, Direction.SERVERBOUND),    
    // Server -> Client  
    PLAY_SERVER_KEEP_ALIVE("ClientboundKeepAlivePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_CHAT("ClientboundSystemChatPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_PLAYER_CHAT("ClientboundPlayerChatPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_EQUIPMENT("ClientboundSetEquipmentPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_POSITION("ClientboundSetDefaultSpawnPositionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_UPDATE_HEALTH("ClientboundSetHealthPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_RESPAWN("ClientboundRespawnPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_POSITION("ClientboundPlayerPositionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_HELD_ITEM_SLOT("ClientboundSetCarriedItemPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ANIMATION("ClientboundAnimatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY("ClientboundAddEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY_LIVING("ClientboundAddMobPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_SPAWN_ENTITY_EXPERIENCE_ORB("ClientboundAddExperienceOrbPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_VELOCITY("ClientboundSetEntityMotionPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_DESTROY("ClientboundRemoveEntitiesPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY("ClientboundMoveEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_LOOK("ClientboundMoveEntityPacket$Rot", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_MOVE_LOOK("ClientboundMoveEntityPacket$PosRot", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_TELEPORT("ClientboundTeleportEntityPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_HEAD_ROTATION("ClientboundRotateHeadPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_STATUS("ClientboundEntityEventPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_METADATA("ClientboundSetEntityDataPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_ENTITY_EFFECT("ClientboundUpdateMobEffectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_REMOVE_ENTITY_EFFECT("ClientboundRemoveMobEffectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_BLOCK_CHANGE("ClientboundBlockUpdatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_MULTI_BLOCK_CHANGE("ClientboundSectionBlocksUpdatePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_EXPLOSION("ClientboundExplodePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_WORLD_PARTICLES("ClientboundLevelParticlesPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_NAMED_SOUND_EFFECT("ClientboundSoundPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_WORLD_EVENT("ClientboundLevelEventPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_CUSTOM_PAYLOAD("ClientboundCustomPayloadPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_KICK_DISCONNECT("ClientboundDisconnectPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_TITLE("ClientboundSetTitleTextPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_TAB_COMPLETE("ClientboundTabListPacket", Protocol.PLAY, Direction.CLIENTBOUND),
    PLAY_SERVER_BUNDLE("ClientboundBundlePacket", Protocol.PLAY, Direction.CLIENTBOUND),
    
    // Special unknown type for packets we don't recognize
    UNKNOWN("Unknown", Protocol.UNKNOWN, Direction.UNKNOWN);
    
    private final String className;
    private final Protocol protocol;
    private final Direction direction;
    
    // Caches for fast lookup
    private static final Map<Class<?>, PacketType> CLASS_TO_TYPE = new ConcurrentHashMap<>();
    private static final Map<String, PacketType> NAME_TO_TYPE = new HashMap<>();
    
    static {
        // Build lookup tables
        for (PacketType type : values()) {
            NAME_TO_TYPE.put(type.className, type);
        }
    }    
    PacketType(String className, Protocol protocol, Direction direction) {
        this.className = className;
        this.protocol = protocol;
        this.direction = direction;
    }
    
    public String getClassName() {
        return className;
    }
    
    public Protocol getProtocol() {
        return protocol;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public boolean isServerbound() {
        return direction == Direction.SERVERBOUND;
    }
    
    public boolean isClientbound() {
        return direction == Direction.CLIENTBOUND;
    }
    
    /**
     * Get packet type from a packet class.
     * Uses cache for performance.
     */
    public static PacketType fromClass(Class<?> clazz) {
        return CLASS_TO_TYPE.computeIfAbsent(clazz, c -> {
            // Try exact class name match
            String simpleName = c.getSimpleName();
            PacketType type = NAME_TO_TYPE.get(simpleName);
            if (type != null) return type;
            
            // Try to find by checking superclasses
            Class<?> current = c;
            while (current != null && current != Object.class) {
                type = NAME_TO_TYPE.get(current.getSimpleName());
                if (type != null) return type;
                current = current.getSuperclass();
            }
            
            // Couldn't find it
            return UNKNOWN;
        });
    }
    
    /**
     * Get packet type from a packet object.
     */
    public static PacketType fromPacket(Object packet) {
        return fromClass(packet.getClass());
    }
    
    /**
     * Protocol stage enum.
     */
    public enum Protocol {
        HANDSHAKE,
        STATUS,
        LOGIN,
        PLAY,
        UNKNOWN
    }
    
    /**
     * Packet direction enum.
     */
    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND,
        UNKNOWN
    }
}