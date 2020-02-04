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
package com.github.beelzebu.coins.common.messaging;

import com.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import com.github.beelzebu.coins.common.utils.RedisManager;
import com.google.gson.JsonObject;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Beelzebu
 */
public class RedisMessaging extends AbstractMessagingService {

    public static final String REDIS_CHANNEL = "coins-messaging";
    private final RedisManager redisManager;
    private PubSubListener psl;

    public RedisMessaging(CommonCoinsPlugin<? extends CoinsBootstrap> coinsPlugin, RedisManager redisManager) {
        super(coinsPlugin);
        this.redisManager = redisManager;
    }

    @Override
    public void publishUser(UUID uuid, double coins) {
        coinsPlugin.getCache().updatePlayer(uuid, coins);
    }

    @Override
    protected void sendMessage(JsonObject message) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.publish(REDIS_CHANNEL, message.toString());
        }
    }

    @Override
    public void start() {
        coinsPlugin.getBootstrap().runAsync(psl = new PubSubListener(new JedisPubSubHandler()));
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.REDIS;
    }

    @Override
    public void stop() {
        psl.poison();
    }

    private class PubSubListener implements Runnable {

        private final JedisPubSub jpsh;

        public PubSubListener(JedisPubSub jpsh) {
            this.jpsh = jpsh;
        }

        @Override
        public void run() {
            boolean broken = false;
            try (Jedis rsc = redisManager.getPool().getResource()) {
                try {
                    rsc.subscribe(jpsh, REDIS_CHANNEL);
                } catch (Exception e) {
                    coinsPlugin.log("PubSub error, attempting to recover.");
                    try {
                        jpsh.unsubscribe();
                    } catch (Exception ignore) {
                    }
                    broken = true;
                }
            }
            if (broken) {
                run();
            }
        }

        void poison() {
            jpsh.unsubscribe();
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            handleMessage(coinsPlugin.getGson().fromJson(message, JsonObject.class));
        }
    }
}
