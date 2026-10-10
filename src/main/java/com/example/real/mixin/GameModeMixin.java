package com.example.real.mixin;

import com.example.real.RealClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class GameModeMixin {
    @Inject(method = "useItem", at = @At("HEAD"))
    private void real$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        RealClient.beforeUse(player, hand);
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void real$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        RealClient.afterUse(cir.getReturnValue());
    }
}
