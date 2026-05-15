package com.sidezbros.double_hotbar.mixin;

import com.sidezbros.double_hotbar.DHModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;

    @Unique
    private TextFieldWidget slotAField;
    @Unique
    private TextFieldWidget slotBField;
    @Unique
    private ButtonWidget swapButton;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!DHModConfig.INSTANCE.debugMode) return;

        if (this.slotAField != null) remove(this.slotAField);
        if (this.slotBField != null) remove(this.slotBField);
        if (this.swapButton != null) remove(this.swapButton);

        MinecraftClient client = MinecraftClient.getInstance();
        int inputWidth = 45;
        int inputHeight = 20;

        int startX = this.x + 5;
        int startY = this.y - 25;
        if (client.interactionManager != null &&
            client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            startY -= 10;
        }

        this.slotAField = new TextFieldWidget(client.textRenderer, startX, startY, inputWidth, inputHeight, Text.empty());
        this.slotBField = new TextFieldWidget(client.textRenderer, startX + inputWidth + 5, startY, inputWidth, inputHeight, Text.empty());
        this.swapButton = ButtonWidget.builder(Text.literal("Swap"), btn -> {
            try {
                int a = Integer.parseInt(slotAField.getText());
                int b = Integer.parseInt(slotBField.getText());
                swapScreenSlots(a, b);
            } catch (NumberFormatException ignored) {
            }
        }).dimensions(startX + 2 * inputWidth + 10, startY, 40, inputHeight).build();

        addDrawableChild(slotAField);
        addDrawableChild(slotBField);
        addDrawableChild(swapButton);

        // 让第一个输入框自动获得焦点（可点击）
        this.slotAField.setFocused(true);
    }

    @Unique
    private void swapScreenSlots(int screenSlotA, int screenSlotB) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) return;

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = self.getScreenHandler();

        if (screenSlotA < 0 || screenSlotA >= handler.slots.size() ||
            screenSlotB < 0 || screenSlotB >= handler.slots.size()) {
            return;
        }
        client.interactionManager.clickSlot(handler.syncId, screenSlotB, screenSlotA, SlotActionType.SWAP, client.player);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!DHModConfig.INSTANCE.debugMode) return;

        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        ScreenHandler handler = self.getScreenHandler();
        MinecraftClient client = MinecraftClient.getInstance();

        // 先画半透明背景
        for (Slot slot : handler.slots) {
            int screenX = this.x + slot.x;
            int screenY = this.y + slot.y;
            context.fill(screenX, screenY, screenX + 18, screenY + 18, 0x40000000);
        }

        // 再画数字
        for (Slot slot : handler.slots) {
            int slotId = slot.getIndex();
            int screenX = this.x + slot.x;
            int screenY = this.y + slot.y;

            String num = String.valueOf(slotId);
            int textWidth = client.textRenderer.getWidth(num);
            int textX = screenX + 9 - textWidth / 2;
            int textY = screenY + 6;

            // 直接绘制文字，不带阴影（可能会被物品覆盖，但背景已画）
            context.drawText(client.textRenderer, num, textX, textY, 0xFFFFFFFF, false);
        }
    }
}