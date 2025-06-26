package net.mrcappy.corelib.protocol.listener;

/**
 * Interface for hot-reloadable packet listeners.
 * 
 * Implement this if your listener needs to be
 * enabled/disabled/reloaded at runtime without
 * restarting the whole fucking server.
 * 
 * Perfect for config-based listeners that need
 * to update when some admin changes a setting
 * because they fucked up the filter syntax again.
 * 
 * Also great for temporarily disabling your
 * packet logger while you debug why the server
 * is shitting itself with 10GB of packet logs.
 */
public interface HotReloadable {
    
    /**
     * Enable this listener.
     * Called when the listener should start processing packets.
     * Time to get back to work, lazy ass.
     */
    void enable();
    
    /**
     * Disable this listener.
     * Called when the listener should stop processing packets.
     * Take a break, you've earned it (or fucked up).
     */
    void disable();
    
    /**
     * Reload this listener's configuration.
     * Called when configs change because someone
     * can't make up their damn mind about the settings.
     */
    void reload();
    
    /**
     * Check if this listener is currently enabled.
     * true = actively fucking with packets
     * false = sitting on the bench
     */
    boolean isEnabled();
}