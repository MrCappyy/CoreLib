package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Packet listener that expires after a certain time or packet count.
 * 
 * Perfect for temporary debugging or one-time packet intercepts.
 * Set it and forget it - it'll clean itself up like a good boy.
 * 
 * Example: Listen for the next 10 chat packets only:
 * new ExpiringPacketListener(plugin, 10, PacketType.PLAY_CLIENT_CHAT)
 */
public class ExpiringPacketListener extends PacketListener {
    
    private final long expiryTime;
    private final int maxPackets;
    private int packetCount = 0;
    private final Runnable onExpire;
    
    /**
     * Create a time-based expiring listener.
     */
    public ExpiringPacketListener(Plugin plugin, long duration, TimeUnit unit,
                                 Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, ListenerPriority.NORMAL, sendingTypes, receivingTypes);
        this.expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        this.maxPackets = Integer.MAX_VALUE;
        this.onExpire = null;
    }
    
    /**
     * Create a count-based expiring listener.
     */
    public ExpiringPacketListener(Plugin plugin, int maxPackets,
                                 Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, ListenerPriority.NORMAL, sendingTypes, receivingTypes);
        this.expiryTime = Long.MAX_VALUE;
        this.maxPackets = maxPackets;
        this.onExpire = null;
    }
    
    /**
     * Create a listener with custom expiry callback.
     * Because sometimes you need to know when it dies.
     */
    public ExpiringPacketListener(Plugin plugin, long duration, TimeUnit unit,
                                 Set<PacketType> types, Runnable onExpire) {
        super(plugin, ListenerPriority.NORMAL, types, types);
        this.expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        this.maxPackets = Integer.MAX_VALUE;
        this.onExpire = onExpire;
    }
    
    @Override
    public boolean onPacketSending(Player player, PacketContainer packet) {
        if (checkExpired()) return true;
        
        packetCount++;
        return handlePacket(player, packet, true);
    }
    
    @Override
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        if (checkExpired()) return true;
        
        packetCount++;
        return handlePacket(player, packet, false);
    }
    
    /**
     * Override this to handle packets.
     * Default implementation does nothing.
     */
    protected boolean handlePacket(Player player, PacketContainer packet, boolean sending) {
        return true;
    }
    
    /**
     * Check if this listener has expired.
     * Unregisters itself if expired.
     */
    private boolean checkExpired() {
        boolean expired = System.currentTimeMillis() > expiryTime || 
                         packetCount >= maxPackets;
        
        if (expired) {
            // Self-destruct sequence initiated
            getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> {
                // Unregister on main thread to be safe
                net.mrcappy.corelib.CoreLibPlugin.getInstance()
                    .getProtocolManager().unregisterListener(this);
                
                if (onExpire != null) {
                    onExpire.run();
                }
            });
        }
        
        return expired;
    }
}