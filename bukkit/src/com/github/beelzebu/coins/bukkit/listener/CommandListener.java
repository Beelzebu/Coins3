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
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * @author Beelzebu
 */
public class CommandListener implements Listener {

    private final CoinsBukkitPlugin plugin;

    public CommandListener(CoinsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandEvent(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        plugin.getBootstrap().runAsync(() -> {
            if (msg.replaceFirst("/", "").startsWith(plugin.getConfig().getCommand()) || plugin.getConfig().getCommandAliases().contains(msg.split(" ")[0].replaceFirst("/", ""))) {
                plugin.debug(e.getPlayer().getName() + " issued command: " + msg);
            }
            if (plugin.getConfig().getDouble("Command Cost." + msg, 0) != 0) {
                if (CoinsAPI.getCoins(e.getPlayer().getUniqueId()) < plugin.getConfig().getDouble("Command Cost." + msg)) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(StringUtils.rep(plugin.getMessages(CompatUtils.getLocale(e.getPlayer())).getString("Errors.No Coins")));
                } else {
                    plugin.debug("Applied command cost for " + e.getPlayer().getName() + " in command: " + msg);
                    CoinsAPI.takeCoins(e.getPlayer().getName(), plugin.getConfig().getDouble("Command Cost." + msg));
                }
            }
        });
    }
}
