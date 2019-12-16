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
package com.github.beelzebu.coins.common.config;

import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.config.MultipliersConfig;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.util.Set;

public class MultipliersConfigImpl extends MultipliersConfig {

    private final AbstractConfigFile configFile;

    public MultipliersConfigImpl(CoinsPlugin coinsPlugin, AbstractConfigFile configFile) {
        super(coinsPlugin);
        this.configFile = configFile;
    }

    @Override
    public Object get(String path) {
        return configFile.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return configFile.getConfigurationSection(path);
    }

    @Override
    public void reload() {
        configFile.reload();
    }
}
