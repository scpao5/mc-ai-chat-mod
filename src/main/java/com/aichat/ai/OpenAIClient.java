package com.aichat.ai;

import com.aichat.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** 负责与 OpenAI 兼容 API 通信（HTTP + JSON），全部异步，不阻塞游戏主线程 */
public class OpenAIClient {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static CompletableFuture<ChatResponse> chatAsync(ModConfig cfg, List<ChatMessage> messages) {
        ChatRequest body = ChatRequest.of(cfg.model, messages);
        String json = GSON.toJson(body);

        String url = cfg.baseUrl;
        if (!url.endsWith("/")) url += "/";
        url += "chat/completions";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(cfg.timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (cfg.apiKey != null && !cfg.apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + cfg.apiKey.trim());
        }

        return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("API 错误 " + resp.statusCode() + ": " + truncate(resp.body(), 500));
                    }
                    ChatResponse parsed = GSON.fromJson(resp.body(), ChatResponse.class);
                    if (parsed == null || parsed.choices() == null || parsed.choices().isEmpty()) {
                        throw new RuntimeException("API 返回了空响应");
                    }
                    return parsed;
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
