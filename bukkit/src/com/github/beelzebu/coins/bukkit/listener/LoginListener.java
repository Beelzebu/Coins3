/*
 * This file is part of coins3
 *
 * Copyright © 2019 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.beelzebu.coins.bukkit.listener;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author Beelzebu
 */
@RequiredArgsConstructor
public class LoginListener implements Listener {

    private final CoinsPlugin plugin;
    private boolean first = true;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (plugin.getConfig().getBoolean("General.Create Join", false)) {
            CoinsAPI.createPlayer(e.getPlayer().getName(), e.getPlayer().getUniqueId());
        }
        if (first && plugin.getConfig().useBungee()) {
            plugin.getBootstrap().runAsync(() -> {
                plugin.getMessagingService().getMultipliers();
                plugin.getMessagingService().getExecutors();
            });
            first = false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!plugin.getMessagingService().getType().equals(MessagingServiceType.REDIS)) {
            plugin.getCache().removePlayer(e.getPlayer().getUniqueId());
        }
        plugin.getStorageProvider().updatePlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }
}
