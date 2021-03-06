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
package com.github.beelzebu.coins.bungee.messaging;

import com.github.beelzebu.coins.api.messaging.ProxyMessaging;
import com.github.beelzebu.coins.bungee.CoinsBungeePlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public final class BungeeMessaging extends ProxyMessaging implements Listener {

    public BungeeMessaging(CoinsBungeePlugin coinsPlugin) {
        super(coinsPlugin);
    }

    @EventHandler
    public void onMessageReceive(@NotNull PluginMessageEvent e) {
        if (!e.getTag().equals(CHANNEL)) {
            return;
        }
        if (e.getSender() instanceof ProxiedPlayer) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        JsonObject data = coinsPlugin.getGson().fromJson(in.readUTF(), JsonObject.class);
        handleMessage(data);
    }

    @Override
    public void start() {
        ProxyServer.getInstance().registerChannel(CHANNEL);
        ProxyServer.getInstance().getPluginManager().registerListener((Plugin) coinsPlugin.getBootstrap(), this);
    }

    @Override
    public void stop() {
        ProxyServer.getInstance().unregisterChannel(CHANNEL);
        ProxyServer.getInstance().getPluginManager().unregisterListener(this);
    }

    @Override
    protected void sendMessage(String message, boolean wait) {
        ProxyServer.getInstance().getServers().values().forEach(server -> sendMessage(message, wait, server));
    }

    private void sendMessage(String message, boolean wait, @NotNull ServerInfo server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);
        server.sendData(CHANNEL, out.toByteArray(), wait);
    }
}
