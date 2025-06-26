package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Processes @PacketHandler annotations.
 * 
 * Scans objects for methods with @PacketHandler and automatically
 * registers them as packet listeners. Because decorators are sexy
 * and Java needs more magic.
 * 
 * Pass any object with annotated methods and watch the magic happen.
 * Or watch it crash and burn. 50/50 really.
 */
public class AnnotationProcessor {
    
    private final ProtocolManager protocol;
    private final Map<Object, List<PacketListener>> registeredListeners = new HashMap<>();
    
    public AnnotationProcessor(ProtocolManager protocol) {
        this.protocol = protocol;
    }
    
    /**
     * Register all @PacketHandler methods in an object.
     * 
     * This uses reflection to find methods, so performance
     * isn't amazing. Don't call this every tick unless you
     * enjoy watching your TPS counter go brrrr (downwards).
     */
    public void registerHandlers(Plugin plugin, Object handler) {
        List<PacketListener> listeners = new ArrayList<>();
        
        // Scan all methods
        for (Method method : handler.getClass().getDeclaredMethods()) {
            PacketHandler annotation = method.getAnnotation(PacketHandler.class);
            if (annotation == null) continue;
            
            // Validate method signature
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2 || params[0] != Player.class || 
                params[1] != PacketContainer.class) {
                plugin.getLogger().warning(
                    "Invalid @PacketHandler method signature: " + method.getName() +
                    " - Expected (Player, PacketContainer)"
                );
                continue;
            }
            
            // Parse packet types
            Set<PacketType> types = new HashSet<>();
            for (String typeName : annotation.types()) {
                try {
                    types.add(PacketType.valueOf(typeName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown packet type: " + typeName);
                }
            }
            
            // Create listener
            PacketListener listener = new MethodPacketListener(
                plugin, annotation, types, handler, method
            );
            
            protocol.registerListener(listener);
            listeners.add(listener);
        }
        
        if (!listeners.isEmpty()) {
            registeredListeners.put(handler, listeners);
        }
    }
    
    /**
     * Unregister all handlers for an object.
     * Call this or leak memory like a sieve.
     */
    public void unregisterHandlers(Object handler) {
        List<PacketListener> listeners = registeredListeners.remove(handler);
        if (listeners != null) {
            listeners.forEach(protocol::unregisterListener);
        }
    }
    
    /**
     * Internal listener that delegates to annotated methods.
     */
    private static class MethodPacketListener extends PacketListener {
        private final Object handler;
        private final Method method;
        private final boolean sending;
        private final boolean receiving;
        private final String filter;
        
        MethodPacketListener(Plugin plugin, PacketHandler annotation,
                           Set<PacketType> types, Object handler, Method method) {
            super(plugin, annotation.priority(), 
                  annotation.sending() ? types : Collections.emptySet(),
                  annotation.receiving() ? types : Collections.emptySet());
            
            this.handler = handler;
            this.method = method;
            this.sending = annotation.sending();
            this.receiving = annotation.receiving();
            this.filter = annotation.filter();
            
            method.setAccessible(true); // Fuck your access modifiers
        }
        
        @Override
        public boolean onPacketSending(Player player, PacketContainer packet) {
            if (!sending) return true;
            return invokeMethod(player, packet);
        }
        
        @Override
        public boolean onPacketReceiving(Player player, PacketContainer packet) {
            if (!receiving) return true;
            return invokeMethod(player, packet);
        }
        
        private boolean invokeMethod(Player player, PacketContainer packet) {
            try {
                // Apply JavaScript filter if specified
                if (filter != null && !filter.isEmpty()) {
                    // Execute filter inline
                    var scriptEngine = net.mrcappy.corelib.CoreLibPlugin.getInstance()
                        .getProtocolManager().getScriptEngine();
                    
                    // Create a temporary filter
                    String tempName = "temp_" + System.nanoTime();
                    scriptEngine.compileFilter(tempName, filter);
                    boolean filterResult = scriptEngine.executeFilter(tempName, player, packet);
                    scriptEngine.removeFilter(tempName);
                    
                    if (!filterResult) {
                        return false; // Filter rejected the packet
                    }
                }
                
                Object result = method.invoke(handler, player, packet);
                
                // If method returns boolean, use it
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                
                return true; // Default to allow
                
            } catch (Exception e) {
                getPlugin().getLogger().severe(
                    "Error in @PacketHandler method " + method.getName() + ": " + 
                    e.getMessage()
                );
                return true;
            }
        }
    }
}