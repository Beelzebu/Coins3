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
package com.github.beelzebu.coins.common.plugin;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.cache.CacheProvider;
import com.github.beelzebu.coins.api.cache.CacheType;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.config.CoinsConfig;
import com.github.beelzebu.coins.api.config.MultipliersConfig;
import com.github.beelzebu.coins.api.executor.Executor;
import com.github.beelzebu.coins.api.executor.ExecutorManager;
import com.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import com.github.beelzebu.coins.api.messaging.MessagingServiceType;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.storage.StorageProvider;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.common.cache.LocalCache;
import com.github.beelzebu.coins.common.cache.RedisCache;
import com.github.beelzebu.coins.common.config.MultipliersConfigImpl;
import com.github.beelzebu.coins.common.dependency.Dependency;
import com.github.beelzebu.coins.common.dependency.DependencyManager;
import com.github.beelzebu.coins.common.dependency.DependencyRegistry;
import com.github.beelzebu.coins.common.dependency.classloader.ReflectionClassLoader;
import com.github.beelzebu.coins.common.messaging.DummyMessaging;
import com.github.beelzebu.coins.common.messaging.RedisMessaging;
import com.github.beelzebu.coins.common.storage.MySQL;
import com.github.beelzebu.coins.common.storage.SQLite;
import com.github.beelzebu.coins.common.utils.FileManager;
import com.github.beelzebu.coins.common.utils.RedisManager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Beelzebu
 */
public class CommonCoinsPlugin <T extends CoinsBootstrap> implements CoinsPlugin<T> {

    @NotNull
    private final T bootstrap;
    private final CoinsConfig config;
    @NotNull
    private final MultipliersConfig multipliersConfig;
    @NotNull
    private final DependencyManager dependencyManager;
    @NotNull
    private final FileManager fileManager;
    private final Map<String, AbstractConfigFile> messagesMap = new HashMap<>();
    @Nullable
    private MessagingServiceType messagingServiceType;
    @Nullable
    private AbstractMessagingService messagingService;
    @Nullable
    private StorageType storageType;
    @Nullable
    private StorageProvider storageProvider;
    @Nullable
    private CacheType cacheType;
    @Nullable
    private CacheProvider cache;
    @Nullable
    private RedisManager redisManager;
    private boolean logEnabled = false;

    public CommonCoinsPlugin(@NotNull T bootstrap, CoinsConfig config) {
        Objects.requireNonNull(bootstrap, "CoinsBootstrap can't be null");
        Objects.requireNonNull(config, "CoinsConfig can't be null");
        this.config = config;
        this.bootstrap = bootstrap;
        multipliersConfig = new MultipliersConfigImpl(this, bootstrap.getFileAsConfig(new File(bootstrap.getDataFolder(), "multipliers.yml")));
        dependencyManager = new DependencyManager(this, new ReflectionClassLoader(bootstrap), new DependencyRegistry());
        fileManager = new FileManager(this);
    }

    @Override
    public void load() {
        try {
            fileManager.onLoad();
        } catch (IOException ex) {
            log("An exception has occurred copying default files.");
            debug(ex);
        }
    }

    @Override
    public void enable() { // now the plugin is enabled and we can read config files
        Objects.requireNonNull(config, "Config file is null");
        Arrays.asList(Objects.requireNonNull(fileManager.getMessagesFolder().listFiles())).forEach(file -> messagesMap.put((file.getName().split("_").length == 2 ? file.getName().split("_")[1] : "default").split(".yml")[0], bootstrap.getFileAsConfig(file)));
        // update files before we read something
        fileManager.onEnable();
        logEnabled = getConfig().isDebugFile();
        // identify storage, messaging service and cache types and load dependencies
        storageType = getConfig().getStorageType();
        dependencyManager.loadStorageDependencies(storageType);
        messagingServiceType = getConfig().getMessagingServiceType();
        cacheType = getConfig().getCacheType();
        if (Objects.equals(messagingServiceType, MessagingServiceType.REDIS) || Objects.equals(cacheType, CacheType.REDIS)) {
            log("Loading JEDIS dependency for redis connections...");
            Set<Dependency> jedisDependency = new HashSet<>();
            jedisDependency.add(Dependency.JEDIS);
            dependencyManager.loadDependencies(jedisDependency);
            redisManager = new RedisManager(this);
            redisManager.start();
        }
        // try to migrate from v2 if possible
        migrateFromV2();
        // load local data
        loadExecutors();
        // setup storage and start messaging service
        if (getStorageProvider() != null) {
            getStorageProvider().setup();
        }
        if (getCache() != null) {
            getCache().start();
        }
        if (getMessagingService() != null) {
            getMessagingService().start();
        }
        motd(true);
        // now that everything is running we'll get things from other servers
        if (getMessagingService() != null) {
            getMessagingService().requestMultipliers();
            getMessagingService().requestExecutors();
        }
        if (getStorageProvider() == null || getCache() == null || getMessagingService() == null) {
            bootstrap.log("Can't setup API");
            bootstrap.log("Storage: " + (getStorageProvider() != null ? getStorageProvider().getStorageType() : "null"));
            bootstrap.log("Cache: " + (getCache() != null ? getCache().getCacheType() : "null"));
            bootstrap.log("Messaging: " + (getMessagingService() != null ? getMessagingService().getType() : "null"));
            return;
        }
        CoinsAPI.setPlugin(this);
    }


