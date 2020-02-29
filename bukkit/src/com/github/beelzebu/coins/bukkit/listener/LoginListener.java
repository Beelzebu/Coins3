/*
 * This file is part of coins3
 *
 * Copyright © 2020 Beelzebu
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
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import com.github.beelzebu.coins.bukkit.messaging.BukkitMessaging;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class LoginListener implements Listener {

    private final CoinsBukkitPlugin plugin;
    private boolean first = true;

    public LoginListener(CoinsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        if (plugin.getConfig().getBoolean("General.Create Join", false)) {
            plugin.getBootstrap().runAsync(() -> CoinsAPI.createPlayer(e.getPlayer().getName(), e.getPlayer().getUniqueId()));
        }
        if (plugin.getConfig().useBungee()) {
            if (first) {
                first = false;
                plugin.getBootstrap().runAsync(() -> {
                    plugin.getMessagingService().requestMultipliers();
                    plugin.getMessagingService().requestExecutors();
                });
            }
            BukkitMessaging bukkitMessaging = (BukkitMessaging) plugin.getMessagingService();
            if (bukkitMessaging.getMessageQueue().isEmpty()) {
                return;
            }
            plugin.getBootstrap().runAsync(() -> {
                String message;
                while ((message = bukkitMessaging.getMessageQueue().poll()) != null) {
                    bukkitMessaging.sendMessage(message, true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent e) {
        plugin.getBootstrap().runAsync(() -> {
            if (!Objects.requireNonNull(plugin.getMessagingService(), "Messaging service is null.").getType().equals(MessagingServiceType.REDIS)) {
                Objects.requireNonNull(plugin.getCache(), "Can't remove '" + e.getPlayer().getUniqueId() + "' (" + e.getPlayer().getName() + ") from cache, cache is null.")
                        .removePlayer(e.getPlayer().getUniqueId());
            }
            if (Objects.requireNonNull(plugin.getStorageProvider(), "Can't update '" + e.getPlayer().getUniqueId() + "' (" + e.getPlayer().getName() + ") in database, StorageProvider is null.").isindb(e.getPlayer().getUniqueId())) {
                plugin.getStorageProvider().updatePlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());
            }
        });
    }
}
