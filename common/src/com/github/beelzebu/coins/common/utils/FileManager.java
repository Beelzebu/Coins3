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

import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import lombok.Getter;

/**
 * @author Beelzebu
 */
@Getter
public class FileManager {

    private final CoinsPlugin plugin;
    private final File messagesFolder;
    private final File logsFolder;
    private final File configFile;
    private final Map<String, File> messagesFiles = new HashMap<>();
    private final int configVersion = 16;

    public FileManager(CoinsPlugin plugin) {
        this.plugin = plugin;
        messagesFolder = new File(plugin.getBootstrap().getDataFolder(), "messages");
        logsFolder = new File(plugin.getBootstrap().getDataFolder(), "logs");
        configFile = new File(plugin.getBootstrap().getDataFolder(), "config.yml");
        messagesFiles.put("default", new File(messagesFolder, "messages.yml"));
        messagesFiles.put("es", new File(messagesFolder, "messages_es.yml"));
        messagesFiles.put("zh", new File(messagesFolder, "messages_zh.yml"));
        messagesFiles.put("cz", new File(messagesFolder, "messages_cz.yml"));
        messagesFiles.put("hu", new File(messagesFolder, "messages_hu.yml"));
        messagesFiles.put("ru", new File(messagesFolder, "messages_ru.yml"));
    }

    public void copyFiles() throws IOException {
        if (!logsFolder.exists()) {
            if (plugin.getConfig() == null || plugin.getConfig().isDebugFile()) {
                logsFolder.mkdirs();
            } else {
                logsFolder.delete();
            }
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
        messagesFiles.keySet().stream().map(filename -> new File(messagesFolder, messagesFiles.get(filename).getName())).filter(file -> !file.exists()).forEach(file -> {
            try {
                Files.copy(plugin.getBootstrap().getResource(file.getName()), file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (!configFile.exists()) {
            Files.copy(plugin.getBootstrap().getResource(configFile.getName()), configFile.toPath());
        }
        File multipliersConfig = new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml");
        if (!multipliersConfig.exists()) {
            Files.copy(plugin.getBootstrap().getResource(multipliersConfig.getName()), multipliersConfig.toPath());
        }
        File executorsConfig = new File(plugin.getBootstrap().getDataFolder(), "executors.yml");
        if (!executorsConfig.exists()) {
            Files.copy(plugin.getBootstrap().getResource(executorsConfig.getName()), executorsConfig.toPath());
        }
    }

    public void updateFiles() {
        updateConfig();
        checkLogs();
        messagesFiles.clear();
    }

    public void updateDatabaseVersion(int version) {
        if (plugin.getConfig().getInt("Database Version") != version) {
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                int index = lines.indexOf("Database Version: " + plugin.getConfig().getInt("Database Version"));
                lines.set(index, "Database Version: " + version);
                Files.write(configFile.toPath(), lines);
                plugin.getConfig().reload();
            } catch (IOException ex) {
                plugin.log("An unexpected error occurred while updating the config file.");
                plugin.debug(ex.getMessage());
            }
        }
    }

    private void updateConfig() {
        try {
            if (plugin.getConfig().getInt("version") == configVersion) {
                plugin.log("The config file is up to date.");
            } else {
                File oldConfig = new File(plugin.getBootstrap().getDataFolder(), "config.old.yml");
                if (configFile.renameTo(oldConfig)) {
                    Files.copy(plugin.getBootstrap().getResource("config.yml"), configFile.toPath());
                    List<String> lines = Files.readAllLines(configFile.toPath());
                    int index = lines.indexOf("Database Version: 2");
                    lines.remove(index);
                    lines.add(index, "Database Version: 1");
                    plugin.log("Your old config file was moved to " + oldConfig.getCanonicalPath() + " the new config was created in the plugin folder, check it.");
                } else {
                    plugin.log("An error has occurred while renaming old config file.");
                }
            }
        } catch (IOException ex) {
            plugin.log("An unexpected error occurred while updating the config file.");
            plugin.debug(ex.getMessage());
        }
        plugin.getConfig().reload();
    }

    private void checkLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File latestLog = new File(logsFolder, "latest.log");
        if (latestLog.exists()) {
            try {
                int filen = 1;
                while (new File(logsFolder, sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz").exists()) {
                    filen++;
                }
                gzipFile(Files.newInputStream(latestLog.toPath()), logsFolder + File.separator + sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz");
                latestLog.delete();
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
