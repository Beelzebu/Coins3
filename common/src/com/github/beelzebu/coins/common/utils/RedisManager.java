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
package com.github.beelzebu.coins.common.utils;

import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import java.util.Objects;
import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {


    private final String host, password;
    private final int port;
    @Getter
    private JedisPool pool;

    public RedisManager(CommonCoinsPlugin commonCoinsPlugin) {
        host = commonCoinsPlugin.getConfig().getString("Redis.Host", "localhost");
        port = commonCoinsPlugin.getConfig().getInt("Redis.Port", 6379);
        password = commonCoinsPlugin.getConfig().getString("Redis.Password");
    }

    public void start() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(true);
        config.setMinIdle(1);
        if (Objects.nonNull(password) && !Objects.equals(password, "")) {
            pool = new JedisPool(config, host, port, 0, password);
        } else {
            pool = new JedisPool(config, host, port);
        }
    }

    public void stop() {
        pool.close();
        pool.destroy();
    }
}
