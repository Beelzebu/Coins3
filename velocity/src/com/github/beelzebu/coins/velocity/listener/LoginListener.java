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
package com.github.beelzebu.coins.velocity.listener;

import com.github.beelzebu.coins.velocity.CoinsVelocityPlugin;
import com.github.beelzebu.coins.velocity.messaging.VelocityMessaging;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class LoginListener {

    private final CoinsVelocityPlugin coinsVelocityPlugin;

    public LoginListener(CoinsVelocityPlugin coinsVelocityPlugin) {
        this.coinsVelocityPlugin = coinsVelocityPlugin;
    }

    @Subscribe
    public void onPlayerJoin(@NotNull ServerConnectedEvent e) {
        VelocityMessaging velocityMessaging = (VelocityMessaging) coinsVelocityPlugin.getBootstrap().getProxyMessaging();
        Queue<String> messageQueue = velocityMessaging.getMessageQueue().get(e.getServer().getServerInfo().getName());
        String message;
        while (messageQueue != null && (message = messageQueue.poll()) != null) {
            velocityMessaging.sendMessage(message, true, e.getServer());
        }
    }
}