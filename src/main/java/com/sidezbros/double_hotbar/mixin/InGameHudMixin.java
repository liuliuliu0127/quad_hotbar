package com.sidezbros.double_hotbar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sidezbros.double_hotbar.DHModConfig;
import com.sidezbros.double_hotbar.DoubleHotbar;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

	private static final Logger LOGGER = LoggerFactory.getLogger("double_hotbar");

	@Shadow @Final private static Identifier HOTBAR_SPRITE;
	@Shadow private Player getCameraPlayer() { throw new AssertionError(); }
	@Shadow private void extractSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker tickCounter, Player player, ItemStack stack, int seed) { throw new AssertionError(); }
	@Shadow @Final private static Identifier HOTBAR_SELECTION_SPRITE;
	@Shadow @Final private static Identifier HOTBAR_OFFHAND_LEFT_SPRITE;
	@Shadow @Final private static Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE;
	@Shadow @Final private static Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE;

	// ==================== HEAD: Cancel vanilla, render everything ourselves ====================

	@Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
	private void onExtractItemHotbarHead(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo ci) {
		try {
			if (DHModConfig.INSTANCE.disableMod) return;

			if (DHModConfig.INSTANCE.displayDoubleHotbar) {
				if (DHModConfig.INSTANCE.quadHotbar) {
					renderQuadHotbar(graphics, tickCounter);
				} else {
					renderDoubleHotbar(graphics, tickCounter);
				}
				ci.cancel();
			}
		} catch (Exception e) {
			LOGGER.error("[QuadHotbar] Error in hotbar render", e);
		}
	}

	// ==================== Quad mode 2×2 rendering ====================

	private void renderQuadHotbar(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Player player = getCameraPlayer();
		if (player == null) return;

		boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();

		int totalWidth = 181 * 2;
		int leftX = screenWidth / 2 - totalWidth / 2;
		int rightX = leftX + 181;

		int bottomY = screenHeight - 21 - DHModConfig.INSTANCE.shift;
		int topY = bottomY - 21;

		int realHotbarY = DHModConfig.INSTANCE.reverseBars ? topY : bottomY;
		int extraY = DHModConfig.INSTANCE.reverseBars ? bottomY : topY;

		renderHotbarRow(graphics, tickCounter, player, 18, leftX, extraY, false, -1);
		renderHotbarRow(graphics, tickCounter, player, 9, rightX, extraY, false, -1);

		boolean leftReal = DoubleHotbar.leftIsRealHotbar;
		int highlightSlot = DoubleHotbar.extendedHotbarSlot % 9;

		int bottomLeftStart = leftReal ? 0 : 27;
		int bottomRightStart = leftReal ? 27 : 0;

		renderHotbarRow(graphics, tickCounter, player, bottomLeftStart, leftX, realHotbarY, leftReal, highlightSlot);
		renderHotbarRow(graphics, tickCounter, player, bottomRightStart, rightX, realHotbarY, !leftReal, highlightSlot);

		// Quad offhand: leftX-29 or rightX+182+9
		// Offhand: right-handed→LEFT(leftX-29), left-handed→RIGHT(rightX+182+9)
		renderOffhand(graphics, tickCounter, player, leftHanded,
				leftX - 29, rightX + 182 + 9, realHotbarY);
		// Attack indicator: right-handed→RIGHT(rightX+182+6), left-handed→LEFT(leftX-26)
		renderAttackIndicator(graphics, player, leftHanded,
				rightX + 182 + 6, leftX - 26, realHotbarY);
	}

	// ==================== Double mode rendering ====================

	private void renderDoubleHotbar(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Player player = getCameraPlayer();
		if (player == null) return;

		int screenWidth = graphics.guiWidth();
		int screenHeight = graphics.guiHeight();
		int centerX = screenWidth / 2 - 91;
		int shiftY = 21 + DHModConfig.INSTANCE.shift;

		int mainBarY, extraBarY;
		if (DHModConfig.INSTANCE.reverseBars) {
			mainBarY = screenHeight - 22 - shiftY;        // top
			extraBarY = screenHeight - 22 - DHModConfig.INSTANCE.shift; // bottom
		} else {
			mainBarY = screenHeight - 22 - DHModConfig.INSTANCE.shift;  // bottom
			extraBarY = screenHeight - 22 - shiftY;                      // top
		}

		// Main row with selector highlight
		int selectedSlot = player.getInventory().getSelectedSlot();
		renderHotbarRow(graphics, tickCounter, player, 0, centerX, mainBarY, true, selectedSlot);

		// Extra row (inventory)
		int extraStart = DHModConfig.INSTANCE.inventoryRow * 9;
		renderHotbarRow(graphics, tickCounter, player, extraStart, centerX, extraBarY, false, -1);
		if (DHModConfig.INSTANCE.renderCrop > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0,
					centerX, extraBarY, 182, 22 - DHModConfig.INSTANCE.renderCrop);
		}

		// Offhand + attack indicator positioned relative to the single centered bar
		boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
		// Offhand: right-handed→LEFT(centerX-29), left-handed→RIGHT(centerX+182+9)
		renderOffhand(graphics, tickCounter, player, leftHanded,
				centerX - 29, centerX + 182 + 9, mainBarY);
		// Attack indicator: right-handed→RIGHT(centerX+182+6), left-handed→LEFT(centerX-26)
		renderAttackIndicator(graphics, player, leftHanded,
				centerX + 182 + 6, centerX - 26, mainBarY);
	}

	// ==================== Shared helpers ====================

	private void renderHotbarRow(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, Player player,
	                             int startIndex, int x, int y, boolean isActiveRow, int highlightSlot) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, x, y, 182, 22);

		int seed = 1;
		for (int slot = 0; slot < 9; slot++) {
			int itemX = x + slot * 20 + 3;
			int itemY = y + 3;
			ItemStack stack = player.getInventory().getItem(startIndex + slot);
			if (!stack.isEmpty()) {
				this.extractSlot(graphics, itemX, itemY, tickCounter, player, stack, seed++);
			}
		}

		if (isActiveRow && highlightSlot >= 0 && highlightSlot < 9) {
			int selX = x + highlightSlot * 20 - 1;
			int selY = y - 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, 24, 23, 0, 0, selX, selY, 24, 23);
		}
	}

	private void renderOffhand(GuiGraphicsExtractor graphics, DeltaTracker tickCounter,
	                           Player player, boolean leftHanded, int rightHandX, int leftHandX, int barY) {
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) {
			int offhandX = leftHanded ? leftHandX : rightHandX;
			int offhandY = barY + 5;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE,
					29, 24, 0, 0, offhandX - 1, offhandY - 6, 29, 24);
			this.extractSlot(graphics, offhandX + 2, offhandY - 2, tickCounter, player, offhand, 0);
		}
	}

	private void renderAttackIndicator(GuiGraphicsExtractor graphics, Player player,
	                                   boolean leftHanded, int rightHandX, int leftHandX, int barY) {
		Minecraft client = Minecraft.getInstance();
		if (client.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			float cooldown = player.getAttackStrengthScale(0.0F);
			if (cooldown < 1.0F) {
				int indicatorX = leftHanded ? leftHandX : rightHandX;
				int indicatorY = barY + 3;
				int h = (int)(cooldown * 19.0F);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE,
						18, 18, 0, 0, indicatorX, indicatorY, 18, 18);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE,
						18, 18, 0, 18 - h, indicatorX, indicatorY + 18 - h, 18, h);
			}
		}
	}

	// ==================== Status bar shifting ====================

	@Inject(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;canHurtPlayer()Z"))
	public void shiftStatusBars(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo info) {
		try {
			if (!DHModConfig.INSTANCE.displayDoubleHotbar || DHModConfig.INSTANCE.disableMod) return;
			if (getCameraPlayer() == null || getCameraPlayer().isSpectator()) return;

			int offset;
			if (DHModConfig.INSTANCE.quadHotbar) {
				offset = DHModConfig.INSTANCE.shift + 22;
			} else {
				offset = 21 + DHModConfig.INSTANCE.shift;
			}
			graphics.pose().translate(0, -offset);
		} catch (Exception e) {
			LOGGER.error("[QuadHotbar] Error in status bar shift", e);
		}
	}

	@Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
	public void returnStatusBars(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo info) {
		try {
			if (!DHModConfig.INSTANCE.displayDoubleHotbar || DHModConfig.INSTANCE.disableMod) return;
			if (getCameraPlayer() == null || getCameraPlayer().isSpectator()) return;

			int offset;
			if (DHModConfig.INSTANCE.quadHotbar) {
				offset = DHModConfig.INSTANCE.shift + 22;
			} else {
				offset = 21 + DHModConfig.INSTANCE.shift;
			}
			graphics.pose().translate(0, offset);
		} catch (Exception e) {
			LOGGER.error("[QuadHotbar] Error in status bar return", e);
		}
	}

	// ==================== Held-item tooltip auto-shift ====================
	// Exact source @ModifyArg logic. MC 26.1.2 draw call is:
	//   graphics.textWithBackdrop(font, str, x, y, strWidth, color)
	//   Vanilla y = guiHeight - 59 (or +14 without status bars)

	@ModifyArg(method = "extractSelectedItemName",
		at = @At(value = "INVOKE",
				target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
		index = 3)
	private int fixTooltipYForStatusBars(int originalY) {
		if (DHModConfig.INSTANCE.disableMod) return originalY;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gameMode == null) return originalY;
		if (!client.gameMode.canHurtPlayer()) return originalY;

		Player player = client.player;
		int scaledHeight = client.getWindow().getGuiScaledHeight();

		int barBottomY = scaledHeight - 39;

		float maxHealth = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
		float absorption = player.getAbsorptionAmount();
		int healthRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int lineHeight = Math.max(10 - (healthRows - 2), 3);
		int healthTop = barBottomY - (healthRows - 1) * lineHeight;

		int armor = player.getArmorValue();
		int statusTop = (armor > 0) ? (healthTop - 10) : healthTop;

		int tooltipBottom = originalY + 9;
		if (tooltipBottom > statusTop) {
			int newY = statusTop - 9 - 2;
			int minY = barBottomY - DHModConfig.INSTANCE.tooltipOffset;
			if (newY < minY) {
				newY = minY;
			}
			return newY;
		}
		return originalY;
	}
}