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
package com.github.beelzebu.coins.velocity;

import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.messaging.ProxyMessaging;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.velocity.config.VelocityConfig;
import com.github.beelzebu.coins.velocity.config.VelocityMessages;
import com.github.beelzebu.coins.velocity.messaging.VelocityMessaging;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

/**
 * @author Beelzebu
 */
public class CoinsVelocityMain implements CoinsBootstrap {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final CoinsVelocityPlugin plugin;
    private final Path configDirectory;
    private VelocityMessaging messaging;

    @Inject
    public CoinsVelocityMain(ProxyServer proxyServer, Logger logger, @DataDirectory Path configDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configDirectory = configDirectory;
        plugin = new CoinsVelocityPlugin(this, new VelocityConfig(new File(getDataFolder(), "config.yml").toPath()));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        plugin.load();
        plugin.enable();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onReload(ProxyReloadEvent e) {
        plugin.reload();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
        plugin.disable();
    }

    @Override
    public CoinsPlugin getPlugin() {
        return plugin;
    }

    @Override
    public AbstractConfigFile getFileAsConfig(File file) {
        return new VelocityMessages(file.toPath());
    }

    @Override
    public void runAsync(Runnable rn) {
        proxyServer.getScheduler().buildTask(this, rn).schedule();
    }

    @Override
    public void runSync(Runnable rn) {
        proxyServer.getScheduler().buildTask(this, rn).schedule();
    }

    @Override
    public void schedule(Runnable rn, long interval) {
        proxyServer.getScheduler().buildTask(this, rn).repeat(interval * 50 /* 1 tick = 50 ms*/, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleAsync(Runnable rn, long interval) {
        proxyServer.getScheduler().buildTask(this, rn).repeat(interval * 50 /* 1 tick = 50 ms*/, TimeUnit.MILLISECONDS);
    }

    @Override
    public void executeCommand(String cmd) {
        proxyServer.getCommandManager().execute(proxyServer.getConsoleCommandSource(), cmd);
    }

    @Override
    public void log(String msg) {
        logger.info(msg);
    }

    @Override
    public Object getConsole() {
        return proxyServer.getConsoleCommandSource();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Object commandSender, String msg) {
        if (!(commandSender instanceof CommandSource)) {
            throw new ClassCastException(commandSender.getClass().getName() + "is not an instance of CommandSource");
        }
        CommandSource commandSource = (CommandSource) commandSender;
        commandSource.sendMessage(LegacyComponentSerializer.INSTANCE.deserialize(msg));
    }

    @Override
    public File getDataFolder() {
        return configDirectory.toFile();
    }

    @Override
    public InputStream getResource(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    @Override
    public String getVersion() {
        Optional<PluginContainer> pluginContainer = proxyServer.getPluginManager().getPlugin("coins");
        if (pluginContainer.isPresent()) {
            if (pluginContainer.get().getDescription().getVersion().isPresent()) {
                return pluginContainer.get().getDescription().getVersion().get();
            }
        }
        return "UNKNOWN";
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return proxyServer.getPlayer(uuid).isPresent();
    }

    @Override
    public boolean isOnline(String name) {
        return proxyServer.getPlayer(name).isPresent();
    }

    @Override
    public UUID getUUID(String name) {
        Optional<Player> player = proxyServer.getPlayer(name);
        return player.map(Player::getUniqueId).orElse(null);
    }

    @Override
    public String getName(UUID uuid) {
        Optional<Player> player = proxyServer.getPlayer(uuid);
        return player.map(Player::getUsername).orElse(null);
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        //  TODO
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        // TODO
    }

    @Override
    public List<String> getPermissions(UUID uuid) {
        List<String> permissions = new ArrayList<>();
        return permissions;
    }

    @Override
    public ProxyMessaging getProxyMessaging() {
        return messaging == null ? messaging = new VelocityMessaging(plugin) : messaging;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Path getConfigDirectory() {
        return configDirectory;
    }
}
