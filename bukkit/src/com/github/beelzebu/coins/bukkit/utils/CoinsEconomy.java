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
package com.github.beelzebu.coins.bukkit.utils;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.CoinsResponse;
import com.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

/**
 * @author Beelzebu
 */
public class CoinsEconomy implements Economy {

    private final CoinsBukkitMain plugin;

    public CoinsEconomy(CoinsBukkitMain main) {
        plugin = main;
    }

    public void setup() {
        Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.High);
    }

    public void shutdown() {
        Bukkit.getServicesManager().unregister(Economy.class, this);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double d) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(d);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getPlugin().getConfig().getString("Vault.Name.Plural", "Coins");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getPlugin().getConfig().getString("Vault.Name.Singular", "Coin");
    }

    @Override
    @Deprecated
    public boolean hasAccount(String string) {
        return CoinsAPI.isindb(string);
    }

    @Override
    public boolean hasAccount(OfflinePlayer op) {
        return CoinsAPI.isindb(op.getUniqueId());
    }

    @Override
    @Deprecated
    public boolean hasAccount(String string, String string1) {
        return CoinsAPI.isindb(string);
    }

    @Override
    public boolean hasAccount(OfflinePlayer op, String string) {
        return CoinsAPI.isindb(op.getUniqueId());
    }

    @Override
    @Deprecated
    public double getBalance(String string) {
        return CoinsAPI.getCoins(string);
    }

    @Override
    public double getBalance(OfflinePlayer op) {
        return CoinsAPI.getCoins(op.getUniqueId());
    }

    @Override
    @Deprecated
    public double getBalance(String string, String string1) {
        return CoinsAPI.getCoins(string);
    }

    @Override
    public double getBalance(OfflinePlayer op, String string) {
        return CoinsAPI.getCoins(op.getUniqueId());
    }

    @Override
    @Deprecated
    public boolean has(String string, double d) {
        return CoinsAPI.getCoins(string) >= d;
    }

    @Override
    public boolean has(OfflinePlayer op, double d) {
        return CoinsAPI.getCoins(op.getUniqueId()) >= d;
    }

    @Override
    @Deprecated
    public boolean has(String string, String string1, double d) {
        return CoinsAPI.getCoins(string) >= d;
    }

    @Override
    public boolean has(OfflinePlayer op, String string, double d) {
        return CoinsAPI.getCoins(op.getUniqueId()) >= d;
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String string, double d) {
        CoinsResponse response = CoinsAPI.takeCoins(string, d);
        if (response.isSuccess()) {
            return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(d, getBalance(string), ResponseType.FAILURE, response.getMessage(""));
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, double d) {
        CoinsResponse response = CoinsAPI.takeCoins(op.getUniqueId(), d);
        if (response.isSuccess()) {
            return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(d, getBalance(op), ResponseType.FAILURE, response.getMessage(""));
        }
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String string, String string1, double d) {
        return withdrawPlayer(string, d);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, String string, double d) {
        return withdrawPlayer(op, d);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String string, double d) {
        CoinsResponse response = CoinsAPI.addCoins(string, d, plugin.getPlugin().getConfig().vaultMultipliers());
        if (response.isSuccess()) {
            return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(d, getBalance(string), ResponseType.FAILURE, response.getMessage(""));
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, double d) {
        CoinsResponse response = CoinsAPI.addCoins(op.getUniqueId(), d, plugin.getPlugin().getConfig().vaultMultipliers());
        if (response.isSuccess()) {
            return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
        } else {
            return new EconomyResponse(d, getBalance(op), ResponseType.FAILURE, response.getMessage(""));
        }
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String string, String string1, double d) {
        return depositPlayer(string, d);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, String string, double d) {
        return depositPlayer(op, d);
    }

    @Override
    @Deprecated
    public EconomyResponse createBank(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse createBank(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse deleteBank(String string) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankBalance(String string) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankHas(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankWithdraw(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankDeposit(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankOwner(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankMember(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String string) {
        UUID uuid = plugin.getPlugin().getUniqueId(string, false);
        return CoinsAPI.createPlayer(string, uuid != null ? uuid : UUID.randomUUID()).getResponse() == CoinsResponse.CoinsResponseType.SUCCESS;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op) {
        return CoinsAPI.createPlayer(op.getName() != null ? op.getName() : "", op.getUniqueId()).getResponse() == CoinsResponse.CoinsResponseType.SUCCESS;
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String string, String string1) {
        return createPlayerAccount(string);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op, String string) {
        return createPlayerAccount(op);
    }
}
