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
package com.github.beelzebu.coins.common.utils;

import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * @author Beelzebu
 */
public class FileManager {

    public static final int CONFIG_VERSION = 16;
    private final CommonCoinsPlugin<? extends CoinsBootstrap> plugin;
    private final File messagesFolder;
    private final File logsFolder;
    private final File configFile;
    private final File executorsFile;
    private final Map<String, File> messagesFiles = new HashMap<>();
    private final Queue<String> logQueue = new LinkedList<>();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    private final File logFile;
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    public FileManager(CommonCoinsPlugin<? extends CoinsBootstrap> plugin) {
        this.plugin = plugin;
        messagesFolder = new File(plugin.getBootstrap().getDataFolder(), "messages");
        logsFolder = new File(plugin.getBootstrap().getDataFolder(), "logs");
        configFile = new File(plugin.getBootstrap().getDataFolder(), "config.yml");
        executorsFile = new File(plugin.getBootstrap().getDataFolder(), "executors.yml");
        messagesFiles.put("default", new File(messagesFolder, "messages.yml"));
        messagesFiles.put("es", new File(messagesFolder, "messages_es.yml"));
        messagesFiles.put("zh", new File(messagesFolder, "messages_zh.yml"));
        messagesFiles.put("cz", new File(messagesFolder, "messages_cz.yml"));
        messagesFiles.put("hu", new File(messagesFolder, "messages_hu.yml"));
        messagesFiles.put("ru", new File(messagesFolder, "messages_ru.yml"));
        logFile = new File(plugin.getBootstrap().getDataFolder(), "/logs/latest.log");
    }

