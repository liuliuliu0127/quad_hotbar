package com.sidezbros.double_hotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class DoubleHotbar implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("double_hotbar");

    // --- 四行模式核心状态 ---
    /** 虚拟槽位 0~17：0~8 表示左下是真实快捷栏，9~17 表示右下是真实快捷栏 */
    public static int extendedHotbarSlot = 0;
    /** true 表示左侧(0~8)是当前玩家的快捷栏，false 表示右侧(27~35)是快捷栏 */
    public static boolean leftIsRealHotbar = true;

    // 防快速滚动跨边界交换的计时
    private static long lastBorderSwapTime = 0;
    private static final long BORDER_SWAP_COOLDOWN = 250; // 毫秒

    // --- 原模组保留字段 ---
    private static KeyBinding keyBinding;
    private boolean[] hotbarKeys = new boolean[10];
    private boolean[] quadCtrlState = new boolean[9];
    private long[] timer = new long[10];
    private boolean alreadySwapped = false;
    // --- 新增 ---
    private boolean ctrlWhenRPressed = false;  // 记录按下 R 时是否按着 Ctrl

    public static final Identifier WOOSH_SOUND_ID = Identifier.of("double_hotbar", "woosh");
    private static final KeyBinding.Category KEYBIND_CATEGORY = KeyBinding.Category.create(Identifier.of("double_hotbar", "keybinds"));
    public static SoundEvent WOOSH_SOUND_EVENT = SoundEvent.of(WOOSH_SOUND_ID);

    @Override
    public void onInitializeClient() {
        DHModConfig.init();
        Registry.register(Registries.SOUND_EVENT, WOOSH_SOUND_ID, WOOSH_SOUND_EVENT);

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.double_hotbar.swap", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R, KEYBIND_CATEGORY));

        // 每一 tick 处理按键和滚轮（滚轮逻辑已移至 MouseMixin，这里不再处理）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DHModConfig.INSTANCE.disableMod) return;
            if (DHModConfig.INSTANCE.quadHotbar) {
                // ========== 四行模式 ==========
                if (DHModConfig.INSTANCE.holdToSwap) {
                    boolean ctrlDown = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                            || GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

                    if (keyBinding.isPressed() != this.hotbarKeys[9]) {
                        this.hotbarKeys[9] = keyBinding.isPressed();
                        if (keyBinding.isPressed()) {
                            timer[9] = Instant.now().toEpochMilli();
                            ctrlWhenRPressed = ctrlDown;   // 记录按下时的 Ctrl 状态
                        } else {
                            if (Instant.now().toEpochMilli() - timer[9] < DHModConfig.INSTANCE.holdTime) {
                                // 短按
                                if (ctrlWhenRPressed) {
                                    performCtrlShortSwap();
                                } else {
                                    // 原有短按逻辑
                                    if (DHModConfig.INSTANCE.holdToSwapBar) {
                                        swapSelectedSlotWithAbove();
                                    } else {
                                        onSwapKeyPressed();
                                    }
                                }
                            } else {
                                this.alreadySwapped = false;
                            }
                        }
                    }
                    if (!this.alreadySwapped && keyBinding.isPressed()
                            && Instant.now().toEpochMilli() - timer[9] > DHModConfig.INSTANCE.holdTime) {
                        // 长按
                        if (ctrlWhenRPressed) {
                            performCtrlLongSwap();
                        } else {
                            // 原有长按逻辑
                            if (DHModConfig.INSTANCE.holdToSwapBar) {
                                onSwapKeyPressed();
                            } else {
                                swapSelectedSlotWithAbove();
                            }
                        }
                        this.alreadySwapped = true;
                    }
                } else {
                    while (keyBinding.wasPressed()) {
                        boolean ctrlDown = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                                || GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                        if (ctrlDown) {
                            performCtrlShortSwap();
                        } else {
                            onSwapKeyPressed();
                        }
                    }
                }
                // 双击数字键（四行模式用 swapSlotWithRowBelow）
                handleQuadHotbarKeybinds(client);
                //if (client.player != null) {
                //    client.player.getInventory().setSelectedSlot(extendedHotbarSlot % 9);
                //}
                // 同步外部 selectedSlot 变化（服务器/其他模组）
                if (client.player != null) {
                    int realSlot = client.player.getInventory().getSelectedSlot();
                    int expectedSlot = extendedHotbarSlot % 9;
                    if (realSlot != expectedSlot) {
                        // 外部修改了选中栏，更新虚拟槽位（保持当前左右侧不变，不触发交换）
                        extendedHotbarSlot = (leftIsRealHotbar ? 0 : 9) + realSlot;
                    }
                }
            } else {
                // ========== 双行模式（原版逻辑，完全复原） ==========
                if (DHModConfig.INSTANCE.holdToSwap) {
                    if (keyBinding.isPressed() != this.hotbarKeys[9]) {
                        this.hotbarKeys[9] = keyBinding.isPressed();
                        if (keyBinding.isPressed()) {
                            timer[9] = Instant.now().toEpochMilli();
                        } else {
                            if (Instant.now().toEpochMilli() - timer[9] < DHModConfig.INSTANCE.holdTime) {
                                this.swapStack(client.player, !DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
                            } else {
                                this.alreadySwapped = false;
                            }
                        }
                    }
                    if (!this.alreadySwapped && keyBinding.isPressed()
                            && Instant.now().toEpochMilli() - timer[9] > DHModConfig.INSTANCE.holdTime) {
                        this.swapStack(client.player, DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
                        this.alreadySwapped = true;
                    }
                } else {
                    while (keyBinding.wasPressed()) {
                        this.swapStack(client.player, true, 0);
                    }
                }
                if (DHModConfig.INSTANCE.allowDoubleTap) {
                    for (int i = 0; i < 9; i++) {
                        if (client.options.hotbarKeys[i].isPressed() != this.hotbarKeys[i]) {
                            this.hotbarKeys[i] = client.options.hotbarKeys[i].isPressed();
                            if (client.options.hotbarKeys[i].isPressed()) {
                                if (Instant.now().toEpochMilli() - timer[i] < DHModConfig.INSTANCE.doubleTapWindow) {
                                    this.swapStack(client.player, false, i);
                                    timer[i] = 0;
                                } else {
                                    timer[i] = Instant.now().toEpochMilli();
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    // ==================== 双行模式交换（原版逻辑） ====================
    public void swapStack(PlayerEntity player, boolean fullRow, int slot) {
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        int inventoryRow = DHModConfig.INSTANCE.inventoryRow * 9;
        boolean playSound = false;

        if (interactionManager == null || DHModConfig.INSTANCE.disableMod) {
            return;
        }

        if (fullRow) {
            for (int i = 0; i < 9; i++) {
                if(!player.getInventory().getStack(i).equals(player.getInventory().getStack(inventoryRow + i))) {
                    interactionManager.clickSlot(player.playerScreenHandler.syncId, inventoryRow + i, i, SlotActionType.SWAP, player);
                    playSound = true;
                }
            }
        } else if(!player.getInventory().getStack(slot).equals(player.getInventory().getStack(inventoryRow + slot))) {
            interactionManager.clickSlot(player.playerScreenHandler.syncId, inventoryRow + slot, slot, SlotActionType.SWAP, player);
            playSound = true;
        }

        if (playSound) {
            player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
        }
    }

    // ==================== 核心交换逻辑 ====================

    /**
     * 由滚轮事件调用（在 MouseMixin 中）。
     * @param amount 滚动量：正数向上滚，负数向下滚
     */
    public static void onMouseScroll(double amount) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || DHModConfig.INSTANCE.disableMod||!DHModConfig.INSTANCE.quadHotbar) return;

        int oldSlot = extendedHotbarSlot;
        // 上滚增加槽位，下滚减少槽位（符合多数玩家习惯）
        int newSlot = oldSlot - (int) Math.signum(amount);
        if (newSlot < 0) newSlot = 17;
        if (newSlot > 17) newSlot = 0;

        boolean oldLeft = oldSlot < 9;
        boolean newLeft = newSlot < 9;

        // 跨边界时需要整行交换
        if (oldLeft != newLeft) {
            long now = System.currentTimeMillis();
            if (now - lastBorderSwapTime < BORDER_SWAP_COOLDOWN) {
                // 冷却中：不处理本次滚动，保持槽位不变
                return;
            }
            // 交换快捷栏行 (0~8) 与背包最后一行 (27~35)
            swapTwoEntireRows(0, 27);
            leftIsRealHotbar = newLeft;
            lastBorderSwapTime = now;
        }

        extendedHotbarSlot = newSlot;
        // 同步原版 selectedSlot
        client.player.getInventory().setSelectedSlot(newSlot % 9);
    }

    /**
     * R 键按下时调用，根据当前快捷栏位置执行对应的四行交换。
     */
    private static void onSwapKeyPressed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (leftIsRealHotbar) {
            // 左真实：交换 左下(0~8) ↔ 左上(18~26)，同时 右下(27~35) ↔ 右上(9~17)
            swapTwoEntireRows(0, 18);
            swapTwoEntireRows(27, 9);
        } else {
            // 右真实：交换 右下(27~35) ↔ 右上(9~17)，同时 左下(0~8) ↔ 左上(18~26)
            swapTwoEntireRows(0, 9);
            swapTwoEntireRows(27, 18);
        }
        if (client.player != null) {
            client.player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
        }
    }

    /**
     * 四行模式：长按R时，将当前选中的槽位与上方对应的槽位交换。
     * 左真实：左下(i) ↔ 左上(i+18)
     * 右真实：右下(i) ↔ 右上(i+9)
     */
    private void swapSelectedSlotWithAbove() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int selected = extendedHotbarSlot % 9;
        int sourceSlot, targetSlot;
        if (leftIsRealHotbar) {
            sourceSlot = selected;          // 左下0~8
            targetSlot = selected + 18;     // 左上18~26
        } else {
            sourceSlot = selected;
            targetSlot = selected + 9;      // 右上9~17
        }
        swapSlots(sourceSlot, targetSlot);
        client.player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
    }

        /**
     * Ctrl + 短按 R：只交换真实快捷栏整行与对应的背包行
     * 左真实：左下(0~8) ↔ 左上(18~26)
     * 右真实：右下(0~8) ↔ 右上(9~17)
     */
    private void performCtrlShortSwap() {
        if (leftIsRealHotbar) {
            swapTwoEntireRows(0, 18);
        } else {
            swapTwoEntireRows(0, 9);
        }
        MinecraftClient.getInstance().player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
    }

    /**
     * Ctrl + 长按 R：只交换背包内部的两整行
     * 左真实：最底层(27~35) ↔ 最顶层(9~17)
     * 右真实：最底层(27~35) ↔ 中间层(18~26)
     */
    private void performCtrlLongSwap() {
        if (leftIsRealHotbar) {
            swapTwoEntireRows(27, 9);
        } else {
            swapTwoEntireRows(27, 18);
        }
        MinecraftClient.getInstance().player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
    }

    private void handleQuadHotbarKeybinds(MinecraftClient client) {
        if (client.player == null) return;
        long windowHandle = client.getWindow().getHandle();
        boolean ctrlDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
        || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        for (int i = 0; i < 9; i++) {
            boolean isPressed = client.options.hotbarKeys[i].isPressed();
            if (isPressed != hotbarKeys[i]) {
                hotbarKeys[i] = isPressed;
                if (isPressed) {
                    long now = Instant.now().toEpochMilli();
                    if (now - timer[i] < DHModConfig.INSTANCE.doubleTapWindow && ctrlDown == quadCtrlState[i]) {
                        selectSlot(i, ctrlDown, true);  // 双击
                    } else {
                        selectSlot(i, ctrlDown, false); // 单击
                    }
                    timer[i] = now;
                    quadCtrlState[i] = ctrlDown;
                }
            }
        }
    }

    private void selectSlot(int keyIndex, boolean ctrl, boolean doubleClick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int targetSlot = ctrl ? keyIndex + 9 : keyIndex;
        boolean oldLeft = extendedHotbarSlot < 9;
        boolean newLeft = targetSlot < 9;

        if (oldLeft != newLeft) {
            long now = System.currentTimeMillis();
            if (now - lastBorderSwapTime >= BORDER_SWAP_COOLDOWN) {
                swapTwoEntireRows(0, 27);
                leftIsRealHotbar = newLeft;
                lastBorderSwapTime = now;
                //client.player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
            } else {
                return; // 冷却中无效
            }
        }

        extendedHotbarSlot = targetSlot;
        client.player.getInventory().setSelectedSlot(targetSlot % 9);

        if (doubleClick) {
            swapSelectedSlotWithAbove(); // 双击额外交换
        }
    }


    // ==================== 底层槽位操作 ====================

    /**
     * 无条件交换整行物品（包括空位），用于跨边界和R键交换。
     * @param rowAStart 物品栏索引起始（0, 9, 18, 27）
     * @param rowBStart 物品栏索引起始
     */
    private static void swapTwoEntireRows(int rowAStart, int rowBStart) {
        for (int i = 0; i < 9; i++) {
            swapSlots(rowAStart + i, rowBStart + i);
        }
    }

    /**
     * 交换两个物品栏槽位（通过三次 PICKUP 操作，安全且不依赖快捷栏关系）。
     * @param slotA 物品栏索引（0~35）
     * @param slotB 物品栏索引（0~35）
     */
    private static void swapSlots(int slotA, int slotB) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null||client.interactionManager == null || client.player == null) return;
        ScreenHandler handler = client.player.playerScreenHandler;
        int syncId = handler.syncId;

        int screenSlotA = inventoryIndexToScreenSlot(slotA);
        int screenSlotB = inventoryIndexToScreenSlot(slotB);

        // 如果启用了 SWAP，且一方是快捷栏（0~8），另一方是背包（9~35），则使用 SWAP
        if (DHModConfig.INSTANCE.useSwapForHotbar) {
            boolean aIsHotbar = (slotA >= 0 && slotA <= 8);
            boolean bIsHotbar = (slotB >= 0 && slotB <= 8);
            if (aIsHotbar != bIsHotbar) { // 一方快捷栏，一方背包
                int hotbarSlot = aIsHotbar ? slotA : slotB;
                //int inventorySlot = aIsHotbar ? slotB : slotA;
                int screenInventorySlot = aIsHotbar ? screenSlotB : screenSlotA;
                // SWAP：点击背包槽位，热键栏编号 = hotbarSlot
                client.interactionManager.clickSlot(syncId, screenInventorySlot, hotbarSlot, SlotActionType.SWAP, client.player);
                return;
            }
        }

        // 标准的三次点击交换
        client.interactionManager.clickSlot(syncId, screenSlotA, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, screenSlotB, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, screenSlotA, 0, SlotActionType.PICKUP, client.player);
    }

    /**
     * 将玩家物品栏索引（0~35）转换为 playerScreenHandler 中的槽位 ID。
     */
    private static int inventoryIndexToScreenSlot(int invIndex) {
        // 0~8  -> 36~44 (快捷栏)
        // 9~35 -> 9~35 (主背包)
        if (invIndex < 9) {
            return 36 + invIndex;  // 快捷栏槽位
        } else {
            return invIndex;       // 主背包槽位
        }
    }

    // ==================== 保留原模组声音等 ====================
    // 如果需要播声效，可在 swapSlots 中加上判断，这里暂时略去以保持清晰。
}