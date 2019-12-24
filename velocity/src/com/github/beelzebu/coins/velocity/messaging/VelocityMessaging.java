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
package com.github.beelzebu.coins.velocity.messaging;

import com.github.beelzebu.coins.api.messaging.ProxyMessaging;
import com.github.beelzebu.coins.velocity.CoinsVelocityMain;
import com.github.beelzebu.coins.velocity.CoinsVelocityPlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author Beelzebu
 */
public class VelocityMessaging extends ProxyMessaging {

    private final Map<String, Queue<String>> messageQueue = new LinkedHashMap<>();
    private final MinecraftChannelIdentifier channelIdentifier = MinecraftChannelIdentifier.create(CHANNEL.split(":")[0], CHANNEL.split(":")[1]);

    public VelocityMessaging(CoinsVelocityPlugin coinsPlugin) {
        super(coinsPlugin);
    }

    @Subscribe
    public void onMessageReceive(PluginMessageEvent e) {
        if (!e.getIdentifier().equals(channelIdentifier)) {
            return;
        }
        if (e.getSource() instanceof Player) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        JsonObject data = coinsPlugin.getGson().fromJson(in.readUTF(), JsonObject.class);
        handleMessage(data);
    }

    @Override
    public void start() {
        CoinsVelocityMain bootstrap = (CoinsVelocityMain) coinsPlugin.getBootstrap();
        bootstrap.getProxyServer().getEventManager().register(bootstrap, this);
        bootstrap.getProxyServer().getChannelRegistrar().register(channelIdentifier);
    }

    @Override
    public void stop() {
        CoinsVelocityMain bootstrap = (CoinsVelocityMain) coinsPlugin.getBootstrap();
        bootstrap.getProxyServer().getEventManager().unregisterListener(bootstrap, this);
        bootstrap.getProxyServer().getChannelRegistrar().unregister(channelIdentifier);
    }

    @Override
    protected void sendMessage(String message, boolean wait) {
        CoinsVelocityMain bootstrap = (CoinsVelocityMain) coinsPlugin.getBootstrap();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);
        bootstrap.getProxyServer().getAllServers().forEach(registeredServer -> sendMessage(message, wait, registeredServer));
    }

    public void sendMessage(String message, boolean wait, RegisteredServer server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);
        if (wait && !server.sendPluginMessage(channelIdentifier, out.toByteArray())) {
            Queue<String> actualQueue = getMessageQueue().getOrDefault(server.getServerInfo().getName(), new LinkedList<>());
            actualQueue.add(message);
            getMessageQueue().put(server.getServerInfo().getName(), actualQueue);
        }
    }

    public Map<String, Queue<String>> getMessageQueue() {
        return messageQueue;
    }
}
