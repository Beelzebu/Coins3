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
package com.github.beelzebu.coins.common.cache;

import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.common.utils.RedisManager;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Beelzebu
 */
@RequiredArgsConstructor
public final class RedisCache implements CacheProvider {

    private static final String COINS_KEY = "coins:";
    private static final String MULTIPLIER_KEY = "multiplier:";
    private static final int CACHE_SECONDS = 1800;
    private final CoinsPlugin plugin;
    private final RedisManager redisManager;

    @Override
    public void start() {
        if (plugin.getConfig().isRedisLoadMultipliers()) {
            plugin.log("Updating all multipliers from database in redis...");
            long start = System.currentTimeMillis();
            plugin.getStorageProvider().getMultipliers().forEach(multiplier -> {
                Optional<Multiplier> optionalMultiplier = getMultiplier(multiplier.getId());
                if (optionalMultiplier.isPresent() && !Objects.equals(optionalMultiplier.get(), multiplier)) {
                    addMultiplier(multiplier);
                }
            });
            long end = System.currentTimeMillis();
            plugin.log("Updated all multipliers in redis. Took " + (start - end) + "ms");
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public OptionalDouble getCoins(UUID uuid) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return getDouble(jedis.get(COINS_KEY + uuid));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting coins for '" + uuid + "' from redis cache.");
            plugin.debug(ex);
        }
        return OptionalDouble.empty();
    }

    @Override
    public void updatePlayer(UUID uuid, double coins) {
        Objects.requireNonNull(uuid, "UUID can't be null");
        plugin.debug("Setting coins for '" + uuid + "' to '" + coins + "' in redis.");
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.setex(COINS_KEY + uuid, CACHE_SECONDS, Double.toString(coins));
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding user '" + uuid + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        plugin.log("Removing '" + uuid + "' from redis.");
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del(COINS_KEY + uuid);
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing user '" + uuid + "' from redis.");
            plugin.debug(ex);
        }
    }

    @Override
    public Optional<Multiplier> getMultiplier(int id) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return Optional.ofNullable(getMultiplier(jedis, String.valueOf(id)));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting multiplier with id '" + id + "' from redis cache.");
            plugin.debug(ex);
        }
        return Optional.empty();
    }

    private Multiplier getMultiplier(Jedis jedis, String id) {
        String multiplierString = jedis.get(MULTIPLIER_KEY + id);
        return multiplierString != null ? Multiplier.fromJson(multiplierString) : null;
    }

    @Override
    public void addMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.setex(MULTIPLIER_KEY + multiplier.getId(), (int) TimeUnit.MINUTES.toSeconds(multiplier.getData().getMinutes()), multiplier.toJson().toString());
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding multiplier '" + multiplier.toJson() + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void deleteMultiplier(int id) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del(MULTIPLIER_KEY + id);
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing multiplier with id '" + id + "' from cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanResult<String> scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY + "*").count(Integer.MAX_VALUE));
            do {
                multipliers.addAll(scan.getResult().stream().map(key -> getMultiplier(jedis, key.split(":")[1])).collect(Collectors.toSet()));
                scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY + "*").count(Integer.MAX_VALUE));
            } while (!Objects.equals(cursor = scan.getCursor(), ScanParams.SCAN_POINTER_START));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting all multipliers from cache.");
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Set<UUID> getPlayers() {
        Set<UUID> players = new HashSet<>();
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanResult<String> scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY + "*").count(Integer.MAX_VALUE));
            do {
                players.addAll(scan.getResult().stream().map(key -> UUID.fromString(key.split(":")[1])).collect(Collectors.toSet()));
                scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY + "*").count(Integer.MAX_VALUE));
            } while (!Objects.equals(cursor = scan.getCursor(), ScanParams.SCAN_POINTER_START));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting all players from cache.");
            plugin.debug(ex);
        }
        return players;
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.REDIS;
    }

    private OptionalDouble getDouble(String string) {
        try {
            return OptionalDouble.of(Double.parseDouble(string));
        } catch (NumberFormatException | NullPointerException ignore) {
        }
        return OptionalDouble.empty();
    }
}
