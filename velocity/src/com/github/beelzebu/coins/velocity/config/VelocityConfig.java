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
package com.github.beelzebu.coins.velocity.config;

import com.github.beelzebu.coins.api.config.CoinsConfig;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.common.config.ConfigurateAbstractConfigFile;
import java.nio.file.Path;
import java.util.Set;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

/**
 * @author Beelzebu
 */
public class VelocityConfig extends CoinsConfig {

    private final ConfigurateConfig configurateConfig;

    public VelocityConfig(CoinsPlugin coinsPlugin, Path path) {
        super(coinsPlugin);
        configurateConfig = new ConfigurateConfig(path);
    }

    @Override
    public Object get(String path) {
        return configurateConfig.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return configurateConfig.getConfigurationSection(path);
    }

    @Override
    public void reload() {
        configurateConfig.reload();
    }

    private class ConfigurateConfig extends ConfigurateAbstractConfigFile {

        public ConfigurateConfig(Path path) {
            super(path);
        }

        @Override
        protected ConfigurationLoader<? extends ConfigurationNode> getLoader(Path path) {
            return YAMLConfigurationLoader.builder().setPath(path).build();
        }
    }
}
