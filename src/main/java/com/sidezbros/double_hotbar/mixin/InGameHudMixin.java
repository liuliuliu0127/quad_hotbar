package com.sidezbros.double_hotbar.mixin;

import com.sidezbros.double_hotbar.DHModConfig;
import com.sidezbros.double_hotbar.DoubleHotbar;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    private static Identifier HOTBAR_TEXTURE;
    @Shadow
    protected abstract PlayerEntity getCameraPlayer();
	@Shadow 
	private static Identifier HOTBAR_SELECTION_TEXTURE; // 新增
	@Shadow 
	private static Identifier HOTBAR_OFFHAND_LEFT_TEXTURE;
	@Shadow 
	private static Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE;
	@Shadow 
	private static Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_TEXTURE;
    @Shadow
    protected abstract void renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed);

	private boolean reverseShifted = false;

    // ============= 四行模式渲染接管 =============
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (DHModConfig.INSTANCE.displayDoubleHotbar
            && !DHModConfig.INSTANCE.disableMod
            && DHModConfig.INSTANCE.quadHotbar) {
			renderQuadHotbar(context, tickCounter);
			ci.cancel();
		}
	}

	/**
	 * 绘制 2×2 四行快捷栏
	 */
	private void renderQuadHotbar(DrawContext context, RenderTickCounter tickCounter) {
		PlayerEntity player = getCameraPlayer();
		if (player == null||DHModConfig.INSTANCE.disableMod) return;

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();

		// 两列紧贴，无缝隙
		int totalWidth = 181 * 2;
		int leftX = screenWidth / 2 - totalWidth / 2;
		int rightX = leftX + 181;

		int bottomY = screenHeight - 21 - DHModConfig.INSTANCE.shift;
		int topY = bottomY - 21;

		int realHotbarY = bottomY;   // 默认在下面
		int extraY = topY;           // 额外行在上面
		if (DHModConfig.INSTANCE.reverseBars) {
			realHotbarY = topY;
			extraY = bottomY;
		}

		// 上半固定行
		renderHotbarRow(context, tickCounter, player, 18, leftX, extraY, false, -1);
		renderHotbarRow(context, tickCounter, player, 9, rightX, extraY, false, -1);

		// 下半动态行
		boolean leftReal = DoubleHotbar.leftIsRealHotbar;
		int highlightSlot = DoubleHotbar.extendedHotbarSlot % 9;

		int bottomLeftStart = leftReal ? 0 : 27;
		int bottomRightStart = leftReal ? 27 : 0;

		renderHotbarRow(context, tickCounter, player, bottomLeftStart, leftX, realHotbarY, leftReal, highlightSlot);
		renderHotbarRow(context, tickCounter, player, bottomRightStart, rightX, realHotbarY, !leftReal, highlightSlot);

		// ---- 副手物品绘制（带背景框） ----
		ItemStack offhand = player.getOffHandStack();
		if (!offhand.isEmpty()) {
			int offhandX = leftX - 29;      // 左列左侧 29 像素
			int offhandY = realHotbarY + 3;     // 与快捷栏物品垂直对齐

			// 1. 绘制副手槽位背景（24x23）
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_TEXTURE,
					offhandX - 1, offhandY - 3, 24, 23);

			// 2. 绘制副手物品
			renderHotbarItem(context, offhandX, offhandY, tickCounter, player, offhand, 0);
		}
		// ---- 攻击指示器（进度条模式）绘制 ----
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
			float cooldown = player.getAttackCooldownProgress(0.0F);
			if (cooldown < 1.0F) {
				int indicatorX = rightX + 182 + 6;  // 右快捷栏右边缘 + 间距
				int indicatorY = realHotbarY + 3;//context.getScaledWindowHeight() - 20 - DHModConfig.INSTANCE.shift;

				int progressHeight = (int)(cooldown * 19.0F);
				// 背景 (18x18)
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE,
						indicatorX, indicatorY, 18, 18);
				// 进度条 (从下往上)
				context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_TEXTURE,
						18, 18,                                      // 纹理尺寸
						0, 18 - progressHeight,                      // UV 起点
						indicatorX, indicatorY + 18 - progressHeight, // 屏幕绘制位置
						18, progressHeight);
			}
		}
	}

	private void renderHotbarRow(DrawContext context, RenderTickCounter tickCounter, PlayerEntity player,
								int startIndex, int x, int y, boolean isActiveRow, int highlightSlot) {
		// 绘制行背景（182×22）
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, x, y, 182, 22);

		int seed = 1;
		for (int slot = 0; slot < 9; slot++) {
			int itemX = x + slot * 20 + 3;
			int itemY = y + 3;
			ItemStack stack = player.getInventory().getStack(startIndex + slot);
			if (!stack.isEmpty()) {
				// 原版方法会自动绘制冷却遮罩，无需手动处理
				renderHotbarItem(context, itemX, itemY, tickCounter, player, stack, seed++);
				// --- 新增：攻击指示器进度条（仅活跃行的选中槽，且设置开启） ---
				if (isActiveRow && slot == highlightSlot) {
					MinecraftClient client = MinecraftClient.getInstance();
					if (client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
						float cooldown = player.getItemCooldownManager().getCooldownProgress(
								stack, tickCounter.getDynamicDeltaTicks());
						if (cooldown > 0.0F) {
							int coverHeight = (int) (16 * cooldown);
							int top = itemY + 16 - coverHeight;
							context.fill(itemX, top, itemX + 16, itemY + 16, 0x80FFFFFF);
						}
					}
				}
			}
		}

		// 高亮选择框（仅活跃行）
		if (isActiveRow && highlightSlot >= 0 && highlightSlot < 9) {
			int selX = x + highlightSlot * 20 - 1;
			int selY = y - 1;
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_TEXTURE, selX, selY, 24, 23);
		}
	}

    // ============= 状态栏位置调整 =============
    @Inject(method = "renderMainHud", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;hasStatusBars()Z"))
	public void shiftStatusBars(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (DHModConfig.INSTANCE.displayDoubleHotbar && !DHModConfig.INSTANCE.disableMod
				&& getCameraPlayer() != null && !getCameraPlayer().isSpectator()) {
			// 快捷栏向上移动了 shift 像素，同时快捷栏本身增高了 22 像素（多出一行），
			// 所以状态栏必须上移 shift + 22，才能避免重叠。
			int offset;
			if (DHModConfig.INSTANCE.quadHotbar) {
				offset = DHModConfig.INSTANCE.shift + 22;   // 四行
			} else {
				offset = 21 + DHModConfig.INSTANCE.shift;
			}
			context.getMatrices().translate(0, -offset);
		}
	}

    @Inject(method = "renderMainHud", at = @At("TAIL"))
	public void returnStatusBars(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (DHModConfig.INSTANCE.displayDoubleHotbar && !DHModConfig.INSTANCE.disableMod
				&& getCameraPlayer() != null && !getCameraPlayer().isSpectator()) {
			int offset;
			if (DHModConfig.INSTANCE.quadHotbar) {
				offset = DHModConfig.INSTANCE.shift + 22;   // 四行
			} else {
				offset = 21 + DHModConfig.INSTANCE.shift;
			}
			context.getMatrices().translate(0, offset);
		}
	}

	// ==================== 双行模式渲染（quadHotbar = false 时工作） ====================

	@Inject(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
	private void renderHotbarFrame(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (!DHModConfig.INSTANCE.quadHotbar && DHModConfig.INSTANCE.displayDoubleHotbar && !DHModConfig.INSTANCE.disableMod) {
			int shiftY = 21 + DHModConfig.INSTANCE.shift;
			int y = context.getScaledWindowHeight() - 22 - shiftY;
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
					context.getScaledWindowWidth() / 2 - 91,
					y,
					182, 22 - DHModConfig.INSTANCE.renderCrop);
		}
	}

	@Inject(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 1))
	private void shiftHotbarSelector(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (!DHModConfig.INSTANCE.quadHotbar && DHModConfig.INSTANCE.displayDoubleHotbar
				&& DHModConfig.INSTANCE.reverseBars && !DHModConfig.INSTANCE.disableMod) {
			context.getMatrices().translate(0, -21);
		}
	}

	@Inject(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0))
	private void returnHotbarSelector(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (!DHModConfig.INSTANCE.quadHotbar && DHModConfig.INSTANCE.displayDoubleHotbar
				&& DHModConfig.INSTANCE.reverseBars && !DHModConfig.INSTANCE.disableMod) {
			context.getMatrices().translate(0, 21);
		}
	}

	@Inject(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I", ordinal = 4))
	private void shiftHotbarItems(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (!DHModConfig.INSTANCE.quadHotbar && DHModConfig.INSTANCE.displayDoubleHotbar
				&& DHModConfig.INSTANCE.reverseBars && !DHModConfig.INSTANCE.disableMod && !reverseShifted) {
			context.getMatrices().translate(0, -21);
			reverseShifted = true;
		}
	}

	@Inject(method = "renderHotbar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 1))
	private void renderHotbarItems(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
		if (!DHModConfig.INSTANCE.quadHotbar && DHModConfig.INSTANCE.displayDoubleHotbar && !DHModConfig.INSTANCE.disableMod) {
			int shiftY = 21 + DHModConfig.INSTANCE.shift;
			// 临时恢复矩阵：画底部物品时禁用上移
			if (DHModConfig.INSTANCE.reverseBars) {
				context.getMatrices().translate(0, shiftY);
			}
			if (getCameraPlayer() != null) {
				int m = 1;
				for (int n2 = 0; n2 < 9; ++n2) {
					int o = context.getScaledWindowWidth() / 2 - 90 + n2 * 20 + 2;
					int p = context.getScaledWindowHeight() - 19 - (DHModConfig.INSTANCE.reverseBars ? 0 : shiftY);
					this.renderHotbarItem(context, o, p, tickCounter, getCameraPlayer(),
							getCameraPlayer().getInventory().getMainStacks().get(n2 + DHModConfig.INSTANCE.inventoryRow * 9), m++);
				}
			}
			if (DHModConfig.INSTANCE.reverseBars) {
				//context.getMatrices().translate(0, -shiftY);   // 恢复矩阵
				reverseShifted = false;
			}
		}
	}

	//------------------额外功能：修复原版MC状态栏工具提示有时可能和血条或护甲值重合的BUG-------------------------------------
	
	@ModifyArg(method = "renderHeldItemTooltip",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithBackground(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIII)V"),
    index = 3)
	private int fixTooltipYForStatusBars(int originalY) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.interactionManager == null) return originalY;
		if (!client.interactionManager.hasStatusBars()) return originalY;

		PlayerEntity player = client.player;
		int scaledHeight = client.getWindow().getScaledHeight();

		// 计算状态栏顶部（与原版逻辑严格一致）
		float maxHealth = (float) player.getAttributeValue(EntityAttributes.MAX_HEALTH);
		float absorption = player.getAbsorptionAmount();
		int healthRows = MathHelper.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int lineHeight = Math.max(10 - (healthRows - 2), 3);
		int baseY = scaledHeight - 39;
		int healthTop = baseY - (healthRows - 1) * lineHeight;

		int armor = player.getArmor();
		int statusTop = (armor > 0) ? (healthTop - 10) : healthTop;

		int tooltipBottom = originalY + 9;
		if (tooltipBottom > statusTop) {
			int newY = statusTop - 9 - 2;
			// 限制最高不得超过屏幕高度的 1/1.5（可根据需要调整分母 1.5 为其他数，如 1.3f）
			int minY = (int) (scaledHeight / 1.4f); 
			if (newY < minY) {
				newY = minY;
			}
			return newY;
		}
		return originalY;
	}
}