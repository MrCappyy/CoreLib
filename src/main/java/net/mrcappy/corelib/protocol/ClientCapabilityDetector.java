package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client protocol capability detector.
 * 
 * Figures out what version, mods, and capabilities each
 * client has by snooping on their packets like the NSA.
 * 
 * "Is this player on 1.20.4 with Fabric and 47 mods?"
 * "Does this client support compression?"
 * "Is this person using a hacked client?"
 * 
 * All questions this tries to answer by being nosy AF
 * about their handshake and login packets.
 */
public class ClientCapabilityDetector {
    
    // Player capabilities
    private final Map<UUID, ClientInfo> clientInfo = new ConcurrentHashMap<>();
    
    /**
     * Process handshake packet to detect client info.
     * This is where clients spill their secrets.
     */
    public void processHandshake(UUID playerUuid, PacketContainer packet) {
        if (packet.getType() != PacketType.HANDSHAKE_CLIENT_SET_PROTOCOL) {
            return;
        }
        
        ClientInfo info = clientInfo.computeIfAbsent(playerUuid, k -> new ClientInfo());
        
        // Protocol version is the first integer
        info.protocolVersion = packet.getIntegers().read(0);
        
        // Server address might contain Forge/Fabric markers
        String serverAddress = packet.getStrings().read(0);
        if (serverAddress.contains("\0FML\0")) {
            info.modded = true;
            info.modLoader = "Forge";
        } else if (serverAddress.contains("fabric")) {
            info.modded = true;
            info.modLoader = "Fabric";
        }
        
        // Next state (1=status, 2=login)
        info.nextState = packet.getIntegers().read(1);
    }
    
    /**
     * Process login packets for more info.
     */
    public void processLogin(UUID playerUuid, PacketContainer packet) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info == null) return;
        
        if (packet.getType() == PacketType.LOGIN_CLIENT_CUSTOM_PAYLOAD) {
            // Custom payload might contain mod list
            String channel = packet.getStrings().read(0);
            info.customChannels.add(channel);
            
            // Check for known hacked client signatures
            if (channel.contains("wdl") || channel.contains("5zig") || 
                channel.contains("liteloader")) {
                info.suspiciousClient = true;
            }
        }
    }
    
    /**
     * Process plugin channel registrations.
     */
    public void processChannelRegistration(UUID playerUuid, PacketContainer packet) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info == null) return;
        
        if (packet.getType() == PacketType.PLAY_CLIENT_CUSTOM_PAYLOAD) {
            String channel = packet.getStrings().read(0);
            if (channel.equals("minecraft:register") || channel.equals("REGISTER")) {
                // Parse registered channels
                byte[] data = packet.getByteArrays().read(0);
                String channels = new String(data);
                for (String ch : channels.split("\0")) {
                    info.registeredChannels.add(ch);
                }
            }
        }
    }
    
    /**
     * Check if compression was enabled for a client.
     */
    public void compressionEnabled(UUID playerUuid, int threshold) {
        ClientInfo info = clientInfo.get(playerUuid);
        if (info != null) {
            info.compressionEnabled = true;
            info.compressionThreshold = threshold;
        }
    }
    
    /**
     * Get client info.
     */
    public ClientInfo getClientInfo(UUID playerUuid) {
        return clientInfo.get(playerUuid);
    }
    
    /**
     * Clean up when player disconnects.
     */
    public void removePlayer(UUID playerUuid) {
        clientInfo.remove(playerUuid);
    }
    
    /**
     * Client information holder.
     * Everything we've learned by spying on them.
     */
    public static class ClientInfo {
        public int protocolVersion = -1;
        public int nextState = -1;
        public boolean modded = false;
        public String modLoader = "Vanilla";
        public boolean compressionEnabled = false;
        public int compressionThreshold = -1;
        public boolean suspiciousClient = false;
        public final Set<String> customChannels = new HashSet<>();
        public final Set<String> registeredChannels = new HashSet<>();
        
        /**
         * Get Minecraft version from protocol version.
         * This mapping is cancer to maintain.
         */
        public String getMinecraftVersion() {
            // Just a few examples, there are hundreds
            switch (protocolVersion) {
                case 765: return "1.20.4";
                case 764: return "1.20.3";
                case 763: return "1.20.1";
                case 762: return "1.20";
                case 761: return "1.19.4";
                case 760: return "1.19.3";
                // ... about 100 more versions
                default: return "Unknown (" + protocolVersion + ")";
            }
        }
    }
}