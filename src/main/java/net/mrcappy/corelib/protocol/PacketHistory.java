package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tracks packet history for debugging.
 * 
 * Ever had a player complain about lag and you have no
 * fucking clue what's happening? This class is your 
 * forensic evidence. It's like a black box recorder for
 * packets, except instead of plane crashes it's for when
 * xXx_MinecraftGod_xXx claims the server is eating his inputs.
 * 
 * "The server didn't register my click!"
 * *checks packet history*
 * "You sent 47 block place packets in 1 tick you absolute donkey"
 * 
 * It's a circular buffer because we're not storing the entire
 * internet's worth of packets. Your RAM will thank me.
 */
public class PacketHistory {
    
    private final int maxSize;
    private final Deque<PacketEntry> incoming;
    private final Deque<PacketEntry> outgoing;
    
    public PacketHistory(int maxSize) {
        this.maxSize = maxSize;
        this.incoming = new ConcurrentLinkedDeque<>();
        this.outgoing = new ConcurrentLinkedDeque<>();
    }
    
    /**
     * Add an incoming packet to history.
     * Oldest packet gets yeeted when we hit max size.
     */
    public void addIncoming(PacketContainer packet) {
        incoming.addLast(new PacketEntry(packet, System.currentTimeMillis()));
        
        // Trim to size like a responsible adult
        while (incoming.size() > maxSize) {
            incoming.removeFirst();
        }
    }
    
    /**
     * Add an outgoing packet to history.
     */
    public void addOutgoing(PacketContainer packet) {
        outgoing.addLast(new PacketEntry(packet, System.currentTimeMillis()));
        
        while (outgoing.size() > maxSize) {
            outgoing.removeFirst();
        }
    }
    
    /**
     * Get recent incoming packets.
     * Most recent last because that's how time works.
     */
    public List<PacketEntry> getIncoming(int limit) {
        List<PacketEntry> result = new ArrayList<>();
        Iterator<PacketEntry> it = incoming.descendingIterator();
        
        int count = 0;
        while (it.hasNext() && count++ < limit) {
            result.add(0, it.next()); // Add to front to maintain order
        }
        
        return result;
    }
    
    /**
     * Get recent outgoing packets.
     */
    public List<PacketEntry> getOutgoing(int limit) {
        List<PacketEntry> result = new ArrayList<>();
        Iterator<PacketEntry> it = outgoing.descendingIterator();
        
        int count = 0;
        while (it.hasNext() && count++ < limit) {
            result.add(0, it.next());
        }
        
        return result;
    }
    
    /**
     * Clear all history.
     * For when you want to forget the past.
     */
    public void clear() {
        incoming.clear();
        outgoing.clear();
    }
    
    /**
     * Single packet entry with timestamp.
     * Immutable because thread safety is important or whatever.
     */
    public static class PacketEntry {
        private final PacketType type;
        private final long timestamp;
        private final String summary;
        
        PacketEntry(PacketContainer packet, long timestamp) {
            this.type = packet.getType();
            this.timestamp = timestamp;
            this.summary = generateSummary(packet);
        }
        
        /**
         * Generate a summary of the packet for debugging.
         * Shows the first few fields so you know what's in it.
         */
        private String generateSummary(PacketContainer packet) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(type.name()).append(" [");
                
                // Add some field values
                if (packet.getIntegers().size() > 0) {
                    sb.append("ints:").append(packet.getIntegers().getValues()).append(" ");
                }
                if (packet.getStrings().size() > 0) {
                    sb.append("strings:").append(packet.getStrings().getValues()).append(" ");
                }
                
                sb.append("]");
                return sb.toString();
            } catch (Exception e) {
                return type.name() + " [error reading]";
            }
        }
        
        public PacketType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getSummary() {
            return summary;
        }
    }
}