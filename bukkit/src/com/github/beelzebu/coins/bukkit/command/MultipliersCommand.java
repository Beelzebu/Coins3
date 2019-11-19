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

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierType;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.utils.UUIDUtil;
import com.github.beelzebu.coins.bukkit.menus.CoinsMenu;
import com.github.beelzebu.coins.bukkit.menus.PaginatedMenu;
import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class MultipliersCommand extends AbstractCoinsCommand {

    // TODO: update messages files to match new menus

    private final AbstractConfigFile multipliersConfig = plugin.getBootstrap().getFileAsConfig(new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml"));

    MultipliersCommand(String command) {
        super(command);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        execute(sender, args, sender instanceof Player ? CompatUtils.getLocale((Player) sender) : "");
        return true;
    }

    private void execute(CommandSender sender, String[] args, String lang) {
        if (getPermission() != null && !sender.hasPermission(getPermission())) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                createMultipliersMenu(false, player.getUniqueId(), lang).open(player);
            } else {
                sender.sendMessage(plugin.getString("Errors.Console", lang));
            }
            return;
        } else {
            if (!sender.hasPermission(getPermission() + ".admin.multiplier")) {
                return;
            }
            switch (args[0].toLowerCase()) {
                case "help":
                    _help(sender, lang, args);
                    break;
                case "create":
                    _create(sender, lang, args);
                    break;
                case "enable": // add a command to enable any multiplier by the ID
                    break;
                case "disable":
                    break;
                case "edit": // add a command to edit existing multipliers
                    break;
                default: {
                    String name = args[0];
                    if (CoinsAPI.isindb(name)) {
                        createMultipliersMenu(true, UUIDUtil.getUniqueId(name), lang);
                    }
                    break;
                }
            }
        }
        if (args[0].equalsIgnoreCase("get")) {
            sender.sendMessage(CoinsAPI.getMultipliers().stream().findFirst().get().getMultiplierTimeFormatted());
        }
    }

    private void _help(CommandSender sender, String lang, String... args) {
        plugin.getStringList("Help.Multiplier", lang).forEach(sender::sendMessage);
    }

    private void _create(CommandSender sender, String lang, String... args) {
        if (args.length >= 4 && args.length <= 6 && CoinsAPI.isindb(args[1])) {
            if (!isNumber(args[2]) || !isNumber(args[3])) {
                sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
                return;
            }
            int multiplier = Integer.parseInt(args[2]);
            int minutes = Integer.parseInt(args[3]);
            String server = args.length == 5 && !args[4].equals("") ? args[4] : plugin.getConfig().getServerName();
            MultiplierType type = args.length == 6 && args[5] != null && Stream.of(MultiplierType.values()).map(Enum::toString).anyMatch(mtype -> mtype.equalsIgnoreCase(args[5])) ? MultiplierType.valueOf(args[5].toUpperCase()) : MultiplierType.SERVER;
            CoinsAPI.createMultiplier(plugin.getUniqueId(args[1], false), multiplier, minutes, server, type);
            sender.sendMessage(plugin.getString("Multipliers.Created", lang).replaceAll("%player%", args[1]));
        } else {
            sender.sendMessage(plugin.getString("Help.Multiplier Create", lang));
        }
    }

    private CoinsMenu createMultipliersMenu(boolean admin, UUID target, String lang) {
        String extraTitle = (admin ? " " + plugin.getName(target, true) : "");
        return new CoinsMenu(multipliersConfig.getInt("Menus.Main.Size"), plugin.getString("Menus.Main.Title", lang) + extraTitle) {
            @Override
            public void open(Player p) {
                setItems(p);
                super.open(p);
            }

            private void setItems(Player player) {
                String section = "Menus.Main.Items";
                multipliersConfig.getConfigurationSection(section).forEach(key -> setItem(multipliersConfig.getInt(section + "." + key + ".Slot"), getItem(multipliersConfig, section + "." + key, player), p -> {
                    List<String> commands = multipliersConfig.getStringList(section + "." + key + ".Commands", new ArrayList<>());
                    p.closeInventory();
                    String menu = multipliersConfig.getString(section + "." + key + ".Menu");
                    if (menu != null && (Objects.equals(menu.toLowerCase(), "local") || Objects.equals(menu.toLowerCase(), "global"))) {
                        Collection<Multiplier> multipliers;
                        boolean global = menu.toLowerCase().equals("global");
                        if (global) {
                            multipliers = CoinsAPI.getMultipliersFor(target);
                        } else {
                            multipliers = CoinsAPI.getMultipliersFor(target, true);
                        }
                        PaginatedMenu.createPaginatedGUI(p, global, multipliers, extraTitle).open(p);
                    }
                    commands.forEach(cmd -> {
                        String command = cmd.toLowerCase();
                        if (command.startsWith("console: ")) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), normalizeCommand(command));
                        } else {
                            Bukkit.dispatchCommand(p, normalizeCommand(command));
                        }
                    });
                }));
            }

            private String normalizeCommand(String command) {
                command = command.split(":")[1];
                while (command.startsWith(" ")) {
                    command = command.replaceFirst(" ", "");
                }
                return command;
            }
        };
    }
}
