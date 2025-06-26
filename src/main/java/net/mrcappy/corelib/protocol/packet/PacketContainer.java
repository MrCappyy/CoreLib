package net.mrcappy.corelib.protocol.packet;

import net.mrcappy.corelib.protocol.reflect.FieldAccessor;
import net.mrcappy.corelib.protocol.reflect.StructureModifier;
import net.mrcappy.corelib.version.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Container for a packet with easy field access.
 * 
 * This wraps a NMS packet and provides convenient
 * methods to read/write fields without dealing with
 * reflection directly.
 * 
 * The StructureModifier system is inspired by ProtocolLib
 * but built from scratch. It's slower than direct field
 * access but way more convenient and version-independent.
 */
public class PacketContainer {
    
    private final Object handle; // The actual NMS packet
    private final PacketType type;
    private final boolean cancelled;
    
    // Structure modifiers for different field types
    private StructureModifier<Byte> bytes;
    private StructureModifier<Short> shorts;
    private StructureModifier<Integer> integers;
    private StructureModifier<Long> longs;
    private StructureModifier<Float> floats;
    private StructureModifier<Double> doubles;
    private StructureModifier<String> strings;
    private StructureModifier<Boolean> booleans;
    private StructureModifier<UUID> uuids;
    private StructureModifier<byte[]> byteArrays;
    private StructureModifier<int[]> intArrays;
    private StructureModifier<Object> modifier;
    
    public PacketContainer(Object handle) {
        this.handle = handle;
        this.type = PacketType.fromPacket(handle);
        this.cancelled = false;
        
        // Initialize base modifier
        this.modifier = new StructureModifier<>(handle.getClass(), Object.class);
    }
    
    public PacketContainer(PacketType type, Object handle) {
        this.handle = handle;
        this.type = type;
        this.cancelled = false;
        this.modifier = new StructureModifier<>(handle.getClass(), Object.class);
    }
    
