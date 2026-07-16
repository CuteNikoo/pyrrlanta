package com.pyrrlanta.pyrrlanta.item;

import com.pyrrlanta.pyrrlanta.Pyrrlanta;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

// Two independent Twilight E.G.O. armor perks, both driven by a throttled server tick check
// (no clean per-equip-change event covers "is this full set currently worn", so this follows
// the same periodic-scan pattern already used elsewhere in this mod, e.g. TribeFireGuard):
//
// - Helmet alone: permanent Blindness I, in exchange for highlighting all nearby entities.
//   This is the helmet's own trade-off, independent of the rest of the set. The highlight
//   itself is handled entirely client-side by MixinTwilightEgoGlow (not here) so it's only
//   visible to the wearer -- no real MobEffects.GLOWING is ever applied.
// - Full 4-piece set: an armor-bypassing damage pulse on nearby hostile mobs every few
//   seconds. Radius (and damage) are increased if the weapon is also somewhere in the
//   player's inventory (not necessarily held).
public final class TwilightEgoSetEffects {
    public static final ResourceKey<DamageType> AURA_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Pyrrlanta.MODID, "twilight_ego_aura"));

    private static final int HELMET_EFFECT_INTERVAL_TICKS = 20; // refresh every second
    private static final int AURA_INTERVAL_TICKS = 60; // every 3 seconds

    // Reapplied every HELMET_EFFECT_INTERVAL_TICKS but given a much longer duration than
    // that interval, so it never gets close enough to expiry to cause the vignette's
    // fade-out-then-fade-in animation to visibly flash between refreshes.
    private static final int BLINDNESS_DURATION_TICKS = 200; // 10 seconds

    private static final double AURA_RADIUS = 10.0;
    private static final double AURA_RADIUS_BUFFED = 15.0;
    private static final float AURA_DAMAGE = 4.0f;
    private static final float AURA_DAMAGE_BUFFED = 6.0f;

    private static int tickCounter = 0;

    private TwilightEgoSetEffects() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TwilightEgoSetEffects::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (tickCounter % HELMET_EFFECT_INTERVAL_TICKS == 0) {
                applyHelmetEffect(player);
            }
            if (tickCounter % AURA_INTERVAL_TICKS == 0) {
                applyFullSetAura(player);
            }
        }
    }

    private static boolean wearingHelmet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TWILIGHT_EGO_HELMET.get());
    }

    private static boolean wearingFullSet(ServerPlayer player) {
        return wearingHelmet(player)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.TWILIGHT_EGO_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.TWILIGHT_EGO_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.TWILIGHT_EGO_BOOTS.get());
    }

    private static boolean hasWeaponInInventory(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.TWILIGHT_EGO_WEAPON.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.TWILIGHT_EGO_WEAPON.get())) {
                return true;
            }
        }
        return false;
    }

    private static void applyHelmetEffect(ServerPlayer player) {
        if (!wearingHelmet(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0, true, false, false));
    }

    private static void applyFullSetAura(ServerPlayer player) {
        if (!wearingFullSet(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean buffed = hasWeaponInInventory(player);
        double radius = buffed ? AURA_RADIUS_BUFFED : AURA_RADIUS;
        float damage = buffed ? AURA_DAMAGE_BUFFED : AURA_DAMAGE;

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive());
        if (targets.isEmpty()) {
            return;
        }
        Holder<DamageType> damageType = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(AURA_DAMAGE_TYPE);
        DamageSource source = new DamageSource(damageType, player);
        for (LivingEntity target : targets) {
            target.hurt(source, damage);
        }
    }
}
