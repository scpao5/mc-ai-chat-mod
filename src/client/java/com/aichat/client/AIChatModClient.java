package com.aichat.client;

import com.aichat.AIChatMod;
import com.aichat.ai.ChatMessage;
import com.aichat.ai.ChatResponse;
import com.aichat.ai.Conversation;
import com.aichat.ai.OpenAIClient;
import com.aichat.command.CommandExecutor;
import com.aichat.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端入口：监听玩家聊天，命中触发前缀后交给 AI 处理，
 * AI 可以通过 run_command 工具调用游戏指令。
 * 聊天输入 "!ai config" 或执行 /aichat 可打开游戏内配置界面。
 */
public class AIChatModClient implements ClientModInitializer {
    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<UUID, Conversation> CONVERSATIONS = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.ALLOW_CHAT.register(this::onPlayerSend);

        // 客户端命令 /aichat 打开配置界面
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("aichat")
                        .executes(ctx -> {
                            openConfigScreen();
                            return Command.SINGLE_SUCCESS;
                        })));

        AIChatMod.LOGGER.info("[aichat] 客户端已就绪（/aichat 或 !ai config 打开配置）");
    }

    /** 打开游戏内配置界面 */
    public static void openConfigScreen() {
        Minecraft.getInstance().setScreenAndShow(new ConfigScreen());
    }

    /** 玩家发送聊天消息；返回 false 表示拦截（不发给服务器） */
    private boolean onPlayerSend(String message) {
        ModConfig cfg = AIChatMod.config();
        if (cfg == null || message == null) return true;
        if (!message.startsWith(cfg.triggerPrefix)) return true;

        String prompt = message.substring(cfg.triggerPrefix.length()).trim();
        if (prompt.isEmpty()) return true;

        // 特殊指令：打开配置界面
        if (prompt.equalsIgnoreCase("config") || prompt.equalsIgnoreCase("设置")) {
            openConfigScreen();
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        UUID uuid = client != null && client.player != null
                ? client.player.getUUID()
                : UUID.randomUUID();

        handleAsync(cfg, uuid, prompt);
        return false; // 拦截原消息，不发给服务器
    }

    /** 异步处理一轮 AI 对话 */
    private void handleAsync(ModConfig cfg, UUID uuid, String prompt) {
        Conversation conv = CONVERSATIONS.computeIfAbsent(uuid, u -> new Conversation(cfg.maxHistory));
        List<ChatMessage> pending = new ArrayList<>();
        pending.add(ChatMessage.user(prompt));
        runTurn(cfg, conv, pending, uuid, prompt, 0);
    }

    /** 递归执行"请求 API → 若返回工具调用则执行 → 再请求"，直到得到最终文本回复 */
    private void runTurn(ModConfig cfg, Conversation conv, List<ChatMessage> pending,
                         UUID uuid, String originalPrompt, int turn) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(cfg.effectiveSystemPrompt()));
        messages.addAll(conv.snapshot());
        messages.addAll(pending);

        OpenAIClient.chatAsync(cfg, messages).whenComplete((resp, err) -> {
            if (err != null) {
                sendToChat("§c[AI] 出错了: §f" + err.getMessage());
                return;
            }
            ChatResponse.Message msg = resp.choices().get(0).message();
            List<ChatResponse.ToolCall> calls = msg.toolCalls();
            boolean canTool = cfg.allowCommands && turn < cfg.maxToolTurns;

            if (calls != null && !calls.isEmpty()) {
                // 把 assistant 的工具调用请求加入上下文
                List<ChatMessage> next = new ArrayList<>(pending);
                next.add(ChatMessage.assistantWithToolCalls(msg.content(), toJsonArray(calls)));

                if (!canTool) {
                    // 工具被禁用或超轮次：告知 AI 无法执行
                    for (ChatResponse.ToolCall call : calls) {
                        next.add(ChatMessage.tool(call.id(),
                                "错误：指令执行被禁用或达到轮次上限，请直接回复玩家"));
                    }
                    runTurn(cfg, conv, next, uuid, originalPrompt, turn + 1);
                    return;
                }

                // 逐个执行工具，结果作为 tool 消息加入上下文
                CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                for (ChatResponse.ToolCall call : calls) {
                    chain = chain.thenCompose(v -> executeOneTool(call))
                            .thenAccept(result -> next.add(ChatMessage.tool(call.id(), result)));
                }
                chain.thenRun(() -> runTurn(cfg, conv, next, uuid, originalPrompt, turn + 1))
                     .exceptionally(ex -> {
                         sendToChat("§c[AI] 工具执行出错: §f" + ex.getMessage());
                         return null;
                     });
                return;
            }

            // 最终文本回复
            String reply = msg.content();
            if (reply != null && !reply.isBlank()) {
                conv.add(ChatMessage.user(originalPrompt));
                conv.add(ChatMessage.assistant(reply));
                sendToChat("§b[AI] §f" + reply.replace("\n", "\n§7    "));
            }
        });
    }

    /** 执行单个工具调用，返回结果文本 */
    private CompletableFuture<String> executeOneTool(ChatResponse.ToolCall call) {
        if (!"run_command".equals(call.function().name())) {
            return CompletableFuture.completedFuture("未知工具: " + call.function().name());
        }
        try {
            JsonObject args = GSON.fromJson(call.function().arguments(), JsonObject.class);
            String command = args.get("command").getAsString();
            AIChatMod.LOGGER.info("[aichat] AI 请求执行指令: {}", command);
            return CommandExecutor.executeAsync(command);
        } catch (Exception e) {
            return CompletableFuture.completedFuture("参数解析失败: " + e.getMessage());
        }
    }

    /** 把 List<ToolCall> 转成 OpenAI 需要的 JsonArray 结构 */
    private static JsonArray toJsonArray(List<ChatResponse.ToolCall> calls) {
        JsonArray arr = new JsonArray();
        for (ChatResponse.ToolCall call : calls) {
            arr.add(GSON.toJsonTree(call));
        }
        return arr;
    }

    /** 在主线程把消息显示到聊天框（系统消息样式，仅本地显示） */
    private static void sendToChat(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.gui != null && client.gui.hud != null) {
                client.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
            }
        });
    }
}
