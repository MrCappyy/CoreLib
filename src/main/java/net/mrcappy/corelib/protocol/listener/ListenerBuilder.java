package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Fluent builder for packet listeners.
 * 
 * Because writing packet listeners shouldn't feel like
 * filing tax returns. Chain methods like a boss.
 * 
 * Example:
 * ListenerBuilder.create(plugin)
 *     .priority(ListenerPriority.HIGH)
 *     .packets(PacketType.PLAY_CLIENT_CHAT)
 *     .onReceive((player, packet) -> {
 *         String msg = packet.getMessage();
 *         if (msg.contains("diamond")) {
 *             return false; // Cancel, no diamonds for you
 *         }
 *         return true;
 *     })
 *     .expireAfter(5, TimeUnit.MINUTES)
 *     .register(protocol);
 * 
 * Way better than implementing 47 abstract methods
 * just to block someone from saying "diamond" in chat.
 */public class ListenerBuilder {
    
    private final Plugin plugin;
    private ListenerPriority priority = ListenerPriority.NORMAL;
    private final Set<PacketType> sendingTypes = new HashSet<>();
    private final Set<PacketType> receivingTypes = new HashSet<>();
    private BiFunction<Player, PacketContainer, Boolean> sendHandler;
    private BiFunction<Player, PacketContainer, Boolean> receiveHandler;
    private Predicate<Player> playerFilter;
    private Long expirationTime;
    private Integer maxPackets;
    private Runnable onExpire;
    
    private ListenerBuilder(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a new listener builder.
     */
    public static ListenerBuilder create(Plugin plugin) {
        return new ListenerBuilder(plugin);
    }
    
    /**
     * Set the priority.
     * MONITOR = you're last and can't change shit
     * HIGHEST = you're basically god
     * LOWEST = everyone else goes after you
     */
    public ListenerBuilder priority(ListenerPriority priority) {
        this.priority = priority;
        return this;
    }
    
    /**
     * Add packet types to listen for (both sending and receiving).
     */
    public ListenerBuilder packets(PacketType... types) {
        for (PacketType type : types) {
            if (type.getDirection() == PacketType.Direction.SERVERBOUND) {
                receivingTypes.add(type);
            } else {
                sendingTypes.add(type);
            }
        }
        return this;
    }    
    /**
     * Add sending packet types.
     */
    public ListenerBuilder sending(PacketType... types) {
        for (PacketType type : types) {
            sendingTypes.add(type);
        }
        return this;
    }
    
    /**
     * Add receiving packet types.
     */
    public ListenerBuilder receiving(PacketType... types) {
        for (PacketType type : types) {
            receivingTypes.add(type);
        }
        return this;
    }
    
    /**
     * Set handler for outgoing packets.
     */
    public ListenerBuilder onSend(BiFunction<Player, PacketContainer, Boolean> handler) {
        this.sendHandler = handler;
        return this;
    }
    
    /**
     * Set handler for incoming packets.
     */
    public ListenerBuilder onReceive(BiFunction<Player, PacketContainer, Boolean> handler) {
        this.receiveHandler = handler;
        return this;
    }
    
    /**
     * Filter by player.
     */
    public ListenerBuilder forPlayer(Predicate<Player> filter) {
        this.playerFilter = filter;
        return this;
    }
    
    /**
     * Make this listener expire after a duration.
     */
    public ListenerBuilder expireAfter(long duration, TimeUnit unit) {
        this.expirationTime = System.currentTimeMillis() + unit.toMillis(duration);
        return this;
    }    
    /**
     * Make this listener expire after a packet count.
     */
    public ListenerBuilder expireAfterPackets(int count) {
        this.maxPackets = count;
        return this;
    }
    
    /**
     * Set expiration callback.
     */
    public ListenerBuilder onExpire(Runnable callback) {
        this.onExpire = callback;
        return this;
    }
    
    /**
     * Build and register the listener.
     */
    public PacketListener register(ProtocolManager protocol) {
        PacketListener listener;
        
        if (expirationTime != null || maxPackets != null) {
            // Create expiring listener
            listener = new ExpiringPacketListener(plugin, 
                expirationTime != null ? expirationTime - System.currentTimeMillis() : Long.MAX_VALUE,
                TimeUnit.MILLISECONDS,
                sendingTypes, receivingTypes) {
                
                @Override
                protected boolean handlePacket(Player player, PacketContainer packet, boolean sending) {
                    // Apply player filter
                    if (playerFilter != null && !playerFilter.test(player)) {
                        return true;
                    }
                    
                    // Call appropriate handler
                    if (sending && sendHandler != null) {
                        return sendHandler.apply(player, packet);
                    } else if (!sending && receiveHandler != null) {
                        return receiveHandler.apply(player, packet);
                    }
                    
                    return true;
                }
            };
        } else {
            // Create normal listener
            listener = new PacketAdapter(plugin, priority, sendingTypes, receivingTypes) {
                @Override
                public boolean onPacketSending(Player player, PacketContainer packet) {
                    if (playerFilter != null && !playerFilter.test(player)) {
                        return true;
                    }
                    return sendHandler != null ? sendHandler.apply(player, packet) : true;
                }                
                @Override
                public boolean onPacketReceiving(Player player, PacketContainer packet) {
                    if (playerFilter != null && !playerFilter.test(player)) {
                        return true;
                    }
                    return receiveHandler != null ? receiveHandler.apply(player, packet) : true;
                }
            };
        }
        
        protocol.registerListener(listener);
        return listener;
    }
}