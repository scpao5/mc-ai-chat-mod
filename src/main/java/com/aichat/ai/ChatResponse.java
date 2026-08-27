package com.aichat.ai;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** /chat/completions 响应体（只解析需要的字段） */
public record ChatResponse(List<Choice> choices) {

    public record Choice(Message message, @SerializedName("finish_reason") String finishReason) {}

    public record Message(String role, String content, @SerializedName("tool_calls") List<ToolCall> toolCalls) {}

    public record ToolCall(String id, String type, FunctionCall function) {}

    public record FunctionCall(String name, String arguments) {}
}
