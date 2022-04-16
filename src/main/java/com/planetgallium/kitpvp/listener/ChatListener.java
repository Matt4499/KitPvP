package com.planetgallium.kitpvp.listener;
import com.planetgallium.kitpvp.Game;
import com.planetgallium.kitpvp.game.Arena;
import com.planetgallium.kitpvp.util.Resources;
import com.planetgallium.kitpvp.util.Toolkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Objects;
public class ChatListener implements Listener {
    private final Arena arena;
    private final Resources resources;
    public ChatListener(Game plugin) {
        this.arena = plugin.getArena();
        this.resources = plugin.getResources();
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (resources.getConfig().getBoolean("Chat.Enabled") && Toolkit.inArena(e.getPlayer())) {
            Player p = e.getPlayer();
            String playerLevel = String.valueOf(arena.getStats().getStat("level", p.getName()));
            String levelPrefix = Objects.requireNonNull(resources.getLevels().getString("Levels.Levels." + playerLevel + ".Prefix"))
                    .replace("%level%", playerLevel);
            String format = Objects.requireNonNull(resources.getConfig().getString("Chat.Format"))
                    .replace("%player%", "%s")
                    .replace("%message%", "%s")
                    .replace("%level%", levelPrefix);
            e.setFormat(Toolkit.addPlaceholdersIfPossible(p, format));
        }
    }
}
