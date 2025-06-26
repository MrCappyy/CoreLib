# CoreLib

[![Version](https://img.shields.io/badge/MC%20Version-1.19--1.21-brightgreen)](https://github.com/mrcappyy/CoreLib)
[![Paper](https://img.shields.io/badge/Platform-Paper-blue)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)

CoreLib is a comprehensive utility library that makes Minecraft plugin development actually enjoyable. If you've ever found yourself writing the same boilerplate code for the hundredth time, or fighting with version-specific implementations, this library is for you.

## Why CoreLib?

Minecraft plugin development can be frustrating. Simple tasks often require excessive boilerplate, version-specific code, or external dependencies that break with every update. CoreLib solves these pain points by providing a clean, modern API that just works.

## Key Features

### Command Framework
No more implementing CommandExecutor repeatedly. CoreLib uses a modern, lambda-based approach:
```java
CoreLib.commands().register("test", ctx -> {
    ctx.reply("It works!");
});
```

### Packet Manipulation System
Advanced packet handling through Netty pipeline injection:
- Intercept, modify, or cancel any packet
- JavaScript-based packet filters (powered by Rhino)
- Client-side only entities and blocks
- Packet history tracking for debugging
- Network traffic export for analysis

### Modern Text Handling
Full MiniMessage support with an intuitive placeholder system:
```java
player.send("<gold>Welcome back, %player%! <gray>You have <green>%balance%</green> coins.");
```
- Built-in placeholders for common values
- Custom placeholder registration
- Legacy color code support for compatibility

### Event Bus
Lambda-based event handling that eliminates boilerplate:
```java
EventBus.on(PlayerJoinEvent.class, event -> {
    event.getPlayer().sendMessage("Welcome!");
});
```

### Enhanced Scheduler
A sane wrapper around Bukkit's scheduler with static access:
```java
CoreScheduler.runLater(() -> {
    // Your code here
}, 20L); // 1 second delay
```

### NBT Operations
Cross-version NBT manipulation without external dependencies:
```java
NBTItem nbt = CoreLib.nbt().getItem(itemStack);
nbt.setString("custom-id", "special_sword");
nbt.setInt("custom-damage", 150);
```

### Additional Features
- **Player Cache**: High-performance player data caching
- **Config Manager**: Centralized configuration handling
- **Version Detection**: Automatic version compatibility checking
- **Text Components**: Adventure API integration for rich text

## Installation

### Gradle
**Step 1.** Add JitPack repository to your build file
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2.** Add the dependency
```gradle
dependencies {
    implementation 'com.github.mrcappyy:CoreLib:main-SNAPSHOT'
}
```

### Maven
**Step 1.** Add the JitPack repository
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**Step 2.** Add the dependency
```xml
<dependency>
    <groupId>com.github.mrcappyy</groupId>
    <artifactId>CoreLib</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Setup
```java
public class MyPlugin extends JavaPlugin {
    private CoreLib coreLib;
    
    @Override
    public void onEnable() {
        // CoreLib auto-initializes when loaded
        coreLib = CoreLib.getInstance();
        
        // Register a command
        coreLib.getCommandManager()
            .command("hello")
            .executor(ctx -> ctx.reply("Hello, world!"))
            .register();
    }
}
```

### Packet Listening
```java
// Listen for chat packets
CoreLib.packets().onReceive(PacketType.PLAY_CLIENT_CHAT, event -> {
    String message = event.getPacket().getStrings().read(0);
    if (message.contains("blocked")) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("That word is not allowed!");
    }
});
```

## Requirements

- Paper 1.19 - 1.21
- Java 21 or higher
- No external dependencies (everything is bundled)

## Commands

CoreLib includes several built-in commands for debugging and management:

- `/corelib` - Main management command
- `/corelib reload` - Reload configurations
- `/corelib debug` - Toggle debug mode
- `/packet` - Packet manipulation tools
- `/clutil` - Various utility commands

## Performance

CoreLib is designed with performance in mind:
- Concurrent data structures for thread safety
- Lazy initialization where possible
- Minimal reflection caching
- Zero external runtime dependencies

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

CoreLib is licensed under the MIT License. See LICENSE file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/mrcappyy/CoreLib/issues)

---

Built with love (and mild frustration) by MrCappy.
