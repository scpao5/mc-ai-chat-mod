package com.aichat.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;

import java.util.concurrent.CompletableFuture;

/** 在游戏中执行指令。仅支持单人游戏/局域网世界（集成服务器），多人需要服务端版本 */
public class CommandExecutor {

    /** 获取当前游戏的集成服务器；多人游戏返回 null */
    public static IntegratedServer currentServer() {
        Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.getSingleplayerServer();
    }

    /** 异步在服务器线程执行一条指令，返回结果文本 */
    public static CompletableFuture<String> executeAsync(String command) {
        IntegratedServer server = currentServer();
        if (server == null) {
            return CompletableFuture.completedFuture(
                    "无法执行：当前不在单人游戏/局域网世界中（多人游戏需要服务端 mod 支持）");
        }
        String raw = command.trim();
        final String cmd = raw.startsWith("/") ? raw.substring(1) : raw;

        CompletableFuture<String> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
                future.complete("指令执行成功: /" + cmd);
            } catch (Exception e) {
                future.complete("指令执行失败: " + e.getMessage());
            }
        });
        return future;
    }
}
