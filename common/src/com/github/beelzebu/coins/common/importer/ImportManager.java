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
package com.github.beelzebu.coins.common.importer;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.github.beelzebu.coins.api.storage.sql.SQLDatabase;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import com.github.beelzebu.coins.common.storage.MySQL;
import com.github.beelzebu.coins.common.storage.SQLite;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class ImportManager {

    private final CommonCoinsPlugin<? extends CoinsBootstrap> plugin;
    private final Importer importer;

    public ImportManager(CommonCoinsPlugin<? extends CoinsBootstrap> plugin, Importer importer) {
        this.plugin = plugin;
        this.importer = importer;
    }

    public void importFrom(PluginToImport plugin) {
        if (importer != null) {
            importer.importFrom(plugin);
        } else {
            this.plugin.log("Seems that the importer is not defined yet.");
        }
    }

    public void importFromStorage(@NotNull StorageType storage) {
        switch (storage) {
            case MYSQL:
                if (plugin.getStorageProvider().getStorageType().equals(StorageType.MYSQL)) {
                    plugin.log("You can't migrate information from the same storageProvider that you are using.");
                    return;
                }
                SQLDatabase mysql = new MySQL(plugin);
                mysql.setup();
                Map<String, Double> mysqlData = mysql.getAllPlayers();
                if (!mysqlData.isEmpty()) {
                    plugin.log("Starting the migration from MySQL, this may take a moment.");
                    copy(mysqlData);
                } else {
                    plugin.log("There are no users to migrate in the storageProvider.");
                }
                mysql.shutdown();
                break;
            case SQLITE:
                if (plugin.getStorageProvider().getStorageType().equals(StorageType.SQLITE)) {
                    plugin.log("You can't migrate information from the same storageProvider that you are using.");
                    return;
                }
                SQLDatabase sqlite = new SQLite(plugin);
                sqlite.setup();
                Map<String, Double> sqliteData = sqlite.getAllPlayers();
                if (!sqliteData.isEmpty()) {
                    plugin.log("Starting the migration from SQLite, this may take a moment.");
                    copy(sqliteData);
                } else {
                    plugin.log("There are no users to migrate in the storageProvider.");
                }
                sqlite.shutdown();
                break;
            default:
                break;
        }
    }

    private void copy(@NotNull Map<String, Double> data) {
        data.forEach((key, value) -> {
            String name = null;
            UUID uuid = null;
            try {
                name = key.split(",")[0];
                uuid = UUID.fromString(key.split(",")[1]);
                double balance = value;
                CoinsAPI.createPlayer(name, uuid, balance);
                plugin.debug("Migrated the data for: " + uuid);
            } catch (Exception ex) {
                plugin.log("An error has occurred while migrating the data for: " + name + " (" + uuid + ")");
                plugin.debug(ex);
            }
        });
        plugin.log("The migration was completed, check the plugin logs for more information.");
    }
}
