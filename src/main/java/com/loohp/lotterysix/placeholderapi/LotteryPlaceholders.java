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

package com.loohp.lotterysix.placeholderapi;

import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.utils.LotteryUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class LotteryPlaceholders extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return String.join(", ", LotterySixPlugin.plugin.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "lotterysix";
    }

    @Override
    public String getVersion() {
        return LotterySixPlugin.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getRequiredPlugin() {
        return LotterySixPlugin.plugin.getName();
    }

    @Override
    public String onRequest(OfflinePlayer offlineplayer, String identifier) {
        if (identifier.startsWith("currentgame_")) {
            if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                return "";
            } else {
                String str = "{" + identifier.substring("currentgame_".length()) + "}";
                return LotteryUtils.formatPlaceholders(offlineplayer, str, LotterySixPlugin.getInstance(), LotterySixPlugin.getInstance().getCurrentGame());
            }
        } else if (identifier.startsWith("lastgame_")) {
            if (LotterySixPlugin.getInstance().getCompletedGames().isEmpty()) {
                return "";
            } else {
                String str = "{" + identifier.substring("lastgame_".length()) + "}";
                return LotteryUtils.formatPlaceholders(offlineplayer, str, LotterySixPlugin.getInstance(), LotterySixPlugin.getInstance().getCompletedGames().get(0));
            }
        }
        return null;
    }

}