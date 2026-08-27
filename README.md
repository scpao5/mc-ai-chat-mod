# AI Chat Assistant (aichat)

Minecraft **26.2** (Fabric) 客户端模组：在游戏里和 AI 聊天，AI 可以通过工具调用帮你执行游戏指令。

## 构建

```bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-25-openjdk
export PATH=$JAVA_HOME/bin:$PATH
gradle build --no-daemon
```

产物：`build/libs/ai-chat-mod-0.1.0.jar`，放进 `.minecraft/mods/` 即可。

## 配置

首次启动自动生成 `config/aichat.json`：

| 字段 | 默认值 | 说明 |
|---|---|---|
| baseUrl | https://api.openai.com/v1 | OpenAI 兼容 API 地址（DeepSeek/Kimi/通义/Ollama 均可） |
| apiKey | (空) | 填你的 key |
| model | gpt-4o-mini | 模型名 |
| triggerPrefix | "!ai " | 触发前缀 |
| allowCommands | true | 是否允许 AI 执行指令 |
| maxToolTurns | 4 | 单轮最多工具调用次数 |
| maxHistory | 20 | 记忆的历史消息条数 |
| timeoutSeconds | 60 | HTTP 超时 |
| maxReplyChars | 400 | 每条回复最大字符数（<=0 不限制），通过提示词约束，不截断文本 |
| plainTextOnly | true | 只输出纯文本，禁止 Markdown/格式符号（通过提示词约束） |

## 使用

1. 进单人游戏（或开局域网）
2. **游戏内配置界面**：聊天框输入 `!ai config`（或执行 `/aichat`）打开配置界面，
   直接填写 API 地址 / Key / 模型，点"保存并应用"即可，无需手改 JSON
3. 聊天框输入 `!ai 给我 64 个钻石` → 回车
4. AI 决定是否调用 `run_command` 工具执行 `/give @p diamond 64`，然后回复你

## 原理

- `ClientSendMessageEvents.ALLOW_CHAT` 拦截触发前缀的消息（不发给服务器）
- `java.net.http.HttpClient` 异步请求 OpenAI 兼容 `/chat/completions`，带 `tools`（run_command）工具定义
- AI 返回 `tool_calls` → 在集成服务器线程执行指令（`IntegratedServer.getCommands().performPrefixedCommand(...)`）→ 结果作为 tool 消息回传 → 直到 AI 给出最终文本回复
- 回复以系统消息样式显示在聊天框（仅本地可见）

## 已知限制

- **仅客户端 mod**：指令执行依赖单人游戏的集成服务器；多人服务器上无法执行指令（可后续加服务端版本）
- 非流式输出：等待完整回复后一次性显示
- 无确认弹窗：`requireConfirm` 字段预留（暂未实现 UI）
