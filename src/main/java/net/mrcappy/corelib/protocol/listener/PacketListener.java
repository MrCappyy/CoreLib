package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Base class for packet listeners.
 * 
 * Extend this bad boy to listen for packets. It's like
 * Bukkit's Listener but for packets instead of events.
 * 
 * Pro tip: If your onPacketReceiving/Sending methods are
 * being called 1000 times per second, you fucked up your
 * packet filter. Add proper filtering or enjoy your CPU
 * catching fire.
 */
public abstract class PacketListener implements HotReloadable {
    
    private final Plugin plugin;
    private final ListenerPriority priority;
    private final Set<PacketType> sendingTypes;
    private final Set<PacketType> receivingTypes;
    private boolean enabled = true;
    
    public PacketListener(Plugin plugin, ListenerPriority priority, 
                         Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        this.plugin = plugin;
        this.priority = priority;
        this.sendingTypes = sendingTypes;
        this.receivingTypes = receivingTypes;
    }
    
    /**
     * Called when a packet is being sent to a player.
     * Return false to cancel the packet.
     * 
     * This runs on Netty's thread pool, so:
     * 1. Don't do blocking operations you muppet
     * 2. Don't touch Bukkit API without scheduling to main thread
     * 3. Keep it fast or you'll lag the entire server
     * 
     * @param player The poor bastard receiving the packet
     * @param packet The packet about to violate their client
     * @return true to allow, false to cancel
     */
    public boolean onPacketSending(Player player, PacketContainer packet) {
        if (!shouldProcess()) return true;
        return true;
    }
    
    /**
     * Called when a packet is received from a player.
     * Return false to cancel the packet.
     * 
     * Same threading rules apply. If you block here,
     * you're blocking the entire network thread and
     * everyone will hate you.
     * 
     * @param player The player who sent this garbage
     * @param packet The packet they're trying to sneak past us
     * @return true to allow, false to cancel
     */
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        if (!shouldProcess()) return true;
        return true;
    }
    
    /**
     * Check if this listener wants a specific outgoing packet type.
     * Used for filtering so we don't call your listener for
     * every fucking packet in existence.
     */
    public boolean isListeningForSending(PacketType type) {
        return sendingTypes.isEmpty() || sendingTypes.contains(type);
    }
    
    /**
     * Check if this listener wants a specific incoming packet type.
     */
    public boolean isListeningForReceiving(PacketType type) {
        return receivingTypes.isEmpty() || receivingTypes.contains(type);
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
    
    public ListenerPriority getPriority() {
        return priority;
    }
    
    // HotReloadable implementation
    
    @Override
    public void enable() {
        this.enabled = true;
    }
    
    @Override
    public void disable() {
        this.enabled = false;
    }
    
    @Override
    public void reload() {
        // Override in subclasses if needed
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Internal check for enabled state.
     * Called before processing packets.
     */
    protected final boolean shouldProcess() {
        return enabled;
    }
}