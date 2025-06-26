package net.mrcappy.corelib.command;

import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command framework that doesn't make you want to kill yourself.
 * 
 * Bukkit's command system is from 2011 and it fucking shows.
 * This brings it into the modern era with:
 * - Builder pattern for command creation
 * - Automatic tab completion
 * - Subcommand support
 * - Permission checking
 * - Argument parsing that doesn't suck
 * 
 * No more implementing CommandExecutor and TabCompleter
 * for every goddamn command.
 */
public class CommandManager {
    
    private final Plugin plugin;
    private final Map<String, CoreCommand> commands = new ConcurrentHashMap<>();
    
    public CommandManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a new command builder.
     * This is where the magic starts.
     * 
     * Example:
     * commandManager.command("test")
     *     .permission("corelib.test")
     *     .usage("/test <player> <amount>")
     *     .description("Test command")
     *     .executor((ctx) -> {
     *         Player target = ctx.getPlayer(0);
     *         int amount = ctx.getInt(1, 1);
     *         // do stuff
     *     })
     *     .tabComplete((ctx) -> {
     *         if (ctx.getArgs().length == 1) {
     *             return ctx.getOnlinePlayerNames();
     *         }
     *         return List.of();
     *     })
     *     .register();
     */
    public CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }
    
    /**
     * Get all registered commands.
     * For debugging and info purposes.
     */
    public Map<String, CoreCommand> getRegisteredCommands() {
        return new HashMap<>(commands);
    }    
    /**
     * Register a command with Bukkit.
     * Internal use only, called by CommandBuilder.
     */
    void registerCommand(CoreCommand command) {
        commands.put(command.getName(), command);
        
        // Get Bukkit's command map through reflection
        // because of course there's no API for this
        try {
            var commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());
            
            // Create a Bukkit command that delegates to our system
            Command bukkitCommand = new Command(command.getName()) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    return command.execute(sender, label, args);
                }
                
                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    return command.tabComplete(sender, alias, args);
                }
            };
            
            // Set command properties
            bukkitCommand.setDescription(command.getDescription());
            bukkitCommand.setUsage(command.getUsage());
            bukkitCommand.setPermission(command.getPermission());
            bukkitCommand.setAliases(command.getAliases());
            
            // Register with Bukkit
            commandMap.register(plugin.getName().toLowerCase(), bukkitCommand);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + command.getName());
            e.printStackTrace();
        }
    }    
    /**
     * Builder class for creating commands.
     * Fluent API because we're fancy like that.
     */
    public class CommandBuilder {
        private final CoreCommand command;
        
        CommandBuilder(String name) {
            this.command = new CoreCommand(name);
        }
        
        public CommandBuilder permission(String permission) {
            command.setPermission(permission);
            return this;
        }
        
        public CommandBuilder description(String description) {
            command.setDescription(description);
            return this;
        }
        
        public CommandBuilder usage(String usage) {
            command.setUsage(usage);
            return this;
        }
        
        public CommandBuilder aliases(String... aliases) {
            command.setAliases(Arrays.asList(aliases));
            return this;
        }
        
        public CommandBuilder executor(CommandExecutor executor) {
            command.setExecutor(executor);
            return this;
        }
        
        public CommandBuilder tabCompleter(TabCompleter completer) {
            command.setTabCompleter(completer);
            return this;
        }
        
        /**
         * Add a subcommand.
         * Because nested commands are cool.
         */
        public CommandBuilder subcommand(String name, CommandExecutor executor) {
            command.addSubcommand(name, executor);
            return this;
        }
        
        /**
         * Register the command with Bukkit.
         * This is the final step. Don't forget it!
         */
        public CoreCommand register() {
            registerCommand(command);
            return command;
        }
    }
}