package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.scheduler.CoreScheduler;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scheduled packet injector.
 * 
 * Want to send packets with delays? Repeating packets?
 * Packet bundles that arrive together? This is your guy.
 * 
 * Uses the server scheduler because Netty's scheduling
 * is a pain in the ass and we're already in async hell.
 */
public class PacketScheduler {
    
    private final ProtocolManager protocol;
    
    // Queued packets waiting to be sent
    private final Queue<ScheduledPacket> queue = new ConcurrentLinkedQueue<>();
    
    // Packet bundles - sent together in one tick
    private final Map<String, PacketBundle> bundles = new HashMap<>();
    
    // Active tasks for cancellation
    private final Map<Integer, CoreScheduler.ScheduledTask> activeTasks = new HashMap<>();
    private final Map<Integer, RepeatTracker> repeatTrackers = new HashMap<>();
    
    public PacketScheduler(ProtocolManager protocol) {
        this.protocol = protocol;
        
        // Process queue every tick
        CoreScheduler.runTimer(this::processQueue, 1L, 1L);
    }
    
    /**
     * Send a packet after a delay.
     * 
     * @param delay Delay in ticks (20 = 1 second)
     */    public void sendLater(Player player, PacketContainer packet, long delay) {
        queue.offer(new ScheduledPacket(
            player,
            packet,
            System.currentTimeMillis() + (delay * 50),
            false,
            0
        ));
    }
    
    /**
     * Send a packet repeatedly.
     * 
     * @param delay Initial delay in ticks
     * @param period Repeat period in ticks
     * @param count Number of times to send (0 = infinite)
     * @return Task ID for cancellation
     */
    public int sendRepeating(Player player, PacketContainer packet, 
                            long delay, long period, int count) {
        int taskId = new Random().nextInt(Integer.MAX_VALUE);
        
        var task = CoreScheduler.runTimer(() -> {
            if (!player.isOnline()) {
                cancel(taskId); // Cancel if player leaves
                return;
            }
            
            protocol.sendPacket(player, packet);
            
            // Track count and cancel when done
            if (count > 0) {
                RepeatTracker tracker = repeatTrackers.computeIfAbsent(
                    taskId, k -> new RepeatTracker(count)
                );
                tracker.sent++;
                
                if (tracker.sent >= tracker.total) {
                    cancel(taskId);
                }
            }
        }, delay, period);
        
        activeTasks.put(taskId, task);
        return taskId;
    }    
    /**
     * Cancel a scheduled packet task.
     */
    public void cancel(int taskId) {
        var task = activeTasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
        repeatTrackers.remove(taskId);
    }
    
    /**
     * Cancel all scheduled packets for a player.
     */
    public void cancelAll(Player player) {
        // Remove from queue
        queue.removeIf(p -> p.player.equals(player));
        
        // Cancel active tasks for this player
        // Note: We can't easily track which tasks belong to which player
        // without more complex tracking. This is a limitation.
    }
    
    /**
     * Create a packet bundle.
     * Bundles are sent together in the same tick.
     */
    public PacketBundle createBundle(String name) {
        PacketBundle bundle = new PacketBundle(name);
        bundles.put(name, bundle);
        return bundle;
    }
    
    /**
     * Send a bundle to players.
     */
    public void sendBundle(String bundleName, Player... players) {
        PacketBundle bundle = bundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalArgumentException("Unknown bundle: " + bundleName);
        }
        
        // Send all packets in rapid succession
        for (Player player : players) {
            for (PacketContainer packet : bundle.packets) {
                protocol.sendPacket(player, packet);
            }
        }
    }
    
    /**
     * Send a bundle after a delay.
     */
    public void sendBundleLater(String bundleName, long delay, Player... players) {
        PacketBundle bundle = bundles.get(bundleName);
        if (bundle == null) {
            throw new IllegalArgumentException("Unknown bundle: " + bundleName);
        }
        
        CoreScheduler.runLater(() -> {
            sendBundle(bundleName, players);
        }, delay);
    }    
    /**
     * Process the packet queue.
     * Called every tick.
     */
    private void processQueue() {
        long now = System.currentTimeMillis();
        
        Iterator<ScheduledPacket> it = queue.iterator();
        while (it.hasNext()) {
            ScheduledPacket scheduled = it.next();
            
            if (scheduled.sendTime <= now) {
                // Time to send this packet
                if (scheduled.player.isOnline()) {
                    protocol.sendPacket(scheduled.player, scheduled.packet);
                }
                
                it.remove();
            }
        }
    }
    
    /**
     * Clear all scheduled packets and bundles.
     */
    public void clear() {
        queue.clear();
        bundles.clear();
        
        // Cancel all active tasks
        for (var task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        repeatTrackers.clear();
    }    
    /**
     * Scheduled packet entry.
     */
    private static class ScheduledPacket {
        final Player player;
        final PacketContainer packet;
        final long sendTime;
        final boolean repeat;
        final long period;
        
        ScheduledPacket(Player player, PacketContainer packet, long sendTime,
                       boolean repeat, long period) {
            this.player = player;
            this.packet = packet;
            this.sendTime = sendTime;
            this.repeat = repeat;
            this.period = period;
        }
    }
    
    /**
     * Tracks repeating packet counts.
     */
    private static class RepeatTracker {
        final int total;
        int sent = 0;
        
        RepeatTracker(int total) {
            this.total = total;
        }
    }    
    /**
     * Packet bundle - multiple packets sent together.
     * 
     * Useful for complex visual effects that need
     * multiple packets to work properly.
     */
    public static class PacketBundle {
        private final String name;
        private final List<PacketContainer> packets = new ArrayList<>();
        
        PacketBundle(String name) {
            this.name = name;
        }
        
        public PacketBundle add(PacketContainer packet) {
            packets.add(packet);
            return this;
        }
        
        public PacketBundle clear() {
            packets.clear();
            return this;
        }
        
        public String getName() {
            return name;
        }
        
        public int size() {
            return packets.size();
        }
    }
}