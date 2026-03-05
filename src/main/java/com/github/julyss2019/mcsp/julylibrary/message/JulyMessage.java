package com.github.julyss2019.mcsp.julylibrary.message;

import com.github.julyss2019.mcsp.julylibrary.utilv2.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JulyMessage {
    private static final boolean TITLE_ENABLED = true;
    private static final boolean RAW_MESSAGE_ENABLED = true;

    public static void broadcastRawMessage(@NotNull String json) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRawMessage(player, json);
        }
    }

    public static void sendRawMessage(@NotNull Player player, @NotNull String json) {
        BaseComponent[] components = ComponentSerializer.parse(json);
        player.spigot().sendMessage(components);
    }

    @Deprecated
    public static List<String> toColoredMessages(@NotNull List<String> messages) {
        List<String> result = new ArrayList<>();

        messages.forEach(s -> result.add(toColoredMessage(s)));
        return result;
    }

    @Deprecated
    public static String toColoredMessage(@NotNull String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendBlankLine(@NotNull CommandSender cs) {
        sendColoredMessage(cs, "");
    }

    public static void broadcastColoredMessage(@NotNull String msg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendColoredMessage(player, msg);
        }

        sendColoredMessage(Bukkit.getConsoleSender(), msg);
    }

    public static void sendColoredMessages(CommandSender cs, @NotNull String... messages) {
        for (String msg : messages) {
            sendColoredMessage(cs, msg);
        }
    }

    @Deprecated
    public static void sendColoredMessages(@NotNull CommandSender cs, @NotNull List<String> messages) {
        sendColoredMessages(cs, (Collection<String>) messages);
    }

    public static void sendColoredMessages(@NotNull CommandSender cs, @NotNull Collection<String> messages) {
        for (String msg : messages) {
            sendColoredMessage(cs, msg);
        }
    }

    public static void sendColoredMessage(@NotNull CommandSender cs, @NotNull String msg) {
        cs.sendMessage(toColoredMessage(msg));
    }

    public static boolean sendColoredMessageIfOnline(@NotNull Player player, @NotNull Collection<String> messages) {
        if (!PlayerUtil.isOnline(player)) {
            return false;
        }

        for (String msg : messages) {
            sendColoredMessage(player, msg);
        }

        return true;
    }

    public static boolean sendColoredMessageIfOnline(@NotNull Player player, @NotNull String... messages) {
        if (!PlayerUtil.isOnline(player)) {
            return false;
        }

        for (String msg : messages) {
            sendColoredMessage(player, msg);
        }

        return true;
    }

    public static boolean sendColoredMessageIfOnline(@NotNull Player player, @NotNull String msg) {
        if (!PlayerUtil.isOnline(player)) {
            return false;
        }

        sendColoredMessage(player, msg);
        return true;
    }

    public static void broadcastTitle(@NotNull Title title) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title);
        }
    }

    public static void sendTitle(@NotNull Player player, @NotNull Title title) {
        if (!isTitleEnabled()) {
            throw new RuntimeException("当前版本不支持发送 Title");
        }

        switch (title.getTitleType()) {
            case TITLE:
                player.sendTitle(title.getText(), "", title.getFadeIn(), title.getStay(), title.getFadeOut());
                break;
            case SUBTITLE:
                player.sendTitle("", title.getText(), title.getFadeIn(), title.getStay(), title.getFadeOut());
                break;
            case ACTIONBAR:
                player.sendActionBar(Component.text(title.getText()));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(title.getText()));
                break;
            default:
                throw new RuntimeException("未知 Title 类型: " + title.getTitleType());
        }
    }

    public static boolean isTitleEnabled() {
        return TITLE_ENABLED;
    }

    public static boolean isRawMessageEnabled() {
        return RAW_MESSAGE_ENABLED;
    }
}
