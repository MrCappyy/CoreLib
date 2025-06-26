package net.mrcappy.corelib.protocol.command;

import net.mrcappy.corelib.CoreLibPlugin;
import net.mrcappy.corelib.command.CommandContext;
import net.mrcappy.corelib.command.CommandExecutor;
import net.mrcappy.corelib.protocol.ProtocolManager;
import net.mrcappy.corelib.protocol.listener.ListenerPriority;
import net.mrcappy.corelib.protocol.listener.PacketAdapter;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Commands for packet manipulation.
 * 
 * /packet - Because sometimes you need to debug this shit
 * 
 * Features:
 * - List active listeners
 * - Dump packet history
 * - Send custom packets
 * - Add JS filters on the fly
 * 
 * Basically a Swiss Army knife for packet fuckery.
 */
public class PacketCommands {
    
    private final CoreLibPlugin plugin;
    private final ProtocolManager protocol;
    
    // Active packet dumps per player
    private final Map<UUID, PacketDump> activeDumps = new HashMap<>();
    
    public PacketCommands(CoreLibPlugin plugin) {
        this.plugin = plugin;
        this.protocol = plugin.getProtocolManager();
        
        registerCommands();
    }
    
    private void registerCommands() {
        plugin.getCommandManager().command("packet")
            .permission("corelib.packet")
            .description("Packet manipulation commands")
            .usage("/packet <subcommand>")
            .executor(this::handlePacket)
            .subcommand("list", this::listListeners)
            .subcommand("dump", this::toggleDump)
            .subcommand("history", this::showHistory)
            .subcommand("send", this::sendPacket)
            .subcommand("filter", this::manageFilter)
            .subcommand("debug", this::toggleDebug)
            .register();
    }
    
    private void handlePacket(CommandContext ctx) {
        ctx.reply("§6CoreLib Packet System");
        ctx.reply("§7/packet list - Show active listeners");
        ctx.reply("§7/packet dump - Toggle packet dumping");
        ctx.reply("§7/packet history <player> - Show packet history");
        ctx.reply("§7/packet send <type> <player> - Send a packet");
        ctx.reply("§7/packet filter <add|remove|list> - Manage JS filters");
        ctx.reply("§7/packet debug - Toggle debug mode");
    }
    
    private void listListeners(CommandContext ctx) {
        ctx.reply("§6Active packet listeners:");
        
        // Get all priority levels
        for (ListenerPriority priority : ListenerPriority.values()) {
            var listeners = protocol.getListenerManager()
                .getListenersByPriority(priority);
            
            if (!listeners.isEmpty()) {
                ctx.reply("§e" + priority.name() + ":");
                for (var listener : listeners) {
                    String plugin = listener.getPlugin().getName();
                    String className = listener.getClass().getSimpleName();
                    ctx.reply("  §7- " + plugin + ": " + className);
                }
            }
        }
        
        // Show total count
        int total = protocol.getListenerManager().getTotalListeners();
        ctx.reply("§7Total listeners: " + total);
    }    
    private void toggleDump(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.reply("§cOnly players can dump packets, console peasant.");
            return;
        }
        
        Player player = ctx.getPlayer();
        PacketDump dump = activeDumps.remove(player.getUniqueId());
        
