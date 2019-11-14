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
import com.github.beelzebu.coins.api.CoinsAPI;
import com.github.beelzebu.coins.api.config.AbstractConfigFile;
import com.github.beelzebu.coins.api.plugin.CoinsPlugin;
import com.github.beelzebu.coins.api.utils.StringUtils;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
public abstract class CoinsMenu {

    private static final Map<UUID, CoinsMenu> inventoriesByUUID = new HashMap<>();
    private static final Map<UUID, UUID> openInventories = new HashMap<>();
    protected final CoinsPlugin plugin = CoinsAPI.getPlugin();
    protected final AbstractConfigFile multipliersConfig = plugin.getBootstrap().getFileAsConfig(new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml"));
    protected final Inventory inv;
    protected final UUID uuid;
    private final Map<Integer, GUIAction> actions;

    public CoinsMenu(int size, String name) {
        if (size % 9 != 0 && (size * 9) % 9 != 0) {
            plugin.log("Menu size must be a multiple of 9, " + size + " isn't.");
            size = 54;
        }
        inv = Bukkit.createInventory(null, size < 9 ? size * 9 : size, name != null && !"".equals(name) ? name : new TranslatableComponent("tile.chest.name").toLegacyText());
        actions = new HashMap<>();
        uuid = UUID.randomUUID();
        inventoriesByUUID.put(uuid, this);
    }

    public static Map<UUID, CoinsMenu> getInventoriesByUUID() {
        return Collections.unmodifiableMap(inventoriesByUUID);
    }

    public static Map<UUID, UUID> getOpenInventories() {
        return openInventories;
    }

    public final Inventory getInv() {
        return inv;
    }

    public final void setItem(int slot, ItemStack is, GUIAction action) {
        inv.setItem(slot, is);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public final void setItem(int slot, ItemStack is) {
        setItem(slot, is, null);
    }

    public void open(Player p) {
        plugin.getBootstrap().runSync(() -> {
            p.closeInventory();
            if (inv.getContents().length == 0) {
                setItems();
            }
            p.openInventory(inv);
            openInventories.put(p.getUniqueId(), uuid);
        });
    }

    public final Map<Integer, GUIAction> getActions() {
        return Collections.unmodifiableMap(actions);
    }

    public final void delete() {
        Bukkit.getOnlinePlayers().stream().filter(p -> openInventories.get(p.getUniqueId()) != null && openInventories.get(p.getUniqueId()).equals(uuid)).forEach(Player::closeInventory);
        inventoriesByUUID.remove(uuid);
    }

    public ItemStack getItem(AbstractConfigFile config, String path) {
        return getItem(config, path, null);
    }

    public ItemStack getItem(AbstractConfigFile config, String path, Player player) {
        Objects.requireNonNull(config, "Config can't be null");
        Objects.requireNonNull(path, "Item path can't be null");
        Material mat;
        try {
            mat = Material.valueOf(config.getString(path + ".Material").toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.log("\"" + config.getString(path + ".Material").toUpperCase() + "\" is invalid, it will be set as STONE.");
            mat = Material.STONE;
        }
        ItemStack is = new ItemStack(mat);
        if (config.getInt(path + ".Damage") >= 0) {
            is = CompatUtils.setDamage(is, config.getInt(path + ".Damage"));
        }
        ItemMeta meta = is.getItemMeta();
        if (config.getString(path + ".Name") != null) {
            meta.setDisplayName(StringUtils.rep(config.getString(path + ".Name")));
        }
        if (config.getStringList(path + ".Lore") != null) {
            meta.setLore(StringUtils.rep(config.getStringList(path + ".Lore")));
        }
        if (config.getInt(path + ".Amount") >= 1) {
            is.setAmount(config.getInt(path + ".Amount"));
        }
        if (config.getBoolean(path + ".HideFlags")) {
            meta.addItemFlags(ItemFlag.values());
        }
        if (!config.getStringList(path + ".Enchantments").isEmpty()) {
            config.getStringList(path + ".Enchantments").forEach(enchantmentFormatted -> {
                String enchantmentString = enchantmentFormatted.split(":")[0];
                int level = enchantmentFormatted.split(":").length == 2 && isNumber(enchantmentFormatted.split(":")[1]) ? Integer.parseInt(enchantmentFormatted.split(":")[1]) : 1;
                Enchantment enchantment = CompatUtils.getEnchantment(enchantmentString);
                if (enchantment != null) {
                    meta.addEnchant(enchantment, level >= 1 ? level : 1, true);
                } else {
                    plugin.log("\"" + enchantmentString + "\" is not a valid Enchantment, please see https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html");
                }
            });
        }
        if (config.getString(path + ".PotionType") != null && is.getType().equals(Material.POTION)) {
            try {
                CompatUtils.setPotionType((PotionMeta) meta, PotionType.valueOf(config.getString(path + ".PotionType").toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.log("\"" + config.getString(path + ".PotionType") + "\" is not a valid PotionType");
            }
        }
        // override name and lore using player's lang
        if (player != null) {
            if (!plugin.getString(path + ".Name", CompatUtils.getLocale(player)).equals("")) {
                meta.setDisplayName(plugin.getString(path + ".Name", CompatUtils.getLocale(player)));
            }
            if (!plugin.getStringList(path + ".Lore", CompatUtils.getLocale(player)).isEmpty()) {
                meta.setLore(plugin.getStringList(path + ".Lore", CompatUtils.getLocale(player)));
            }
        }
        is.setItemMeta(meta);
        return is;
    }

    protected void setItems() {
        // NOOP
    }

    protected boolean isNumber(String string) {
        if (string == null) {
            return false;
        }
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public interface GUIAction {

        void click(Player p);
    }
}
