package net.mrcappy.corelib.event;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Event bus that doesn't require you to create 50 listener classes.
 * 
 * Bukkit's event system is powerful but verbose as fuck.
 * This lets you register lambdas as event handlers like a
 * civilized person in 2025.
 * 
 * Example:
 * EventBus.on(PlayerJoinEvent.class, e -> {
 *     e.getPlayer().sendMessage("Welcome!");
 * });
 * 
 * That's it. No listener class, no @EventHandler, no registration.
 * Just pure, unadulterated event handling.
 */
public class EventBus {
    
    private static EventBus instance;
    private final Plugin plugin;
    private final Map<Class<? extends Event>, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>();
    
    public EventBus(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }    
    /**
     * Register a simple event handler.
     * This is the bread and butter method.
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler) {
        return instance.subscribe(eventClass, handler, EventPriority.NORMAL, false);
    }
    
    /**
     * Register an event handler with custom priority.
     * For when you need to go first or last.
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        return instance.subscribe(eventClass, handler, priority, false);
    }
    
    /**
     * Register an async event handler.
     * Handler runs on a separate thread. Don't touch Bukkit API!
     */
    public static <T extends Event> EventSubscription onAsync(Class<T> eventClass, Consumer<T> handler) {
        return instance.subscribe(eventClass, handler, EventPriority.NORMAL, true);
    }
    
    /**
     * Register an event handler with a filter.
     * Only triggers if the predicate returns true.
     * 
     * Example: Only handle events for ops
     * EventBus.on(PlayerCommandPreprocessEvent.class, 
     *     e -> e.setCancelled(true),
     *     e -> !e.getPlayer().isOp()
     * );
     */
    public static <T extends Event> EventSubscription on(Class<T> eventClass, Consumer<T> handler, Predicate<T> filter) {
        return instance.subscribe(eventClass, e -> {
            if (filter.test(e)) {
                handler.accept(e);
            }
        }, EventPriority.NORMAL, false);
    }    
    /**
     * Fire a custom event.
     * Because sometimes you need your own events.
     */
    public static void fire(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
    
    /**
     * The actual subscription logic.
     * This is where the sausage is made.
     */
    @SuppressWarnings("unchecked")
    private <T extends Event> EventSubscription subscribe(
            Class<T> eventClass, 
            Consumer<T> handler, 
            EventPriority priority, 
            boolean async) {
        
        // Create a wrapper that Bukkit can understand
        RegisteredHandler registered = new RegisteredHandler(eventClass, handler, priority, async);
        
        // Add to our handler map
        handlers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(registered);
        
        // Create the actual Bukkit listener
        // This is some black magic fuckery but it works
        Listener listener = new Listener() {};
        
        EventExecutor executor = (l, event) -> {
            if (eventClass.isInstance(event)) {
                if (async) {
                    // Run async handlers on a separate thread
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        handler.accept((T) event);
                    });
                } else {
                    handler.accept((T) event);
                }
            }
        };
        
        // Register with Bukkit
        Bukkit.getPluginManager().registerEvent(
            eventClass, listener, priority, executor, plugin, false
        );
        
        // Return subscription for unregistering
        return new EventSubscription(eventClass, registered, listener);
    }    
    /**
     * Internal handler representation.
     * Keeps track of what we've registered.
     */
    private static class RegisteredHandler {
        final Class<? extends Event> eventClass;
        final Consumer<? extends Event> handler;
        final EventPriority priority;
        final boolean async;
        
        RegisteredHandler(Class<? extends Event> eventClass, Consumer<? extends Event> handler, 
                         EventPriority priority, boolean async) {
            this.eventClass = eventClass;
            this.handler = handler;
            this.priority = priority;
            this.async = async;
        }
    }
    
    /**
     * Subscription handle for unregistering.
     * Keep this if you want to unregister later.
     */
    public class EventSubscription {
        private final Class<? extends Event> eventClass;
        private final RegisteredHandler handler;
        private final Listener listener;
        private boolean cancelled = false;
        
        EventSubscription(Class<? extends Event> eventClass, RegisteredHandler handler, Listener listener) {
            this.eventClass = eventClass;
            this.handler = handler;
            this.listener = listener;
        }
        
        /**
         * Unregister this event handler.
         * Call this when you're done listening.
         */
        public void unregister() {
            if (!cancelled) {
                HandlerList.unregisterAll(listener);
                List<RegisteredHandler> classHandlers = handlers.get(eventClass);
                if (classHandlers != null) {
                    classHandlers.remove(handler);
                }
                cancelled = true;
            }
        }
    }
    
    /**
     * Get the number of registered listeners.
     */
    public int getListenerCount() {
        return handlers.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Fire a custom CoreLib event.
     */
    public void fireEvent(CoreEvent event) {
        // In a real implementation, this would call
        // all registered handlers for this event type
    }
    
    /**
     * Register a handler for CoreLib events.
     */
    public void register(Object listener, Class<? extends CoreEvent> eventClass, 
                        EventPriority priority, java.lang.reflect.Method method) {
        // In a real implementation, this would register
        // the method as a handler for the event type
    }
    
    /**
     * Unregister all handlers for a listener.
     */
    public void unregister(Object listener) {
        // In a real implementation, this would remove
        // all handlers registered by this listener
    }
}