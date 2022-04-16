package com.planetgallium.kitpvp.item;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import com.planetgallium.kitpvp.Game;
import com.planetgallium.kitpvp.api.Ability;
import com.planetgallium.kitpvp.util.Cooldown;
import com.planetgallium.kitpvp.util.Resource;
import com.planetgallium.kitpvp.util.Toolkit;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
public class AttributeParser {
    // PotionEffect
    private static final PotionEffectType FALLBACK_EFFECT_TYPE = PotionEffectType.CONFUSION;
    private static final int FALLBACK_EFFECT_DURATION_SECONDS = 10;
    private static final int FALLBACK_EFFECT_AMPLIFIER_NON_ZERO_BASED = 1;
    // ItemStack
    private static final Material FALLBACK_ITEM_MATERIAL = Material.BEDROCK;
    private static final int FALLBACK_ITEM_AMOUNT = 1;
    private static final Enchantment FALLBACK_ITEM_ENCHANTMENT = Enchantment.THORNS;
    private static final int FALLBACK_ITEM_ENCHANTMENT_AMPLIFIER = 1;
    public static List<PotionEffect> getEffectsFromPath(Resource resource, String path) {
        List<PotionEffect> effects = new ArrayList<>();
        ConfigurationSection effectsSection = resource.getConfigurationSection(path);
        if (effectsSection != null) {
            for (String effectName : effectsSection.getKeys(false)) {
                PotionEffectType type = XPotion.matchXPotion(effectName).get().getPotionEffectType();
                int amplifier = resource.getInt(path + "." + effectName + ".Amplifier");
                int duration = resource.getInt(path + "." + effectName + ".Duration");
                effects.add(new PotionEffect(type != null ? type : FALLBACK_EFFECT_TYPE,
                        duration != 0 ? duration : FALLBACK_EFFECT_DURATION_SECONDS,
                        amplifier != 0 ? amplifier : FALLBACK_EFFECT_AMPLIFIER_NON_ZERO_BASED));
            }
        }
        return effects;
    }
    public static List<Ability> getAbilitiesFromResource(Resource resource) {
        List<Ability> abilities = new ArrayList<>();
        ConfigurationSection abilitySection = resource.getConfigurationSection("Abilities");
        if (abilitySection == null) return new ArrayList<>();
        for (String abilityName : abilitySection.getKeys(false)) {
            Ability ability = new Ability(abilityName);
            String pathPrefix = "Abilities." + abilityName;
            String activatorMaterial = resource.getString(pathPrefix + ".Activator.Material");
            String activatorName = resource.getString(pathPrefix + ".Activator.Name");
            assert activatorMaterial != null;
            ItemStack activator = new ItemStack(Objects.requireNonNull(XMaterial.matchXMaterial(activatorMaterial).get().parseMaterial()));
            ItemMeta abilityMeta = activator.getItemMeta();
            assert abilityMeta != null;
            abilityMeta.setDisplayName(activatorName);
            activator.setItemMeta(abilityMeta);
            ability.setActivator(activator);
            if (resource.contains(pathPrefix + ".Cooldown")) {
                String formattedCooldown = resource.getString(pathPrefix + ".Cooldown.Cooldown");
                assert formattedCooldown != null;
                ability.setCooldown(new Cooldown(formattedCooldown));
            }
            if (resource.contains(pathPrefix + ".Message")) {
                String abilityMessage = resource.getString(pathPrefix + ".Message.Message");
                ability.setMessage(abilityMessage);
            }
            if (resource.contains(pathPrefix + ".Sound")) {
                Sound abilitySound = XSound.matchXSound(Objects.requireNonNull(resource.getString(pathPrefix + ".Sound.Sound"))).get().parseSound();
                int abilitySoundPitch = resource.getInt(pathPrefix + ".Sound.Pitch");
                int abilitySoundVolume = resource.getInt(pathPrefix + ".Sound.Volume");
                ability.setSound(abilitySound, abilitySoundPitch, abilitySoundVolume);
            }
            if (resource.contains(pathPrefix + ".Effects")) {
                ConfigurationSection effectSection = resource.getConfigurationSection(pathPrefix + ".Effects");
                assert effectSection != null;
                for (String effectName : effectSection.getKeys(false)) {
                    PotionEffectType effectType = XPotion.matchXPotion(effectName).get().getPotionEffectType();
                    int amplifier = resource.getInt(pathPrefix + ".Effects." + effectName + ".Amplifier");
                    int duration = resource.getInt(pathPrefix + ".Effects." + effectName + ".Duration");
                    ability.addEffect(effectType, amplifier, duration);
                }
            }
            if (resource.contains(pathPrefix + ".Commands")) {
                for (String command : resource.getStringList(pathPrefix + ".Commands")) {
                    ability.addCommand(command);
                }
            }
            abilities.add(ability);
        }
        return abilities;
    }
    public static ItemStack getItemStackFromPath(Resource resource, String path) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (!resource.contains(path)) return null;
        ItemStack item = XMaterial.matchXMaterial(FALLBACK_ITEM_MATERIAL).parseItem();
        assert item != null;
        ItemMeta meta = item.getItemMeta();
        //          BASIC ITEM INFORMATION          //
        if (resource.contains(path + ".Name")) {
            assert meta != null;
            meta.setDisplayName(resource.getString(path + ".Name"));
        }
        if (resource.contains(path + ".Lore")) {
            assert meta != null;
            meta.setLore(resource.getStringList(path + ".Lore"));
        }
        if (resource.contains(path + ".Material")) {
            String materialValue = resource.getString(path + ".Material");
            assert materialValue != null;
            Optional<XMaterial> possibleMaterial = XMaterial.matchXMaterial(materialValue);
            if (possibleMaterial.isPresent()) {
                item.setType(Objects.requireNonNull(possibleMaterial.get().parseMaterial()));
            } else {
                Toolkit.printToConsole(String.format("&7[&b&lKIT-PVP&7] &cUnknown material [%s], defaulting to [%s].",
                        materialValue, FALLBACK_ITEM_MATERIAL));
            }
        }
        if (resource.contains(path + ".Amount")) {
            item.setAmount(resource.getInt(path + ".Amount"));
        } else {
            item.setAmount(FALLBACK_ITEM_AMOUNT);
        }
        item.setItemMeta(meta);
        //              CUSTOM ATTRIBUTES            //
        if (resource.contains(path + ".Effects")) {
            item = setEffectsFromPath(item, resource, path);
        }
        if (Game.getInstance().getResources().getConfig().getBoolean("Arena.PreventItemDurabilityDamage")) {
            setUnbreakable(item);
        }
        if (resource.contains(path + ".Dye")) {
            dyeItem(item, Toolkit.getColorFromConfig(resource, path + ".Dye"));
        }
        if (resource.contains(path + ".Skull")) {
            setSkull(item, resource.getString(path + ".Skull"));
        }
        if (resource.contains(path + ".Durability")) {
            setCustomDurability(item, resource.getInt(path + ".Durability"));
        }
        if (resource.contains(path + ".Enchantments")) {
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            ConfigurationSection section = resource.getConfigurationSection(path + ".Enchantments");
            assert section != null;
            for (String enchantmentName : section.getKeys(false)) {
                Enchantment enchantment = FALLBACK_ITEM_ENCHANTMENT;
                Optional<XEnchantment> enchantmentFromConfig = XEnchantment.matchXEnchantment(enchantmentName.toUpperCase());
                if (enchantmentFromConfig.isPresent()) {
                    enchantment = enchantmentFromConfig.get().getEnchant();
                } else {
                    Toolkit.printToConsole(String.format("&7[&b&lKIT-PVP&7] &cUnknown enchantment [%s], defaulting to [THORNS].", enchantmentName));
                }
                int amplifier = resource.getInt(path + ".Enchantments." + enchantmentName);
                enchantments.put(enchantment != null ? enchantment : FALLBACK_ITEM_ENCHANTMENT,
                        amplifier != 0 ? amplifier : FALLBACK_ITEM_ENCHANTMENT_AMPLIFIER);
            }
            item.addUnsafeEnchantments(enchantments);
        }
        return item;
    }
    private static void setCustomDurability(ItemStack item, int damageAmount) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable) {
                ((Damageable) meta).setDamage(damageAmount);
                item.setItemMeta(meta);
            }
    }
    private static void setSkull(ItemStack item, String skullOwner) {
        assert XMaterial.PLAYER_HEAD.parseMaterial() != null;
        item.setType(XMaterial.PLAYER_HEAD.parseMaterial());
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
        item.setItemMeta(skullMeta);
    }
    private static void dyeItem(ItemStack item, Color color) {
        if (item.getType() == XMaterial.LEATHER_HELMET.parseMaterial() ||
                item.getType() == XMaterial.LEATHER_CHESTPLATE.parseMaterial() ||
                item.getType() == XMaterial.LEATHER_LEGGINGS.parseMaterial() ||
                item.getType() == XMaterial.LEATHER_BOOTS.parseMaterial()) {
            LeatherArmorMeta dyedMeta = (LeatherArmorMeta) item.getItemMeta();
            assert dyedMeta != null;
            dyedMeta.setColor(color);
            item.setItemMeta(dyedMeta);
        }
    }
    private static void setUnbreakable(ItemStack item) {
        Objects.requireNonNull(item.getItemMeta()).setUnbreakable(true);
    }
    private static ItemStack setEffectsFromPath(ItemStack item, Resource resource, String path) {
        String effectsPath = path + ".Effects";
        String firstChildEffectName = Objects.requireNonNull(resource.getConfigurationSection(effectsPath)).getKeys(false).toArray()[0].toString();
        String firstChildPath = path + ".Effects." + firstChildEffectName;
        boolean isCustom = resource.contains(firstChildPath + ".Duration");
        if (Toolkit.versionToNumber() == 18 && !isCustom) {
            return setVanillaEffectsFromPath(item, resource, effectsPath);
        }
        if (isCustom) {
            setCustomEffectsFromPath(item, resource, effectsPath);
        } else {
            setVanillaEffectsFromPath(item, resource, effectsPath);
        }
        return item;
    }
    private static ItemStack setVanillaEffectsFromPath(ItemStack item, Resource resource, String path) {
        PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
        String firstChildEffectName = Objects.requireNonNull(resource.getConfigurationSection(path)).getKeys(false).toArray()[0].toString();
        String firstChildPath = path + "." + firstChildEffectName;
        PotionType potionType = XPotion.matchXPotion(firstChildEffectName).get().getPotionType();
        boolean isUpgraded = resource.getBoolean(firstChildPath + ".Upgraded");
        boolean isExtended = resource.getBoolean(firstChildPath + ".Extended");
        assert potionMeta != null;
        assert potionType != null;
        potionMeta.setBasePotionData(new PotionData(potionType, isExtended, isUpgraded));
        item.setItemMeta(potionMeta);
        return item;
    }
    private static void setCustomEffectsFromPath(ItemStack item, Resource resource, String path) {
        PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
        for (String effectName : Objects.requireNonNull(resource.getConfigurationSection(path)).getKeys(false)) {
            String specificPath = path + "." + effectName;
            assert potionMeta != null;
            potionMeta.addCustomEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(effectName)),
                    resource.getInt(specificPath + ".Duration") * 20,
                    resource.getInt(specificPath + ".Amplifier") - 1), true);
        }
        item.setItemMeta(potionMeta);
    }
    private static int getVanillaLevel(PotionType type, boolean isUpgraded) {
        switch (type) {
            case INVISIBILITY:
            case FIRE_RESISTANCE:
            case NIGHT_VISION:
            case SLOWNESS:
            case WATER_BREATHING:
            case WEAKNESS:
                return 1;
            case INSTANT_DAMAGE:
            case INSTANT_HEAL:
            case JUMP:
            case POISON:
            case REGEN:
            case SPEED:
            case STRENGTH:
                return isUpgraded ? 2 : 1;
        }
        return 1;
    }
}
