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

import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import java.math.BigDecimal;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Beelzebu
 */
public abstract class AbstractCoinsCommand extends Command {

    protected final CoinsBukkitPlugin plugin;

    AbstractCoinsCommand(CoinsBukkitPlugin plugin, @NotNull String name) {
        super(name);
        this.plugin = plugin;
    }

    protected boolean isNumber(@Nullable String number) {
        if (number == null) {
            return false;
        }
        try {
            BigDecimal bd = new BigDecimal(number);
            if (bd.compareTo(new BigDecimal(Double.MAX_VALUE)) <= 0 && !Double.isNaN(Double.parseDouble(number)) && !Double.isInfinite(Double.parseDouble(number))) {
                return true;
            }
        } catch (NumberFormatException ignore) {
        }
        return false;
    }
}
