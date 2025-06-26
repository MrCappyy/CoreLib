package net.mrcappy.corelib.command;

/**
 * Functional interface for command execution.
 * Because Bukkit's CommandExecutor returns a boolean
 * that nobody fucking uses.
 */
@FunctionalInterface
public interface CommandExecutor {
    
    /**
     * Execute the command with context.
     * Context has all the shit you need.
     */
    void execute(CommandContext context);
}