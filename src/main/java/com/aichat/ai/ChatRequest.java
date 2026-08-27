package com.aichat.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** /chat/completions 请求体 */
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        List<ToolDef> tools,
        @SerializedName("tool_choice") Object toolChoice
) {
    public record ToolDef(String type, FunctionDef function) {}

    public record FunctionDef(String name, String description, JsonObject parameters) {}

    /** 构建带 run_command 工具的请求 */
    public static ChatRequest of(String model, List<ChatMessage> messages) {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject command = new JsonObject();
        command.addProperty("type", "string");
        command.addProperty("description",
                "要在 Minecraft 中执行的游戏指令，例如 /give @p diamond 64 或 /tp @p 0 100 0。不要带斜杠以外的多余内容。");
        properties.add("command", command);
        params.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("command");
        params.add("required", required);

        ToolDef tool = new ToolDef("function", new FunctionDef(
                "run_command", "在玩家的 Minecraft 世界中执行一条游戏指令", params));

        return new ChatRequest(model, messages, List.of(tool), "auto");
    }
}
