package net.mrcappy.corelib.protocol.listener;

import net.mrcappy.corelib.protocol.ScriptEngine;
import net.mrcappy.corelib.protocol.packet.PacketContainer;
import net.mrcappy.corelib.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * JavaScript-based packet filter listener.
 * 
 * This abomination lets you filter packets with JavaScript.
 * It's like giving a toddler a flamethrower - entertaining
 * but probably going to end badly.
 * 
 * The JS code gets the packet and player objects and returns
 * true to allow or false to cancel. Simple enough that even
 * JavaScript developers can understand it.
 */
public class JavaScriptPacketListener extends PacketListener {
    
    private final ScriptEngine scriptEngine;
    private final String filterName;
    
    public JavaScriptPacketListener(Plugin plugin, ScriptEngine scriptEngine,
                                   String filterName, ListenerPriority priority,
                                   Set<PacketType> sendingTypes, 
                                   Set<PacketType> receivingTypes) {
        super(plugin, priority, sendingTypes, receivingTypes);
        this.scriptEngine = scriptEngine;
        this.filterName = filterName;
    }
    
    @Override
    public boolean onPacketSending(Player player, PacketContainer packet) {
        return scriptEngine.executeFilter(filterName, player, packet);
    }
    
    @Override
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        return scriptEngine.executeFilter(filterName, player, packet);
    }
    
    public String getFilterName() {
        return filterName;
    }
}