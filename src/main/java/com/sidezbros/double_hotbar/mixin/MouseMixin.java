package com.sidezbros.double_hotbar.mixin;

import com.sidezbros.double_hotbar.DHModConfig;
import com.sidezbros.double_hotbar.DoubleHotbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

	/**
	 * Inject at the very beginning of mouse scroll event handling, taking over vanilla hotbar switching.
	 * When the game window is focused and no GUI is open (i.e. normal gameplay),
	 * redirect scroll events to our custom scroll logic and cancel vanilla handling.
	 */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		// Only handle events when player exists and no other screen is open
		// (The window == client.getWindow().window check is implicit — Minecraft only has one window)
		if (DHModConfig.INSTANCE.quadHotbar &&
			!DHModConfig.INSTANCE.disableMod &&
			client.screen == null &&
			client.player != null) {
			// Pass vertical scroll amount (positive = up, negative = down) to core logic
			DoubleHotbar.onMouseScroll(vertical);
			// Cancel vanilla hotbar switching, fully handled by us
			ci.cancel();
		}
	}
}