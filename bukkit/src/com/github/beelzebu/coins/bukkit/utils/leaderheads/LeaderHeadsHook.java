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
package com.github.beelzebu.coins.bukkit.utils.leaderheads;

import com.github.beelzebu.coins.api.CoinsAPI;
import java.util.Arrays;
import me.robin.leaderheads.datacollectors.OnlineDataCollector;
import me.robin.leaderheads.objects.BoardType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class LeaderHeadsHook extends OnlineDataCollector {

    public LeaderHeadsHook() {
        super("coins-top", "Coins", BoardType.DEFAULT, "Coins - Wealthiest Accounts", "coinstop", Arrays.asList(null, "&9{name}", "&6{amount}", null));
    }

    @Override
    public Double getScore(@NotNull Player player) {
        return CoinsAPI.getCoins(player.getUniqueId());
    }
}
