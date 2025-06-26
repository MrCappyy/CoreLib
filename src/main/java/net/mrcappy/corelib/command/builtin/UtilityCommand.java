package net.mrcappy.corelib.command.builtin;

import net.mrcappy.corelib.CoreLibPlugin;
import net.mrcappy.corelib.command.CommandContext;
import net.mrcappy.corelib.nbt.ItemNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Developer utility commands for testing CoreLib features.
 * 
 * /clutil - Various utilities for devs
 * 
 * Because sometimes you need to test shit quickly.
 */
public class UtilityCommand {
    
    private final CoreLibPlugin plugin;
    
    public UtilityCommand(CoreLibPlugin plugin) {
        this.plugin = plugin;        
        registerCommands();
    }
    
    private void registerCommands() {
        plugin.getCommandManager().command("clutil")
            .permission("corelib.admin")
            .description("CoreLib utility commands")
            .usage("/clutil <subcommand>")
            .executor(this::handleMain)
            .subcommand("nbt", this::handleNBT)
            .subcommand("text", this::handleText)
            .subcommand("item", this::handleItem)
            .subcommand("schedule", this::handleSchedule)
            .subcommand("cache", this::handleCache)
            .register();
    }
    
    private void handleMain(CommandContext ctx) {
        ctx.reply("§6═══ CoreLib Utilities ═══");
        ctx.reply("§eActually useful shit:");
        ctx.reply("§7/clutil nbt §f- Fuck with NBT data on items");
        ctx.reply("§7/clutil text <text> §f- Test text formatting (hex colors ftw)");
        ctx.reply("§7/clutil item §f- Get test items with custom NBT");
        ctx.reply("§7/clutil schedule §f- Make sure scheduler isn't broken");
        ctx.reply("§7/clutil cache §f- Player cache stats and control");
    }
    
    private void handleNBT(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.reply("§cThis command requires a player!");
            return;
        }
        
