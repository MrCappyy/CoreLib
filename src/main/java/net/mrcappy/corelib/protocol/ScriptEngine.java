package net.mrcappy.corelib.protocol;

import net.mrcappy.corelib.protocol.packet.PacketContainer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mozilla.javascript.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaScript engine for packet filtering.
 * 
 * Because sometimes you want to write packet filters in JS
 * like an absolute madman. "Why not just use Java?" Because
 * some people wake up and choose chaos, that's why.
 * 
 * Uses Rhino because it's pure Java and doesn't require
 * selling your soul to native libraries. Is it fast? No.
 * Is it a good idea? Also no. Will people use it? Absolutely.
 * 
 * Example filter that definitely won't cause problems:
 * ```js
 * while(true) { 
 *   // Your CPU goes brrrrrrr
 * }
 * ```
 * 
 * Please don't actually do that. I'm begging you.
 */
public class ScriptEngine {
    
    private final Plugin plugin;
    private final Context context;
    private final ScriptableObject scope;
    private final Map<String, Script> compiledScripts = new ConcurrentHashMap<>();
    
    public ScriptEngine(Plugin plugin) {
        this.plugin = plugin;
        
        // Initialize Rhino context
        this.context = Context.enter();
        context.setLanguageVersion(Context.VERSION_ES6);
        context.setOptimizationLevel(9); // Maximum optimization because speed
        
        // Create global scope
        this.scope = context.initStandardObjects();
        
        // Add some useful globals
        ScriptableObject.putProperty(scope, "console", new ConsoleObject());
    }
    
    /**
     * Compile a JavaScript filter.
     * 
     * @param name Unique name for this filter
     * @param source JavaScript source code
     * @throws ScriptException if the script is fucked
     */
    public void compileFilter(String name, String source) throws ScriptException {
        try {
            Script script = context.compileString(source, name, 1, null);
            compiledScripts.put(name, script);
        } catch (Exception e) {
            throw new ScriptException("Failed to compile script: " + e.getMessage());
        }
    }
    
    /**
     * Execute a filter on a packet.
     * 
     * @return true to allow packet, false to cancel
     */
    public boolean executeFilter(String name, Player player, PacketContainer packet) {
        Script script = compiledScripts.get(name);
        if (script == null) {
            plugin.getLogger().warning("Unknown script filter: " + name);
            return true; // Allow by default
        }
        
        try {
            // Create a new scope for this execution
            Scriptable executionScope = context.newObject(scope);
            executionScope.setPrototype(scope);
            executionScope.setParentScope(null);
            
            // Add packet and player to scope
            ScriptableObject.putProperty(executionScope, "packet", 
                Context.javaToJS(packet, executionScope));
            ScriptableObject.putProperty(executionScope, "player", 
                Context.javaToJS(player, executionScope));
            
            // Execute the script
            Object result = script.exec(context, executionScope);
            
            // Convert result to boolean
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).intValue() != 0;
            } else {
                return true; // Default to allow
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing script filter " + name + ": " + e.getMessage());
            return true; // Allow on error
        }
    }    
    /**
     * Remove a compiled filter.
     */
    public void removeFilter(String name) {
        compiledScripts.remove(name);
    }
    
    /**
     * Shutdown the script engine.
     * Cleans up Rhino context.
     */
    public void shutdown() {
        compiledScripts.clear();
        Context.exit();
    }
    
    /**
     * Custom exception for script errors.
     * Because we need to know when JavaScript fucks up.
     */
    public static class ScriptException extends Exception {
        public ScriptException(String message) {
            super(message);
        }
    }
    
    /**
     * Fake console object for JavaScript.
     * So script kiddies can console.log() to their heart's content.
     */
    private class ConsoleObject extends ScriptableObject {
        @Override
        public String getClassName() {
            return "Console";
        }
        
        public void log(Object... args) {
            StringBuilder sb = new StringBuilder("[JS] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().info(sb.toString());
        }
        
        public void error(Object... args) {
            StringBuilder sb = new StringBuilder("[JS ERROR] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().severe(sb.toString());
        }
        
        public void warn(Object... args) {
            StringBuilder sb = new StringBuilder("[JS WARN] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().warning(sb.toString());
        }
    }
}