    /**
     * Create a new packet of the given type.
     * Uses reflection to instantiate.
     */
    public static PacketContainer createPacket(PacketType type) {
        try {
            // Get the packet class
            Class<?> packetClass = ReflectionUtil.getNMSClass(
                getPacketPath(type)
            );
            
            // Create instance
            Object packet = ReflectionUtil.getConstructor(packetClass).newInstance();
            
            return new PacketContainer(type, packet);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create packet of type: " + type, e
            );
        }
    }    
    /**
     * Get the packet class path for a type.
     * This is version-specific bullshit.
     */
    private static String getPacketPath(PacketType type) {
        switch (type.getProtocol()) {
            case HANDSHAKE:
                return "network.protocol.handshake." + type.getClassName();
            case STATUS:
                return "network.protocol.status." + type.getClassName();
            case LOGIN:
                return "network.protocol.login." + type.getClassName();
            case PLAY:
                return "network.protocol.game." + type.getClassName();
            default:
                throw new IllegalArgumentException("Unknown protocol: " + type.getProtocol());
        }
    }
    
    /**
     * Get the underlying NMS packet.
     */
    public Object getHandle() {
        return handle;
    }
    
    /**
     * Get the packet type.
     */
    public PacketType getType() {
        return type;
    }
    
    /**
     * Clone this packet.
     * Creates a deep copy using serialization.
     */
    public PacketContainer deepClone() {
        try {
            // This is hacky but works
            Object cloned = handle.getClass().newInstance();
            
            // Copy all fields
            StructureModifier<Object> source = getModifier();
            StructureModifier<Object> target = new StructureModifier<>(
                cloned.getClass(), Object.class
            );
            
            for (int i = 0; i < source.size(); i++) {
                target.write(i, source.read(i));
            }
            
            return new PacketContainer(type, cloned);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone packet", e);
        }
    }
    
    // Field accessors with lazy initialization
    
    public StructureModifier<Byte> getBytes() {
        if (bytes == null) {
            bytes = modifier.withType(byte.class);
        }
        return bytes;
    }
    
    public StructureModifier<Short> getShorts() {
        if (shorts == null) {
            shorts = modifier.withType(short.class);
        }
        return shorts;
    }    
    public StructureModifier<Integer> getIntegers() {
        if (integers == null) {
            integers = modifier.withType(int.class);
        }
        return integers;
    }
    
    public StructureModifier<Long> getLongs() {
        if (longs == null) {
            longs = modifier.withType(long.class);
        }
        return longs;
    }
    
    public StructureModifier<Float> getFloats() {
        if (floats == null) {
            floats = modifier.withType(float.class);
        }
        return floats;
    }
    
    public StructureModifier<Double> getDoubles() {
        if (doubles == null) {
            doubles = modifier.withType(double.class);
        }
        return doubles;
    }
    
    public StructureModifier<String> getStrings() {
        if (strings == null) {
            strings = modifier.withType(String.class);
        }
        return strings;
    }
    
    public StructureModifier<Boolean> getBooleans() {
        if (booleans == null) {
            booleans = modifier.withType(boolean.class);
        }
        return booleans;
    }
    
    public StructureModifier<UUID> getUUIDs() {
        if (uuids == null) {
            uuids = modifier.withType(UUID.class);
        }
        return uuids;
    }
    
    public StructureModifier<byte[]> getByteArrays() {
        if (byteArrays == null) {
            byteArrays = modifier.withType(byte[].class);
        }
        return byteArrays;
    }
    
    public StructureModifier<int[]> getIntArrays() {
        if (intArrays == null) {
            intArrays = modifier.withType(int[].class);
        }
        return intArrays;
    }
    
    /**
     * Get the base modifier for custom types.
     */
    public StructureModifier<Object> getModifier() {
        return modifier;
    }
    
    /**
     * Get a specific modifier for a type.
     */
    @SuppressWarnings("unchecked")
    public <T> StructureModifier<T> getSpecificModifier(Class<T> type) {
        return (StructureModifier<T>) modifier.withType(type);
    }
    
    // Convenience methods for common operations
    
    /**
     * Get entity ID from packets that have one.
     */
    public int getEntityId() {
        return getIntegers().read(0);
    }
    
    /**
     * Set entity ID for packets that have one.
     */
    public void setEntityId(int id) {
        getIntegers().write(0, id);
    }
    
    /**
     * Get chat message from chat packets.
     * Works for both legacy string and component chat.
     */
    public String getMessage() {
        if (type == PacketType.PLAY_CLIENT_CHAT || 
            type == PacketType.PLAY_SERVER_CHAT) {
            return getStrings().read(0);
        }
        throw new IllegalStateException(
            "getMessage() called on non-chat packet: " + type
        );
    }
    
    /**
     * Set chat message.
     */
    public void setMessage(String message) {
        if (type == PacketType.PLAY_CLIENT_CHAT || 
            type == PacketType.PLAY_SERVER_CHAT) {
            getStrings().write(0, message);
        } else {
            throw new IllegalStateException(
                "setMessage() called on non-chat packet: " + type
            );
        }
    }
    
    /**
     * Get block position from block-related packets.
     * Returns as Location with world = null.
     */
    public org.bukkit.Location getBlockPosition() {
        if (type.name().contains("BLOCK")) {
            long packed = getLongs().read(0);
            int x = (int)(packed >> 38);
            int y = (int)(packed & 0xFFF);
            int z = (int)(packed << 26 >> 38);
            return new org.bukkit.Location(null, x, y, z);
        }
        throw new IllegalStateException(
            "getBlockPosition() called on non-block packet: " + type
        );
    }
    
    /**
     * Get raw bytes for low-level manipulation.
     * Here be dragons.
     */
    public byte[] getRawBytes() {
        try {
            // Get the packet serializer
            Class<?> packetDataSerializerClass = ReflectionUtil.getNMSClass("network.FriendlyByteBuf");
            Class<?> unpooledClass = ReflectionUtil.getClass("io.netty.buffer.Unpooled");
            
            // Create a new buffer
            Method buffer = ReflectionUtil.getMethod(unpooledClass, "buffer");
            Object byteBuf = ReflectionUtil.invoke(buffer, null);
            
            // Create packet data serializer
            Constructor<?> serializerConstructor = ReflectionUtil.getConstructor(
                packetDataSerializerClass, byteBuf.getClass()
            );
            Object serializer = ReflectionUtil.newInstance(serializerConstructor, byteBuf);
            
            // Write packet to buffer
            Method writeMethod = ReflectionUtil.getMethod(
                handle.getClass(), "write", packetDataSerializerClass
            );
            ReflectionUtil.invoke(writeMethod, handle, serializer);
            
            // Read bytes from buffer
            Method readableBytes = ReflectionUtil.getMethod(byteBuf.getClass(), "readableBytes");
            int length = ReflectionUtil.invoke(readableBytes, byteBuf);
            
            byte[] data = new byte[length];
            Method readBytes = ReflectionUtil.getMethod(byteBuf.getClass(), "readBytes", byte[].class);
            ReflectionUtil.invoke(readBytes, byteBuf, data);
            
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize packet to bytes", e);
        }
    }
}