        Player player = ctx.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            ctx.reply("§cHold an item in your main hand!");
            return;
        }
        
        String action = ctx.getString(0, "view");
        
        switch (action.toLowerCase()) {
            case "view":
                ItemNBT nbt = plugin.getNBTManager().getNBT(item);
                ctx.reply("§6NBT Data for " + item.getType().name() + ":");
                
                if (nbt.getKeys().isEmpty()) {
                    ctx.reply("§7No custom NBT data");
                } else {
                    for (String key : nbt.getKeys()) {
                        Object value = nbt.get(key);
                        ctx.reply("§7- " + key + ": §f" + value);
                    }
                }
                break;
                
            case "set":
                String key = ctx.getString(1, null);
                String value = ctx.getString(2, null);
                
                if (key == null || value == null) {
                    ctx.reply("§cUsage: /clutil nbt set <key> <value>");
                    return;
                }
                
                ItemNBT setNbt = plugin.getNBTManager().getNBT(item);
                setNbt.setString(key, value);
                ItemStack newItem = plugin.getNBTManager().setNBT(item, setNbt);
                player.getInventory().setItemInMainHand(newItem);
                
                ctx.reply("§aSet NBT: " + key + " = " + value);
                break;
                
            case "clear":
                ItemNBT clearNbt = plugin.getNBTManager().createNBT();
                ItemStack clearedItem = plugin.getNBTManager().setNBT(item, clearNbt);                player.getInventory().setItemInMainHand(clearedItem);
                ctx.reply("§aCleared all NBT data");
                break;
                
            default:
                ctx.reply("§cUsage: /clutil nbt [view|set|clear]");
        }
    }
    
    private void handleText(CommandContext ctx) {
        String text = ctx.getAllArgs();
        if (text.isEmpty()) {
            // Show examples
            ctx.reply("§6Text formatting examples:");
            ctx.reply(plugin.getTextManager().colorize("&aGreen &bBlue &cRed §7- Basic shit"));
            ctx.reply(plugin.getTextManager().colorize("&lBold &nUnderline &oItalic §7- For when you need emphasis"));
            ctx.reply(plugin.getTextManager().colorize("&x&F&F&0&0&0&0Hex colors! §7- Because 16 colors isn't enough"));
            ctx.reply(plugin.getTextManager().colorize("&#FF0000R&#00FF00G&#0000FFB §7- Full RGB support baby"));
            ctx.reply("§7Usage: /clutil text <your text here>");
        } else {
            String formatted = plugin.getTextManager().colorize(text);
            ctx.reply("§7Formatted: " + formatted);
            ctx.reply("§7Looks sick, right?");
        }
    }
        private void handleItem(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.reply("§cThis command requires a player!");
            return;
        }
        
        Player player = ctx.getPlayer();
        String type = ctx.getString(0, "list");
        
        switch (type.toLowerCase()) {
            case "nbt":
                ItemStack nbtItem = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = nbtItem.getItemMeta();
                meta.setDisplayName(plugin.getTextManager().colorize("&6&lNBT Test Sword"));
                meta.setLore(Arrays.asList(
                    plugin.getTextManager().colorize("&7A sword with custom NBT"),
                    plugin.getTextManager().colorize("&7Use /clutil nbt to view")
                ));
                nbtItem.setItemMeta(meta);
                
                ItemNBT nbt = plugin.getNBTManager().createNBT();
                nbt.setString("CoreLib", "TestItem");
                nbt.setInt("CustomDamage", 9999);
                nbt.setBoolean("Unbreakable", true);
                ItemStack finalItem = plugin.getNBTManager().setNBT(nbtItem, nbt);                
                player.getInventory().addItem(finalItem);
                ctx.reply("§aGave NBT test item!");
                break;
                
            case "text":
                ItemStack textItem = new ItemStack(Material.BOOK);
                ItemMeta textMeta = textItem.getItemMeta();
                textMeta.setDisplayName(plugin.getTextManager().colorize("&#FF0000C&#FF7F00o&#FFFF00l&#00FF00o&#0000FFr&#8B00FFf&#FF00FFu&#FF007Fl"));
                textMeta.setLore(Arrays.asList(
                    plugin.getTextManager().colorize("&x&F&F&0&0&0&0Red Hex"),
                    plugin.getTextManager().colorize("&x&0&0&F&F&0&0Green Hex"),
                    plugin.getTextManager().colorize("&x&0&0&0&0&F&FBlue Hex"),
                    "",
                    plugin.getTextManager().colorize("&7&oAll the colors!")
                ));
                textItem.setItemMeta(textMeta);
                
                player.getInventory().addItem(textItem);
                ctx.reply("§aGave text formatting test item!");
                break;
                
            default:
                ctx.reply("§6Test items:");
                ctx.reply("§7/clutil item nbt - Item with custom NBT");                ctx.reply("§7/clutil item text - Item with colored text");
        }
    }
    
    private void handleSchedule(CommandContext ctx) {
        String type = ctx.getString(0, "sync");
        int delay = ctx.getInt(1, 20); // Default 1 second
        
        switch (type.toLowerCase()) {
            case "sync":
                plugin.getCoreScheduler().runLater(() -> {
                    ctx.reply("§aSync task executed after " + delay + " ticks!");
                }, delay);
                ctx.reply("§7Scheduled sync task for " + delay + " ticks...");
                break;
                
            case "async":
                plugin.getCoreScheduler().runAsyncLater(() -> {
                    plugin.getCoreScheduler().runSync(() -> {
                        ctx.reply("§aAsync task executed after " + delay + " ticks!");
                    });
                }, delay);
                ctx.reply("§7Scheduled async task for " + delay + " ticks...");
                break;
                
            case "repeat":                final int[] count = {0};
                var task = plugin.getCoreScheduler().runTimer(() -> {
                    count[0]++;
                    ctx.reply("§7Repeating task #" + count[0]);
                    if (count[0] >= 5) {
                        ctx.reply("§cTask cancelled after 5 executions");
                    }
                }, 0, delay);
                
                // Cancel after 5 executions
                plugin.getCoreScheduler().runLater(() -> task.cancel(), delay * 5);
                ctx.reply("§7Started repeating task every " + delay + " ticks...");
                break;
                
            default:
                ctx.reply("§6Scheduler test:");
                ctx.reply("§7/clutil schedule sync [ticks]");
                ctx.reply("§7/clutil schedule async [ticks]");
                ctx.reply("§7/clutil schedule repeat [interval]");
        }
    }
    
    private void handleCache(CommandContext ctx) {
        var cache = plugin.getPlayerCache();
        String action = ctx.getString(0, "info");
                switch (action.toLowerCase()) {
            case "info":
                ctx.reply("§6Player Cache Info:");
                ctx.reply("§7Cached players: §f" + cache.getCachedPlayerCount());
                ctx.reply("§7Cache hits: §f" + cache.getCacheHits());
                ctx.reply("§7Cache misses: §f" + cache.getCacheMisses());
                
                double hitRate = cache.getCacheHits() + cache.getCacheMisses() > 0 ?
                    (double) cache.getCacheHits() / (cache.getCacheHits() + cache.getCacheMisses()) * 100 : 0;
                ctx.reply("§7Hit rate: §f" + String.format("%.1f%%", hitRate));
                break;
                
            case "clear":
                cache.clearCache();
                ctx.reply("§aPlayer cache cleared!");
                break;
                
            case "save":
                cache.save();
                ctx.reply("§aPlayer cache saved to disk!");
                break;
                
            default:
                ctx.reply("§6Player cache commands:");
                ctx.reply("§7/clutil cache info - Show cache statistics");
                ctx.reply("§7/clutil cache clear - Clear the cache");                ctx.reply("§7/clutil cache save - Force save to disk");
        }
    }
}