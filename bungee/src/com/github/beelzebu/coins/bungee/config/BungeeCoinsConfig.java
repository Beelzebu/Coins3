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
package com.github.beelzebu.coins.bungee.config;

import com.github.beelzebu.coins.api.config.CoinsConfig;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * @author Beelzebu
 */
public class BungeeCoinsConfig extends CoinsConfig {

    private final File configFile;
    private net.md_5.bungee.config.Configuration config;

    public BungeeCoinsConfig(File file) {
        configFile = file;
        reload();
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return new LinkedHashSet<>(config.getSection(path).getKeys());
    }

    @Override
    public final void reload() {
        if (!configFile.exists()) {
            Logger.getLogger(BungeeCoinsConfig.class.getName()).log(Level.SEVERE, "Config file doesn't exists yet");
            return;
        }
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException ex) {
            Logger.getLogger(BungeeCoinsConfig.class.getName()).log(Level.SEVERE, "An unexpected error has occurred reloading the config. {0}", ex.getMessage());
        }
    }
}
