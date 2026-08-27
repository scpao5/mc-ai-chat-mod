package com.aichat;

import com.aichat.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIChatMod implements ModInitializer {
    public static final String MOD_ID = "aichat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;

    public static ModConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {
        config = ModConfig.load();
        LOGGER.info("[aichat] AI Chat Assistant 已加载，触发前缀: \"{}\"，模型: {}", config.triggerPrefix, config.model);
    }
}
