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
package com.github.beelzebu.coins.bukkit.config;

import com.github.beelzebu.coins.api.config.CoinsConfig;
import com.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * @author Beelzebu
 */
public class BukkitCoinsConfig extends CoinsConfig {

    private final FileConfiguration config;

    public BukkitCoinsConfig(FileConfiguration config) {
        this.config = config;
        reload();
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return config.isConfigurationSection(path) && config.getConfigurationSection(path) != null ? config.getConfigurationSection(path).getKeys(false) : new HashSet<>();
    }

    @Override
    public final void reload() {
        CoinsBukkitMain.getProvidingPlugin(CoinsBukkitMain.class).reloadConfig();
    }
}
