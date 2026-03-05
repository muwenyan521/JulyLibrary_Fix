package com.github.julyss2019.mcsp.julylibrary.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class NMSUtil {
    private static final Pattern BUKKIT_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");
    @Deprecated
    public static final String SERVER_VERSION = resolveServerVersion();
    public static final String NMS_VERSION = SERVER_VERSION;

    private static String resolveServerVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String[] split = packageName.split("\\.");

        if (split.length > 3 && split[3].startsWith("v")) {
            return split[3];
        }

        Matcher matcher = BUKKIT_VERSION_PATTERN.matcher(Bukkit.getBukkitVersion());
        if (matcher.find()) {
            return "v" + matcher.group(1) + "_" + matcher.group(2) + "_R1";
        }

        throw new RuntimeException("无法识别 Bukkit 版本: " + Bukkit.getBukkitVersion());
    }

    /**
     * 得到NMS类
     * @param name 类名
     */
    public static Class<?> getNMSClass(@NotNull String name) {
        try {
            return Class.forName("net.minecraft.server." + NMS_VERSION + "." + name);
        } catch (ClassNotFoundException ignored) {}

        try {
            return Class.forName("net.minecraft.server." + name);
        } catch (ClassNotFoundException ignored) {}

        return null;
    }
}
