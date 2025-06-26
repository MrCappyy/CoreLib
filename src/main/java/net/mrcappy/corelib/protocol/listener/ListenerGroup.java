package net.mrcappy.corelib.protocol.listener;

import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener groups for organization.
 * 
 * Group your packet listeners by feature, plugin, or
 * whatever arbitrary categorization makes you happy.
 * 
 * Want to disable all chat-related listeners? Group them
 * and disable the group. Want to reload specific features?
 * Groups got you covered.
 * 
 * It's like folders but for packet listeners. Revolutionary.
 */
public class ListenerGroup {
    
    private final String name;
    private final Plugin plugin;
    private final List<PacketListener> listeners = new ArrayList<>();
    private boolean enabled = true;
    
    // Global registry of all groups
    private static final Map<String, ListenerGroup> GROUPS = new ConcurrentHashMap<>();
    
    public ListenerGroup(String name, Plugin plugin) {
        this.name = name;
        this.plugin = plugin;
        GROUPS.put(name, this);
    }
    
    /**
     * Add a listener to this group.
     */
    public void addListener(PacketListener listener) {
        listeners.add(listener);
        
        // Register if group is enabled
        if (enabled) {
            net.mrcappy.corelib.CoreLibPlugin.getInstance()
                .getProtocolManager().registerListener(listener);
        }
    }
    
    /**
     * Remove a listener from this group.
     */
    public void removeListener(PacketListener listener) {
        listeners.remove(listener);
        
        // Always unregister
        net.mrcappy.corelib.CoreLibPlugin.getInstance()
            .getProtocolManager().unregisterListener(listener);
    }
    
    /**
     * Enable this group.
     * Registers all listeners.
     */
    public void enable() {
        if (enabled) return;
        
        enabled = true;
        var protocol = net.mrcappy.corelib.CoreLibPlugin.getInstance()
            .getProtocolManager();
        
        listeners.forEach(protocol::registerListener);
    }
    
    /**
     * Disable this group.
     * Unregisters all listeners.
     */
    public void disable() {
        if (!enabled) return;
        
        enabled = false;
        var protocol = net.mrcappy.corelib.CoreLibPlugin.getInstance()
            .getProtocolManager();
        
        listeners.forEach(protocol::unregisterListener);
    }
    
    /**
     * Reload this group.
     * Because hot-reloading is hot.
     */
    public void reload() {
        disable();
        enable();
    }
    
    /**
     * Clear all listeners in this group.
     */
    public void clear() {
        disable();
        listeners.clear();
    }
    
    // Static methods for global management
    
    /**
     * Get a group by name.
     */
    public static ListenerGroup getGroup(String name) {
        return GROUPS.get(name);
    }
    
    /**
     * Get all groups for a plugin.
     */
    public static List<ListenerGroup> getGroups(Plugin plugin) {
        return GROUPS.values().stream()
            .filter(g -> g.plugin.equals(plugin))
            .toList();
    }
    
    /**
     * Disable all groups for a plugin.
     * Call on plugin disable.
     */
    public static void disablePlugin(Plugin plugin) {
        getGroups(plugin).forEach(ListenerGroup::disable);
    }
    
    /**
     * Remove all groups for a plugin.
     * Permanent cleanup.
     */
    public static void removePlugin(Plugin plugin) {
        getGroups(plugin).forEach(group -> {
            group.clear();
            GROUPS.remove(group.name);
        });
    }
    
    public String getName() {
        return name;
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public List<PacketListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}