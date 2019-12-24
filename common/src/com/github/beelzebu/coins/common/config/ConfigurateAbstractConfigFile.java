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
import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

/**
 * @author Beelzebu
 */
public abstract class ConfigurateAbstractConfigFile extends AbstractConfigFile {

    private final Path path;
    private ConfigurationNode root;

    public ConfigurateAbstractConfigFile(Path path) {
        this.path = path;
        reload();
    }

    protected abstract ConfigurationLoader<? extends ConfigurationNode> getLoader(Path path);

    @Override
    public final Object get(String path) {
        if (root == null) {
            throw new RuntimeException("Config file is not loaded yet");
        }
        return root.getNode(Splitter.on('.').splitToList(path).toArray()).getValue();
    }

    @Override
    public final Set<String> getConfigurationSection(String path) {
        if (root == null) {
            throw new RuntimeException("Config file is not loaded yet");
        }
        return root.getNode(Splitter.on('.').splitToList(path).toArray()).getChildrenList().stream().map(ConfigurationNode::getKey).map(String::valueOf).collect(Collectors.toSet());
    }

    @Override
    public final void reload() {
        ConfigurationLoader<? extends ConfigurationNode> loader = getLoader(path);
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
