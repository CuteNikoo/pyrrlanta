package com.pyrrlanta.pyrrlanta.item;

import com.pyrrlanta.pyrrlanta.Pyrrlanta;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModItems {
    private ModItems() {
    }

    // A true violet purple, distinct from vanilla's washed-out LIGHT_PURPLE chat color.
    public static final Style EGO_NAME_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xA060FF));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Pyrrlanta.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Pyrrlanta.MODID);

    // Same defense-per-slot and knockback resistance as netherite; toughness bumped from
    // netherite's 3.0 to 4.0 per piece ("slightly more armor toughness"). The armor Layer
    // deliberately points at vanilla's own "netherite" texture set as a placeholder 3D
    // model -- only the 2D item icons are custom for now.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TWILIGHT_EGO_ARMOR_MATERIAL = ARMOR_MATERIALS.register("twilight_ego",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 6);
                        map.put(ArmorItem.Type.CHESTPLATE, 8);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 11);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.NETHERITE_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("netherite"))),
                    4.0f,
                    0.1f));

    public static final DeferredHolder<Item, TwilightEgoWeapon> TWILIGHT_EGO_WEAPON = ITEMS.register("twilight_ego_weapon",
            () -> new TwilightEgoWeapon(new Item.Properties()
                    .attributes(weaponAttributes())
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    public static final DeferredHolder<Item, TwilightEgoArmor> TWILIGHT_EGO_HELMET = ITEMS.register("twilight_ego_helmet",
            () -> new TwilightEgoArmor(TWILIGHT_EGO_ARMOR_MATERIAL, ArmorItem.Type.HELMET, armorProperties(ArmorItem.Type.HELMET)));

    public static final DeferredHolder<Item, TwilightEgoArmor> TWILIGHT_EGO_CHESTPLATE = ITEMS.register("twilight_ego_chestplate",
            () -> new TwilightEgoArmor(TWILIGHT_EGO_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, armorProperties(ArmorItem.Type.CHESTPLATE)));

    public static final DeferredHolder<Item, TwilightEgoArmor> TWILIGHT_EGO_LEGGINGS = ITEMS.register("twilight_ego_leggings",
            () -> new TwilightEgoArmor(TWILIGHT_EGO_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, armorProperties(ArmorItem.Type.LEGGINGS)));

    public static final DeferredHolder<Item, TwilightEgoArmor> TWILIGHT_EGO_BOOTS = ITEMS.register("twilight_ego_boots",
            () -> new TwilightEgoArmor(TWILIGHT_EGO_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, armorProperties(ArmorItem.Type.BOOTS)));

    private static Item.Properties armorProperties(ArmorItem.Type type) {
        return new Item.Properties()
                .durability(type.getDurability(37))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true));
    }

    // 15 attack damage (base 1.0 + 14.0), axe-speed (base 4.0 - 3.0 = 1.0 attacks/sec,
    // matching netherite axe), and entity_interaction_range +0.5 for "slightly more range"
    // (vanilla fallback for when Better Combat isn't installed -- Better Combat has its own
    // range_bonus mechanic set in the weapon_attributes compat file). Attack damage/speed
    // deliberately use vanilla's own canonical modifier IDs (Item.BASE_ATTACK_DAMAGE_ID/
    // BASE_ATTACK_SPEED_ID) instead of a custom one, since tooltip-aware mods (including
    // Better Combat) key off these well-known IDs to render the normal absolute-value
    // "15 Attack Damage" style tooltip instead of a raw "+14" delta.
    private static ItemAttributeModifiers weaponAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 14.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Pyrrlanta.MODID, "weapon.range"), 0.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TWILIGHT_EGO_WEAPON.get());
            event.accept(TWILIGHT_EGO_HELMET.get());
            event.accept(TWILIGHT_EGO_CHESTPLATE.get());
            event.accept(TWILIGHT_EGO_LEGGINGS.get());
            event.accept(TWILIGHT_EGO_BOOTS.get());
        }
    }
}
