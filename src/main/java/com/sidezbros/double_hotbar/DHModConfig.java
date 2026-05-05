package com.sidezbros.double_hotbar;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

@Config(name = "double_hotbar")
public class DHModConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static DHModConfig INSTANCE;

    public static void init() {
        AutoConfig.register(DHModConfig.class, JanksonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(DHModConfig.class).getConfig();
    }

    // ========== 全局开关 ==========
    public boolean disableMod = false;

    // ========== 四行模式（2×2）专属 ==========
    @ConfigEntry.Gui.Tooltip
    public boolean quadHotbar = true;               // 启用 2×2 四行快捷栏；关闭后恢复原版双行模式

    // ========== 原版双行模式设置（quadHotbar=false 时有效） ==========
    @ConfigEntry.BoundedDiscrete(min = 1, max = 3)
    public int inventoryRow = 3;                    // 额外显示的行（1=背包第一行，3=背包最后一行）

    @ConfigEntry.BoundedDiscrete(min = 0, max = 22)
    public int renderCrop = 0;                      // 裁剪底部像素（双行模式用）

    public boolean reverseBars = false;             // 交换底部两行与状态栏的位置

    // ========== 按键与交互 ==========
    public boolean holdToSwap = true;               // 启用按住 R 键交换
    public boolean holdToSwapBar = false;           // 长按 R 时交换整行
    public boolean allowDoubleTap = true;           // 允许双击数字键交换单格
    public boolean useSwapForHotbar = true;         // 对涉及快捷栏的交换使用 SWAP（修复服务器空位失败），关闭则用原版 PICKUP

    @ConfigEntry.BoundedDiscrete(min = 50, max = 1000)
    public int holdTime = 200;                      // 长按判定时间（毫秒）

    @ConfigEntry.BoundedDiscrete(min = 150, max = 600)
    public int doubleTapWindow = 300;               // 双击判定窗口（毫秒）

    // ========== 视觉 ==========
    public boolean displayDoubleHotbar = true;     // 是否显示额外的快捷栏（双行或四行都受此控制）

    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int shift = 0;                          // 整个快捷栏区域向上移动的像素数

    // 手持物品名称最大上移量（相对状态栏底部的像素数），防止与状态栏重叠
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int tooltipOffset = 40;

    // ========== 音效 ==========
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int wooshVolume = 100;                   // 交换音效音量
}