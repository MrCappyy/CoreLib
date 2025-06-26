package net.mrcappy.corelib.event;

/**
 * Base class for custom events in the CoreLib event system.
 * 
 * Extend this when you want to create your own events
 * that work with our lambda-based event bus.
 * 
 * Way simpler than Bukkit's 47 required methods.
 */
public abstract class CoreEvent {
    
    private boolean cancelled = false;
    
    /**
     * Check if this event is cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Set the cancelled state of this event.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}