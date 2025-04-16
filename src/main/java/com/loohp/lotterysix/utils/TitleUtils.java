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

package com.loohp.lotterysix.utils;

import org.bukkit.entity.Player;
import xyz.tozymc.spigot.api.title.TitleApi;

public class TitleUtils {

    private static boolean bukkitHasTitle;

    static {
        try {
            Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            bukkitHasTitle = true;
        } catch (NoSuchMethodException e) {
            bukkitHasTitle = false;
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (bukkitHasTitle) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } else {
            TitleApi.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

}
