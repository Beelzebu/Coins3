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
package com.github.beelzebu.coins.bukkit;

import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.messaging.ProxyMessaging;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bukkit.command.CommandManager;
import com.github.beelzebu.coins.bukkit.config.BukkitCoinsConfig;
import com.github.beelzebu.coins.bukkit.config.BukkitConfigFile;
import com.github.beelzebu.coins.bukkit.messaging.BukkitMessaging;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CoinsBukkitMain extends JavaPlugin implements CoinsBootstrap {

    private CommandManager commandManager;
    private CoinsBukkitPlugin plugin;
    private BukkitMessaging messaging;

    @Override
    public void onLoad() {
        plugin = new CoinsBukkitPlugin(this, new BukkitCoinsConfig(getConfig()));
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
    public CoinsBukkitPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public AbstractConfigFile getFileAsConfig(File file) {
        return new BukkitConfigFile(file);
    }

    @Override
    public void runAsync(@NotNull Runnable rn) {
        Bukkit.getScheduler().runTaskAsynchronously(this, rn);
    }

    @Override
    public void runSync(@NotNull Runnable rn) {
        Bukkit.getScheduler().runTask(this, rn);
    }

    @Override
    public void schedule(@NotNull Runnable rn, long interval) {
        Bukkit.getScheduler().runTaskTimer(this, rn, 0, interval);
    }

    @Override
    public void scheduleAsync(@NotNull Runnable rn, long interval) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, rn, 0, interval);
    }

    @Override
    public void executeCommand(@NotNull String cmd) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(StringUtils.rep("&8[&cCoins&8] &7" + msg));
    }

    @NotNull
    @Override
    public Object getConsole() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public void sendMessage(@NotNull Object commandSender, @NotNull String msg) {
        if (!(commandSender instanceof CommandSender)) {
            throw new IllegalArgumentException(commandSender + " is not an instance of CommandSender");
        }
        ((CommandSender) commandSender).sendMessage(msg);
    }

    @NotNull
    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public boolean isOnline(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public boolean isOnline(@NotNull String name) {
        return Bukkit.getPlayer(name) != null;
    }

    @Nullable
    @Override
    public UUID getUUID(@NotNull String name) {
        Player player = Bukkit.getPlayer(name);
        return player != null ? player.getUniqueId() : null;
    }

    @Nullable
    @Override
    public String getName(@NotNull UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : null;
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        // TODO: re add
        //runAsync(() -> Bukkit.getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins)));
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        // TODO: re add
        //runAsync(() -> Bukkit.getPluginManager().callEvent(new MultiplierEnableEvent(multiplier)));
    }

    @NotNull
    @Override
    public List<String> getPermissions(@NotNull UUID uuid) {
        List<String> permissions = new ArrayList<>();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).forEach(permissions::add);
        }
        return permissions;
    }

    @NotNull
    @Override
    public ProxyMessaging getProxyMessaging() {
        return messaging == null ? messaging = new BukkitMessaging(plugin) : messaging;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
