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
package com.github.beelzebu.coins.bukkit.command;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.CoinsResponse;
import com.github.beelzebu.coins.api.CoinsUser;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierData;
import com.github.beelzebu.coins.api.executor.Executor;
import com.github.beelzebu.coins.api.executor.ExecutorManager;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import com.github.beelzebu.coins.bukkit.importer.BukkitImporter;
import com.github.beelzebu.coins.bukkit.utils.CoinsEconomy;
import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import com.github.beelzebu.coins.common.importer.ImportManager;
import com.github.beelzebu.coins.common.importer.PluginToImport;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class CoinsCommand extends AbstractCoinsCommand {

    private final DecimalFormat df = new DecimalFormat("#.#");

    CoinsCommand(CoinsBukkitPlugin plugin, @NotNull String command) {
        super(plugin, command);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        String lang = sender instanceof Player ? CompatUtils.getLocale((Player) sender).split("_")[0] : "";
        execute(sender, args, lang);
        return true;
    }

    private void execute(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (getPermission() != null && !sender.hasPermission(getPermission())) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length == 0) {
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getString("Coins.Own coins", lang).replace("%coins%", CoinsAPI.getCoinsString(sender.getName())));
            } else {
                sender.sendMessage(plugin.getString("Errors.Console", ""));
            }
        } else if (args[0].equalsIgnoreCase("execute")) {
            executor(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("help") && args.length == 1) {
            help(sender, lang);
        } else if (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("p")) {
            pay(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("give")) {
            give(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("take")) {
            take(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("reset")) {
            reset(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("set")) {
            set(sender, args, lang);
        } else if (args[0].equalsIgnoreCase("top") && args.length == 1) {
            top(sender, lang);
        } else if (args[0].equalsIgnoreCase("import")) {
            importer(sender, args);
        } else if (args[0].equalsIgnoreCase("importdb")) {
            importDB(sender, args);
        } else if (args[0].equalsIgnoreCase("reload")) {
            reload(sender);
        } else if (args[0].equalsIgnoreCase("about")) {
            about(sender);
        } else if (args.length == 1 && CoinsAPI.isindb(args[0])) {
            target(sender, args, lang);
        } else {
            sender.sendMessage(plugin.getString("Errors.Unknown command", lang));
        }
    }

    private void help(@NotNull CommandSender sender, @NotNull String lang) {
        plugin.getMessages(lang).getStringList("Help.User").forEach(line -> sender.sendMessage(StringUtils.rep(line)));
        if (sender.hasPermission(getPermission() + ".admin.help")) {
            plugin.getMessages(lang).getStringList("Help.Admin").forEach(line -> sender.sendMessage(StringUtils.rep(line)));
        }
    }

    private void target(@NotNull CommandSender sender, String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".target")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        sender.sendMessage(plugin.getString("Coins.Get", lang).replace("%coins%", CoinsAPI.getCoinsString(args[0])).replace("%target%", args[0]));
    }

    private void pay(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".pay")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args[1].equalsIgnoreCase("?") || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Pay Usage", lang));
            return;
        }
        if (sender instanceof Player && args.length == 3 && !args[1].equalsIgnoreCase(sender.getName())) {
            if (CoinsAPI.isindb(args[1])) {
                double coins = Double.parseDouble(args[2]);
                if (coins > 0) {
                    CoinsResponse response = CoinsAPI.payCoins(((Player) sender).getUniqueId(), CoinsAPI.getPlugin().getUniqueId(args[1], false), coins);
                    response.ifSuccess(r -> {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (!plugin.getString("Coins.Pay", lang).equals("")) {
                            sender.sendMessage(plugin.getString("Coins.Pay", lang).replace("%coins%", new DecimalFormat("#.#").format(coins)).replace("%target%", target != null ? target.getName() : args[1]));
                        }
                        if (target != null && !plugin.getString("Coins.Pay target", CompatUtils.getLocale(target)).equals("")) {
                            target.sendMessage(plugin.getString("Coins.Pay target", CompatUtils.getLocale(target)).replace("%coins%", df.format(coins)).replace("%from%", sender.getName()));
                        }
                    });
                    response.ifFailed(r -> sender.sendMessage(r.getMessage(CompatUtils.getLocale((Player) sender))));
                } else {
                    sender.sendMessage(plugin.getString("Errors.No Zero", lang));
                }
            } else {
                sender.sendMessage(plugin.getString("Errors.Unknown player", lang));
            }
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getString("Errors.Console", lang));
        }
    }

    private void give(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.give")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if ((args.length != 3 && args.length != 4) || !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Give Usage", lang));
            return;
        }
        UUID targetUniqueId = plugin.getUniqueId(args[1], false);
        if (targetUniqueId == null || !CoinsAPI.isindb(targetUniqueId)) {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replace("%target%", args[1]));
            return;
        }
        double coins = Double.parseDouble(args[2]);
        boolean multiply = args.length == 4 && getBoolean(args[3].toLowerCase());

        CoinsResponse response = CoinsAPI.addCoins(targetUniqueId, coins, multiply);
        response.ifSuccess(r -> {
            String multiplierFormat = "";
            Player target = Bukkit.getPlayer(targetUniqueId);
            String targetName = target != null ? target.getName() : args[1];
            if (!plugin.getString("Coins.Give", lang).equals("")) {
                sender.sendMessage(plugin.getString("Coins.Give", lang).replace("%coins%", df.format(coins)).replace("%target%", targetName));
            }
            if (target != null && target.isOnline()) {
                if (multiply) {
                    Collection<Multiplier> usableMultipliers = CoinsAPI.getUsableMultipliers(targetUniqueId);
                    int multiplyAmmount = usableMultipliers.stream().mapToInt(multiplier -> multiplier.getData().getAmount()).sum();
                    if (multiplyAmmount >= 0) {
                        multiplierFormat = plugin.getString("Multipliers.Format", CompatUtils.getLocale(target)).replace("%multiplier%", df.format(multiplyAmmount)).replace("%enabler%", CoinsAPI.getUsableMultipliers(target.getUniqueId()).stream().map(Multiplier::getData).map(MultiplierData::getEnablerName).collect(Collectors.joining(", ")));
                    }
                }
                if (!plugin.getString("Coins.Give target", CompatUtils.getLocale(target)).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Give target", CompatUtils.getLocale(target)).replace("%coins%", df.format(coins)).replace("%multiplier_format%", multiplierFormat));
                }
            }
        });
        response.ifFailed(r -> sender.sendMessage(r.getMessage(sender instanceof Player ? CompatUtils.getLocale(((Player) sender)) : "")));
    }

    private void take(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.take")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Take Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replace("%target%", args[1]));
            return;
        }
        double currentCoins = CoinsAPI.getCoins(args[1]);
        double coins = Double.parseDouble(args[2]);
        if (currentCoins < coins) {
            sender.sendMessage(plugin.getString("Errors.No Negative", lang));
            return;
        }
        double finalCoins = currentCoins - coins;
        if (args.length == 3) {
            Player target = Bukkit.getPlayer(args[1]);
            CoinsResponse response = CoinsAPI.takeCoins(args[1], coins);
            response.ifSuccess(r -> {
                if (!plugin.getString("Coins.Take", lang).equals("")) {
                    sender.sendMessage(plugin.getString("Coins.Take", lang).replace("%coins%", df.format(coins)).replace("%newcoins%", df.format(finalCoins)).replace("%target%", args[1]));
                }
                if (target != null && !plugin.getString("Coins.Take target", CompatUtils.getLocale(target)).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Take target", CompatUtils.getLocale(target)).replace("%coins%", df.format(finalCoins)));
                }
            });
            response.ifFailed(r -> sender.sendMessage(sender instanceof Player ? CompatUtils.getLocale(((Player) sender)) : ""));
        }
    }

    private void reset(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.reset")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getString("Help.Reset Usage", lang));
            return;
        }
        if (!CoinsAPI.isindb(args[1])) {
            sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replace("%target%", args[1]));
            return;
        }
        CoinsResponse response = CoinsAPI.resetCoins(args[1]);
        response.ifSuccess(r -> {
            Player target = Bukkit.getPlayer(args[1]);
            if (!plugin.getString("Coins.Reset", lang).equals("")) {
                sender.sendMessage(plugin.getString("Coins.Reset", lang).replace("%target%", args[1]));
            }
            if (target != null && !plugin.getString("Coins.Reset target", CompatUtils.getLocale(target)).equals("")) {
                target.sendMessage(plugin.getString("Coins.Reset target", CompatUtils.getLocale(target)));
            }
        });
        response.ifFailed(r -> sender.sendMessage(r.getMessage(sender instanceof Player ? CompatUtils.getLocale((Player) sender) : "")));
    }

    private void set(@NotNull CommandSender sender, @NotNull String[] args, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".admin.set")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        if (args.length < 3 || args.length == 3 && !isNumber(args[2])) {
            sender.sendMessage(plugin.getString("Help.Set Usage", lang));
            return;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                double coins = Double.parseDouble(args[2]);
                if (CoinsAPI.isindb(args[1])) {
                    CoinsAPI.setCoins(args[1], coins);
                    if (!plugin.getString("Coins.Set", lang).equals("")) {
                        sender.sendMessage(plugin.getString("Coins.Set", lang).replace("%target%", args[1]).replace("%coins%", df.format(coins)));
                    }
                } else {
                    sender.sendMessage(plugin.getString("Errors.Unknown player", lang).replace("%target%", args[1]));
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && !plugin.getString("Coins.Set target", CompatUtils.getLocale(target)).equals("")) {
                    target.sendMessage(plugin.getString("Coins.Set target", CompatUtils.getLocale(target)).replace("%coins%", args[2]));
                }
            }
        }
    }

    private void top(@NotNull CommandSender sender, @NotNull String lang) {
        if (!sender.hasPermission(getPermission() + ".top")) {
            sender.sendMessage(plugin.getString("Errors.No permissions", lang));
            return;
        }
        sender.sendMessage(plugin.getString("Coins.Top.Header", lang));
        int i = 0;
        for (CoinsUser coinsTopEntry : CoinsAPI.getTopPlayers(10)) {
            if (coinsTopEntry == null) {
                continue;
            }
            Player player = Bukkit.getPlayer(coinsTopEntry.getUniqueId());
            sender.sendMessage(plugin.getString("Coins.Top.List", lang)
                    .replace("%top%", Integer.toString(++i))
                    .replace("%player%", player != null ? player.getName() : coinsTopEntry.getName())
                    .replace("%coins%", df.format(coinsTopEntry.getCoins())));
        }
    }

    private void executor(CommandSender sender, String[] args, @NotNull String lang) {
        if (sender instanceof Player) {
            Executor ex = ExecutorManager.getExecutor(args[1]);
            if (ex == null) {
                sender.sendMessage(plugin.getString("Errors.No Execute", lang));
            } else {
                if (ex.getCost() > 0) {
                    if (CoinsAPI.getCoins(((Player) sender).getUniqueId()) >= ex.getCost()) {
                        CoinsAPI.takeCoins(((Player) sender).getUniqueId(), ex.getCost());
                    } else {
                        sender.sendMessage(plugin.getString("Errors.No Coins", lang));
                        return;
                    }
                }
                if (!ex.getCommands().isEmpty()) {
                    plugin.getBootstrap().runSync(() -> { // who knows ¯\_(ツ)_/¯
                        String command;
                        for (String str : ex.getCommands()) {
                            command = StringUtils.rep(str).replace("%player%", sender.getName());
                            if (command.startsWith("message:")) {
                                sender.sendMessage(StringUtils.rep(command.replaceFirst("message:", "")));
                            } else if (command.startsWith("broadcast:")) {
                                Bukkit.getServer().broadcastMessage(StringUtils.rep(command.replaceFirst("broadcast:", "")));
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                            }
                        }
                    });
                }
            }
        } else {
            sender.sendMessage(plugin.getString("Errors.Console", lang));
        }
    }

    private void importer(CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(StringUtils.rep("%prefix% &cThis command must be executed from the console."));
            return;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager(plugin, new BukkitImporter(plugin));
            for (PluginToImport pluginToImport : PluginToImport.values()) {
                if (pluginToImport.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFrom(pluginToImport);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(StringUtils.rep("%prefix% You specified an invalid plugin to import, possible values:"));
                sender.sendMessage(Arrays.toString(PluginToImport.values()));
            }
        } else {
            sender.sendMessage(StringUtils.rep("%prefix% Command usage: /coins import <plugin>"));
            sender.sendMessage(StringUtils.rep("&cCurrently supported plugins to import: " + Arrays.toString(PluginToImport.values())));
        }
    }

    private void importDB(CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(StringUtils.rep("%prefix% &cThis command must be executed from the console."));
            return;
        }
        if (args.length == 2) {
            boolean worked = false;
            ImportManager importManager = new ImportManager(plugin, new BukkitImporter(plugin));
            for (StorageType storage : StorageType.values()) {
                if (storage.toString().equals(args[1].toUpperCase())) {
                    worked = true;
                    importManager.importFromStorage(storage);
                    break;
                }
            }
            if (!worked) {
                sender.sendMessage(StringUtils.rep("%prefix% You specified an invalid storage to import, possible values:"));
                sender.sendMessage(Arrays.toString(StorageType.values()));
            }
        } else {
            sender.sendMessage(StringUtils.rep("%prefix% Command usage: /coins importdb <storage>"));
            sender.sendMessage(StringUtils.rep("&cCurrently supported storages to import: " + Arrays.toString(StorageType.values())));
        }
    }

    private void reload(@NotNull CommandSender sender) {
        if (sender.hasPermission(getPermission() + ".admin.reload")) {
            if (plugin.getCoinsEconomy() != null) {
                plugin.getCoinsEconomy().shutdown();
            }
            plugin.getConfig().reload();
            plugin.reloadMessages();
            if (plugin.getConfig().getBoolean("Vault.Use", false)) {
                plugin.setCoinsEconomy(new CoinsEconomy(plugin.getBootstrap()));
                plugin.getCoinsEconomy().setup();
            }
            ExecutorManager.getExecutors().clear();
            plugin.getBootstrap().getPlugin().loadExecutors();
            plugin.getMessagingService().requestMultipliers();
            plugin.getMessagingService().requestExecutors();
            sender.sendMessage(StringUtils.rep("%prefix% Reloaded config and all loaded messages files. If you want reload the command, you need to restart the server."));
        }
    }

    private void about(@NotNull CommandSender sender) {
        sender.sendMessage(StringUtils.rep("%prefix% Coins plugin by Beelzebu, plugin info:"));
        sender.sendMessage("");
        sender.sendMessage(StringUtils.rep(" &cAPI Version:&7 " + CoinsAPI.API_VERSION));
        sender.sendMessage(StringUtils.rep(" &cVersion:&7 " + plugin.getBootstrap().getVersion()));
        if (sender.hasPermission("coins.admin.info") || sender.getName().equals("Beelzebu")) {
            sender.sendMessage(StringUtils.rep(" &cExecutors:&7 " + ExecutorManager.getExecutors().size()));
            sender.sendMessage(StringUtils.rep(" &cStorage Type:&7 " + plugin.getStorageProvider().getStorageType()));
            sender.sendMessage(StringUtils.rep(" &cCache Type:&7 " + plugin.getCache().getCacheType()));
            sender.sendMessage(StringUtils.rep(" &cMessaging Service:&7 " + plugin.getMessagingService().getType()));
            sender.sendMessage(StringUtils.rep(" &cMultipliers in cache:&7 " + plugin.getCache().getMultipliers().size()));
            sender.sendMessage(StringUtils.rep(" &cPlayers in cache:&7 " + plugin.getCache().getPlayers().size()));
            sender.sendMessage("");
        }
        sender.sendMessage(StringUtils.rep(" &cSource Code:&7 https://github.com/Beelzebu/Coins3"));
        sender.sendMessage(StringUtils.rep(" &cLicense:&7 GNU AGPL v3 (&ahttp://www.gnu.org/licenses/#AGPL&7)"));
    }

    private boolean getBoolean(String string) {
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception ex) {
            return false;
        }
    }
}
