package net.mrcappy.corelib.command;

import java.util.List;

/**
 * Functional interface for tab completion.
 * Returns a list of suggestions based on context.
 */
@FunctionalInterface
public interface TabCompleter {
    
    /**
     * Get tab completion suggestions.
     * Return empty list for no suggestions.
     */
    List<String> complete(CommandContext context);
}