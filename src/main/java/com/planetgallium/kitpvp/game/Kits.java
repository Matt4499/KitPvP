package com.planetgallium.kitpvp.game;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import com.planetgallium.kitpvp.Game;
import com.planetgallium.kitpvp.api.Ability;
import com.planetgallium.kitpvp.api.Kit;
import com.planetgallium.kitpvp.api.PlayerSelectKitEvent;
import com.planetgallium.kitpvp.item.AttributeParser;
import com.planetgallium.kitpvp.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public class Kits {
    private final Game plugin;
    private final Arena arena;
    private final Resources resources;
    private final Resource messages;
    private final Map<String, String> kits;
    public Kits(Game plugin, Arena arena) {
        this.arena = arena;
        this.plugin = plugin;
        this.resources = plugin.getResources();
        this.messages = resources.getMessages();
        this.kits = new HashMap<>();
    }
    public void createKit(Player fromPlayer, String kitName) {
        Kit kitToCreate = createKitFromPlayer(fromPlayer, kitName);
        Resource kitResource = new Resource(plugin, "kits/" + kitToCreate.getName() + ".yml");
        kitToCreate.toResource(kitResource);
        resources.addResource(kitToCreate.getName() + ".yml", kitResource);
        plugin.getDatabase().addKitCooldownTable(kitName);
        if (plugin.getConfig().getBoolean("Other.AutomaticallyAddKitToMenu")) {
            int nextAvailableMenuSlot = Toolkit.getNextAvailable(resources.getMenu(), "Menu.Items", resources.getMenu().getInt("Menu.General.Size") - 1, true, -1);
            if (nextAvailableMenuSlot != -1) {
                Resource menuConfig = resources.getMenu();
                String pathPrefix = "Menu.Items." + nextAvailableMenuSlot;
                menuConfig.set(pathPrefix + ".Name", "&a&l" + kitToCreate.getName() + " Kit");
                menuConfig.set(pathPrefix + ".Material", "BEDROCK");
                menuConfig.set(pathPrefix + ".Lore", new String[]{
                        "&7This information can be modified in the",
                        "&7menu.yml file. To disable automatic adding to the",
                        "&7menu, disable AutomaticallyAddKitToMenu in the",
                        "&7config.yml.",
                        " ",
                        "&e&eLeft-click to select.",
                        "&eRight-click to preview."});
                menuConfig.set(pathPrefix + ".Commands.Left-Click", new String[]{"player: kp kit " + kitToCreate.getName()});
                menuConfig.set(pathPrefix + ".Commands.Right-Click", new String[]{"player: kp preview " + kitToCreate.getName()});
                menuConfig.save();
                menuConfig.load();
                arena.getMenus().getKitMenu().clearCache();
            } else {
                fromPlayer.sendMessage(Objects.requireNonNull(messages.getString("Messages.Error.Menu")));
            }
        }
    }
    public Kit createKitFromPlayer(Player player, String name) {
        //          KIT             //
        Kit kit = new Kit(name);
        kit.setHelmet(player.getInventory().getHelmet());
        kit.setChestplate(player.getInventory().getChestplate());
        kit.setLeggings(player.getInventory().getLeggings());
        kit.setBoots(player.getInventory().getBoots());
        kit.setHealth(Toolkit.getMaxHealth(player));
        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffectType type = effect.getType();
            int amplifier = effect.getAmplifier();
            int duration = effect.getDuration();
            int amplifierNonZeroBased = amplifier + 1;
            int durationSeconds = duration / 20;
            kit.addEffect(type, amplifierNonZeroBased, durationSeconds);
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                if (item.getType() == XMaterial.MUSHROOM_STEW.parseMaterial()) {
                    ItemMeta itemMeta = item.getItemMeta();
                    assert itemMeta != null;
                    itemMeta.setDisplayName(resources.getConfig().getString("Soups.Name"));
                    itemMeta.setLore(Toolkit.colorizeList(resources.getConfig().getStringList("Soups.Lore")));
                    item.setItemMeta(itemMeta);
                }
                kit.setInventoryItem(i, item);
            }
        }
        if (Toolkit.versionToNumber() >= 19) {
            ItemStack offhandItem = player.getInventory().getItemInOffHand();
            kit.setOffhand(offhandItem);
        }
        //          ABILITY         //
        Ability sampleAbility = new Ability("Example");
        ItemStack activator = XMaterial.EMERALD.parseItem();
        assert activator != null;
        ItemMeta activatorMeta = activator.getItemMeta();
        assert activatorMeta != null;
        activatorMeta.setDisplayName(Toolkit.translate("&aDefault Ability &7(Must be modified in kit file)"));
        activator.setItemMeta(activatorMeta);
        sampleAbility.setActivator(activator);
        sampleAbility.setMessage("%prefix% &7You have used your ability.");
        sampleAbility.setSound(XSound.BLOCK_NOTE_BLOCK_PLING.parseSound(), 1, 1);
        sampleAbility.addEffect(XPotion.SPEED.getPotionEffectType(), 1, 10);
        sampleAbility.addCommand("console: This command is run from the console, you can use %player%");
        sampleAbility.addCommand("player: This command is run from the player, you can use %player%");
        kit.addAbility(sampleAbility);
        return kit;
    }
    private Kit createKitFromResource(Resource resource) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Kit kit = new Kit(trimName(resource.getName()));
        kit.setPermission(resource.getString("Kit.Permission"));
        kit.setLevel(resource.getInt("Kit.Level"));
        kit.setCooldown(new Cooldown(Objects.requireNonNull(resource.getString("Kit.Cooldown"))));
        kit.setHealth(resource.contains("Kit.Health") ? resource.getInt("Kit.Health") : 20);
        kit.setHelmet(AttributeParser.getItemStackFromPath(resource, "Inventory.Armor.Helmet"));
        kit.setChestplate(AttributeParser.getItemStackFromPath(resource, "Inventory.Armor.Chestplate"));
        kit.setLeggings(AttributeParser.getItemStackFromPath(resource, "Inventory.Armor.Leggings"));
        kit.setBoots(AttributeParser.getItemStackFromPath(resource, "Inventory.Armor.Boots"));
        for (PotionEffect effect : AttributeParser.getEffectsFromPath(resource, "Effects")) {
            kit.addEffect(effect.getType(), effect.getAmplifier(), effect.getDuration());
        }
        for (int i = 0; i < 36; i++) {
            if (resource.contains("Inventory.Items." + i)) {
                kit.setInventoryItem(i, AttributeParser.getItemStackFromPath(resource, "Inventory.Items." + i));
            }
        }
        kit.setFill(AttributeParser.getItemStackFromPath(resource, "Inventory.Items.Fill"));
        kit.setOffhand(AttributeParser.getItemStackFromPath(resource, "Inventory.Items.Offhand"));
        AttributeParser.getAbilitiesFromResource(resource).forEach(kit::addAbility);
        return kit;
    }
    public void attemptToGiveKitToPlayer(Player player, Kit kit) {
        if (kit == null) {
            player.sendMessage(Objects.requireNonNull(messages.getString("Messages.Error.Lost")));
            return;
        }
        if (!player.hasPermission(kit.getPermission())) {
            player.sendMessage(Objects.requireNonNull(messages.getString("Messages.General.Permission")).replace("%permission%", kit.getPermission()));
            return;
        }
        if (!(Toolkit.getPermissionAmount(player, "kp.levelbypass.", 0) >= kit.getLevel() ||
                arena.getStats().getStat("level", player.getName()) >= kit.getLevel())) {
            player.sendMessage(Objects.requireNonNull(messages.getString("Messages.Other.Needed")).replace("%level%", String.valueOf(kit.getLevel())));
            return;
        }
        Cooldown cooldownRemaining = arena.getCooldowns().getRemainingCooldown(player, kit);
        if (!player.hasPermission("kp.cooldownbypass") && cooldownRemaining.toSeconds() > 0) {
            player.sendMessage(Objects.requireNonNull(messages.getString("Messages.Error.CooldownKit")).replace("%cooldown%", cooldownRemaining.formatted(false)));
            return;
        }
        if (hasKit(player.getName())) {
            player.sendMessage(Objects.requireNonNull(messages.getString("Messages.Error.Selected")));
            assert XSound.ENTITY_ENDER_DRAGON_HURT.parseSound() != null;
            player.playSound(player.getLocation(), XSound.ENTITY_ENDER_DRAGON_HURT.parseSound(), 1, 1);
            return;
        }
        if (resources.getConfig().getBoolean("Arena.ClearInventoryOnKit")) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
        kit.apply(player);
        player.sendMessage(Objects.requireNonNull(messages.getString("Messages.Commands.Kit")).replace("%kit%", kit.getName()));
        assert XSound.ENTITY_HORSE_ARMOR.parseSound() != null;
        player.playSound(player.getLocation(), XSound.ENTITY_HORSE_ARMOR.parseSound(), 1, 1);
        Bukkit.getPluginManager().callEvent(new PlayerSelectKitEvent(player, kit));
        setKit(player.getName(), kit.getName());
        Cooldown kitCooldown = kit.getCooldown();
        if (kitCooldown != null && kitCooldown.toSeconds() > 0 && !player.hasPermission("kp.cooldownbypass")) {
            arena.getCooldowns().setKitCooldown(player.getName(), kit.getName());
        }
        Resource kitResource = resources.getKit(kit.getName());
        if (kitResource != null && kitResource.contains("Commands")) {
            List<String> commands = kitResource.getStringList("Commands");
            Toolkit.runCommands(player, commands, "none", "none");
        }
    }
    public void deleteKit(String kitName) {
        resources.removeResource(kitName + ".yml");
        plugin.getDatabase().deleteKitCooldownTable(kitName);
    }
    public Kit getKitByName(String kitName) {
        return loadKitFromCacheOrCreate(kitName);
    }
    private String trimName(String kitNameWithFileEnding) {
        String[] splitName = kitNameWithFileEnding.split(".yml");
        return splitName[0];
    }
    public void setKit(String playerName, String kitName) {
        kits.put(playerName, kitName);
    }
    public boolean hasKit(String playerName) {
        return kits.containsKey(playerName);
    }
    public Kit getKitOfPlayer(String playerName) {
        String kitName = kits.get(playerName);
        if (kitName == null) {
            return null;
        }
        return loadKitFromCacheOrCreate(kitName);
    }
    private Kit loadKitFromCacheOrCreate(String kitName) {
        if (!CacheManager.getKitCache().containsKey(kitName)) {
            if (resources.getKitList(false).contains(kitName)) {
                Resource kit = resources.getKit(kitName);
                try {
                    CacheManager.getKitCache().put(kitName, createKitFromResource(kit));
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                Toolkit.printToConsole("&7[&b&lKIT-PVP&7] &cNo kit with name " + kitName + " found in the kits folder. Try reloading the server.");
            }
        }
        return CacheManager.getKitCache().get(kitName);
    }
    public void resetKit(String playerName) {
        kits.remove(playerName);
    }
    public boolean isKit(String kitName) {
        return resources.getKitList(false).contains(kitName);
    }
}
