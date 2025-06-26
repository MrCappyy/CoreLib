package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Convenience adapter for packet listeners.
 * 
 * Like Bukkit's Listener but you only override what you need.
 * Most people only care about a few packet types so this
 * saves them from implementing empty methods like cavemen.
 * 
 * Example:
 * new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.PLAY_CLIENT_CHAT) {
 *     @Override
 *     public boolean onPacketReceiving(Player player, PacketContainer packet) {
 *         // Player said something stupid, cancel it
 *         return false;
 *     }
 * }
 */
public abstract class PacketAdapter extends PacketListener {
    
    /**
     * Create an adapter for specific packet types.
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority, PacketType... types) {
        this(plugin, priority, new HashSet<>(Arrays.asList(types)));
    }
    
    /**
     * Create an adapter with separate sending/receiving types.
     * For when you're fancy and want different types each direction.
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority, 
                        Set<PacketType> types) {
        super(plugin, priority, types, types);
    }
    
    /**
     * Create an adapter with fully custom type sets.
     * Maximum control for maximum confusion.
     */
    public PacketAdapter(Plugin plugin, ListenerPriority priority,
                        Set<PacketType> sendingTypes, Set<PacketType> receivingTypes) {
        super(plugin, priority, sendingTypes, receivingTypes);
    }
    
    /**
     * Builder for packet adapters because builders are cool.
     * 
     * PacketAdapter adapter = PacketAdapter.builder(plugin)
     *     .priority(ListenerPriority.HIGH)
     *     .sending(PacketType.PLAY_SERVER_CHAT)
     *     .receiving(PacketType.PLAY_CLIENT_CHAT)
     *     .onSending((player, packet) -> {
     *         // Do shit
     *         return true;
     *     })
     *     .build();
     */
    public static Builder builder(Plugin plugin) {
        return new Builder(plugin);
    }
    
    public static class Builder {
        private final Plugin plugin;
        private ListenerPriority priority = ListenerPriority.NORMAL;
        private final Set<PacketType> sendingTypes = new HashSet<>();
        private final Set<PacketType> receivingTypes = new HashSet<>();
        private PacketHandler sendingHandler = null;
        private PacketHandler receivingHandler = null;
        
        Builder(Plugin plugin) {
            this.plugin = plugin;
        }
        
        public Builder priority(ListenerPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder sending(PacketType... types) {
            sendingTypes.addAll(Arrays.asList(types));
            return this;
        }
        
        public Builder receiving(PacketType... types) {
            receivingTypes.addAll(Arrays.asList(types));
            return this;
        }
        
        public Builder onSending(PacketHandler handler) {
            this.sendingHandler = handler;
            return this;
        }
        
        public Builder onReceiving(PacketHandler handler) {
            this.receivingHandler = handler;
            return this;
        }
        
        public PacketAdapter build() {
            return new PacketAdapter(plugin, priority, sendingTypes, receivingTypes) {
                @Override
                public boolean onPacketSending(org.bukkit.entity.Player player, 
                                              net.mrcappy.corelib.protocol.packet.PacketContainer packet) {
                    return sendingHandler != null ? 
                        sendingHandler.handle(player, packet) : true;
                }
                
                @Override
                public boolean onPacketReceiving(org.bukkit.entity.Player player, 
                                                net.mrcappy.corelib.protocol.packet.PacketContainer packet) {
                    return receivingHandler != null ? 
                        receivingHandler.handle(player, packet) : true;
                }
            };
        }
    }
    
    /**
     * Functional interface for packet handlers.
     * Because lambdas are sexy.
     */
    @FunctionalInterface
    public interface PacketHandler {
        boolean handle(org.bukkit.entity.Player player, 
                      net.mrcappy.corelib.protocol.packet.PacketContainer packet);
    }
}