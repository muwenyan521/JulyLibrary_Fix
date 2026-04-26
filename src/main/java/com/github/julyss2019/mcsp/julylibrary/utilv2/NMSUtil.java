package com.github.julyss2019.mcsp.julylibrary.utilv2;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NMSUtil {

    private NMSUtil() {}

    private static final Logger LOGGER = Bukkit.getLogger();
    private static final String TAG    = "[JulyLib/NMSUtil] ";


    private static final Pattern MC_VER_PATTERN =
            Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static final Pattern NMS_VER_PATTERN =
            Pattern.compile("v(\\d+)_(\\d+)_R(\\d+)");

    private static final Pattern MC_FROM_BUKKIT_VER =
            Pattern.compile("\\(MC:\\s*(\\d+\\.\\d+(?:\\.\\d+)?)\\)");

    // 对照表
    private static final Map<String, Integer> MC_TO_R = new HashMap<>();
    static {
        // 1.16.x
        MC_TO_R.put("1.16.1", 1); MC_TO_R.put("1.16.2", 2); MC_TO_R.put("1.16.3", 2);
        MC_TO_R.put("1.16.4", 3); MC_TO_R.put("1.16.5", 3);

        // 1.17.x
        MC_TO_R.put("1.17",   1); MC_TO_R.put("1.17.0", 1); MC_TO_R.put("1.17.1", 1);

        // 1.18.x
        MC_TO_R.put("1.18",   1); MC_TO_R.put("1.18.0", 1); MC_TO_R.put("1.18.1", 1);
        MC_TO_R.put("1.18.2", 2);

        // 1.19.x
        MC_TO_R.put("1.19",   1); MC_TO_R.put("1.19.0", 1); MC_TO_R.put("1.19.1", 1);
        MC_TO_R.put("1.19.2", 1); MC_TO_R.put("1.19.3", 2); MC_TO_R.put("1.19.4", 3);

        // 1.20.x
        MC_TO_R.put("1.20",   1); MC_TO_R.put("1.20.0", 1); MC_TO_R.put("1.20.1", 1);
        MC_TO_R.put("1.20.2", 2); MC_TO_R.put("1.20.3", 3); MC_TO_R.put("1.20.4", 3);
        MC_TO_R.put("1.20.5", 4); MC_TO_R.put("1.20.6", 4);

        // 1.21.x
        MC_TO_R.put("1.21",    1); MC_TO_R.put("1.21.0",  1); MC_TO_R.put("1.21.1",  1);
        MC_TO_R.put("1.21.2",  2); MC_TO_R.put("1.21.3",  2);
        MC_TO_R.put("1.21.4",  3);
        MC_TO_R.put("1.21.5",  4);
        MC_TO_R.put("1.21.6",  5); MC_TO_R.put("1.21.7",  5); MC_TO_R.put("1.21.8",  5);
        MC_TO_R.put("1.21.9",  6); MC_TO_R.put("1.21.10", 6);
        MC_TO_R.put("1.21.11", 7);
    }

    private static final int MAX_R_PROBE = 20;

\
    public static final String NMS_VERSION;
    private static final int[] CURRENT_VER_ARRAY;

    private static final Class<?> NOT_FOUND = Void.class;
    private static final Map<String, Class<?>> NMS_CLASS_CACHE = new ConcurrentHashMap<>();

    static {
        NMS_VERSION       = resolveNmsVersion();
        CURRENT_VER_ARRAY = parseNmsVersionArray(NMS_VERSION);
        LOGGER.info(TAG + "NMS 版本确定：" + NMS_VERSION);
    }

    /**
     * 比较传入版本与当前服务端版本。
     *
     * @param version NMS 版本字符串，如 "v1_21_R3"
     * @return 正数 → version &gt; 当前；负数 → version &lt; 当前；0 → 相等
     */
    public static int compareVersion(@NotNull String version) {
        int[] target = parseNmsVersionArray(version);
        for (int i = 0; i < 3; i++) {
            if (target[i] > CURRENT_VER_ARRAY[i]) return  1;
            if (target[i] < CURRENT_VER_ARRAY[i]) return -1;
        }
        return 0;
    }

    public static boolean isAtLeast(@NotNull String version) {
        return compareVersion(version) <= 0;
    }

    public static boolean isBelow(@NotNull String version) {
        return compareVersion(version) > 0;
    }
    @Nullable
    public static Class<?> getNMSClass(@NotNull String name) {
        Class<?> result = NMS_CLASS_CACHE.computeIfAbsent(name, k -> {
            Class<?> found = loadNMSClass(k);
            return found != null ? found : NOT_FOUND;
        });
        return result == NOT_FOUND ? null : result;
    }

    private static String resolveNmsVersion() {

        String result = strategyPackageName();
        if (result != null) {
            LOGGER.info(TAG + "[策略1/CraftBukkit包名] → " + result);
            return result;
        }

        result = strategyBukkitVersionTag();
        if (result != null) {
            LOGGER.info(TAG + "[策略2/BukkitVersion标记] → " + result);
            return result;
        }

        result = strategySharedConstants();
        if (result != null) {
            LOGGER.info(TAG + "[策略3/SharedConstants反射] → " + result);
            return result;
        }

        int major = 1, minor = 21, patch = 0; // 安全默认值
        boolean parsedOk = false;
        try {
            int[] v = parseMcVersion(Bukkit.getBukkitVersion());
            major = v[0]; minor = v[1]; patch = v[2];
            parsedOk = true;
        } catch (Exception e) {
            LOGGER.severe(TAG + "BukkitVersion 解析失败（" + Bukkit.getBukkitVersion()
                + "），策略4-7 将跳过：" + e.getMessage());
        }

        if (parsedOk) {
            result = strategyLookupTable(major, minor, patch);
            if (result != null) {
                LOGGER.info(TAG + "[策略4/对照表] → " + result);
                return result;
            }

            result = strategyClasspathScan(major, minor);
            if (result != null) {
                LOGGER.warning(TAG + "[策略5/Classpath扫描] " + mcStr(major, minor, patch)
                    + " 不在对照表，扫描到 " + result + "，请向作者报告！");
                return result;
            }

            result = strategyHighestKnownR(major, minor);
            if (result != null) {
                LOGGER.warning(TAG + "[策略6/最高已知R] " + mcStr(major, minor, patch)
                    + " 未知，沿用同 minor 最高已知版本 " + result + "，请向作者报告！");
                return result;
            }

            result = strategyPatchFallback(major, minor, patch);
            if (result != null) {
                LOGGER.warning(TAG + "[策略7/patch回退] " + mcStr(major, minor, patch)
                    + " 未知，回退至 " + result + "，请向作者报告！");
                return result;
            }
        }

        String fallback = "v" + major + "_" + minor + "_R1";
        LOGGER.severe(TAG + "[策略8/终极兜底] 所有策略均失败"
            + "（BukkitVersion=" + Bukkit.getBukkitVersion()
            + ", getVersion=" + Bukkit.getVersion()
            + "），最终使用 " + fallback + "。NMS 访问可能异常，请向插件作者报告！");
        return fallback;
    }

    @Nullable
    private static String strategyPackageName() {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            // org.bukkit.craftbukkit.v1_21_R3
            String[] parts = pkg.split("\\.");
            if (parts.length >= 4 && NMS_VER_PATTERN.matcher(parts[3]).matches()) {
                return parts[3];
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static String strategyBukkitVersionTag() {
        try {
            String raw = Bukkit.getVersion(); // "git-Paper-xxx (MC: 1.21.4)"
            Matcher m = MC_FROM_BUKKIT_VER.matcher(raw);
            if (m.find()) {
                int[] v = parseMcVersion(m.group(1));
                return strategyLookupTable(v[0], v[1], v[2]);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static String strategySharedConstants() {
        try {
            Class<?> sc = Class.forName("net.minecraft.SharedConstants");
            Method getVer = sc.getMethod("getCurrentVersion");
            Object verObj = getVer.invoke(null);
            Method getName = verObj.getClass().getMethod("getName");
            String mcVer = (String) getName.invoke(verObj);

            if (mcVer == null || mcVer.isBlank()) return null;

            int[] v = parseMcVersion(mcVer);
            String exact = strategyLookupTable(v[0], v[1], v[2]);
            if (exact != null) return exact;

            String highest = strategyHighestKnownR(v[0], v[1]);
            if (highest != null) {
                LOGGER.warning(TAG + "[策略3内部推断] SharedConstants 返回 " + mcVer
                    + " 不在对照表，使用同 minor 最高已知 R：" + highest
                    + "，请向作者报告新版本！");
                return highest;
            }
        } catch (Exception ignored) {
            
        return null;
    }

    @Nullable
    private static String strategyLookupTable(int major, int minor, int patch) {
        Integer r = MC_TO_R.get(major + "." + minor + "." + patch);
        if (r != null) return build(major, minor, r);

        if (patch == 0) {
            r = MC_TO_R.get(major + "." + minor);
            if (r != null) return build(major, minor, r);
        }
        return null;
    }

    @Nullable
    private static String strategyClasspathScan(int major, int minor) {
        for (int r = MAX_R_PROBE; r >= 1; r--) {
            String ver = build(major, minor, r);
            try {
                Class.forName("net.minecraft.server." + ver + ".MinecraftServer");
                return ver;
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    @Nullable
    private static String strategyHighestKnownR(int major, int minor) {
        int maxR = -1;
        for (Map.Entry<String, Integer> e : MC_TO_R.entrySet()) {
            String[] seg = e.getKey().split("\\.");
            if (seg.length < 2) continue;
            try {
                if (Integer.parseInt(seg[0]) == major
                 && Integer.parseInt(seg[1]) == minor
                 && e.getValue() > maxR) {
                    maxR = e.getValue();
                }
            } catch (NumberFormatException ignored) {}
        }
        return maxR > 0 ? build(major, minor, maxR) : null;
    }

    @Nullable
    private static String strategyPatchFallback(int major, int minor, int patch) {
        for (int p = patch - 1; p >= 0; p--) {
            Integer r = MC_TO_R.get(major + "." + minor + "." + p);
            if (r != null) return build(major, minor, r);
        }
        return null;
    }


    @Nullable
    private static Class<?> loadNMSClass(@NotNull String name) {
        String[] candidates = {
            "net.minecraft.server." + NMS_VERSION + "." + name,
            "net.minecraft.server." + name,
            "net.minecraft." + name,
        };
        for (String fqn : candidates) {
            try { return Class.forName(fqn); }
            catch (ClassNotFoundException ignored) {}
        }
        LOGGER.warning(TAG + "找不到 NMS 类：" + name
            + "（NMS_VERSION=" + NMS_VERSION + "）");
        return null;
    }


    private static String build(int major, int minor, int r) {
        return "v" + major + "_" + minor + "_R" + r;
    }

    private static String mcStr(int major, int minor, int patch) {
        return major + "." + minor + "." + patch;
    }

    private static int[] parseNmsVersionArray(@NotNull String version) {
        Matcher m = NMS_VER_PATTERN.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException(TAG + "非法 NMS 版本格式：" + version);
        }
        return new int[]{
            Integer.parseInt(m.group(1)),
            Integer.parseInt(m.group(2)),
            Integer.parseInt(m.group(3))
        };
    }

    private static int[] parseMcVersion(@NotNull String version) {
        Matcher m = MC_VER_PATTERN.matcher(version);
        if (!m.find()) {
            throw new RuntimeException(TAG + "无法解析 MC 版本字符串：" + version);
        }
        return new int[]{
            Integer.parseInt(m.group(1)),
            Integer.parseInt(m.group(2)),
            m.group(3) != null ? Integer.parseInt(m.group(3)) : 0
        };
    }
}