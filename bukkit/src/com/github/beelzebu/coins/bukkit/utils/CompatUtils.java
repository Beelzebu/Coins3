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

import com.github.beelzebu.coins.bukkit.CoinsBukkitMain;
import com.github.beelzebu.coins.bukkit.CoinsBukkitPlugin;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
@SuppressWarnings("deprecation")
public final class CompatUtils {

    private static final CoinsBukkitPlugin PLUGIN = CoinsBukkitMain.getPlugin(CoinsBukkitMain.class).getPlugin();

    public enum MinecraftVersion {
        MINECRAFT_1_8(1),
        MINECRAFT_1_9(2),
        MINECRAFT_1_10(3),
        MINECRAFT_1_11(4),
        MINECRAFT_1_12(5),
        MINECRAFT_1_13(6),
        MINECRAFT_1_14(7),
        MINECRAFT_1_15(8);

        private final int id;

        MinecraftVersion(int id) {
            this.id = id;
        }

        public boolean isAfterOrEq(MinecraftVersion another) {
            return id >= another.id;
        }
    }

    public static MinecraftVersion VERSION;
    private static final Map<MaterialItem, ItemStack> materials = new EnumMap<>(MaterialItem.class);
    private static Method localeMethod;

    public static void setup() {
        PLUGIN.log("Detected " + getRawVersion() + " server version.");
        switch (getMinorVersion()) {
            case 8:
                VERSION = MinecraftVersion.MINECRAFT_1_8;
                break;
            case 9:
                VERSION = MinecraftVersion.MINECRAFT_1_9;
                break;
            case 10:
                VERSION = MinecraftVersion.MINECRAFT_1_10;
                break;
            case 11:
                VERSION = MinecraftVersion.MINECRAFT_1_11;
                break;
            case 12:
                VERSION = MinecraftVersion.MINECRAFT_1_12;
                break;
            case 13:
                VERSION = MinecraftVersion.MINECRAFT_1_13;
                break;
            case 14:
                VERSION = MinecraftVersion.MINECRAFT_1_14;
                break;
            case 15:
                VERSION = MinecraftVersion.MINECRAFT_1_15;
                break;
            default:
                PLUGIN.log("We don't have official support for: " + getRawVersion() + " using support for 1.15");
                VERSION = MinecraftVersion.MINECRAFT_1_15;
                break;
        }
    }

    public static ItemStack getItem(MaterialItem materialItem) {
        return materials.computeIfAbsent(materialItem, mi -> {
            switch (materialItem) {
                case MAGENTA_STAINED_GLASS_PANE:
                    if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
                        return new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 2);
                    }
                case GREEN_STAINED_GLASS:
                    if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
                        return new ItemStack(Material.GREEN_STAINED_GLASS);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS"), 1, (short) 13);
                    }
                case RED_STAINED_GLASS:
                    if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
                        return new ItemStack(Material.RED_STAINED_GLASS);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS"), 1, (short) 14);
                    }
                default:
                    return new ItemStack(Material.AIR);
            }
        });
    }

    public static String getLocale(Player player) {
        if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_12)) {
            return player.getLocale();
        } else { // this doesn't exists in 1.8
            if (localeMethod != null) {
                try {
                    return (String) localeMethod.invoke(player);
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }
            try {
                return (String) (localeMethod = Player.Spigot.class.getMethod("getLocale")).invoke(player);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        return "en";
    }

    public static void setPotionType(PotionMeta meta, PotionType type) {
        if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
            meta.setBasePotionData(new PotionData(type));
        } else {
            if (type.getEffectType() != null) {
                meta.setMainEffect(type.getEffectType()); // it was deprecated in 1.13
            }
        }
    }

    public static Enchantment getEnchantment(@NonNull String string) {
        if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
            Optional<Enchantment> enchantmentOptional = Stream.of(Enchantment.values()).filter(enchantment -> enchantment.getKey().getKey().equalsIgnoreCase(string)).findFirst();
            if (enchantmentOptional.isPresent()) {
                return enchantmentOptional.get();
            }
        } else {
            if (Enchantment.getByName(string.toUpperCase()) != null) {
                return Enchantment.getByName(string.toUpperCase());
            }
        }
        return null;
    }

    public static ItemStack setDamage(ItemStack itemStack, int damage) {
        if (VERSION.isAfterOrEq(MinecraftVersion.MINECRAFT_1_13)) {
            if (itemStack.getItemMeta() instanceof Damageable) {
                Damageable meta = (Damageable) itemStack.getItemMeta();
                meta.setDamage(damage);
                itemStack.setItemMeta((ItemMeta) meta);
            }
        } else {
            itemStack.setDurability((short) damage);
        }
        return itemStack;
    }

    public enum MaterialItem {
        MAGENTA_STAINED_GLASS_PANE, GREEN_STAINED_GLASS, RED_STAINED_GLASS
    }

    private static String getRawVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf(".") + 1);
    }

    private static int getMinorVersion() {
        String ver = getRawVersion();
        int verInt = -1;
        try {
            verInt = Integer.parseInt(ver.split("_")[1]);
        } catch (IllegalArgumentException e) {
            Bukkit.getScheduler().runTask(CoinsBukkitMain.getPlugin(CoinsBukkitMain.class), () -> {
                CoinsBukkitMain.getPlugin(CoinsBukkitMain.class).getPlugin().log("An error occurred getting server version, please contact developer.");
                CoinsBukkitMain.getPlugin(CoinsBukkitMain.class).getPlugin().log("Detected version " + ver);
            });
        }
        return verInt;
    }
}
