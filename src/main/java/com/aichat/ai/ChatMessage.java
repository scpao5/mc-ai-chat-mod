package com.aichat.ai;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

/**
 * OpenAI 兼容协议中的一条聊天消息。
 * Gson 默认跳过 null 字段。
 */
public record ChatMessage(
        String role,
        String content,
        @SerializedName("tool_call_id") String toolCallId,
        @SerializedName("tool_calls") JsonArray toolCalls
) {
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    /** assistant 的普通回复 */
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    /** assistant 带工具调用请求 */
    public static ChatMessage assistantWithToolCalls(String content, JsonArray toolCalls) {
        return new ChatMessage("assistant", content, null, toolCalls);
    }

    /** 工具执行结果回传 */
    public static ChatMessage tool(String toolCallId, String result) {
        return new ChatMessage("tool", result, toolCallId, null);
    }
}
