package net.mrcappy.corelib.version;

import org.bukkit.Bukkit;

/**
 * Holy shit, managing Minecraft versions is a fucking nightmare.
 * This enum tracks all the NMS versions we support because Mojang
 * can't keep their shit consistent between releases.
 * 
 * If you're reading this and wondering why we need this:
 * 1. NMS package names change every goddamn version
 * 2. Method signatures get refactored randomly
 * 3. Classes disappear and reappear like fucking magic
 * 
 * So yeah, we need to know EXACTLY what version we're on
 * to not blow up the server with NoSuchMethodErrors.
 */
public enum MinecraftVersion {
    // 1.19.x series - The Chat Reporting Apocalypse
    v1_19_R1("1.19", "1.19.1", "1.19.2"),
    v1_19_R2("1.19.3"),
    v1_19_R3("1.19.4"),
    
    // 1.20.x series - The Trails and Tales clusterfuck
    v1_20_R1("1.20", "1.20.1"),
    v1_20_R2("1.20.2"),
    v1_20_R3("1.20.3", "1.20.4"),
    
    // 1.21.x series - Whatever the fuck they're calling this one
    v1_21_R1("1.21", "1.21.1"),
    v1_21_R2("1.21.2", "1.21.3"),
    
    // Unknown version - we're fucked
    UNKNOWN;
    
    private final String[] versionStrings;
    private static MinecraftVersion current;
    
    MinecraftVersion(String... versions) {
        this.versionStrings = versions;
    }
    
    /**
     * Get the current server version.
     * Caches the result because reflection is slow as balls
     * and we don't need to check this shit every tick.
     */
    public static MinecraftVersion getCurrent() {
        if (current != null) return current;
        
        // Get the actual server version string
        String version = Bukkit.getVersion();
        String mcVersion = version.substring(version.indexOf("MC: ") + 4, version.length() - 1);
        
        // Try to match it to our known versions
        for (MinecraftVersion v : values()) {
            if (v == UNKNOWN) continue;
            
            for (String s : v.versionStrings) {
                if (mcVersion.equals(s)) {
                    current = v;
                    return current;
                }
            }
        }
        
        // Well shit, we don't support this version
        current = UNKNOWN;
        return current;
    }
    
    /**
     * Check if we're on a specific version or newer.
     * Used for feature detection because Mojang loves
     * adding random shit in minor versions.
     */
    public boolean isAtLeast(MinecraftVersion version) {
        return this.ordinal() >= version.ordinal();
    }
    
    /**
     * Get the NMS package string for this version.
     * Example: net.minecraft.server.v1_20_R3
     * 
     * This is deprecated as fuck in newer versions but
     * we still need it for compatibility with older shit.
     */
    public String getNMSPackage() {
        if (this == UNKNOWN) {
            throw new UnsupportedOperationException(
                "Can't get NMS package for unknown version you absolute donkey"
            );
        }
        return "net.minecraft.server." + this.name();
    }
    
    /**
     * Check if this version uses Mojang mappings.
     * Spoiler: They all fucking do now, but we keep this
     * for when we need to support older versions.
     */
    public boolean usesMojangMappings() {
        // Everything 1.17+ uses Mojang mappings
        // but we're only supporting 1.19+ so... yeah
        return true;
    }
}