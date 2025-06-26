package net.mrcappy.corelib.command;

import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a command with all its properties.
 * This is what actually handles the execution.
 * 
 * Supports subcommands because every plugin eventually
 * needs them and implementing them sucks.
 */
public class CoreCommand {
    
    private final String name;
    private String permission = null;
    private String description = "";
    private String usage = "";
    private List<String> aliases = new ArrayList<>();
    private CommandExecutor executor = null;
    private TabCompleter tabCompleter = null;
    private final Map<String, CommandExecutor> subcommands = new ConcurrentHashMap<>();
    
    CoreCommand(String name) {
        this.name = name.toLowerCase();
    }
    
    /**
     * Execute the command.
     * Handles permission checking and subcommand routing.
     */
    public boolean execute(CommandSender sender, String label, String[] args) {
        // Permission check
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        // Check for subcommands
        if (args.length > 0 && subcommands.containsKey(args[0].toLowerCase())) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            CommandContext context = new CommandContext(sender, label, subArgs);
            subcommands.get(args[0].toLowerCase()).execute(context);
            return true;
        }
        
        // Execute main command
        if (executor != null) {
            CommandContext context = new CommandContext(sender, label, args);
            executor.execute(context);
            return true;
        }
        
        // No executor set, show usage
        sender.sendMessage("§cUsage: " + usage);
        return true;
    }    
    /**
     * Handle tab completion.
     * Returns suggestions based on current input.
     */
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        // Permission check
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        
        // Subcommand completion
        if (args.length == 1 && !subcommands.isEmpty()) {
            return subcommands.keySet().stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
        }
        
        // Custom tab completer
        if (tabCompleter != null) {
            CommandContext context = new CommandContext(sender, alias, args);
            return tabCompleter.complete(context);
        }
        
        return Collections.emptyList();
    }
    
    // Getters and setters for the builder
    
    public String getName() {
        return name;
    }
    
    public String getPermission() {
        return permission;
    }
    
    void setPermission(String permission) {
        this.permission = permission;
    }
    
    public String getDescription() {
        return description;
    }
    
    void setDescription(String description) {
        this.description = description;
    }
    
    public String getUsage() {
        return usage;
    }
    
    void setUsage(String usage) {
        this.usage = usage;
    }    
    public List<String> getAliases() {
        return aliases;
    }
    
    void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
    
    void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }
    
    void setTabCompleter(TabCompleter completer) {
        this.tabCompleter = completer;
    }
    
    void addSubcommand(String name, CommandExecutor executor) {
        subcommands.put(name.toLowerCase(), executor);
    }
}