package net.mrcappy.corelib;

import net.mrcappy.corelib.command.CommandManager;
import net.mrcappy.corelib.config.ConfigManager;
import net.mrcappy.corelib.event.EventBus;
import net.mrcappy.corelib.nbt.NBTManager;
import net.mrcappy.corelib.player.PlayerCache;
import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.scheduler.CoreScheduler;
import net.mrcappy.corelib.text.TextManager;
import net.mrcappy.corelib.version.MinecraftVersion;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * CoreLib - The fucking Swiss Army knife of Minecraft plugin development.
 * 
 * Listen up future me, or whoever is reading this disaster:
 * This is the main entry point. It initializes all the subsystems
 * that make plugin development slightly less painful than slamming
 * your balls in a car door.
 * 
 * Everything here is designed to be:
 * 1. Thread-safe (because async is everywhere now)
 * 2. Version-independent (1.19-1.21, fuck knows what comes next)
 * 3. Actually useful (unlike 90% of "utility" plugins)
 * 
 * If shit breaks, check the version first. Always.
 */
public class CoreLibPlugin extends JavaPlugin {
    
    private static CoreLibPlugin instance;
    
    // Core managers - these bastards handle everything
    private ConfigManager configManager;
    private CoreScheduler scheduler;
    private EventBus eventBus;
    private CommandManager commandManager;
    private PlayerCache playerCache;
    private NBTManager nbtManager;
    private TextManager textManager;
    private ProtocolManager protocolManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Check if we're on a supported version
        MinecraftVersion version = MinecraftVersion.getCurrent();
        if (version == MinecraftVersion.UNKNOWN) {
            getLogger().severe("╔════════════════════════════════════════╗");
            getLogger().severe("║         UNSUPPORTED VERSION!           ║");
            getLogger().severe("║                                        ║");
            getLogger().severe("║  CoreLib only supports MC 1.19-1.21    ║");
            getLogger().severe("║  Your version: " + getServer().getVersion());
            getLogger().severe("║                                        ║");
            getLogger().severe("║  Shit WILL break. You've been warned. ║");
            getLogger().severe("╚════════════════════════════════════════╝");
        } else {
            getLogger().info("Detected Minecraft version: " + version.name());
        }        
        try {
            // Initialize all the managers in order
            // Some of these fuckers depend on each other
            
            getLogger().info("Initializing CoreLib systems...");
            
            // Config manager first - everything needs config
            this.configManager = new ConfigManager(this);
            
            // Scheduler - because Bukkit's scheduler API is garbage
            this.scheduler = new CoreScheduler(this);
            
            // Event bus - for when you're too lazy to register listeners properly
            this.eventBus = new EventBus(this);
            
            // Text manager - because color codes are so 2010
            this.textManager = new TextManager(this);
            
            // Player cache - because Bukkit's offline player API is slow as fuck
            this.playerCache = new PlayerCache(this);
            
            // Command framework - because brigadier is overkill
            this.commandManager = new CommandManager(this);
            
            // NBT manager - the real black magic shit
            this.nbtManager = new NBTManager(this);
            
            // Protocol manager - packet manipulation fuckery
            this.protocolManager = new ProtocolManager(this);
            
            // Register built-in commands
            new net.mrcappy.corelib.command.builtin.CoreLibCommand(this);
            new net.mrcappy.corelib.command.builtin.UtilityCommand(this);
            new net.mrcappy.corelib.command.builtin.EventCommand(this);
            new net.mrcappy.corelib.protocol.command.PacketCommands(this);
            
            getLogger().info("CoreLib initialized successfully!");
            getLogger().info("Ready to make your life slightly less miserable.");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize CoreLib. RIP.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        // Clean up our mess
        if (scheduler != null) scheduler.shutdown();
        if (playerCache != null) playerCache.save();
        if (configManager != null) configManager.saveAll();
        if (protocolManager != null) protocolManager.shutdown();
        
        getLogger().info("CoreLib disabled. Good luck without me.");
    }    
    /**
     * Get the plugin instance.
     * Singleton pattern because there can only be one.
     */
    public static CoreLibPlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "CoreLib not initialized yet! Did you try to use it before onEnable?"
            );
        }
        return instance;
    }
    
    // Getters for all the managers
    // These are what other plugins will actually use
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CoreScheduler getCoreScheduler() {
        return scheduler;
    }
    
    public EventBus getEventBus() {
        return eventBus;
    }
    
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    public PlayerCache getPlayerCache() {
        return playerCache;
    }
    
    public NBTManager getNBTManager() {
        return nbtManager;
    }
    
    public TextManager getTextManager() {
        return textManager;
    }
    
    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}