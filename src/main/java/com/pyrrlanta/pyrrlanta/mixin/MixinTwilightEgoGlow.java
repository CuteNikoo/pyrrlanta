package com.pyrrlanta.pyrrlanta.mixin;

import com.pyrrlanta.pyrrlanta.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Makes the Twilight E.G.O. helmet's "highlight nearby entities" perk purely client-side:
// only the wearer's own client is told these entities should render with a glow outline.
// No real MobEffects.GLOWING is ever applied server-side, so no other player's client
// considers these entities glowing -- this mirrors how vanilla's own spectator-mode entity
// outlining works (a client-local rendering decision, not a synced entity state), confirmed
// by checking a real working reference implementation (SioGabx/ReEntityOutliner) that hooks
// the exact same method for the same "client-only glow" purpose.
@Mixin(Minecraft.class)
public abstract class MixinTwilightEgoGlow {
    @Shadow
    public LocalPlayer player;

    private static final double HIGHLIGHT_RADIUS_SQR = 16.0 * 16.0;

    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void pyrrlanta$twilightEgoGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || entity == player) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TWILIGHT_EGO_HELMET.get())) {
            return;
        }
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (player.distanceToSqr(entity) > HIGHLIGHT_RADIUS_SQR) {
            return;
        }
        cir.setReturnValue(true);
        cir.cancel();
    }
}
