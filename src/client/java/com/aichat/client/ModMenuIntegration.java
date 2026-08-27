package com.aichat.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 集成：在 Mod 列表中点"配置"即可打开配置界面。
 * 仅当玩家安装了 ModMenu 时才会被加载，是可选的。
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen();
    }
}
