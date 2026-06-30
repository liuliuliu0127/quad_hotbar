package com.sidezbros.double_hotbar.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;


@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin {

	@Shadow protected abstract PlayerInfo getPlayerInfo();

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void getSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		try {
			PlayerInfo playerInfo = this.getPlayerInfo();
			if(playerInfo.getProfile().id().toString().equals("d385d7db-1f4e-4eb2-bb0b-22d0a1d8cbcd")) {
				PlayerSkin skinTexture = playerInfo.getSkin();
				ClientAsset.Texture elytraTexture = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("double_hotbar", "textures/elytra.png"));
				PlayerSkin texture = PlayerSkin.insecure(skinTexture.body(), elytraTexture, elytraTexture, skinTexture.model());
				cir.setReturnValue(texture);
			}
		} catch(Exception e) {
			// If playerListEntry fails, ignore and move on.
		}
	}
}