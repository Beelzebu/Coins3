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
package com.github.beelzebu.coins.bukkit.utils.placeholders;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierData;
import com.github.beelzebu.coins.api.MultiplierType;
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import java.util.List;
import java.util.stream.Collectors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class MultipliersPlaceholders extends PlaceholderExpansion {

    private final CoinsBukkitPlugin plugin;

    public MultipliersPlaceholders(CoinsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "coins-multiplier";
    }

    @Override
    public String getPlugin() {
        return "Coins";
    }

    @Override
    public String getAuthor() {
        return "Beelzebu";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String placeholder) {
        if (p == null) {
            return "Player needed!";
        }
        try {
            String server = placeholder.split("_")[1];
            List<Multiplier> multipliers = CoinsAPI.getMultipliers(server, true).stream()
                    .filter(multiplier -> multiplier.getData().getType() != MultiplierType.PERSONAL)
                    .filter(multiplier -> !multiplier.getData().isServerEnabler()).collect(Collectors.toList());
            if (placeholder.startsWith("enabler_")) {
                if (multipliers.isEmpty()) {
                    return plugin.getString("Multipliers.Placeholders.Enabler.Anyone", CompatUtils.getLocale(p));
                } else {
                    String enabler = multipliers.stream().map(Multiplier::getData).map(MultiplierData::getEnablerName).collect(Collectors.joining(", "));
                    return plugin.getString("Multipliers.Placeholders.Enabler.Message", CompatUtils.getLocale(p)).replaceAll("%enabler%", enabler);
                }
            }
            if (placeholder.startsWith("amount_")) {
                int multiplyTotal = multipliers.stream().mapToInt(multiplier -> multiplier.getData().getAmount()).sum();
                return String.valueOf(multiplyTotal);
            }
            if (placeholder.startsWith("time_")) {
                Multiplier less = null;
                for (Multiplier multiplier : multipliers) {
                    if (less == null || less.getEndTime() < multiplier.getEndTime()) {
                        less = multiplier;
                    }
                }
                return less != null ? less.getEndTimeFormatted() : "";
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return "coins multiplier error";
    }
}
