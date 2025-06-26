package net.mrcappy.corelib.command.builtin;

import net.mrcappy.corelib.CoreLibPlugin;
import net.mrcappy.corelib.command.CommandContext;
import net.mrcappy.corelib.version.MinecraftVersion;
import org.bukkit.Bukkit;

/**
 * Main CoreLib command for plugin management.
 * 
 * /corelib - Show info and help
 * /corelib reload - Reload configs
 * /corelib debug - Toggle debug mode
 * /corelib info - Show system info
 * /corelib test - Run test commands
 * 
 * Because every library needs a way to check if it's working.
 */
public class CoreLibCommand {
    
    private final CoreLibPlugin plugin;
    private boolean debugMode = false;
    
    public CoreLibCommand(CoreLibPlugin plugin) {
        this.plugin = plugin;        
        registerCommands();
    }
    
    private void registerCommands() {
        plugin.getCommandManager().command("corelib")
            .permission("corelib.admin")
            .description("CoreLib management commands")
            .usage("/corelib <subcommand>")
            .executor(this::handleMain)
            .subcommand("reload", this::handleReload)
            .subcommand("debug", this::handleDebug)
            .subcommand("info", this::handleInfo)
            .subcommand("test", this::handleTest)
            .subcommand("managers", this::handleManagers)
            .register();
    }
    
    private void handleMain(CommandContext ctx) {
        ctx.reply("§6═══ CoreLib v" + plugin.getDescription().getVersion() + " ═══");
        ctx.reply("§7The Swiss Army knife of Minecraft plugin development");
        ctx.reply("§7Making plugin dev suck less since 2025");
        ctx.reply("");
        ctx.reply("§eCommands:");
        ctx.reply("§7/corelib reload §f- Reload configs without restarting");
        ctx.reply("§7/corelib debug §f- Toggle debug mode (prepare for spam)");
        ctx.reply("§7/corelib info §f- Show system info");
        ctx.reply("§7/corelib test §f- Run tests to make sure shit works");
        ctx.reply("§7/corelib managers §f- Show all the managers doing their thing");
        ctx.reply("");
        ctx.reply("§7/packet §f- Packet manipulation fuckery (see /packet help)");
    }
    
    private void handleReload(CommandContext ctx) {
        long start = System.currentTimeMillis();
        
        try {
            // Reload main config
            plugin.reloadConfig();
            plugin.getConfigManager().reloadAll();
            
            long time = System.currentTimeMillis() - start;
            ctx.reply("§aConfiguration reloaded in " + time + "ms");
            
        } catch (Exception e) {
            ctx.reply("§cFailed to reload config: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleDebug(CommandContext ctx) {
        debugMode = !debugMode;        plugin.getProtocolManager().setDebugMode(debugMode);
        
        if (debugMode) {
            ctx.reply("§aDebug mode enabled");
            ctx.reply("§7- Verbose logging active");
            ctx.reply("§7- Packet history tracking enabled");
            ctx.reply("§7- Performance metrics visible");
        } else {
            ctx.reply("§aDebug mode disabled");
        }
    }
    
    private void handleInfo(CommandContext ctx) {
        ctx.reply("§6═══ System Information ═══");
        ctx.reply("§eServer:");
        ctx.reply("§7- Version: §f" + Bukkit.getVersion());
        ctx.reply("§7- MC Version: §f" + MinecraftVersion.getCurrent().name());
        ctx.reply("§7- Players: §f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        ctx.reply("");        ctx.reply("§eMemory:");
        ctx.reply("§7- Used: §f" + usedMemory + "MB / " + totalMemory + "MB");
        ctx.reply("§7- Max: §f" + maxMemory + "MB");
        
        ctx.reply("");
        ctx.reply("§eCoreLib:");
        ctx.reply("§7- Version: §f" + plugin.getDescription().getVersion());
        ctx.reply("§7- Debug Mode: §f" + (debugMode ? "Enabled" : "Disabled"));
        ctx.reply("§7- Registered Commands: §f" + plugin.getCommandManager().getRegisteredCommands().size());
        ctx.reply("§7- Active Listeners: §f" + plugin.getProtocolManager().getListenerManager().getTotalListeners());
        ctx.reply("§7- Cached Players: §f" + plugin.getPlayerCache().getCachedPlayerCount());
    }
    
    private void handleTest(CommandContext ctx) {
        ctx.reply("§6Running CoreLib tests...");
        
        // Test text formatting
        String formatted = plugin.getTextManager().colorize("&6&lHello &aWorld!");
        ctx.reply("§7Text Manager: " + formatted);
        
        // Test scheduler
        ctx.reply("§7Scheduler: Running async task...");
        plugin.getCoreScheduler().runAsync(() -> {
            plugin.getCoreScheduler().runSync(() -> {                ctx.reply("§aAsync → Sync task completed!");
            });
        });
        
        // Test event bus
        int listeners = plugin.getEventBus().getListenerCount();
        ctx.reply("§7Event Bus: §f" + listeners + " listeners registered");
        
        // Test NBT if player
        if (ctx.isPlayer()) {
            var item = ctx.getPlayer().getInventory().getItemInMainHand();
            if (item != null && !item.getType().isAir()) {
                var nbt = plugin.getNBTManager().getNBT(item);
                ctx.reply("§7NBT Manager: Item has " + nbt.getKeys().size() + " NBT tags");
            } else {
                ctx.reply("§7NBT Manager: Hold an item to test NBT");
            }
        }
        
        ctx.reply("§aAll tests completed!");
    }
    
    private void handleManagers(CommandContext ctx) {
        ctx.reply("§6═══ Loaded Managers ═══");
        ctx.reply("§7✓ ConfigManager §f- Handles your yaml bullshit");
        ctx.reply("§7✓ CoreScheduler §f- Because Bukkit's scheduler is ass");
        ctx.reply("§7✓ EventBus §f- Lambda-based event handling");
        ctx.reply("§7✓ CommandManager §f- No more plugin.yml hell");
        ctx.reply("§7✓ PlayerCache §f- Fast as fuck player lookups");
        ctx.reply("§7✓ NBTManager §f- Dark magic for item data");
        ctx.reply("§7✓ TextManager §f- Modern text formatting that doesn't suck");
        ctx.reply("§7✓ ProtocolManager §f- Where the real fuckery happens");
        ctx.reply("");
        ctx.reply("§7All systems operational. Probably won't explode.");
    }
}