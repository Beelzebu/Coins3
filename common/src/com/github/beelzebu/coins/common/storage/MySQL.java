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

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.storage.StorageType;
import com.github.beelzebu.coins.api.storage.sql.DatabaseUtils;
import com.github.beelzebu.coins.api.storage.sql.SQLDatabase;
import com.github.beelzebu.coins.api.storage.sql.SQLQuery;
import com.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * @author Beelzebu
 */
public final class MySQL extends SQLDatabase {

    public MySQL(CoinsPlugin plugin) {
        super(plugin);
    }

    @Override
    public void setup() {
        int maxPoolSize = plugin.getConfig().getInt("MySQL.Connection Pool", 8);
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("Coins MySQL Connection Pool");
        String urlprefix = "jdbc:mysql://";
        if (plugin.getStorageProvider().getStorageType().equals(StorageType.MARIADB)) {
            urlprefix = "jdbc:mariadb://";
            hc.setDriverClassName("org.mariadb.jdbc.Driver");
        } else {
            hc.setDriverClassName("com.mysql.jdbc.Driver");
        }
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("useServerPrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("encoding", "UTF-8");
        hc.addDataSourceProperty("characterEncoding", "utf8");
        hc.addDataSourceProperty("useUnicode", "true");
        hc.setJdbcUrl(urlprefix + plugin.getConfig().getString("MySQL.Host") + ":" + plugin.getConfig().get("MySQL.Port", "3306") + "/" + plugin.getConfig().getString("MySQL.Database") + "?autoReconnect=true&useSSL=false");
        hc.setUsername(plugin.getConfig().getString("MySQL.User"));
        hc.setPassword(plugin.getConfig().getString("MySQL.Password"));
        hc.setMaxLifetime(60000);
        hc.setMinimumIdle(Math.max(4, maxPoolSize));
        hc.setIdleTimeout(30000);
        hc.setConnectionTimeout(10000);
        hc.setMaximumPoolSize(maxPoolSize);
        hc.setLeakDetectionThreshold(30000);
        hc.validate();
        try {
            ds = new HikariDataSource(hc);
        } catch (Exception ex) {
            plugin.log("An exception has occurred while starting connection pool, check your database credentials.");
            plugin.debug(ex);
            plugin.log("We will change your storage type to SQLite.");
            plugin.setStorageType(StorageType.SQLITE);
            return;
        }
        updateDatabase();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.MYSQL;
    }

    @Override
    protected void updateDatabase() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            String data = "CREATE TABLE IF NOT EXISTS `" + DATA_TABLE + "` (" +
                    "  `id` INT NOT NULL AUTO_INCREMENT," +
                    "  `uuid` CHAR(36) NOT NULL," +
                    "  `name` VARCHAR(16) NOT NULL," +
                    "  `balance` DOUBLE NOT NULL DEFAULT 0," +
                    "  `lastlogin` BIGINT(19) UNSIGNED NOT NULL DEFAULT 0," +
                    "  PRIMARY KEY (`id`)," +
                    "  UNIQUE INDEX `uuid_name_UQ` (`uuid` ASC, `name` ASC));";
            String multiplier = "CREATE TABLE IF NOT EXISTS `" + MULTIPLIERS_TABLE + "` (" +
                    "  `id` INT NOT NULL AUTO_INCREMENT," +
                    "  `server` VARCHAR(50) NOT NULL," +
                    "  `type` CHAR(8) NOT NULL," +
                    "  `amount` INT NOT NULL," +
                    "  `minutes` INT NOT NULL," +
                    "  `start` BIGINT(19) UNSIGNED NOT NULL," +
                    "  `enabled` TINYINT NOT NULL," +
                    "  `queue` TINYINT NOT NULL," +
                    "  `data_id` INT NOT NULL," +
                    "  PRIMARY KEY (`id`, `data_id`)," +
                    "  INDEX `fk_coins_multiplier_data_idx` (`data_id` ASC)," +
                    "  CONSTRAINT `fk_coins_multiplier_coins_data`" +
                    "    FOREIGN KEY (`data_id`)" +
                    "    REFERENCES `" + DATA_TABLE + "` (`id`));";
            st.executeUpdate(data);
            st.executeUpdate(multiplier);
            if (plugin.getConfig().getInt("Database Version", 1) < 2) {
                try (PreparedStatement ps = c.prepareStatement("SELECT uuid,nick,balance,lastlogin FROM " + prefix + "Data;"); ResultSet res = ps.executeQuery()) {
                    if (res.next() && !c.prepareStatement("SELECT id FROM " + DATA_TABLE + " LIMIT 1;").executeQuery().next()) {
                        res.previous(); // we used next to verify that this result set isn't empty
                        plugin.log("Seems that your database is outdated, we'll try to update it...");
                        int migrated = 0;
                        while (res.next()) { // start migrating data
                            DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_USER, res.getString("uuid"), res.getString("nick"), res.getDouble("balance"), res.getLong("lastlogin")).executeUpdate();
                            plugin.debug("Migrated the data for " + res.getString("nick") + " (" + res.getString("uuid") + ")");
                            migrated++;
                        }
                        plugin.log("Migrated " + migrated + " accounts!");
                        plugin.log("Successfully updated database to version 2");
                    }
                    ((CommonCoinsPlugin) CoinsAPI.getPlugin()).getFileManager().updateDatabaseVersion(2);
                } catch (SQLException ex) {
                    for (int i = 0; i < 5; i++) {
                        plugin.log("An error has occurred migrating the data from the old database, check the logs ASAP!");
                    }
                    plugin.debug(ex);
                    return;
                }
            }
            if (plugin.getConfig().getBoolean("General.Purge.Enabled", true) && plugin.getConfig().getInt("General.Purge.Days") > 0) {
                st.executeUpdate("DELETE FROM " + DATA_TABLE + " WHERE lastlogin < " + (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(plugin.getConfig().getInt("General.Purge.Days", 60))) + ";");
                plugin.debug("Inactive users were removed from the database.");
            }
        } catch (SQLException ex) {
            plugin.log("Something was wrong creating the default databases. Please check the debug log.");
            plugin.debug(ex);
        }
    }
}
