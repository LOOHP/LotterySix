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

import com.cronutils.model.Cron;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.utils.CronUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.time.ZonedDateTime;
import java.util.Date;

public class LotteryPlaceholders extends PlaceholderExpansion {

    private static String asString(Object input) {
        return input == null ? "N/A" : input.toString();
    }

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
        } else if (identifier.startsWith("preference_")) {
            PlayerPreferenceKey key = PlayerPreferenceKey.fromKey(identifier.substring("preference_".length()));
            if (key == null) {
                return "";
            } else {
                return asString(LotterySixPlugin.getInstance().getPlayerPreferenceManager().getLotteryPlayer(offlineplayer.getUniqueId()).getPreference(key));
            }
        } else if (identifier.startsWith("stats_")) {
            PlayerStatsKey key = PlayerStatsKey.fromKey(identifier.substring("stats_".length()));
            if (key == null) {
                return "";
            } else {
                return asString(LotterySixPlugin.getInstance().getPlayerPreferenceManager().getLotteryPlayer(offlineplayer.getUniqueId()).getStats(key));
            }
        } else if (identifier.startsWith("scheduler_")) {
            Cron cron = LotterySixPlugin.getInstance().runInterval;
            if (cron == null) {
                return asString(null);
            } else {
                String type = identifier.substring("scheduler_".length());
                if (type.equalsIgnoreCase("interval")) {
                    return CronUtils.getDescriptor(LotterySixPlugin.getInstance().locale).describe(cron);
                } else if (type.equalsIgnoreCase("next")) {
                    ZonedDateTime dateTime = CronUtils.getNextExecution(cron, CronUtils.getNow(LotterySixPlugin.getInstance().timezone));
                    if (dateTime == null) {
                        return asString(null);
                    } else {
                        return LotterySixPlugin.getInstance().dateFormat.format(new Date(dateTime.toInstant().toEpochMilli()));
                    }
                }
            }
        }
        return null;
    }

}