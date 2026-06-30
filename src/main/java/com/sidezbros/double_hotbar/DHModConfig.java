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

	// ========== Global toggle ==========
	public boolean disableMod = false;

	// ========== Quad mode (2×2) exclusive ==========
	@ConfigEntry.Gui.Tooltip
	public boolean quadHotbar = true;               // Enable 2×2 quad hotbar layout

	// ========== Double-row mode settings (effective when quadHotbar=false) ==========
	@ConfigEntry.BoundedDiscrete(min = 1, max = 3)
	public int inventoryRow = 3;                    // Which inventory row to display as second hotbar

	@ConfigEntry.BoundedDiscrete(min = 0, max = 22)
	public int renderCrop = 0;                      // Crop pixels from second hotbar frame

	public boolean reverseBars = false;             // Swap main hotbar and second hotbar positions

	// ========== Key bindings & interaction ==========
	public boolean holdToSwap = true;               // Enable hold-to-swap mode
	public boolean holdToSwapBar = false;           // Long-press R to swap entire row
	public boolean allowDoubleTap = true;           // Enable double-tap number key swap
	public boolean useSwapForHotbar = true;         // Use SWAP for hotbar exchanges (fixes server empty-slot failures)

	@ConfigEntry.BoundedDiscrete(min = 50, max = 1000)
	public int holdTime = 200;                      // Hold threshold (ms)

	@ConfigEntry.BoundedDiscrete(min = 150, max = 600)
	public int doubleTapWindow = 300;               // Double-tap window (ms)

	// ========== Visual ==========
	public boolean displayDoubleHotbar = true;      // Show extra hotbars (quad or double)

	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int shift = 0;                           // Pixel shift for entire hotbar area

	// Maximum upward offset for held item tooltip (prevents overlap with status bars)
	@ConfigEntry.BoundedDiscrete(min = 0, max = 200)
	public int tooltipOffset = 40;

	// ========== Sound ==========
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	public int wooshVolume = 100;                   // Swap sound volume

	// ========== Debug ==========
	public boolean debugMode = false;               // Show slot numbers on container screens + slot swap tool
}