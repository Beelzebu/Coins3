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
package com.github.beelzebu.coins.bukkit.command;

import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

/**
 * @author Beelzebu
 */
public class CommandManager {

    private final CoinsPlugin plugin;
    private Command cmd;

    public CommandManager(CoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommand() {
        cmd = new CoinsCommand(plugin.getConfig().getString("General.Command.Coins.Name", "coins")).setDescription(plugin.getConfig().getString("General.Command.Coins.Description", "Base command of the Coins plugin")).setAliases(plugin.getConfig().getStringList("General.Command.Coins.Aliases", new ArrayList<>())).setUsage(plugin.getConfig().getString("General.Command.Coins.Usage", "/coins"));
        cmd.setPermission(plugin.getConfig().getString("General.Command.Coins.Permission", "coins.use"));
        registerCommand((Plugin) plugin.getBootstrap(), cmd);
        Command multiplier = new MultipliersCommand(plugin.getConfig().getString("General.Command.Multiplier.Name", "multiplier")).setDescription(plugin.getConfig().getString("General.Command.Multiplier.Description", "Command to manage coins multipliers")).setAliases(plugin.getConfig().getStringList("General.Command.Multiplier.Aliases", new ArrayList<>())).setUsage(plugin.getConfig().getString("General.Command.Multiplier.Usage", "/multiplier"));
        multiplier.setPermission(plugin.getConfig().getString("General.Command.Multiplier.Permission", "coins.multiplier"));
        registerCommand((Plugin) plugin.getBootstrap(), multiplier);
    }

    public void unregisterCommand() {
        if (cmd != null) {
            unregisterCommand(cmd);
        }
    }

    private void registerCommand(Plugin plugin, Command cmd) {
        unregisterCommand(cmd);
        getCommandMap().register(plugin.getName(), cmd);
    }

    private void unregisterCommand(Command cmd) {
        Map<String, Command> knownCommands = getKnownCommandsMap();
        knownCommands.remove(cmd.getName());
        cmd.getAliases().forEach(knownCommands::remove);
    }

    private Object getPrivateField(Object object, String field) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        return objectField.get(object);
    }

    private SimpleCommandMap getCommandMap() {
        try {
            return (SimpleCommandMap) getPrivateField(Bukkit.getPluginManager(), "commandMap");
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            return new SimpleCommandMap(Bukkit.getServer());
        }
    }

    private Map<String, Command> getKnownCommandsMap() {
        try {
            return (Map<String, Command>) getPrivateField(getCommandMap(), "knownCommands");
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            return new HashMap<>();
        }
    }
}
