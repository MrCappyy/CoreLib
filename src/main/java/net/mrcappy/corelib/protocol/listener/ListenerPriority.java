package net.mrcappy.corelib.protocol.listener;

/**
 * Listener priority levels.
 * 
 * Works like Bukkit's EventPriority but for packets.
 * Lower runs first, higher runs last. MONITOR should
 * only observe, not modify shit.
 * 
 * If you use LOWEST to cancel packets before other
 * plugins can see them, you're an asshole.
 */
public enum ListenerPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    MONITOR(5);
    
    private final int value;
    
    ListenerPriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}