package com.sidezbros.double_hotbar.mixin;

import com.sidezbros.double_hotbar.DHModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected abstract AbstractContainerMenu getMenu();

	@Unique
	private EditBox slotAField;
	@Unique
	private EditBox slotBField;
	@Unique
	private Button swapButton;

	protected HandledScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		if (!DHModConfig.INSTANCE.debugMode) return;

		if (this.slotAField != null) removeWidget(this.slotAField);
		if (this.slotBField != null) removeWidget(this.slotBField);
		if (this.swapButton != null) removeWidget(this.swapButton);

		Minecraft client = Minecraft.getInstance();
		int inputWidth = 45;
		int inputHeight = 20;

		int startX = this.leftPos + 5;
		int startY = this.topPos - 25;
		if (client.gameMode != null &&
			client.gameMode.getPlayerMode() == GameType.CREATIVE) {
			startY -= 10;
		}

		this.slotAField = new EditBox(client.font, startX, startY, inputWidth, inputHeight, Component.empty());
		this.slotBField = new EditBox(client.font, startX + inputWidth + 5, startY, inputWidth, inputHeight, Component.empty());
		this.swapButton = Button.builder(Component.literal("Swap"), btn -> {
			try {
				int a = Integer.parseInt(slotAField.getValue());
				int b = Integer.parseInt(slotBField.getValue());
				swapScreenSlots(a, b);
			} catch (NumberFormatException ignored) {
			}
		}).bounds(startX + 2 * inputWidth + 10, startY, 40, inputHeight).build();

		addRenderableWidget(slotAField);
		addRenderableWidget(slotBField);
		addRenderableWidget(swapButton);

		// Give the first input field focus
		this.slotAField.setFocused(true);
	}

	@Unique
	private void swapScreenSlots(int screenSlotA, int screenSlotB) {
		Minecraft client = Minecraft.getInstance();
		if (client.gameMode == null || client.player == null) return;

		AbstractContainerMenu handler = getMenu();

		if (screenSlotA < 0 || screenSlotA >= handler.slots.size() ||
			screenSlotB < 0 || screenSlotB >= handler.slots.size()) {
			return;
		}
		client.gameMode.handleContainerInput(handler.containerId, screenSlotB, screenSlotA, ContainerInput.SWAP, client.player);
	}
}