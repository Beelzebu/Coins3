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
package com.github.beelzebu.coins.bungee.listener;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bungee.CoinsBungeePlugin;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class CommandListener implements Listener {

    private final CoinsBungeePlugin plugin;

    public CommandListener(CoinsBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(@NotNull ChatEvent e) {
        if (e.isCommand() && e.getSender() instanceof ProxiedPlayer) {
            String msg = e.getMessage().toLowerCase();
            plugin.getBootstrap().runAsync(() -> {
                ProxiedPlayer pp = (ProxiedPlayer) e.getSender();
                if (msg.replaceFirst("/", "").startsWith(plugin.getConfig().getCommand()) || plugin.getConfig().getCommandAliases().contains(msg.split(" ")[0].replaceFirst("/", ""))) {
                    plugin.debug(pp.getName() + " issued command: " + msg);
                }
                if (plugin.getConfig().getDouble("Command Cost." + msg, 0) != 0) {
                    if (CoinsAPI.getCoins(pp.getUniqueId()) < plugin.getConfig().getDouble("Command Cost." + msg)) {
                        e.setCancelled(true);
                        pp.sendMessage(TextComponent.fromLegacyText(StringUtils.rep(plugin.getMessages(pp.getLocale().getLanguage()).getString("Errors.No Coins"))));
                    } else {
                        plugin.debug("Applied command cost for " + pp.getName() + " in command: " + msg);
                        CoinsAPI.takeCoins(pp.getName(), plugin.getConfig().getDouble("Command Cost." + msg));
                    }
                }
            });
        }
    }
}
