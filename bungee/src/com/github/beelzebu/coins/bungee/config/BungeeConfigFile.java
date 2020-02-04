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

import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * @author Beelzebu
 */
public class BungeeConfigFile extends AbstractConfigFile {

    private File langFile;
    private net.md_5.bungee.config.Configuration messages;

    public BungeeConfigFile(File file) {
        load(langFile = file);
    }

    @Override
    public Object get(String path) {
        return messages.get(path);
    }

    @Override
    public Set<String> getConfigurationSection(String path) {
        return (Set<String>) messages.getSection(path).getKeys();
    }

    @Override
    public final void reload() {
        load(langFile);
    }

    private void load(File file) {
        langFile = file;
        try {
            messages = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException ex) {
            Logger.getLogger(BungeeCoinsConfig.class.getName()).log(Level.WARNING, "An unexpected error has occurred reloading the messages file. {0}", ex.getMessage());
        }
    }
}