    @Override
    public void reload() { // use instances from bootstrap
        getBootstrap().getPlugin().disable();
        getBootstrap().getPlugin().enable();
    }

    @Override
    public void disable() {
        if (getCache() != null) { // stop cache
            getCache().stop();
        }
        if (getMessagingService() != null) { // stop messaging service
            getMessagingService().stop();
        }
        if (redisManager != null) { // stop redis manager if was in use by cache or messaging service
            redisManager.stop();
        }
        if (getStorageProvider() != null) { // stop storage provider
            getStorageProvider().shutdown();
        }
        motd(false);
    }

    @Nullable
    @Override
    public AbstractMessagingService getMessagingService() {
        if (messagingService != null) {
            return messagingService;
        }
        if (messagingServiceType == null) {
            return null;
        }
        switch (messagingServiceType) {
            case PROXY:
                return messagingService = bootstrap.getProxyMessaging();
            case REDIS:
                return messagingService = new RedisMessaging(this, redisManager);
            case NONE:
            default:
                return messagingService = new DummyMessaging(this);
        }
    }

    @Nullable
    @Override
    public final CacheProvider getCache() {
        if (cache != null) {
            return cache;
        }
        if (cacheType == null) {
            return null;
        }
        switch (cacheType) {
            case REDIS:
                return cache = new RedisCache(this, redisManager);
            case LOCAL:
            default:
                dependencyManager.loadDependencies(EnumSet.of(Dependency.CAFFEINE));
                return cache = new LocalCache(this);
        }
    }

    @Nullable
    @Override
    public final StorageProvider getStorageProvider() {
        if (storageProvider != null) {
            return storageProvider;
        }
        if (storageType == null) {
            return null;
        }
        switch (storageType) {
            case MARIADB:
            case MYSQL:
                return storageProvider = new MySQL(this);
            case SQLITE:
            default:
                return storageProvider = new SQLite(this);
        }
    }

    @Override
    public void setStorageType(StorageType storageType) {
        if (!Objects.equals(storageType, this.storageType)) {
            storageProvider = null;
            this.storageType = storageType;
            StorageProvider storageProvider = getStorageProvider();
            if (storageProvider != null) {
                storageProvider.setup();
            } else {
                log("Can't setup storage provider for '" + storageType + "'");
            }
        }
    }

    @Override
    public final void loadExecutors() {
        AbstractConfigFile executorsConfig = bootstrap.getFileAsConfig(new File(bootstrap.getDataFolder(), "executors.yml"));
        executorsConfig.getConfigurationSection("Executors").forEach(id -> ExecutorManager.addExecutor(new Executor(id, executorsConfig.getString("Executors." + id + ".Displayname", id), executorsConfig.getDouble("Executors." + id + ".Cost", 0), executorsConfig.getStringList("Executors." + id + ".Command"))));
    }

    @Override
    public final void log(@NotNull String message, Object... replace) {
        bootstrap.log(message);
        fileManager.logToFile(message);
    }

    @Override
    public final void debug(@NotNull String message, Object... replace) {
        if (getConfig().isDebug()) {
            bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&8[&cCoins&8] &cDebug: &7" + message));
        }
        fileManager.logToFile(message);
    }

    @Override
    public final void debug(Exception ex) {
        if (ex instanceof SQLException) {
            debug((SQLException) ex);
        } else {
            debug("Unknown Exception:");
            debug("   Error message: " + ex.getMessage());
            debug("   Stacktrace: \n" + getStackTrace(ex));
        }
    }

    @Override
    public final void debug(@NotNull SQLException ex) {
        debug("SQLException:");
        debug("   Database state: " + ex.getSQLState());
        debug("   Error code: " + ex.getErrorCode());
        debug("   Error message: " + ex.getMessage());
        debug("   Stacktrace: \n" + getStackTrace(ex));
    }

