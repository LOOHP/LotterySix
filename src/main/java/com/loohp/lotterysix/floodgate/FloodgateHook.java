/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

package com.loohp.lotterysix.floodgate;

import com.cryptomorin.xseries.XMaterial;
import com.loohp.lotterysix.LotterySixPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FloodgateHook {

    private static final Material MATERIAL_WRITTEN_BOOK = XMaterial.WRITTEN_BOOK.parseMaterial();

    public static boolean isBedrockPlayer(UUID uuid) {
        if (!LotterySixPlugin.hasFloodgate) {
            return false;
        }
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    public static void sendAlternateBook(UUID uuid, ItemStack book) {
        if (!isBedrockPlayer(uuid)) {
            throw new IllegalArgumentException("uuid is not a bedrock player");
        }
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        if (!book.getType().equals(MATERIAL_WRITTEN_BOOK)) {
            throw new IllegalArgumentException("Book must be Material.WRITTEN_BOOK");
        }
        List<BaseComponent[]> pages = ((BookMeta) book.getItemMeta()).spigot().getPages();
        List<String> strPages = pages.stream().map(each -> {
            BaseComponent component = Arrays.stream(each).reduce(new TextComponent(), (a, b) -> {
                a.addExtra(b);
                return a;
            });
            appendHoverEvents(component, h -> h.getAction().equals(HoverEvent.Action.SHOW_TEXT));
            return component.toLegacyText();
        }).collect(Collectors.toList());
        String result = String.join("\n\n" + ChatColor.RESET + "---------------------\n\n", strPages);
        FloodgateApi.getInstance().sendForm(uuid, SimpleForm.builder().title("").content(result));
    }

    private static void appendHoverEvents(BaseComponent component, Predicate<HoverEvent> predicate) {
        if (component.getHoverEvent() != null && predicate.test(component.getHoverEvent())) {
            boolean endsWithNewLine = component.toPlainText().endsWith("\n");
            component.addExtra((endsWithNewLine ? "" : "\n") + ChatColor.RESET + "(" + Arrays.stream(component.getHoverEvent().getValue()).reduce(new TextComponent(), (a, b) -> {
                a.addExtra(b);
                return a;
            }).toLegacyText() + ChatColor.RESET + ")\n");
        }
        if (component.getExtra() != null) {
            for (BaseComponent extra : component.getExtra()) {
                appendHoverEvents(extra, predicate);
            }
        }
    }

}