        if (dump != null) {
            // Stop dumping
            protocol.unregisterListener(dump);
            ctx.reply("§aPacket dump stopped. Check your chat for the spam.");
        } else {
            // Start dumping
            dump = new PacketDump(player);
            activeDumps.put(player.getUniqueId(), dump);
            protocol.registerListener(dump);
            ctx.reply("§aPacket dump started. Prepare for spam!");
            ctx.reply("§7Use /packet dump again to stop.");
        }
    }
    
    private void showHistory(CommandContext ctx) {
        String targetName = ctx.getString(0, null);
        if (targetName == null) {
            ctx.reply("§cUsage: /packet history <player>");
            return;
        }
        
        Player target = ctx.getPlayer(0);
        if (target == null) {
            return; // Error message already sent
        }
        
        var history = protocol.getHistory(target);
        ctx.reply("§6=== Packet History for " + target.getName() + " ===");
        
        ctx.reply("§eRecent incoming packets:");
        for (var entry : history.getIncoming(10)) {
            ctx.reply("§7" + entry.getSummary());
        }
        
        ctx.reply("§eRecent outgoing packets:");
        for (var entry : history.getOutgoing(10)) {
            ctx.reply("§7" + entry.getSummary());
        }
    }
    
    private void sendPacket(CommandContext ctx) {
        String typeName = ctx.getString(0, null);
        String playerName = ctx.getString(1, null);
        
        if (typeName == null || playerName == null) {
            ctx.reply("§cUsage: /packet send <type> <player>");
            ctx.reply("§7Example: /packet send PLAY_SERVER_EXPLOSION notch");
            return;
        }
        
        // Parse packet type
        PacketType type;
        try {
            type = PacketType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.reply("§cUnknown packet type: " + typeName);
            ctx.reply("§7Hint: Use TAB completion or check PacketType enum");
            return;
        }
        
        Player target = ctx.getPlayer(1);
        if (target == null) {
            return;
        }
        
        // Create a basic packet (this is very simplified)
        try {
            PacketContainer packet = PacketContainer.createPacket(type);
            
            // Set some default values based on packet type
            // This is a massive switch statement in real implementations
            if (type == PacketType.PLAY_SERVER_EXPLOSION) {
                packet.getDoubles().write(0, target.getLocation().getX());
                packet.getDoubles().write(1, target.getLocation().getY());
                packet.getDoubles().write(2, target.getLocation().getZ());
                packet.getFloats().write(0, 3.0f); // Radius
            }
            
            protocol.sendPacket(target, packet);
            ctx.reply("§aSent " + type + " to " + target.getName());
            
        } catch (Exception e) {
            ctx.reply("§cFailed to send packet: " + e.getMessage());
        }
    }    
    private void manageFilter(CommandContext ctx) {
        String action = ctx.getString(0, "list");
        
        switch (action.toLowerCase()) {
            case "add":
                String name = ctx.getString(1, null);
                if (name == null) {
                    ctx.reply("§cUsage: /packet filter add <n>");
                    ctx.reply("§7Then paste your JavaScript code");
                    ctx.reply("§7Example: /packet filter add nochat");
                    return;
                }
                
                // For demo, just add a simple filter
                String code = """
                    // Block chat packets example
                    if (packet.getType().name().contains("CHAT")) {
                        console.log("Blocked chat from " + player.getName());
                        return false;
                    }
                    return true;
                    """;
                
                try {
                    protocol.getScriptEngine().compileFilter(name, code);
                    
                    // Register listener for this filter
                    var listener = new net.mrcappy.corelib.protocol.listener.JavaScriptPacketListener(
                        plugin, protocol.getScriptEngine(), name,
                        net.mrcappy.corelib.protocol.listener.ListenerPriority.NORMAL, 
                        Set.of(), Set.of() // Listen to all packets
                    );
                    protocol.registerListener(listener);
                    
                    ctx.reply("§aAdded filter: " + name);
                    ctx.reply("§7(Demo filter that blocks chat packets)");
                } catch (Exception e) {
                    ctx.reply("§cFailed to add filter: " + e.getMessage());
                }
                break;
                
            case "remove":
                String removeName = ctx.getString(1, null);
                if (removeName == null) {
                    ctx.reply("§cUsage: /packet filter remove <n>");
                    return;
                }
                
                protocol.getScriptEngine().removeFilter(removeName);
                ctx.reply("§aRemoved filter: " + removeName);
                break;
                
            case "list":
                ctx.reply("§6Active JavaScript filters:");
                
                // Get all JS listeners
                var allListeners = protocol.getListenerManager().getTotalListeners();
                int jsCount = 0;
                
                for (ListenerPriority priority : ListenerPriority.values()) {
                    for (var listener : protocol.getListenerManager()
                            .getListenersByPriority(priority)) {
                        if (listener instanceof net.mrcappy.corelib.protocol.listener.JavaScriptPacketListener jsListener) {
                            ctx.reply("§7- " + jsListener.getFilterName() + 
                                " (Priority: " + priority + ")");
                            jsCount++;
                        }
                    }
                }
                
                if (jsCount == 0) {
                    ctx.reply("§7No JavaScript filters active");
                } else {
                    ctx.reply("§7Total JS filters: " + jsCount);
                }
                break;
                
            default:
                ctx.reply("§cUsage: /packet filter <add|remove|list>");
        }
    }
    
    private void toggleDebug(CommandContext ctx) {
        boolean debug = !protocol.isDebugMode();
        protocol.setDebugMode(debug);
        
        if (debug) {
            ctx.reply("§aPacket debug mode enabled.");
            ctx.reply("§7Packet history tracking is now active.");
            ctx.reply("§7Warning: This can impact performance!");
        } else {
            ctx.reply("§aPacket debug mode disabled.");
        }
    }
    
    /**
     * Packet dump listener that spams the player's chat.
     * 
     * This is terrible UX but great for debugging.
     * Real implementations should use a GUI.
     */
    private class PacketDump extends PacketAdapter {
        private final Player player;
        private final Set<PacketType> ignoredTypes = new HashSet<>();
        
        PacketDump(Player player) {
            super(plugin, ListenerPriority.MONITOR);
            this.player = player;
            
            // Ignore spammy packets by default
            ignoredTypes.add(PacketType.PLAY_CLIENT_POSITION);
            ignoredTypes.add(PacketType.PLAY_CLIENT_POSITION_LOOK);
            ignoredTypes.add(PacketType.PLAY_CLIENT_LOOK);
            ignoredTypes.add(PacketType.PLAY_CLIENT_FLYING);
            ignoredTypes.add(PacketType.PLAY_SERVER_ENTITY);
            ignoredTypes.add(PacketType.PLAY_SERVER_ENTITY_LOOK);
            ignoredTypes.add(PacketType.PLAY_SERVER_ENTITY_MOVE_LOOK);
        }
        
        @Override
        public boolean onPacketSending(Player target, PacketContainer packet) {
            if (target.equals(player) && !ignoredTypes.contains(packet.getType())) {
                player.sendMessage("§e[OUT] §f" + packet.getType().name());
            }
            return true;
        }
        
        @Override
        public boolean onPacketReceiving(Player source, PacketContainer packet) {
            if (source.equals(player) && !ignoredTypes.contains(packet.getType())) {
                player.sendMessage("§b[IN] §f" + packet.getType().name());
            }
            return true;
        }
    }
}