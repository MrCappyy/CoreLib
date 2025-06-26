package net.mrcappy.corelib.protocol.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for packet listener methods.
 * 
 * Because decorators are cool and Java needs more of them.
 * Slap this on a method and it becomes a packet handler.
 * 
 * Example:
 * ```java
 * @PacketHandler(types = {PacketType.PLAY_CLIENT_CHAT})
 * public void onChat(Player player, PacketContainer packet) {
 *     String message = packet.getStrings().read(0);
 *     if (message.contains("herobrine")) {
 *         packet.getStrings().write(0, "I SEE YOU");
 *     }
 * }
 * ```
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketHandler {
    
    /**
     * Packet types to listen for.
     * Empty = listen to everything (you psycho)
     */
    String[] types() default {};
    
    /**
     * Priority for this handler.
     * Default is NORMAL because we're not assholes.
     */
    ListenerPriority priority() default ListenerPriority.NORMAL;
    
    /**
     * Whether to listen for outgoing packets.
     */
    boolean sending() default true;
    
    /**
     * Whether to listen for incoming packets.
     */
    boolean receiving() default true;
    
    /**
     * Optional filter expression.
     * Basic JavaScript that returns true/false.
     * 
     * Example: "packet.getIntegers().read(0) > 100"
     */
    String filter() default "";
}