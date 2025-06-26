package net.mrcappy.corelib.command.builtin;

import net.mrcappy.corelib.CoreLibPlugin;
import net.mrcappy.corelib.command.CommandContext;
import net.mrcappy.corelib.event.CoreEvent;
import net.mrcappy.corelib.event.EventBus;
import org.bukkit.event.EventPriority;

/**
 * Event system test commands.
 * 
 * /clevent - Test the event bus
 * 
 * For when you need to verify the event system isn't fucked.
 */
public class EventCommand {
    
    private final CoreLibPlugin plugin;
    private final EventBus eventBus;
    
    public EventCommand(CoreLibPlugin plugin) {
        this.plugin = plugin;
        this.eventBus = plugin.getEventBus();        
        registerCommands();
    }
    
    private void registerCommands() {
        plugin.getCommandManager().command("clevent")
            .permission("corelib.admin")
            .description("Event system test commands")
            .usage("/clevent <subcommand>")
            .executor(this::handleMain)
            .subcommand("fire", this::fireTestEvent)
            .subcommand("listen", this::toggleListener)
            .subcommand("info", this::showInfo)
            .register();
    }
    
    private void handleMain(CommandContext ctx) {
        ctx.reply("§6═══ CoreLib Event System ═══");
        ctx.reply("§eLambda-based events that don't suck:");
        ctx.reply("§7/clevent fire <message> §f- Fire a test event");
        ctx.reply("§7/clevent listen §f- Toggle test listener (50% cancel rate)");
        ctx.reply("§7/clevent info §f- Show event bus stats");
        ctx.reply("");
        ctx.reply("§7No more implementing 20 methods for one fucking event!");
    }
    
    private void fireTestEvent(CommandContext ctx) {
        String message = ctx.getAllArgs();        if (message.isEmpty()) {
            message = "Test event from " + ctx.getSender().getName();
        }
        
        TestEvent event = new TestEvent(message);
        eventBus.fireEvent(event);
        
        ctx.reply("§aFired test event: " + message);
        ctx.reply("§7Cancelled: " + event.isCancelled());
        ctx.reply("§7Modified message: " + event.getMessage());
    }
    
    private boolean listenerActive = false;
    private Object testListener = null;
    
    private void toggleListener(CommandContext ctx) {
        if (listenerActive) {
            // Remove listener
            if (testListener != null) {
                eventBus.unregister(testListener);
                testListener = null;
            }
            listenerActive = false;
            ctx.reply("§cTest listener disabled");
        } else {
            // Add listener
            testListener = new Object() {                @SuppressWarnings("unused")
                public void onTestEvent(TestEvent event) {
                    plugin.getLogger().info("Test listener received: " + event.getMessage());
                    event.setMessage(event.getMessage() + " [MODIFIED]");
                    
                    // 50% chance to cancel
                    if (Math.random() < 0.5) {
                        event.setCancelled(true);
                        plugin.getLogger().info("Test listener cancelled the event!");
                    }
                }
            };
            
            eventBus.register(testListener, TestEvent.class, 
                EventPriority.NORMAL, testListener.getClass().getDeclaredMethods()[0]);
            
            listenerActive = true;
            ctx.reply("§aTest listener enabled");
            ctx.reply("§7Will modify messages and randomly cancel events");
        }
    }
    
    private void showInfo(CommandContext ctx) {
        ctx.reply("§6Event Bus Information:");
        ctx.reply("§7Registered listeners: §f" + eventBus.getListenerCount());
        ctx.reply("§7Test listener: §f" + (listenerActive ? "Active (check your logs lol)" : "Disabled"));
        
        // Show priority info
        ctx.reply("");
        ctx.reply("§eEvent Priorities (process order):");
        ctx.reply("§7LOWEST → LOW → NORMAL → HIGH → HIGHEST → MONITOR");
        ctx.reply("§7MONITOR = read-only, can't cancel shit");
    }
    
    /**
     * Test event for demonstration.
     */
    public static class TestEvent extends CoreEvent {
        private String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}