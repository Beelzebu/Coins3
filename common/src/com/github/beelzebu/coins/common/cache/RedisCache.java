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
package com.github.beelzebu.coins.common.cache;

import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import com.github.beelzebu.coins.common.utils.RedisManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Beelzebu
 */
public final class RedisCache implements CacheProvider {

    private static final String COINS_KEY_PREFIX = "coins:";
    private static final String MULTIPLIER_KEY_PREFIX = "multiplier:";
    private static final int CACHE_SECONDS = 1800;
    private final CommonCoinsPlugin<? extends CoinsBootstrap> plugin;
    private final RedisManager redisManager;
    @NotNull
    private final MultiplierPoller multiplierPoller;

    public RedisCache(CommonCoinsPlugin<? extends CoinsBootstrap> coinsPlugin, RedisManager redisManager) {
        plugin = coinsPlugin;
        this.redisManager = redisManager;
        multiplierPoller = new MultiplierPoller(plugin);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public synchronized OptionalDouble getCoins(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid can't be null");
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return getDouble(jedis.get(COINS_KEY_PREFIX + uuid));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting coins for '" + uuid + "' from redis cache.");
            plugin.debug(ex);
            removePlayer(uuid);
        } catch (ClassCastException ex) { // Jedis concurrency error
            plugin.log("An error with jedis (lib used by the plugin) has occurred getting coins for '" + uuid + "' from redis cache, this should fallback to a database query.");
            plugin.debug(ex);
            removePlayer(uuid);
        }
        return OptionalDouble.empty();
    }

    @Override
    public synchronized void updatePlayer(@NotNull UUID uuid, double coins) {
        Objects.requireNonNull(uuid, "UUID can't be null");
        plugin.debug("Setting coins for '" + uuid + "' to '" + coins + "' in redis.");
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.setex(COINS_KEY_PREFIX + uuid, CACHE_SECONDS, Double.toString(coins));
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding user '" + uuid + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public synchronized void removePlayer(@NotNull UUID uuid) {
        plugin.log("Removing '" + uuid + "' from redis.");
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del(COINS_KEY_PREFIX + uuid);
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing user '" + uuid + "' from redis.");
            plugin.debug(ex);
        }
    }

    @Override
    public synchronized Optional<Multiplier> getMultiplier(int id) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            return Optional.ofNullable(getMultiplier(jedis, String.valueOf(id)));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting multiplier with id '" + id + "' from redis cache.");
            plugin.debug(ex);
        }
        return Optional.empty();
    }

    @Nullable
    private synchronized Multiplier getMultiplier(@NotNull Jedis jedis, String id) {
        String multiplierString = jedis.get(MULTIPLIER_KEY_PREFIX + id);
        return multiplierString != null ? Multiplier.fromJson(multiplierString) : null;
    }

    @Override
    public synchronized void addMultiplier(@NotNull Multiplier multiplier) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.setex(MULTIPLIER_KEY_PREFIX + multiplier.getId(), (int) TimeUnit.MINUTES.toSeconds(multiplier.getData().getMinutes()), multiplier.toJson().toString());
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding multiplier '" + multiplier.toJson() + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public synchronized void deleteMultiplier(int id) {
        try (Jedis jedis = redisManager.getPool().getResource()) {
            jedis.del(MULTIPLIER_KEY_PREFIX + id);
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing multiplier with id '" + id + "' from cache.");
            plugin.debug(ex);
        }
    }

    @NotNull
    @Override
    public synchronized Set<Multiplier> getMultipliers() {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanResult<String> scan = jedis.scan(cursor, new ScanParams().match(MULTIPLIER_KEY_PREFIX + "*").count(Integer.MAX_VALUE));
            do {
                scan.getResult().forEach(key -> {
                    Multiplier multiplier = getMultiplier(jedis, key.split(":")[1]);
                    if (multiplier != null) {
                        multipliers.add(multiplier);
                    }
                });
                scan = jedis.scan(cursor, new ScanParams().match(MULTIPLIER_KEY_PREFIX + "*").count(Integer.MAX_VALUE));
            } while (!Objects.equals(cursor = scan.getCursor(), ScanParams.SCAN_POINTER_START));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting all multipliers from cache.");
            plugin.debug(ex);
        }
        return multipliers;
    }

    @NotNull
    @Override
    public synchronized Collection<UUID> getPlayers() {
        Set<UUID> players = new HashSet<>();
        try (Jedis jedis = redisManager.getPool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanResult<String> scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY_PREFIX + "*").count(Integer.MAX_VALUE));
            do {
                players.addAll(scan.getResult().stream().map(key -> UUID.fromString(key.split(":")[1])).collect(Collectors.toSet()));
                scan = jedis.scan(cursor, new ScanParams().match(COINS_KEY_PREFIX + "*").count(Integer.MAX_VALUE));
            } while (!Objects.equals(cursor = scan.getCursor(), ScanParams.SCAN_POINTER_START));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting all players from cache.");
            plugin.debug(ex);
        }
        return players;
    }

    @NotNull
    @Override
    public CacheType getCacheType() {
        return CacheType.REDIS;
    }

    @NotNull
    @Override
    public MultiplierPoller getMultiplierPoller() {
        return multiplierPoller;
    }

    private OptionalDouble getDouble(@NotNull String string) {
        try {
            return OptionalDouble.of(Double.parseDouble(string));
        } catch (@NotNull NumberFormatException | NullPointerException ignore) {
        }
        return OptionalDouble.empty();
    }
}
