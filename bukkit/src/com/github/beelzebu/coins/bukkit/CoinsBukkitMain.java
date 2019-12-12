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
package com.github.beelzebu.coins.bukkit;

import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.config.CoinsConfig;
import com.github.beelzebu.coins.api.messaging.ProxyMessaging;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bukkit.command.CommandManager;
import com.github.beelzebu.coins.bukkit.config.BukkitConfig;
import com.github.beelzebu.coins.bukkit.config.BukkitMessages;
import com.github.beelzebu.coins.bukkit.events.CoinsChangeEvent;
import com.github.beelzebu.coins.bukkit.events.MultiplierEnableEvent;
import com.github.beelzebu.coins.bukkit.messaging.BukkitMessaging;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

public class CoinsBukkitMain extends JavaPlugin implements CoinsBootstrap {

    @Getter
    private CommandManager commandManager;
    @Getter
    private CoinsBukkitPlugin plugin;
    private BukkitMessaging messaging;

    @Override
    public void onLoad() {
        plugin = new CoinsBukkitPlugin(this, new BukkitConfig(getConfig(), null));
        plugin.load();
    }

    @Override
    public void onDisable() {
        plugin.disable();
    }

    @Override
    public void onEnable() {
        commandManager = new CommandManager(plugin);
        plugin.enable();
    }

    @Override
    public CoinsConfig getPluginConfig() {
        return plugin.getConfig();
    }

    @Override
    public AbstractConfigFile getFileAsConfig(File file) {
        return new BukkitMessages(file);
    }

    @Override
    public void runAsync(Runnable rn) {
        Bukkit.getScheduler().runTaskAsynchronously(this, rn);
    }

    @Override
    public void runSync(Runnable rn) {
        Bukkit.getScheduler().runTask(this, rn);
    }

    @Override
    public void schedule(Runnable rn, long interval) {
        Bukkit.getScheduler().runTaskTimer(this, rn, 0, interval);
    }

    @Override
    public void scheduleAsync(Runnable rn, long interval) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, rn, 0, interval);
    }

    @Override
    public void executeCommand(String cmd) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(StringUtils.rep("&8[&cCoins&8] &7" + msg));
    }

    @Override
    public Object getConsole() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public void sendMessage(Object commandsender, String msg) {
        ((CommandSender) commandsender).sendMessage(msg);
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public boolean isOnline(String name) {
        return Bukkit.getPlayer(name) != null;
    }

    @Override
    public UUID getUUID(String name) {
        Player player = Bukkit.getPlayer(name);
        return player != null ? player.getUniqueId() : null;
    }

    @Override
    public String getName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : null;
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        runAsync(() -> Bukkit.getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins)));
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        runAsync(() -> Bukkit.getPluginManager().callEvent(new MultiplierEnableEvent(multiplier)));
    }

    @Override
    public List<String> getPermissions(UUID uuid) {
        List<String> permissions = new ArrayList<>();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).forEach(permissions::add);
        }
        return permissions;
    }

    @Override
    public ProxyMessaging getBungeeMessaging() {
        return messaging == null ? messaging = new BukkitMessaging() : messaging;
    }
}
