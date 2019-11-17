/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.github.beelzebu.coins.common.dependency;

import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DependencyRegistry {

    private final CoinsPlugin plugin;
    private final Map<StorageType, List<Dependency>> STORAGE_DEPENDENCIES = ImmutableMap.<StorageType, List<Dependency>>builder().put(StorageType.MARIADB, ImmutableList.of(Dependency.MARIADB_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).put(StorageType.MYSQL, ImmutableList.of(Dependency.MYSQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).put(StorageType.SQLITE, ImmutableList.of(Dependency.SQLITE_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).build();

    public Set<Dependency> resolveStorageDependencies(StorageType storageType) {
        Set<Dependency> dependencies = new LinkedHashSet<>(STORAGE_DEPENDENCIES.get(storageType));
        if (plugin.getConfig().getMessagingService().equals(MessagingServiceType.REDIS)) {
            LinkedHashSet<Dependency> jedis = new LinkedHashSet<>();
            jedis.add(Dependency.COMMONS_POOL_2);
            jedis.add(Dependency.JEDIS);
            dependencies.addAll(jedis);
            plugin.log("Loading messaging service dependencies: " + jedis);
        }
        if (classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory")) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }
        return dependencies;
    }

    public boolean shouldAutoLoad(Dependency dependency) {
        switch (dependency) {
            case ASM:
            case ASM_COMMONS:
            case JAR_RELOCATOR:
                return false;
            default:
                return true;
        }
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
