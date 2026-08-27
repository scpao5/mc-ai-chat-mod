package com.aichat.client;

import com.aichat.AIChatMod;
import com.aichat.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 游戏内配置界面：免改 JSON，直接在游戏里编辑配置。
 * 通过 /aichat 命令、聊天 "!ai config" 或 ModMenu "配置"按钮打开。
 *
 * 注意：StringWidget 的 (x,y,text,font) 构造宽高为 0，文字会被裁剪，
 * 必须使用 (x,y,w,h,text,font) 显式宽高构造。
 */
public class ConfigScreen extends Screen {
    private final ModConfig cfg;

    private EditBox baseUrlBox;
    private EditBox apiKeyBox;
    private EditBox modelBox;
    private EditBox prefixBox;
    private EditBox maxCharsBox;
    private EditBox maxTurnsBox;
    private EditBox maxHistoryBox;
    private CycleButton<Boolean> plainTextButton;
    private CycleButton<Boolean> allowCmdButton;

    public ConfigScreen() {
        super(Component.literal("AI Chat Assistant 配置"));
        this.cfg = AIChatMod.config();
    }

    @Override
    protected void init() {
        // 保留窗口重建前的输入内容
        String oldBase = baseUrlBox != null ? baseUrlBox.getValue() : cfg.baseUrl;
        String oldKey  = apiKeyBox != null ? apiKeyBox.getValue() : cfg.apiKey;
        String oldModel = modelBox != null ? modelBox.getValue() : cfg.model;
        String oldPrefix = prefixBox != null ? prefixBox.getValue() : cfg.triggerPrefix;
        String oldChars = maxCharsBox != null ? maxCharsBox.getValue() : String.valueOf(cfg.maxReplyChars);
        String oldTurns = maxTurnsBox != null ? maxTurnsBox.getValue() : String.valueOf(cfg.maxToolTurns);
        String oldHist  = maxHistoryBox != null ? maxHistoryBox.getValue() : String.valueOf(cfg.maxHistory);
        boolean oldPlain = plainTextButton != null ? plainTextButton.getValue() : cfg.plainTextOnly;
        boolean oldCmd  = allowCmdButton != null ? allowCmdButton.getValue() : cfg.allowCommands;

        clearWidgets();

        int labelW = 100;
        int boxX = labelW + 14;
        int boxW = Math.max(140, width - boxX - 10);
        int rowH = 26;

        // 标题
        StringWidget title = new StringWidget(10, 6, width - 20, 12,
                Component.literal("AI Chat Assistant 配置"), font);
        title.setMaxWidth(width - 20);
        addRenderableWidget(title);

        int y = 26;
        // 第一行：API 地址
        addLabel(y, labelW, "API 地址");
        baseUrlBox = box(boxX, y, boxW, oldBase);
        y += rowH;

        // API Key
        addLabel(y, labelW, "API Key");
        apiKeyBox = box(boxX, y, boxW, oldKey);
        y += rowH;

        // 模型
        addLabel(y, labelW, "模型");
        modelBox = box(boxX, y, boxW, oldModel);
        y += rowH;

        // 触发前缀
        addLabel(y, labelW, "触发前缀");
        prefixBox = box(boxX, y, boxW, oldPrefix);
        y += rowH;

        // 数字字段一行三列：输出字数 / 工具轮次 / 历史条数
        addLabel(y, labelW, "输出限制");
        int colW = Math.max(40, (boxW - 16) / 3);
        int gap = 8;
        maxCharsBox = box(boxX, y, colW, oldChars);
        maxCharsBox.setHint(Component.literal("字数"));
        maxTurnsBox = box(boxX + colW + gap, y, colW, oldTurns);
        maxTurnsBox.setHint(Component.literal("轮次"));
        maxHistoryBox = box(boxX + 2 * (colW + gap), y, colW, oldHist);
        maxHistoryBox.setHint(Component.literal("历史"));
        y += rowH;

        // 开关行1：纯文本输出
        addLabel(y, labelW, "纯文本输出");
        plainTextButton = CycleButton.onOffBuilder(oldPlain)
                .displayOnlyValue()
                .create(boxX, y, Math.min(100, boxW), 18,
                        Component.literal("禁 Markdown"), (btn, val) -> {});
        addRenderableWidget(plainTextButton);
        y += rowH;

        // 开关行2：允许执行指令
        addLabel(y, labelW, "执行指令");
        allowCmdButton = CycleButton.onOffBuilder(oldCmd)
                .displayOnlyValue()
                .create(boxX, y, Math.min(100, boxW), 18,
                        Component.literal("AI 可执行指令"), (btn, val) -> {});
        addRenderableWidget(allowCmdButton);
        y += rowH + 6;

        // 按钮行
        int btnW = Math.min(110, (width - 30) / 3);
        int btnX = 10;
        addRenderableWidget(Button.builder(Component.literal("保存并应用"), b -> saveAndClose())
                .bounds(btnX, y, btnW, 20).build());
        btnX += btnW + 5;
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> resetFields())
                .bounds(btnX, y, btnW, 20).build());
        btnX += btnW + 5;
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(btnX, y, btnW, 20).build());
    }

    private EditBox box(int x, int y, int w, String value) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.literal(""));
        b.setValue(value);
        b.setMaxLength(512);
        addRenderableWidget(b); // 必须添加，否则输入框不显示
        return b;
    }

    private void addLabel(int y, int labelW, String text) {
        int w = Math.max(80, labelW);
        StringWidget label = new StringWidget(10, y + 2, w, 12,
                Component.literal(text), font);
        label.setMaxWidth(w); // maxWidth 默认 0，不设置文字会被裁剪
        addRenderableWidget(label);
    }

    /** 恢复默认值到输入框（不保存，需再点保存） */
    private void resetFields() {
        ModConfig def = new ModConfig();
        baseUrlBox.setValue(def.baseUrl);
        apiKeyBox.setValue(def.apiKey);
        modelBox.setValue(def.model);
        prefixBox.setValue(def.triggerPrefix);
        maxCharsBox.setValue(String.valueOf(def.maxReplyChars));
        maxTurnsBox.setValue(String.valueOf(def.maxToolTurns));
        maxHistoryBox.setValue(String.valueOf(def.maxHistory));
        plainTextButton.setValue(def.plainTextOnly);
        allowCmdButton.setValue(def.allowCommands);
    }

    private void saveAndClose() {
        cfg.baseUrl = baseUrlBox.getValue().trim();
        cfg.apiKey = apiKeyBox.getValue().trim();
        cfg.model = modelBox.getValue().trim();
        cfg.triggerPrefix = prefixBox.getValue().trim();
        cfg.maxReplyChars = parseInt(maxCharsBox, 400);
        cfg.maxToolTurns = parseInt(maxTurnsBox, 4);
        cfg.maxHistory = parseInt(maxHistoryBox, 20);
        cfg.plainTextOnly = plainTextButton.getValue();
        cfg.allowCommands = allowCmdButton.getValue();
        cfg.save();
        AIChatMod.LOGGER.info("[aichat] 配置已保存到 config/aichat.json");
        onClose();
    }

    private int parseInt(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(null);
    }
}
