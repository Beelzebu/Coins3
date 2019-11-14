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
package com.github.beelzebu.coins.bukkit.menus;

import com.github.beelzebu.coins.bukkit.utils.CompatUtils;
import com.github.beelzebu.coins.bukkit.utils.ItemBuilder;
import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.Multiplier;
import com.github.beelzebu.coins.api.MultiplierType;
import com.github.beelzebu.coins.api.utils.StringUtils;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
public class ConfirmMenu extends CoinsMenu {

    private final Multiplier multiplier;

    ConfirmMenu(String name, Multiplier data) {
        super(9, name);
        multiplier = data;
    }

    public void setItems(Player p) {
        ItemStack accept = ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.GREEN_STAINED_GLASS)).setDisplayName(plugin.getString("Multipliers.Menu.Confirm.Accept", CompatUtils.getLocale(p))).build();
        setItem(2, accept, player -> {
            if (CoinsAPI.getMultipliers().stream().filter(multiplier -> !multiplier.getData().getType().equals(MultiplierType.GLOBAL) && !multiplier.getData().getType().equals(MultiplierType.PERSONAL)).collect(Collectors.toSet()).isEmpty()) {
                multiplier.enable(false);
                String sound = plugin.getConfig().getString("Multipliers.GUI.Use.Sound");
                if (sound != null) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf(sound), 10, 2);
                    } catch (IllegalArgumentException ex) {
                        plugin.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
                        plugin.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                                + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
                    }
                }
            } else {
                multiplier.enable(true);
                String sound = plugin.getConfig().getString("Multipliers.GUI.Use.Fail.Sound");
                if (sound != null) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf(sound), 10, 1);
                    } catch (IllegalArgumentException ex) {
                        plugin.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
                        plugin.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                                + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
                    }
                }
                player.sendMessage(plugin.getString("Multipliers.Already active", CompatUtils.getLocale(player)));
            }
            player.closeInventory();
        });
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        CompatUtils.setPotionType(potionMeta, PotionType.FIRE_RESISTANCE);
        potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        potionMeta.setDisplayName(StringUtils.rep(plugin.getString("Multipliers.Menu.Multipliers.Name", CompatUtils.getLocale(p)), multiplier));
        potionMeta.setLore(plugin.getStringList("Multipliers.Menu.Multipliers.Lore", CompatUtils.getLocale(p)));
        potion.setItemMeta(potionMeta);
        setItem(4, potion);
        ItemStack decline = ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.RED_STAINED_GLASS)).setDisplayName(plugin.getString("Multipliers.Menu.Confirm.Decline", CompatUtils.getLocale(p))).build();
        setItem(6, decline, player -> {
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("Multipliers.GUI.Use.Fail.Sound", "VILLAGER_NO")), 10, 1);
            player.closeInventory();
        });
    }

    @Override
    public void open(Player p) {
        setItems(p);
        super.open(p);
    }
}
