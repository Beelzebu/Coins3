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
package com.github.beelzebu.coins.bukkit.utils;

import com.github.beelzebu.coins.api.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * @author Beelzebu
 */
public class ItemBuilder {

    private final Material material;
    private String displayName;
    private List<String> lore;
    private Set<ItemFlag> flags;

    private ItemBuilder(Material material) {
        this.material = material;
    }

    @NotNull
    public static ItemBuilder newBuilder(Material material) {
        return new ItemBuilder(material);
    }

    @NotNull
    public static ItemBuilder newBuilder(@NotNull ItemStack itemStack) {
        return new ItemBuilder(itemStack.getType()).setDisplayName(itemStack.getItemMeta().getDisplayName()).setLore(itemStack.getItemMeta().getLore()).addItemFlag(itemStack.getItemMeta().getItemFlags());
    }

    @NotNull
    public ItemBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    @NotNull
    public ItemBuilder setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    @NotNull
    public ItemBuilder setLore(String... lore) {
        this.lore = Arrays.asList(lore);
        return this;
    }

    @NotNull
    public ItemBuilder setLore(String lore) {
        this.lore = Collections.singletonList(lore);
        return this;
    }

    @NotNull
    public ItemBuilder addLore(String line) {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(line);
        return this;
    }

    @NotNull
    public ItemBuilder addLore(String line, int index) {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(index, line);
        return this;
    }


    @NotNull
    public ItemBuilder addItemFlag(ItemFlag flag) {
        if (flags == null) {
            flags = new HashSet<>();
        }
        flags.add(flag);
        return this;
    }

    @NotNull
    public ItemBuilder addItemFlag(@NotNull Collection<? extends ItemFlag> flags) {
        if (this.flags == null) {
            this.flags = new HashSet<>();
        }
        this.flags.addAll(flags);
        return this;
    }

    @NotNull
    public ItemStack build() {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) {
            meta.setDisplayName(StringUtils.rep(displayName));
        }
        if (lore != null) {
            meta.setLore(StringUtils.rep(lore));
        }
        if (flags != null) {
            meta.addItemFlags(flags.toArray(new ItemFlag[]{}));
        }
        item.setItemMeta(meta);
        return item;
    }
}