    @Override
    public String getStackTrace(@NotNull Exception ex) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            ex.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Error getting the stacktrace";
    }

    @Override
    public final UUID getUniqueId(@NotNull String name, boolean fromdb) {
        name = name.toLowerCase();
        if (!fromdb && bootstrap.getUUID(name) != null) {
            return bootstrap.getUUID(name);
        }
        StorageProvider storageProvider = getStorageProvider();
        if (storageProvider != null) {
            return storageProvider.getUUID(name);
        }
        return null;
    }

    @Override
    public final String getName(UUID uniqueId, boolean fromdb) {
        if (!fromdb && bootstrap.getName(uniqueId) != null) {
            return bootstrap.getName(uniqueId);
        }
        StorageProvider storageProvider = getStorageProvider();
        if (storageProvider != null) {
            return storageProvider.getName(uniqueId);
        }
        return null;
    }

    @Override
    public CoinsConfig getConfig() {
        return config;
    }

    @NotNull
    @Override
    public MultipliersConfig getMultipliersConfig() {
        return multipliersConfig;
    }

    @Override
    public final AbstractConfigFile getMessages(@NotNull String locale) {
        return Optional.ofNullable(messagesMap.get(locale.split("_")[0])).orElse(messagesMap.get("default"));
    }

    @NotNull
    @Override
    public final String getString(String path, @NotNull String locale) {
        return StringUtils.rep(getMessages(locale).getString(path, StringUtils.rep(getMessages("").getString(path, ""))));
    }

    @NotNull
    @Override
    public final List<String> getStringList(String path, @NotNull String locale) {
        return StringUtils.rep(getMessages(locale).getStringList(path, StringUtils.rep(getMessages("").getStringList(path, Collections.emptyList()))));
    }

    @Override
    public final void reloadMessages() {
        messagesMap.keySet().forEach(lang -> messagesMap.get(lang).reload());
    }

    @Override
    public final String removeColor(String string) {
        return ChatColor.stripColor(string);
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    @NotNull
    public FileManager getFileManager() {
        return fileManager;
    }

    @NotNull
    @Override
    public T getBootstrap() {
        return bootstrap;
    }

    @Override
    public void setMessagingServiceType(@NotNull MessagingServiceType messagingServiceType) {
        Objects.requireNonNull(messagingServiceType, "messagingServiceType can't be null");
        this.messagingServiceType = messagingServiceType;
    }

    @Override
    public void setMessagingService(@NotNull AbstractMessagingService messagingService) {
        Objects.requireNonNull(messagingService, "messagingService can't be null");
        this.messagingService = messagingService;
    }

    @Override
    public void setStorageProvider(@NotNull StorageProvider storageProvider) {
        Objects.requireNonNull(storageProvider, "storageProvider can't be null");
        this.storageProvider = storageProvider;
    }

    @Override
    public void setCacheType(@NotNull CacheType cacheType) {
        Objects.requireNonNull(cacheType, "cacheType can't be null");
        this.cacheType = cacheType;
    }

    @Override
    public void setCache(@NotNull CacheProvider cache) {
        Objects.requireNonNull(cache, "cache can't be null");
        this.cache = cache;
    }

    private void motd(boolean enable) {
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("           &4Coins &fBy:  &7Beelzebu"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        StringBuilder version = new StringBuilder();
        int spaces = (42 - ("v: " + bootstrap.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version.append(" ");
        }
        version.append(StringUtils.rep("&4v: &f" + bootstrap.getVersion()));
        bootstrap.sendMessage(bootstrap.getConsole(), version.toString());
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        // Only send this in the onEnable
        if (enable) {
            fileManager.logToFile("Enabled Coins v: " + bootstrap.getVersion());
            debug("Debug mode is enabled.");
            if (!logEnabled) {
                debug("Logging to file is disabled, all debug messages will be sent to the console.");
            }
            debug("Using \"" + storageType + "\" as storage.");
            debug("Using \"" + messagingServiceType + "\" as messaging service.");
            debug("Using \"" + cacheType + "\" as cache.");
            bootstrap.runAsync(() -> { // run update check async, so it doesn't delay the startup
                if (bootstrap.getVersion().contains("SNAPSHOT")) {
                    log("You're using a development version, be careful!");
                    return;
                }
                String upt = "You have the newest version";
                String response = getFromURL("https://api.spigotmc.org/legacy/update.php?resource=48536");
                if (response == null) {
                    upt = "Failed to check for updates :(";
                } else if (!response.equalsIgnoreCase(bootstrap.getVersion())) {
                    upt = "There is a new version available! [" + response + "]";
                }
                log(upt);
            });
        }
    }


    @Nullable
    private String getFromURL(@NotNull String surl) {
        String response = null;
        try {
            URL url = new URL(surl);
            try (Scanner s = new Scanner(url.openStream())) {
                if (s.hasNext()) {
                    response = s.next();
                }
            }
        } catch (IOException ex) {
            debug("Failed to connect to URL: " + surl);
        }
        return response;
    }

    private void migrateFromV2() {
        if (storageType == null) {
            log("Can't migrate from V2, storage is null.");
            return;
        }
        if (storageType.equals(StorageType.SQLITE) && getConfig().getInt("Database Version", 1) < 2) {
            try {
                Files.move(new File(bootstrap.getDataFolder(), "database.db").toPath(), new File(bootstrap.getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has occurred moving the old database");
                debug(ex);
            }
        }
    }
}
