package com.sidezbros.double_hotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import java.time.Instant;

public class DoubleHotbar implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("double_hotbar");

	// --- Quad mode core state ---
	/** Virtual slot 0~17: 0~8 means bottom-left is real hotbar, 9~17 means bottom-right is real hotbar */
	public static int extendedHotbarSlot = 0;
	/** true means left side (0~8) is the current player hotbar, false means right side (27~35) is the hotbar */
	public static boolean leftIsRealHotbar = true;

	// Cooldown to prevent rapid cross-border swap from scrolling
	private static long lastBorderSwapTime = 0;
	private static final long BORDER_SWAP_COOLDOWN = 250; // milliseconds

	// --- Original mod retained fields ---
	private static KeyMapping keyBinding;
	private static KeyMapping modifierKeyBinding;
	private boolean[] hotbarKeys = new boolean[10];
	private boolean[] quadCtrlState = new boolean[9];
	private long[] timer = new long[10];
	private boolean alreadySwapped = false;
	// --- New fields ---
	private boolean ctrlWhenRPressed = false;  // Records whether Ctrl was held when R was pressed

	public static final Identifier WOOSH_SOUND_ID = Identifier.fromNamespaceAndPath("double_hotbar", "woosh");
	private static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("double_hotbar", "keybinds"));
	public static SoundEvent WOOSH_SOUND_EVENT = SoundEvent.createVariableRangeEvent(WOOSH_SOUND_ID);

	@Override
	public void onInitializeClient() {
		DHModConfig.init();
		Registry.register(BuiltInRegistries.SOUND_EVENT, WOOSH_SOUND_ID, WOOSH_SOUND_EVENT);

		modifierKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.double_hotbar.modifier", InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_CONTROL, KEYBIND_CATEGORY));

		keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.double_hotbar.swap", InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R, KEYBIND_CATEGORY));

		// Process key presses each tick (scroll logic moved to MouseMixin)
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (DHModConfig.INSTANCE.disableMod) return;
			if (client.player == null) return;
			if (DHModConfig.INSTANCE.quadHotbar) {
				// ========== Quad mode ==========
				if (DHModConfig.INSTANCE.holdToSwap) {
					boolean ctrlDown = modifierKeyBinding.isDown();
					if (keyBinding.isDown() != this.hotbarKeys[9]) {
						this.hotbarKeys[9] = keyBinding.isDown();
						if (keyBinding.isDown()) {
							timer[9] = Instant.now().toEpochMilli();
							ctrlWhenRPressed = ctrlDown;   // Record Ctrl state on press
						} else {
							if (Instant.now().toEpochMilli() - timer[9] < DHModConfig.INSTANCE.holdTime) {
								// Short press
								if (ctrlWhenRPressed) {
									performCtrlShortSwap();
								} else {
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
					if (!this.alreadySwapped && keyBinding.isDown()
							&& Instant.now().toEpochMilli() - timer[9] > DHModConfig.INSTANCE.holdTime) {
						// Long press
						if (ctrlWhenRPressed) {
							performCtrlLongSwap();
						} else {
							if (DHModConfig.INSTANCE.holdToSwapBar) {
								onSwapKeyPressed();
							} else {
								swapSelectedSlotWithAbove();
							}
						}
						this.alreadySwapped = true;
					}
				} else {
					while (keyBinding.consumeClick()) {
						boolean ctrlDown = modifierKeyBinding.isDown();
						if (ctrlDown) {
							performCtrlShortSwap();
						} else {
							onSwapKeyPressed();
						}
					}
				}
				// Double-tap number keys (quad mode uses selectSlot with double-click swap)
				handleQuadHotbarKeybinds(client);
				// Sync external selectedSlot changes (server/other mods)
				if (client.player != null) {
					int realSlot = client.player.getInventory().getSelectedSlot();
					int expectedSlot = extendedHotbarSlot % 9;
					if (realSlot != expectedSlot) {
						// External change to selected slot, update virtual slot (keep current left/right side, no swap)
						extendedHotbarSlot = (leftIsRealHotbar ? 0 : 9) + realSlot;
					}
				}
			} else {
				// ========== Double-row mode (original logic, fully restored) ==========
				if (DHModConfig.INSTANCE.holdToSwap) {
					if (keyBinding.isDown() != this.hotbarKeys[9]) {
						this.hotbarKeys[9] = keyBinding.isDown();
						if (keyBinding.isDown()) {
							timer[9] = Instant.now().toEpochMilli();
						} else {
							if (Instant.now().toEpochMilli() - timer[9] < DHModConfig.INSTANCE.holdTime) {
								this.swapStack(client.player, !DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
							} else {
								this.alreadySwapped = false;
							}
						}
					}
					if (!this.alreadySwapped && keyBinding.isDown()
							&& Instant.now().toEpochMilli() - timer[9] > DHModConfig.INSTANCE.holdTime) {
						this.swapStack(client.player, DHModConfig.INSTANCE.holdToSwapBar, client.player.getInventory().getSelectedSlot());
						this.alreadySwapped = true;
					}
				} else {
					while (keyBinding.consumeClick()) {
						this.swapStack(client.player, true, 0);
					}
				}
				if (DHModConfig.INSTANCE.allowDoubleTap) {
					for (int i = 0; i < 9; i++) {
						if (client.options.keyHotbarSlots[i].isDown() != this.hotbarKeys[i]) {
							this.hotbarKeys[i] = client.options.keyHotbarSlots[i].isDown();
							if (client.options.keyHotbarSlots[i].isDown()) {
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

	// ==================== Double-row mode swap (original logic) ====================
	public void swapStack(Player player, boolean fullRow, int slot) {
		@SuppressWarnings("resource")
		MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
		int inventoryRow = DHModConfig.INSTANCE.inventoryRow * 9;
		boolean playSound = false;

		if (interactionManager == null || DHModConfig.INSTANCE.disableMod) {
			return;
		}

		if (fullRow) {
			for (int i = 0; i < 9; i++) {
				if(!player.getInventory().getItem(i).equals(player.getInventory().getItem(inventoryRow + i))) {
					interactionManager.handleContainerInput(player.containerMenu.containerId, inventoryRow + i, i, ContainerInput.SWAP, player);
					playSound = true;
				}
			}
		} else if(!player.getInventory().getItem(slot).equals(player.getInventory().getItem(inventoryRow + slot))) {
			interactionManager.handleContainerInput(player.containerMenu.containerId, inventoryRow + slot, slot, ContainerInput.SWAP, player);
			playSound = true;
		}

		if (playSound) {
			player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
		}
	}

	// ==================== Core swap logic ====================

	/**
	 * Called by scroll event (in MouseMixin).
	 * @param amount scroll amount: positive = up, negative = down
	 */
	public static void onMouseScroll(double amount) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || DHModConfig.INSTANCE.disableMod || !DHModConfig.INSTANCE.quadHotbar) return;

		int oldSlot = extendedHotbarSlot;
		// Scroll up increases slot, scroll down decreases slot
		int newSlot = oldSlot - (int) Math.signum(amount);
		if (newSlot < 0) newSlot = 17;
		if (newSlot > 17) newSlot = 0;

		boolean oldLeft = oldSlot < 9;
		boolean newLeft = newSlot < 9;

		// Cross-border requires full row swap
		if (oldLeft != newLeft) {
			long now = System.currentTimeMillis();
			if (now - lastBorderSwapTime < BORDER_SWAP_COOLDOWN) {
				// In cooldown: ignore this scroll, keep slot unchanged
				return;
			}
			// Swap hotbar row (0~8) with inventory bottom row (27~35)
			swapTwoEntireRows(0, 27);
			leftIsRealHotbar = newLeft;
			lastBorderSwapTime = now;
		}

		extendedHotbarSlot = newSlot;
		// Sync vanilla selectedSlot
		client.player.getInventory().setSelectedSlot(newSlot % 9);
	}

	/**
	 * Called on R key press, performs quad swap based on current hotbar position.
	 */
	private static void onSwapKeyPressed() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		if (leftIsRealHotbar) {
			// Left real: swap bottom-left(0~8) ↔ top-left(18~26), simultaneously bottom-right(27~35) ↔ top-right(9~17)
			swapTwoEntireRows(0, 18);
			swapTwoEntireRows(27, 9);
		} else {
			// Right real: swap bottom-right(27~35) ↔ top-right(9~17), simultaneously bottom-left(0~8) ↔ top-left(18~26)
			swapTwoEntireRows(0, 9);
			swapTwoEntireRows(27, 18);
		}
		if (client.player != null) {
			client.player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
		}
	}

	/**
	 * Quad mode: long-press R to swap the currently selected slot with the slot above it.
	 * Left real: bottom-left(i) ↔ top-left(i+18)
	 * Right real: bottom-right(i) ↔ top-right(i+9)
	 */
	private void swapSelectedSlotWithAbove() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		int selected = extendedHotbarSlot % 9;
		int sourceSlot, targetSlot;
		if (leftIsRealHotbar) {
			sourceSlot = selected;          // bottom-left 0~8
			targetSlot = selected + 18;     // top-left 18~26
		} else {
			sourceSlot = selected;
			targetSlot = selected + 9;      // top-right 9~17
		}
		swapSlots(sourceSlot, targetSlot);
		client.player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
	}

	/**
	 * Ctrl + short press R: swap only the real hotbar row with its corresponding backpack row.
	 * Left real: bottom-left(0~8) ↔ top-left(18~26)
	 * Right real: bottom-right(0~8) ↔ top-right(9~17)
	 */
	private void performCtrlShortSwap() {
		if (leftIsRealHotbar) {
			swapTwoEntireRows(0, 18);
		} else {
			swapTwoEntireRows(0, 9);
		}
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
		}
	}

	/**
	 * Ctrl + long press R: swap only the two internal backpack rows.
	 * Left real: bottom-most(27~35) ↔ top-most(9~17)
	 * Right real: bottom-most(27~35) ↔ middle(18~26)
	 */
	private void performCtrlLongSwap() {
		if (leftIsRealHotbar) {
			swapTwoEntireRows(27, 9);
		} else {
			swapTwoEntireRows(27, 18);
		}
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.playSound(WOOSH_SOUND_EVENT, 0.01f * DHModConfig.INSTANCE.wooshVolume, 1f);
		}
	}

	private void handleQuadHotbarKeybinds(Minecraft client) {
		if (client.player == null) return;
		boolean ctrlDown = modifierKeyBinding.isDown();

		for (int i = 0; i < 9; i++) {
			boolean isPressed = client.options.keyHotbarSlots[i].isDown();
			if (isPressed != hotbarKeys[i]) {
				hotbarKeys[i] = isPressed;
				if (isPressed) {
					long now = Instant.now().toEpochMilli();
					if (now - timer[i] < DHModConfig.INSTANCE.doubleTapWindow && ctrlDown == quadCtrlState[i]) {
						selectSlot(i, ctrlDown, true);  // Double-click
					} else {
						selectSlot(i, ctrlDown, false); // Single click
					}
					timer[i] = now;
					quadCtrlState[i] = ctrlDown;
				}
			}
		}
	}

	private void selectSlot(int keyIndex, boolean ctrl, boolean doubleClick) {
		Minecraft client = Minecraft.getInstance();
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
			} else {
				return; // In cooldown, ignore
			}
		}

		extendedHotbarSlot = targetSlot;
		client.player.getInventory().setSelectedSlot(targetSlot % 9);

		if (doubleClick) {
			swapSelectedSlotWithAbove(); // Double-click: swap with slot above
		}
	}

	// ==================== Low-level slot operations ====================

	/**
	 * Unconditionally swap entire rows of items (including empty slots), for cross-border and R-key swaps.
	 * @param rowAStart inventory index start (0, 9, 18, 27)
	 * @param rowBStart inventory index start
	 */
	private static void swapTwoEntireRows(int rowAStart, int rowBStart) {
		for (int i = 0; i < 9; i++) {
			swapSlots(rowAStart + i, rowBStart + i);
		}
	}

	/**
	 * Swap two inventory slots (via three PICKUP operations, safe and independent of hotbar relationships).
	 * @param slotA inventory index (0~35)
	 * @param slotB inventory index (0~35)
	 */
	private static void swapSlots(int slotA, int slotB) {
		Minecraft client = Minecraft.getInstance();
		if (client.screen != null || client.gameMode == null || client.player == null) return;
		AbstractContainerMenu handler = client.player.containerMenu;
		if (handler == null) return;
		int syncId = handler.containerId;

		int screenSlotA = inventoryIndexToScreenSlot(slotA);
		int screenSlotB = inventoryIndexToScreenSlot(slotB);

		// If SWAP is enabled and one side is hotbar (0~8) and the other is backpack (9~35), use SWAP
		if (DHModConfig.INSTANCE.useSwapForHotbar) {
			boolean aIsHotbar = (slotA >= 0 && slotA <= 8);
			boolean bIsHotbar = (slotB >= 0 && slotB <= 8);
			if (aIsHotbar != bIsHotbar) { // One is hotbar, one is backpack
				int hotbarSlot = aIsHotbar ? slotA : slotB;
				int screenInventorySlot = aIsHotbar ? screenSlotB : screenSlotA;
				// SWAP: click backpack slot with hotbar number = hotbarSlot
				client.gameMode.handleContainerInput(syncId, screenInventorySlot, hotbarSlot, ContainerInput.SWAP, client.player);
				return;
			}
		}

		// Standard three-click exchange
		client.gameMode.handleContainerInput(syncId, screenSlotA, 0, ContainerInput.PICKUP, client.player);
		client.gameMode.handleContainerInput(syncId, screenSlotB, 0, ContainerInput.PICKUP, client.player);
		client.gameMode.handleContainerInput(syncId, screenSlotA, 0, ContainerInput.PICKUP, client.player);
	}

	/**
	 * Convert player inventory index (0~35) to screen handler slot ID.
	 */
	private static int inventoryIndexToScreenSlot(int invIndex) {
		// 0~8  -> 36~44 (hotbar)
		// 9~35 -> 9~35 (main inventory)
		if (invIndex < 9) {
			return 36 + invIndex;  // Hotbar slot
		} else {
			return invIndex;       // Main inventory slot
		}
	}
}