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
package com.github.beelzebu.coins.bukkit.menus;

import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.utils.StringUtils;
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import com.github.beelzebu.coins.bukkit.utils.ItemBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Beelzebu
 */
public class PaginatedMenu {

    private static final CoinsBukkitPlugin PLUGIN = (CoinsBukkitPlugin) CoinsAPI.getPlugin();
    private static final AbstractConfigFile MULTIPLIERS_CONFIG = PLUGIN.getBootstrap().getFileAsConfig(new File(PLUGIN.getBootstrap().getDataFolder(), "multipliers.yml"));

    private PaginatedMenu() {
    }

    @NotNull
    public static CoinsMenu createPaginatedGUI(@NotNull Player player, boolean global, @NotNull Collection<Multiplier> multipliers, String extraTitle) {
        return nextPage(player, multipliers, global, multipliers.size() >= 36, 0, null, extraTitle);
    }

    @NotNull
    private static CoinsMenu nextPage(@NotNull Player player, @NotNull Collection<Multiplier> contents, boolean global, boolean hasNext, int start, @Nullable CoinsMenu prevPage, String extraTitle) {
        CoinsMenu menu = new MultipliersMenu(player, PLUGIN.getString("Menus.Multipliers." + (global ? "Global" : "Local") + ".Title" + extraTitle, CompatUtils.getLocale(player)), contents, start, global);
        if (hasNext) {
            menu.setItem(53, menu.getItem(MULTIPLIERS_CONFIG, "Menus.Multipliers." + (global ? "Global" : "Local") + ".Next", player), p -> nextPage(p, contents, global, true, start + 36, menu, extraTitle).open(p));
        }
        if (prevPage != null) {
            menu.setItem(45, menu.getItem(MULTIPLIERS_CONFIG, "Menus.Multipliers." + (global ? "Global" : "Local") + ".Next", player), prevPage::open);
        }
        return menu;
    }

    private static void handleCloseSound(String path, @NotNull Player p) {
        String sound = MULTIPLIERS_CONFIG.getString(path + ".Sound");
        if (sound != null) {
            try {
                p.playSound(p.getLocation(), Sound.valueOf(sound), 10, MULTIPLIERS_CONFIG.getInt(path + ".Pitch", 1));
            } catch (IllegalArgumentException ex) {
                PLUGIN.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
                PLUGIN.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\nIf need more help, please open an issue in https://github.com/Beelzebu/Coins3/issues");
            }
        }
        p.closeInventory();
    }

    private static final class MultipliersMenu extends CoinsMenu {

        private final Player player;
        @NotNull
        private final List<Multiplier> contents;
        private final int start;
        @NotNull
        private final String path;

        MultipliersMenu(Player player, String title, @NotNull Collection<Multiplier> contents, int start, boolean global) {
            super(54, title);
            this.player = player;
            this.contents = new ArrayList<>(contents);
            this.start = start;
            path = "Menus.Multipliers." + (global ? "Global" : "Local");
            setItems();
        }

        @Override
        protected void setItems() {
            for (int i = 36; i < 45; i++) {
                setItem(i, ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.MAGENTA_STAINED_GLASS_PANE)).setDisplayName("&f").build());
            }
            setItem(49, getItem(PaginatedMenu.MULTIPLIERS_CONFIG, path + ".Close"), p -> PaginatedMenu.handleCloseSound(path + ".Close", p));
            if (contents.size() <= 0) {
                setItem(22, ItemBuilder.newBuilder(Material.POTION).setDisplayName(plugin.getString("Multipliers.Menu.No Multipliers.Name", CompatUtils.getLocale(player))).setLore(StringUtils.rep(plugin.getMessages(CompatUtils.getLocale(player)).getStringList("Multipliers.Menu.No Multipliers.Lore"))).addItemFlag(ItemFlag.HIDE_POTION_EFFECTS).build());
            } else {
                for (int i = 0; i <= (Math.min(contents.size() - 1, 35)); i++) {
                    Multiplier multiplier = contents.get(start + i);
                    setItem(i, getMultiplier(player, multiplier), p -> new ConfirmMenu(plugin.getString("Multipliers.Menu.Confirm.Title", CompatUtils.getLocale(p)), multiplier).open(p));
                }
            }
        }

        @NotNull
        private ItemStack getMultiplier(Player player, @NotNull Multiplier multiplier) {
            String path = this.path + ".Multiplier";
            ItemStack is = super.getItem(PaginatedMenu.MULTIPLIERS_CONFIG, path, player);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(StringUtils.rep(meta.getDisplayName(), multiplier));
            meta.setLore(StringUtils.rep(meta.getLore(), multiplier));
            is.setItemMeta(meta);
            return is;
        }
    }
}
