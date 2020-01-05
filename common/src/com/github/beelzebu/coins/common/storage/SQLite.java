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
package com.github.beelzebu.coins.common.storage;

import com.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.github.beelzebu.coins.api.storage.sql.SQLDatabase;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Beelzebu
 */
public final class SQLite extends SQLDatabase {

    public SQLite(CommonCoinsPlugin<? extends CoinsBootstrap> plugin) {
        super(plugin);
    }

    @Override
    public void setup() {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("Coins SQLite Connection Pool");
        hc.setDriverClassName("org.sqlite.JDBC");
        hc.setJdbcUrl("jdbc:sqlite:plugins/Coins/database.db");
        hc.setConnectionTestQuery("SELECT 1");
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout(10000);
        hc.setMaximumPoolSize(4);
        hc.setLeakDetectionThreshold(30000);
        hc.validate();
        ds = new HikariDataSource(hc);
        updateDatabase();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.SQLITE;
    }

    @Override
    protected void updateDatabase() {
        try (Connection c = ds.getConnection()) {
            try (Statement st = c.createStatement()) {
                String data = "CREATE TABLE IF NOT EXISTS `" + DATA_TABLE + "`" +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "`uuid` CHAR," +
                        "`name` VARCHAR," +
                        "`balance` DOUBLE," +
                        "`lastlogin` LONG);" +
                        "CREATE UNIQUE INDEX IF NOT EXISTS `uuid_name_UQ` ON `" + DATA_TABLE + "` (`uuid`, `name`);";
                st.executeUpdate(data);
            } catch (SQLException ex) {
                plugin.log("An error has occurred while creating data table");
                throw ex;
            }
            try (Statement st = c.createStatement()) {
                String multiplier = "CREATE TABLE IF NOT EXISTS `" + MULTIPLIERS_TABLE + "`"
                        + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "`server` VARCHAR"
                        + "`type` VARCHAR,"
                        + "`amount` INTEGER,"
                        + "`minutes` INTEGER,"
                        + "`start` LONG,"
                        + "`queue` LONG,"
                        + "`data_id` INTEGER," +
                        " FOREIGN KEY(data_id) REFERENCES " + DATA_TABLE + "(id));";
                st.executeUpdate(multiplier);
            } catch (SQLException ex) {
                plugin.log("An error has occurred while creating multipliers table");
                throw ex;
            }
            int configDatabaseVersion = plugin.getConfig().getDatabaseVersion();
            if (configDatabaseVersion > DATABASE_VERSION) {
                plugin.log("Seems that someone set your database version to a higher number than the current database version.");
                plugin.log("Config database version: " + configDatabaseVersion + " Plugin database version: " + DATABASE_VERSION);
                plugin.log("This can happen when you downgrade the plugin to an older version, still the plugin will try to work with database version " + DATABASE_VERSION + ", be aware that errors may occur.");
            }
            while (configDatabaseVersion < DATABASE_VERSION) {
                switch (configDatabaseVersion) {
                    case 1:
                        try {
                            if (DriverManager.getConnection("jdbc:sqlite:plugins/Coins/database.old.db").prepareStatement("SELECT * FROM Data;").executeQuery().next() && !c.prepareStatement("SELECT * FROM " + DATA_TABLE + ";").executeQuery().next()) {
                                plugin.log("Seems that your database is outdated, we'll try to update it...");
                                ResultSet res = DriverManager.getConnection("jdbc:sqlite:plugins/Coins/database.old.db").prepareStatement("SELECT * FROM Data;").executeQuery();
                                long migrated = migratePlayersFromOldVersion(c, res);
                                plugin.log("Migrated " + migrated + " accounts!");
                            }
                        } catch (SQLException ex) {
                            for (int i = 0; i < 5; i++) {
                                plugin.log("An error has occurred migrating the data from the old database, check the logs ASAP!");
                            }
                            throw ex;
                        }
                        ((CommonCoinsPlugin) plugin).getFileManager().updateDatabaseVersion(configDatabaseVersion++);
                        break;
                    case 2:
                        try {
                            String multiplier = "CREATE TABLE IF NOT EXISTS `" + MULTIPLIERS_TABLE + "X`"
                                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                    + "`server` VARCHAR"
                                    + "`type` VARCHAR,"
                                    + "`amount` INTEGER,"
                                    + "`minutes` INTEGER,"
                                    + "`start` LONG,"
                                    + "`queue` LONG,"
                                    + "`data_id` INTEGER," +
                                    " FOREIGN KEY(data_id) REFERENCES " + DATA_TABLE + "(id));";
                            try (PreparedStatement ps = c.prepareStatement(multiplier)) {
                                ps.executeUpdate();
                            }
                            try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + MULTIPLIERS_TABLE + "X VALUES ((SELECT id, server, type, amount, minutes, start, min(queue, 0), data_id FROM " + MULTIPLIERS_TABLE + "))")) {
                                ps.executeUpdate();
                            }
                            try (PreparedStatement ps = c.prepareStatement("DROP TABLE " + MULTIPLIERS_TABLE)) {
                                ps.executeUpdate();
                            }
                            try (PreparedStatement ps = c.prepareStatement("ALTER TABLE " + MULTIPLIERS_TABLE + "X RENAME TO " + MULTIPLIERS_TABLE)) {
                                ps.executeUpdate();
                            }
                            ((CommonCoinsPlugin) plugin).getFileManager().updateDatabaseVersion(configDatabaseVersion++);
                        } catch (SQLException ex) {
                            plugin.log("An error has occurred while updating database to version " + (configDatabaseVersion + 1));
                            throw ex;
                        }
                        break;
                    default:
                        plugin.log("Seems that your database is outdated and can't be updated.");
                        configDatabaseVersion = DATABASE_VERSION; // break while loop
                        break;
                }
            }
            plugin.log("Database version is up to date.");
            purgeDatabase(c);
        } catch (SQLException ex) {
            plugin.log("Something was wrong creating the default databases. Please check the debug log.");
            plugin.debug(ex);
        }
    }
}
