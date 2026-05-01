package com.sidezbros.double_hotbar.mixin;

import com.sidezbros.double_hotbar.DHModConfig;
import com.sidezbros.double_hotbar.DoubleHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    /**
     * 在鼠标滚轮事件处理的最开始注入，接管原版的快捷栏切换行为。
     * 当游戏窗口聚焦且没有打开任何 GUI 时（即正常游戏状态），
     * 将滚动事件转交给我们的自定义滚轮逻辑，并取消原版处理。
     */
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // 只处理当前游戏窗口的鼠标事件，并且确保玩家存在且没有打开其他界面
        if (DHModConfig.INSTANCE.quadHotbar && 
            !DHModConfig.INSTANCE.disableMod && 
            window == client.getWindow().getHandle() && 
            client.currentScreen == null && 
            client.player != null) {
            // 将垂直滚动量（正为向上滚，负为向下滚）传入核心逻辑
            DoubleHotbar.onMouseScroll(vertical);
            // 取消原版的快捷栏切换，完全由我们接管
            ci.cancel();
        }
    }
}