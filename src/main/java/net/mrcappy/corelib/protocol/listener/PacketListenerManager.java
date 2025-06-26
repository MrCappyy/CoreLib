package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all packet listeners.
 * 
 * This clusterfuck keeps track of who wants what packets
 * and makes sure they get called in the right order.
 * 
 * It's thread-safe because packets come from Netty threads
 * and we don't want race conditions fucking up our day.
 * CopyOnWriteArrayList is slow as balls for writes but
 * we read way more than we write so it's fine.
 */
public class PacketListenerManager {
    
    private final ProtocolManager manager;
    
    // Listeners sorted by priority
    private final Map<ListenerPriority, List<PacketListener>> listenersByPriority = 
        new ConcurrentHashMap<>();
    
    // Quick lookup for packet types
    private final Map<PacketType, List<PacketListener>> sendingListeners = 
        new ConcurrentHashMap<>();
    private final Map<PacketType, List<PacketListener>> receivingListeners = 
        new ConcurrentHashMap<>();
    
    public PacketListenerManager(ProtocolManager manager) {
        this.manager = manager;
        
        // Initialize priority lists
        for (ListenerPriority priority : ListenerPriority.values()) {
            listenersByPriority.put(priority, new CopyOnWriteArrayList<>());
        }
    }
    
    /**
     * Register a packet listener.
     * 
     * This rebuilds the entire lookup cache because I'm too
     * lazy to implement incremental updates. Sue me.
     */
    public void register(PacketListener listener) {
        listenersByPriority.get(listener.getPriority()).add(listener);
        rebuildCache();
    }
    
    /**
     * Unregister a packet listener.
     * 
     * If you forget to unregister your listeners, they'll
     * keep processing packets even after your plugin is
     * disabled. Don't be that plugin.
     */
    public void unregister(PacketListener listener) {
        listenersByPriority.get(listener.getPriority()).remove(listener);
        rebuildCache();
    }
    
    /**
     * Clear all listeners.
     * Nuclear option for shutdown.
     */
    public void clear() {
        listenersByPriority.values().forEach(List::clear);
        sendingListeners.clear();
        receivingListeners.clear();
    }
    
    /**
     * Get listeners by priority.
     * For debugging and listing.
     */
    public List<PacketListener> getListenersByPriority(ListenerPriority priority) {
        return new ArrayList<>(listenersByPriority.get(priority));
    }
    
    /**
     * Get total listener count.
     */
    public int getTotalListeners() {
        return listenersByPriority.values().stream()
            .mapToInt(List::size)
            .sum();
    }    
    /**
     * Rebuild the packet type lookup cache.
     * 
     * This is O(n*m) where n is listeners and m is packet types,
     * but it only runs when listeners are registered/unregistered
     * so who gives a fuck.
     */
    private void rebuildCache() {
        sendingListeners.clear();
        receivingListeners.clear();
        
        // Go through all listeners by priority order
        for (ListenerPriority priority : ListenerPriority.values()) {
            for (PacketListener listener : listenersByPriority.get(priority)) {
                // Add to sending cache
                for (PacketType type : PacketType.values()) {
                    if (listener.isListeningForSending(type)) {
                        sendingListeners.computeIfAbsent(type, k -> new ArrayList<>())
                            .add(listener);
                    }
                    if (listener.isListeningForReceiving(type)) {
                        receivingListeners.computeIfAbsent(type, k -> new ArrayList<>())
                            .add(listener);
                    }
                }
            }
        }
    }
    
    /**
     * Handle an incoming packet from a player.
     * 
     * Goes through all listeners in priority order until
     * one of them cancels it or we run out of listeners.
     * 
     * @return true to allow packet, false to cancel
     */
    public boolean handleIncoming(Player player, PacketContainer packet) {
        List<PacketListener> listeners = receivingListeners.get(packet.getType());
        if (listeners == null || listeners.isEmpty()) {
            return true; // No one cares about this packet
        }
        
        // Call listeners in order
        for (PacketListener listener : listeners) {
            try {
                if (!listener.onPacketReceiving(player, packet)) {
                    return false; // Cancelled
                }
            } catch (Exception e) {
                // Listener fucked up, log and continue
                manager.getPlugin().getLogger().severe(
                    "Error in packet listener " + listener.getClass().getName() + 
                    ": " + e.getMessage()
                );
                e.printStackTrace();
            }
        }
        
        return true;
    }
    
    /**
     * Handle an outgoing packet to a player.
     * 
     * Same shit as incoming but the other direction.
     * 
     * @return true to allow packet, false to cancel
     */
    public boolean handleOutgoing(Player player, PacketContainer packet) {
        List<PacketListener> listeners = sendingListeners.get(packet.getType());
        if (listeners == null || listeners.isEmpty()) {
            return true;
        }
        
        // Call listeners in order
        for (PacketListener listener : listeners) {
            try {
                if (!listener.onPacketSending(player, packet)) {
                    return false; // Cancelled
                }
            } catch (Exception e) {
                // Another one bites the dust
                manager.getPlugin().getLogger().severe(
                    "Error in packet listener " + listener.getClass().getName() + 
                    ": " + e.getMessage()
                );
                e.printStackTrace();
            }
        }
        
        return true;
    }
}