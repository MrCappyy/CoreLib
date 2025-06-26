package net.mrcappy.corelib.command;

import net.mrcappy.corelib.CoreLibPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command context with useful utilities.
 * 
 * This is what gets passed to your command executor.
 * Has helper methods for parsing arguments because
 * doing it manually every time is AIDS.
 * 
 * Features:
 * - Type-safe argument parsing
 * - Default values
 * - Player lookup
 * - Common tab completions
 */
public class CommandContext {
    
    private final CommandSender sender;
    private final String label;
    private final String[] args;
    
    public CommandContext(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args;
    }
    
    /**
     * Get the command sender.
     */
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * Check if sender is a player.
     * Use this before casting to Player.
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }
    
    /**
     * Get sender as player.
     * Throws IllegalStateException if not a player.
     */
    public Player getPlayer() {
        if (!isPlayer()) {
            throw new IllegalStateException(
                "Command sender is not a player. Check isPlayer() first, dumbass."
            );
        }
        return (Player) sender;
    }    
    /**
     * Get the command label used.
     * Useful for usage messages.
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Get all arguments.
     */
    public String[] getArgs() {
        return args;
    }
    
    /**
     * Get argument at index.
     * Returns null if out of bounds.
     */
    public String getArg(int index) {
        return index < args.length ? args[index] : null;
    }
    
    /**
     * Get argument as string with default.
     */
    public String getString(int index, String defaultValue) {
        String arg = getArg(index);
        return arg != null ? arg : defaultValue;
    }
    
    /**
     * Get argument as integer.
     * Returns default if not a valid number.
     */
    public int getInt(int index, int defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;
        
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + arg);
            return defaultValue;
        }
    }
    
    /**
     * Get argument as double.
     * Because sometimes you need decimals.
     */
    public double getDouble(int index, double defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;
        
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: " + arg);
            return defaultValue;
        }
    }    
    /**
     * Get argument as boolean.
     * Accepts: true/false, yes/no, on/off, 1/0
     */
    public boolean getBoolean(int index, boolean defaultValue) {
        String arg = getArg(index);
        if (arg == null) return defaultValue;
        
        arg = arg.toLowerCase();
        return arg.equals("true") || arg.equals("yes") || 
               arg.equals("on") || arg.equals("1");
    }
    
    /**
     * Get argument as online player.
     * Returns null if player not found.
     */
    public Player getPlayer(int index) {
        String arg = getArg(index);
        if (arg == null) return null;
        
        Player player = Bukkit.getPlayerExact(arg);
        if (player == null) {
            sender.sendMessage("§cPlayer not found: " + arg);
        }
        return player;
    }
    
    /**
     * Join arguments from index to end.
     * Useful for messages and reasons.
     */
    public String joinArgs(int startIndex) {
        if (startIndex >= args.length) return "";
        
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }
    
    /**
     * Check if sender has permission.
     * Sends error message if not.
     */
    public boolean checkPermission(String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission: " + permission);
            return false;
        }
        return true;
    }
    
    /**
     * Send a message to the sender.
     * Just a convenience method.
     */
    public void reply(String message) {
        sender.sendMessage(message);
    }
    
    /**
     * Get the plugin instance.
     * For when you need it in commands.
     */
    public CoreLibPlugin getPlugin() {
        return CoreLibPlugin.getInstance();
    }    
    // Tab completion helpers
    // Because writing these every time is tedious
    
    /**
     * Get online player names for tab completion.
     * Optionally filtered by prefix.
     */
    public List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get online player names that start with prefix.
     * For smart tab completion.
     */
    public List<String> getOnlinePlayerNames(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Filter a list of options by prefix.
     * Common pattern for tab completion.
     */
    public List<String> filterOptions(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        
        String lower = prefix.toLowerCase();
        return options.stream()
            .filter(opt -> opt.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all arguments as a single string.
     * Useful for message commands.
     */
    public String getAllArgs() {
        return String.join(" ", args);
    }
}