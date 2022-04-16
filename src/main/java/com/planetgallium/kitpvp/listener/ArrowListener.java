package com.planetgallium.kitpvp.listener;
import com.cryptomorin.xseries.XMaterial;
import com.planetgallium.kitpvp.Game;
import com.planetgallium.kitpvp.util.Resource;
import com.planetgallium.kitpvp.util.Toolkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
public class ArrowListener implements Listener {
    private final Resource config;
    public ArrowListener(Game plugin) {
        this.config = plugin.getResources().getConfig();
    }
    @EventHandler
    public void onShot(EntityDamageByEntityEvent e) {
        if (Toolkit.inArena(e.getEntity()) && config.getBoolean("Combat.ArrowHit.Enabled")) {
            if (e.getEntity() instanceof Player damagedPlayer && e.getDamager() instanceof Arrow arrow) {
                if (arrow.getShooter() != null && arrow.getShooter() instanceof Player shooter) {
                    // ARROW HEALTH MESSAGE
                    if (!damagedPlayer.getName().equals(shooter.getName())) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                double health = Math.round(damagedPlayer.getHealth() * 10.0) / 10.0;
                                if (shooter.hasPermission("kp.arrowmessage")) {
                                    if (health != 20.0) {
                                        shooter.sendMessage(Objects.requireNonNull(config.getString("Combat.ArrowHit.Message")).replace("%player%", damagedPlayer.getName()).replace("%health%", String.valueOf(health)));
                                    }
                                }
                            }
                        }.runTaskLater(Game.getInstance(), 2L);
                        // ARROW RETURN
                        if (config.getBoolean("Combat.ArrowReturn.Enabled")) {
                            for (ItemStack items : shooter.getInventory().getContents()) {
                                if (items != null && items.getType() == XMaterial.ARROW.parseMaterial() && items.getAmount() < 64) {
                                    if (shooter.hasPermission("kp.arrowreturn")) {
                                        if (shooter.getInventory().firstEmpty() == -1) {
                                            shooter.sendMessage(Objects.requireNonNull(config.getString("Combat.ArrowReturn.NoSpace")));
                                            return;
                                        }
                                        // TODO: Make this arrow have the same name as the Archer kit arrow, otherwise it won't stack even if player has < 64 archer arrows
                                        ItemStack arrowToAdd = new ItemStack(Material.ARROW);
                                        shooter.getInventory().addItem(arrowToAdd);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
