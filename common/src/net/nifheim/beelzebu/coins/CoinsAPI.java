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
package net.nifheim.beelzebu.coins;

import com.github.beelzebu.coins.api.Multiplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Coins 2.0 API
 *
 * @author Beelzebu
 * @deprecated Will be removed in the future, please use {@link com.github.beelzebu.coins.api.CoinsAPI} for a better and
 * more rich API.
 */
@Deprecated
public class CoinsAPI {

    /**
     * Get the coins of a Player by his name.
     *
     * @param name Player to get the coins.
     * @return
     */
    public static double getCoins(@NotNull String name) {
        return com.github.beelzebu.coins.api.CoinsAPI.getCoins(name);
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return
     */
    public static double getCoins(@NotNull UUID uuid) {
        return com.github.beelzebu.coins.api.CoinsAPI.getCoins(uuid);
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param name Player to get the coins string.
     * @return
     */
    public static String getCoinsString(@NotNull String name) {
        return com.github.beelzebu.coins.api.CoinsAPI.getCoinsString(name);
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param uuid Player to get the coins string.
     * @return
     */
    public static String getCoinsString(@NotNull UUID uuid) {
        return com.github.beelzebu.coins.api.CoinsAPI.getCoinsString(uuid);
    }

    /**
     * Add coins to a player by his name.
     *
     * @param name  The player to add the coins.
     * @param coins The coins to add.
     * @see CoinsAPI#addCoins(String, double, boolean)
     * @deprecated This should not be used.
     */
    @Deprecated
    public static void addCoins(@NotNull String name, double coins) {
        addCoins(name, coins, false);
    }

    /**
     * Add coins to a player by his UUID.
     *
     * @param uuid  The player to add the coins.
     * @param coins The coins to add.
     * @see CoinsAPI#addCoins(UUID, double, boolean)
     * @deprecated This should not be used.
     */
    @Deprecated
    public static void addCoins(@NotNull UUID uuid, double coins) {
        addCoins(uuid, coins, false);
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be used to calculate the coins.
     *
     * @param name     The player to add the coins.
     * @param coins    The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     */
    public static void addCoins(@NotNull String name, double coins, boolean multiply) {
        com.github.beelzebu.coins.api.CoinsAPI.addCoins(name, coins, multiply);
    }

    /**
     * Add coins to a player by his UUID, selecting if the multipliers should be used to calculate the coins.
     *
     * @param uuid     The player to add the coins.
     * @param coins    The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     */
    public static void addCoins(@NotNull UUID uuid, double coins, boolean multiply) {
        com.github.beelzebu.coins.api.CoinsAPI.addCoins(uuid, coins, multiply);
    }

    /**
     * Take coins of a player by his name.
     *
     * @param name
     * @param coins
     */
    public static void takeCoins(@NotNull String name, double coins) {
        com.github.beelzebu.coins.api.CoinsAPI.takeCoins(name, coins);
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param uuid
     * @param coins
     */
    public static void takeCoins(@NotNull UUID uuid, double coins) {
        com.github.beelzebu.coins.api.CoinsAPI.takeCoins(uuid, coins);
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param name
     */
    public static void resetCoins(@NotNull String name) {
        com.github.beelzebu.coins.api.CoinsAPI.resetCoins(name);
    }

    /**
     * Reset the coins of a player by his UUID.
     *
     * @param uuid
     */
    public static void resetCoins(@NotNull UUID uuid) {
        com.github.beelzebu.coins.api.CoinsAPI.resetCoins(uuid);
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param name
     * @param coins
     */
    public static void setCoins(@NotNull String name, double coins) {
        com.github.beelzebu.coins.api.CoinsAPI.setCoins(name, coins);
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param uuid
     * @param coins
     */
    public static void setCoins(@NotNull UUID uuid, double coins) {
        com.github.beelzebu.coins.api.CoinsAPI.setCoins(uuid, coins);
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return true or false if the transaction is completed.
     */
    public static boolean payCoins(@NotNull String from, @NotNull String to, double amount) {
        return com.github.beelzebu.coins.api.CoinsAPI.payCoins(from, to, amount).isSuccess();
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return true or false if the transaction is completed.
     */
    public static boolean payCoins(@NotNull UUID from, @NotNull UUID to, double amount) {
        return com.github.beelzebu.coins.api.CoinsAPI.payCoins(from, to, amount).isSuccess();
    }

    /**
     * Get if a player with the specified name exists in the database. Is not recommended check a player by his name
     * because it can change.
     *
     * @param name The name to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(@NotNull String name) {
        return com.github.beelzebu.coins.api.CoinsAPI.isindb(name);
    }

    /**
     * Get if a player with the specified uuid exists in the database.
     *
     * @param uuid The uuid to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(@NotNull UUID uuid) {
        return com.github.beelzebu.coins.api.CoinsAPI.isindb(uuid);
    }

    /**
     * Get the top players in coins data.
     *
     * @param top The lenght of the top list, for example 5 will get a max of 5 users for the top.
     * @return The ordered top list of players and his balance, separated by a separated by a comma and space. ", "
     * @deprecated see {@link #getTopPlayers(int)}
     */
    @Deprecated
    public static List<String> getTop(int top) {
        return Stream.of(com.github.beelzebu.coins.api.CoinsAPI.getTopPlayers(top)).map(coinsTopEntry -> coinsTopEntry.getName() + ", " + coinsTopEntry.getCoins()).collect(Collectors.toList());
    }

    /**
     * Get the top players in coins data.
     *
     * @param top The lenght of the top list, for example 5 will get a max of 5 users for the top.
     * @return The ordered top list of players and his balance.
     */
    @NotNull
    public static Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topPlayers = new HashMap<>();
        Stream.of(com.github.beelzebu.coins.api.CoinsAPI.getTopPlayers(top)).forEach(coinsTopEntry -> topPlayers.put(coinsTopEntry.getName(), coinsTopEntry.getCoins()));
        return topPlayers;
    }

    /**
     * Register a user in the database with the default starting balance.
     *
     * @param name The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static void createPlayer(@NotNull String name, UUID uuid) {
        com.github.beelzebu.coins.api.CoinsAPI.createPlayer(name, uuid);
    }

    /**
     * Register a user in the database with the specified balance.
     *
     * @param name    The name of the user that will be registered.
     * @param uuid    The uuid of the user.
     * @param balance The balance of the user.
     */
    public static void createPlayer(@NotNull String name, UUID uuid, double balance) {
        com.github.beelzebu.coins.api.CoinsAPI.createPlayer(name, uuid, balance);
    }

    /**
     * Get and modify information about multipliers for the specified server.
     *
     * @param server The server to modify and get info about multiplier.
     * @return The active multiplier for the specified server.
     */
    public static Multiplier getMultiplier(@NotNull String server) {
        return com.github.beelzebu.coins.api.CoinsAPI.getMultipliers(server).stream().findFirst().orElse(null);
    }

    /**
     * Get and modify information about the multiplier for the server specified in the plugin's config or manage other
     * data about multipliers.
     *
     * @return The active multiplier for this server.
     */
    public static Multiplier getMultiplier() {
        return com.github.beelzebu.coins.api.CoinsAPI.getMultipliers(com.github.beelzebu.coins.api.CoinsAPI.getServerName()).stream().findFirst().orElse(null);
    }
}