package net.mrcappy.corelib.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text manager for handling MiniMessage and placeholders.
 * 
 * Adventure API is built into Paper, so we can use it
 * without external dependencies. This handles:
 * - MiniMessage parsing
 * - Placeholder replacement
 * - Legacy color code support (because some people refuse to modernize)
 * 
 * If you're still using &6 color codes in 2025,
 * please reconsider your life choices.
 */
public class TextManager {
    
    private final Plugin plugin;
    private final MiniMessage miniMessage;
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();
    private final Pattern placeholderPattern = Pattern.compile("%([^%]+)%");
    
    // Legacy serializer for backwards compatibility
    private final LegacyComponentSerializer legacySerializer = 
        LegacyComponentSerializer.legacyAmpersand();
    
    public TextManager(Plugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        
        // Register default placeholders
        registerDefaultPlaceholders();
    }    
    /**
     * Register default placeholders.
     * These are available in all messages.
     */
    private void registerDefaultPlaceholders() {
        // Player placeholders
        placeholder("player", Player::getName);
        placeholder("displayname", Player::getDisplayName);
        placeholder("uuid", p -> p.getUniqueId().toString());
        placeholder("health", p -> String.valueOf((int) p.getHealth()));
        placeholder("maxhealth", p -> String.valueOf((int) p.getMaxHealth()));
        placeholder("level", p -> String.valueOf(p.getLevel()));
        placeholder("exp", p -> String.valueOf(p.getTotalExperience()));
        placeholder("food", p -> String.valueOf(p.getFoodLevel()));
        
        // World placeholders
        placeholder("world", p -> p.getWorld().getName());
        placeholder("x", p -> String.valueOf(p.getLocation().getBlockX()));
        placeholder("y", p -> String.valueOf(p.getLocation().getBlockY()));
        placeholder("z", p -> String.valueOf(p.getLocation().getBlockZ()));
        
        // Server placeholders
        placeholder("online", p -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholder("maxplayers", p -> String.valueOf(Bukkit.getMaxPlayers()));
        placeholder("tps", p -> {
            // Get TPS from server
            double[] tps = Bukkit.getTPS();
            return String.format("%.1f", tps[0]);
        });
        
        // Time placeholders
        placeholder("time", p -> {
            long time = p.getWorld().getTime();
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            return String.format("%02d:%02d", hours, minutes);
        });
    }    
    /**
     * Register a custom placeholder.
     * 
     * Example:
     * textManager.placeholder("kills", p -> 
     *     String.valueOf(getKills(p))
     * );
     */
    public void placeholder(String key, Function<Player, String> resolver) {
        placeholders.put(key.toLowerCase(), resolver);
    }
    
    /**
     * Parse a message with MiniMessage and placeholders.
     * This is the main method you'll use.
     * 
     * @param message The message with MiniMessage tags and placeholders
     * @param player The player for placeholder context (can be null)
     * @return Parsed Component ready to send
     */
    public Component parse(String message, Player player) {
        // First, replace placeholders
        if (player != null) {
            message = replacePlaceholders(message, player);
        }
        
        // Then parse MiniMessage
        return miniMessage.deserialize(message);
    }
    
    /**
     * Parse and convert to legacy string.
     * For plugins that still use String-based APIs.
     * 
     * Please stop using this and use Components instead.
     */
    public String parseLegacy(String message, Player player) {
        Component component = parse(message, player);
        return legacySerializer.serialize(component);
    }
    
    /**
     * Parse a message with legacy color codes (&6 etc).
     * Then parse with MiniMessage.
     * 
     * For maximum compatibility with old configs.
     */
    public Component parseMixed(String message, Player player) {
        // Convert legacy codes to MiniMessage format
        message = convertLegacyToMiniMessage(message);
        
        // Then parse normally
        return parse(message, player);
    }    
    /**
     * Send a parsed message to a player.
     * Convenience method.
     */
    public void send(Player player, String message) {
        player.sendMessage(parse(message, player));
    }
    
    /**
     * Broadcast a parsed message to all online players.
     */
    public void broadcast(String message) {
        Component component = parse(message, null);
        Bukkit.getOnlinePlayers().forEach(p -> 
            p.sendMessage(parse(message, p))
        );
    }
    
    /**
     * Replace placeholders in a message.
     * Internal method but public if you need it.
     */
    public String replacePlaceholders(String message, Player player) {
        if (player == null) return message;
        
        Matcher matcher = placeholderPattern.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            Function<Player, String> resolver = placeholders.get(placeholder);
            
            if (resolver != null) {
                try {
                    String replacement = resolver.apply(player);
                    matcher.appendReplacement(result, 
                        Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    // Placeholder resolver fucked up, use placeholder as-is
                    matcher.appendReplacement(result, matcher.group(0));
                }
            } else {
                // Unknown placeholder, leave as-is
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }    
    /**
     * Convert legacy color codes to MiniMessage format.
     * Because some people can't let go of the past.
     */
    private String convertLegacyToMiniMessage(String message) {
        // This is a simplified conversion
        // Doesn't handle all edge cases but good enough
        
        message = message.replace("&0", "<black>");
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");
        
        message = message.replace("&k", "<obfuscated>");
        message = message.replace("&l", "<bold>");
        message = message.replace("&m", "<strikethrough>");
        message = message.replace("&n", "<underlined>");
        message = message.replace("&o", "<italic>");
        message = message.replace("&r", "<reset>");
        
        return message;
    }
    
    /**
     * Strip all formatting from a message.
     * Useful for logs or plain text.
     */
    public String stripFormatting(String message) {
        // Parse and convert to plain text
        Component component = miniMessage.deserialize(message);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    
    /**
     * Colorize a string with legacy color codes.
     * Supports &codes and hex colors.
     */
    public String colorize(String text) {
        if (text == null) return null;
        
        // Convert & codes to §
        text = ChatColor.translateAlternateColorCodes('&', text);
        
        // Handle hex colors like &#FF0000
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "§x§$1");
        
        // Handle spaced hex like &x&F&F&0&0&0&0
        text = text.replaceAll("&x&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])&([A-Fa-f0-9])", 
                              "§x§$1§$2§$3§$4§$5§$6");
        
        return text;
    }
    
    /**
     * Create a component builder for advanced formatting.
     */
    public Component componentBuilder() {
        return Component.text("");
    }
    
    /**
     * Parse a string into a component.
     */
    public Component parseComponent(String text) {
        return miniMessage.deserialize(text);
    }
    
    /**
     * Serialize a component to MiniMessage format.
     */
    public String serializeComponent(Component component) {
        return miniMessage.serialize(component);
    }
}