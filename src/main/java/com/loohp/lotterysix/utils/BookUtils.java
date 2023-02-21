/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.lotterysix.utils;

import com.cryptomorin.xseries.XMaterial;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.floodgate.FloodgateHook;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookUtils {

    private static final Material MATERIAL_WRITTEN_BOOK = XMaterial.WRITTEN_BOOK.parseMaterial();
    private static boolean bukkitHasOpenBook;

    static {
        try {
            Player.class.getMethod("openBook", ItemStack.class);
            bukkitHasOpenBook = true;
        } catch (NoSuchMethodException e) {
            bukkitHasOpenBook = false;
        }
    }

    public static void openBook(Player player, ItemStack book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (!book.getType().equals(MATERIAL_WRITTEN_BOOK)) {
            throw new IllegalArgumentException("Book must be Material.WRITTEN_BOOK");
        }
        if (LotterySixPlugin.hasFloodgate && FloodgateHook.isBedrockPlayer(player.getUniqueId())) {
            FloodgateHook.sendAlternateBook(player.getUniqueId(), book);
        } else {
            if (bukkitHasOpenBook) {
                player.openBook(book);
            } else {
                BookUtil.openPlayer(player, book);
            }
        }
    }

    public static ItemStack setPages(ItemStack book, List<String> pages) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (!book.getType().equals(MATERIAL_WRITTEN_BOOK)) {
            throw new IllegalArgumentException("Book must be Material.WRITTEN_BOOK");
        }
        pages = pages.stream().map(each -> ComponentSerializer.toString(new TextComponent(each))).collect(Collectors.toList());
        BookMeta meta = (BookMeta) book.getItemMeta();
        List<String> dummy = new ArrayList<>(pages.size());
        for (int i = 0; i < pages.size(); i++) {
            dummy.add("");
        }
        meta.setPages(dummy);
        book.setItemMeta(meta);
        for (int i = 0; i < pages.size(); i++) {
            if (!NBTEditor.contains(book, "pages", i)) {
                break;
            }
            book = NBTEditor.set(book, pages.get(i), "pages", i);
        }
        return book;
    }

}