    public void onLoad() throws IOException {
        // copy config file
        if (!configFile.exists()) {
            Files.copy(plugin.getBootstrap().getResource(configFile.getName()), configFile.toPath());
            plugin.getConfig().reload();
        }
        updateConfig();
        if (plugin.getConfig().isDebugFile() && !logsFolder.exists()) {
            logsFolder.mkdirs();
        } else {
            logsFolder.delete();
        }
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }
        // Move messages files from plugin folder to messages folder inside plugin folder
        File[] files = plugin.getBootstrap().getDataFolder().listFiles();
        if (!Objects.isNull(files)) {
            for (File f : files) {
                if (f.isFile() && (f.getName().startsWith("messages") && f.getName().endsWith(".yml"))) {
                    try {
                        Files.move(f.toPath(), new File(messagesFolder, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) {
                        Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has occurred while moving messages files to the new messages folder.", ex);
                    }
                }
            }
        } else {
            Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has occurred while moving messages files to the new messages folder.");
        }
        // copy missing messages files
        messagesFiles.keySet().stream().map(filename -> new File(messagesFolder, messagesFiles.get(filename).getName())).filter(file -> !file.exists()).forEach(file -> {
            try {
                Files.copy(plugin.getBootstrap().getResource(file.getName()), file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // copy multipliers config
        File multipliersConfig = new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml");
        if (!multipliersConfig.exists()) {
            Files.copy(plugin.getBootstrap().getResource(multipliersConfig.getName()), multipliersConfig.toPath());
        }
        // copy executors config
        if (!executorsFile.exists()) {
            Files.copy(plugin.getBootstrap().getResource(executorsFile.getName()), executorsFile.toPath());
        }
    }

    public void onEnable() {
        checkLogs();
        messagesFiles.clear();
        // write to log files async
        String logFormat = "[%time] %msg";
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "Can't create log file", ex);
            }
        }
        plugin.getBootstrap().scheduleAsync(() -> {
            try {
                rwl.writeLock().lock();
                String line;
                while ((line = logQueue.poll()) != null) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                        writer.write(logFormat.replace("%time", simpleDateFormat.format(System.currentTimeMillis())).replace("%msg", line));
                        writer.newLine();
                    } catch (IOException ex) {
                        Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "Can't save the debug to the file", ex);
                    }
                }
            } finally {
                rwl.writeLock().unlock();
            }
        }, 1);
    }

    public File getMessagesFolder() {
        return messagesFolder;
    }

    public File getLogsFolder() {
        return logsFolder;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getExecutorsFile() {
        return executorsFile;
    }

    public Map<String, File> getMessagesFiles() {
        return messagesFiles;
    }

    public Queue<String> getLogQueue() {
        return logQueue;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }

    public File getLogFile() {
        return logFile;
    }

    public void updateDatabaseVersion(int version) {
        if (plugin.getConfig().getInt("Database Version") != version) {
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                int index = lines.indexOf("Database Version: " + plugin.getConfig().getInt("Database Version"));
                lines.set(index, "Database Version: " + version);
                Files.write(configFile.toPath(), lines);
                plugin.getConfig().reload();
                plugin.log("Successfully updated database to version " + version);
            } catch (IOException ex) {
                plugin.log("An unexpected error occurred while updating the config file.");
                plugin.debug(ex);
            }
        }
    }

    public void logToFile(Object msg) {
        if (!plugin.isLogEnabled()) {
            return;
        }
        logQueue.add(StringUtils.removeColor(msg.toString()));
    }

    private void updateConfig() {
        try {
            switch (plugin.getConfig().getInt("version")) {
                case CONFIG_VERSION:
                    plugin.log("Config file is up to date.");
                    break;
                case 13:
                    updateConfigFromV2();
                    break;
            }
        } catch (Exception ex) {
            plugin.log("An error occurred while updating your config file.");
            plugin.debug(ex);
        }
        plugin.getConfig().reload();
    }

    private void updateConfigFromV2() throws IOException {
        List<String> lines = Arrays.asList(
                "# Coins plugin by: Beelzebu",
                "# If you need support or find a bug open a issue in the official github repo",
                "# https://github.com/Beelzebu/Coins3/issues/",
                "",
                "# The version of the config, don't touch!",
                "version: 16",
                "",
                "# This is the prefix used in all the messages.",
                "Prefix: '" + plugin.getConfig().getString("Prefix") + "'",
                "",
                "# Here you can enable Vault to make this plugin manage all the Vault transactions.",
                "Vault:",
                "  Use: " + plugin.getConfig().getBoolean("Vault.Use"),
                "  # Names used by vault for the currency.",
                "  Name:",
                "    Singular: '" + plugin.getConfig().getString("Vault.Name.Singular") + "'",
                "    Plural: '" + plugin.getConfig().getString("Vault.Name.Plural") + "'",
                "",
                "# Which storage method the plugin should use.",
                "#",
                "# Available options:",
                "#  -> sqlite    data is stored locally and can't be shared with other servers.",
                "#  -> mysql     data is stored on a mysql server and can be shared by several servers.",
                "#  -> mariadb   we will use mariadb driver instead of mysql driver.",
                "Storage Type: " + (plugin.getConfig().getBoolean("MySQL.Use") ? "mysql" : "sqlite"),
                "",
                "# Don't touch this setting, this is only for internal usage to auto update the",
                "# database when something changes.",
                "Database Version: 3",
                "",
                "# Settings for messaging service",
                "# If enabled and configured, Coins will use the messaging service to inform other",
                "# connected servers of changes.",
                "#",
                "# Available options:",
                "#  -> bungeecord           uses the plugin messaging channels. You must enable bungeecord",
                "#                          in spigot.yml and install the plugin in BungeeCord to work.",
                "#  -> redis (recommended)  uses redis pub sub to push changes. You redis server must be",
                "#                          configured below.",
                "#  -> none                 nothing.",
                "Messaging Service: none",
                "",
                "# Settings for data caching",
                "# Coins keeps data in cache for better performance on read operations, currently the plugin",
                "# only support two methods for caching",
                "#",
                "# Available options:",
                "#  -> redis                uses redis for storing data and share it across servers, this is",
                "#                          the recommended method for bungee networks. You must enable it in",
                "#                          all servers connected to bungeecord.",
                "#  -> local                all data will be cached locally, this is the recommended method",
                "#                          for single servers.",
                "Cache: local",
                "",
                "# Here are the MySQL server settings.",
                "MySQL:",
                "  Host: '" + plugin.getConfig().getString("MySQL.Host") + "'",
                "  Port: " + plugin.getConfig().getInt("MySQL.Port"),
                "  Database: '" + plugin.getConfig().getString("MySQL.Database") + "'",
                "  User: '" + plugin.getConfig().getString("MySQL.User") + "'",
                "  Password: '" + plugin.getConfig().getString("MySQL.Password") + "'",
                "  Prefix: '" + plugin.getConfig().getString("MySQL.Prefix") + "'",
                "  # Don't change this value if you don't know what it does.",
                "  Connection Pool: 8",
                "  # MySQL table names without prefix, you can change this to use same database",
                "  # for all servers and but keep different balances in every server.",
                "  # Coins 2 used \"Data\" and \"Multipliers\" names for tables, you should delete",
                "  # them after data is properly migrated.",
                "  Data Table: 'data'",
                "  Multipliers Table: 'multipliers'",
                "",
                "# Here are the Redis server settings.",
                "Redis:",
                "  Host: 'localhost'",
                "  Port: 6379",
                "  Password: 'S3CUR3P4SSW0RD'",
                "",
                "# Plugin general configurations.",
                "General:",
                "  # Here you can define the starting coins of a player when is registered in the",
                "  # database or his coins are reset with \"/coins reset\"",
                "  Starting Coins: " + plugin.getConfig().getInt("General.Starting Coins"),
                "  # Here you can configure the base command of the plugin.",
                "  Command:",
                "    Coins:",
                "      Name: '" + plugin.getConfig().getString("General.Command.Name") + "'",
                "      Description: '" + plugin.getConfig().getString("General.Command.Description") + "'",
                "      Usage: '" + plugin.getConfig().getString("General.Command.Usage") + "'",
                "      Permission: '" + plugin.getConfig().getString("General.Command.Permission") + "'",
                "      Aliases: " + plugin.getConfig().getStringList("General.Command.Aliases"),
                "    Multiplier:",
                "      Name: 'multiplier'",
                "      Description: 'Command to see and edit multipliers'",
                "      Usage: '/multiplier'",
                "      Permission: 'coins.multiplier'",
                "      Aliases: [multipliers]",
                "  # Here you can configure the auto purge of inactive accounts.",
                "  Purge:",
                "    Enabled: " + plugin.getConfig().getInt("General.Purge.Enabled") + " # If this is true the old accounts would be purged at server startup.",
                "    Days: " + plugin.getConfig().getInt("General.Purge.Days") + " # The time in days before deleting an account.",
                "    Log Purge:",
                "      Enabled: true",
                "      Days: " + plugin.getConfig().getInt("General.Purge.Log.Days") + " # The days to keep plugin logs.",
                "  # Options about internal logging, by default the plugin will log all messages,",
                "  # errors and executed commands related to this plugin in a file called latest.log",
                "  # that is inside of the plugins/Coins/logs folder.",
                "  Logging:",
                "    Debug:",
                "      # Enable or disable debug mode, all errors and debug messages will be",
                "      # immediately logged to the console.",
                "      Enabled: " + plugin.getConfig().getBoolean("Debug"),
                "      # If debug messages should be written to the file, if this option is disabled",
                "      # logs folder inside Coins folder won't be generated and will be deleted if",
                "      # already exists.",
                "      File: true",
                "  Executor Sign:",
                "    '1': '" + plugin.getConfig().getString("General.Executor Sign.1") + "'",
                "    '2': '" + plugin.getConfig().getString("General.Executor Sign.2") + "'",
                "    '3': '" + plugin.getConfig().getString("General.Executor Sign.3") + "'",
                "    '4': '" + plugin.getConfig().getString("General.Executor Sign.4") + "'",
                "  # If you want the users to be created when they join to the server, enable this,",
                "  # otherwise the players will be created when his coins are modified or consulted",
                "  # to the database for the first time (recommended for big servers).",
                "  Create Join: " + plugin.getConfig().getBoolean("General.Create Join"),
                "",
                "# Here you can define a cost to use a specific command.",
                "Command Cost:"
        );
        for (String command : plugin.getConfig().getConfigurationSection("Command Cost")) {
            lines.add("  " + command + plugin.getConfig().getDouble("Command Cost." + command));
        }
        // Migrate executors to his file
        List<String> executorsLines = Arrays.asList("# Executors file",
                "# Here you can make that Coins run a command from the console and take a cost to the user.",
                "Executors:");
        String path = "Command executor";
        for (String id : plugin.getConfig().getConfigurationSection(path)) {
            String displayName = plugin.getConfig().getString(path + ".Displayname", id);
            double cost = plugin.getConfig().getDouble(path + ".Cost", 0);
            List<String> commands = plugin.getConfig().getStringList(path + ".Command", new ArrayList<>());
            executorsLines.add("  " + id + ":");
            executorsLines.add("    Displayname:" + displayName);
            executorsLines.add("    Cost:" + cost);
            executorsLines.add("    Command:");
            for (String command : commands) {
                executorsLines.add("    - " + command);
            }
        }
        // Add new lines to messages.yml
        List<String> messagesLines = Arrays.asList("",
                "Menus:",
                "  Main:",
                "    Title: '&8Multipliers Menu'",
                "    Items:",
                "      'global':",
                "        Name: '&3Global multipliers'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to see your global multipliers'",
                "      'local':",
                "        Name: '&3Local multipliers'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to see your multipliers for this server'",
                "      'custom':",
                "        Name: '&6Custom item'",
                "        Lore:",
                "        - ''",
                "        - '&7This is a custom item, is an example to'",
                "        - '&7show you how add items that can execute'",
                "        - '&7commands from the console or as the player.'",
                "        - '&7You can edit these names and descriptions'",
                "        - '&7in the messages.yml file, and translate it'",
                "        - '&7to any other language in the respective file'",
                "  Multipliers:",
                "    Global:",
                "      Multiplier:",
                "        Name: '&6Multiplier &cx%amount%'",
                "        Lore:",
                "        - ''",
                "        - '&7Amount: &c%amount%'",
                "        - '&7Server: &c%server%'",
                "        - '&7Minutes: &c%minutes%'",
                "        - ''",
                "        - '&7ID: &c#%id%'",
                "      Close:",
                "        Name: '&6Close inventory'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to close this inventory'",
                "      Next:",
                "        Name: '&aNext page'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to visit the next page'",
                "    Local:",
                "      Multiplier:",
                "        Name: '&6Multiplier &cx%amount%'",
                "        Lore:",
                "        - ''",
                "        - '&7Amount: &c%amount%'",
                "        - '&7Minutes: &c%minutes%'",
                "        - ''",
                "        - '&7ID: &c#%id%'",
                "      Close:",
                "        Name: '&6Close inventory'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to close this inventory'",
                "      Next:",
                "        Name: '&aNext page'",
                "        Lore:",
                "        - ''",
                "        - '&7Click to visit the next page'");
        Files.write(configFile.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(executorsFile.toPath(), executorsLines, StandardOpenOption.TRUNCATE_EXISTING);
        File messagesFile = messagesFiles.get("default");
        if (messagesFile != null) {
            List<String> appendedMessagesLines = new ArrayList<>(Files.readAllLines(messagesFile.toPath())).stream().map(line -> {
                if (Objects.equals(line, "version: 10")) {
                    return "version: 12";
                }
                return line;
            }).collect(Collectors.toList());
            appendedMessagesLines.addAll(messagesLines);
            Files.write(messagesFile.toPath(), appendedMessagesLines, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void checkLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File latestLog = new File(logsFolder, "latest.log");
        if (latestLog.exists()) { // gzip old log file
            try {
                int filen = 1;
                while (new File(logsFolder, sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz").exists()) {
                    filen++;
                }
                gzipFile(Files.newInputStream(latestLog.toPath()), logsFolder + File.separator + sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz");
                if (!latestLog.delete()) {
                    Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "An unexpected error has occurred while deleting old log file");
                }
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "An unexpected error has occurred while trying to compress the latest log file. {0}", ex.getMessage());
            }
        }
        File[] fList = logsFolder.listFiles();
        // Auto purge for old logs
        if (fList != null && fList.length > 0) {
            for (File file : fList) {
                if (file.isFile() && file.getName().contains(".gz") && (System.currentTimeMillis() - file.lastModified()) >= plugin.getConfig().getInt("General.Purge.Logs.Days") * 86400000L) {
                    file.delete();
                }
            }
        }
    }

    private void gzipFile(InputStream in, String to) throws IOException {
        try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
        }
    }
}
