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

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Beelzebu
 */
public final class LocalCache implements CacheProvider {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();
    private final Cache<UUID, Double> players = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
    private final Cache<Integer, Multiplier> multipliers = Caffeine.newBuilder().build();
    private final File multipliersFile = new File(plugin.getBootstrap().getDataFolder(), "multipliers.dat");

    @Override
    public void start() {
        try {
            if (!multipliersFile.exists()) {
                multipliersFile.createNewFile();
            }
            Iterator<String> lines = Files.readAllLines(multipliersFile.toPath()).iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                try {
                    plugin.getCache().addMultiplier(Multiplier.fromJson(line));
                } catch (JsonParseException ignore) { // Invalid line
                    plugin.debug(line + " isn't a valid multiplier in json format.");
                    lines.remove();
                }
            }
            Files.write(multipliersFile.toPath(), Lists.newArrayList(lines));
        } catch (IOException ex) {
            plugin.log("An error has occurred loading multipliers from local storage.");
            plugin.debug(ex.getMessage());
        }
    }

    @Override
    public void stop() {
        players.invalidateAll();
        multipliers.invalidateAll();
    }

    @Override
    public OptionalDouble getCoins(UUID uuid) {
        Double coins = players.getIfPresent(uuid);
        if (coins != null) {
            return OptionalDouble.of(coins);
        }
        return OptionalDouble.empty();
    }

    @Override
    public void updatePlayer(UUID uuid, double coins) {
        if (plugin.getBootstrap().isOnline(uuid)) { // only update data if player is online
            CoinsAPI.getPlugin().debug("Updated local data for: " + uuid + " (" + plugin.getName(uuid, false) + ")");
            players.put(uuid, coins);
        } else { // remove player from cache if is offline
            removePlayer(uuid);
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        players.invalidate(uuid);
    }

    @Override
    public Optional<Multiplier> getMultiplier(int id) {
        return multipliers.asMap().values().stream().filter(multiplier -> multiplier.getId() == id).findFirst();
    }

    @Override
    public void addMultiplier(Multiplier multiplier) {
        if (!multiplier.getServer().equals(plugin.getConfig().getServerName())) {
            return;
        }
        // put the multiplier in the cache
        multipliers.put(multiplier.getId(), multiplier);
        // store it in a local storage to load them again without querying the database if the server is restarted
        try {
            if (!multipliersFile.exists()) {
                multipliersFile.createNewFile();
            }
            Iterator<String> lines = Files.readAllLines(multipliersFile.toPath()).iterator();
            boolean exists = false;
            // check if the multiplier was already stored in this server
            while (lines.hasNext()) {
                String line = lines.next();
                if (Objects.requireNonNull(Multiplier.fromJson(line)).getId() == multiplier.getId()) {
                    exists = true;
                    plugin.debug("Trying to add an existent multiplier: " + line);
                    break;
                }
            }
            if (!exists) {
                try {
                    Files.write(multipliersFile.toPath(), Collections.singletonList(multiplier.toJson().toString() + "\n"), StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    plugin.log("An error has occurred saving a multiplier in the local storage.");
                    plugin.debug(ex.getMessage());
                }
            }
        } catch (IOException ex) {
            plugin.log("An error has occurred saving a multiplier in the local storage.");
            plugin.debug(ex.getMessage());
        }
    }

    @Override
    public void deleteMultiplier(int id) {
        try { // remove it from local multiplier storage
            Iterator<String> lines = Files.readAllLines(multipliersFile.toPath()).iterator();
            while (lines.hasNext()) {
                if (Objects.requireNonNull(Multiplier.fromJson(lines.next())).getId() == id) {
                    multipliers.invalidate(id);
                    lines.remove();
                    break;
                }
            }
            Files.write(multipliersFile.toPath(), Lists.newArrayList(lines));
        } catch (IOException | NullPointerException | JsonSyntaxException ex) {
            plugin.log("An error has occurred removing a multiplier from local storage.");
            plugin.debug(ex.getMessage());
        }
        multipliers.invalidate(id);
    }

    @Override
    public void updateMultiplier(Multiplier multiplier, boolean callenable) {
        Objects.requireNonNull(multiplier, "Multiplier can't be null");
        if (callenable) {
            multiplier.enable();
            plugin.getMessagingService().enableMultiplier(multiplier);
        } else {
            plugin.getMessagingService().updateMultiplier(multiplier);
        }
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        return new HashSet<>(multipliers.asMap().values());
    }

    @Override
    public Set<UUID> getPlayers() {
        return new HashSet<>(players.asMap().keySet());
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.LOCAL;
    }
}
