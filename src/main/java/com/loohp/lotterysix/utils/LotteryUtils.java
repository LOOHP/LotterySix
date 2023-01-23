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

import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.completed.CompletedLotterySixGame;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.playable.PlayableLotterySixGame;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;
import java.util.Date;

public class LotteryUtils {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String str, LotterySix lotterySix) {
        str = str.replace("{PrizePerBet}", lotterySix.pricePerBet + "");
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str.replace("{" + prizeTier.name() + "Odds}", DECIMAL_FORMAT.format(calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier)));
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str : PlaceholderAPI.setPlaceholders(player, str));
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix, PlayableLotterySixGame game) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix, game);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String str, LotterySix lotterySix, PlayableLotterySixGame game) {
        if (game == null) {
            return formatPlaceholders(player, str, lotterySix);
        }
        str = str
                .replace("{Date}", lotterySix.dateFormat.format(new Date(game.getScheduledDateTime())))
                .replace("{PrizePerBet}", lotterySix.pricePerBet + "")
                .replace("{TotalBets}", game.getTotalBets() + "")
                .replace("{PrizePool}", game.estimatedPrizePool(lotterySix.lowestTopPlacesPrize, lotterySix.taxPercentage) + "");
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str.replace("{" + prizeTier.name() + "Odds}", DECIMAL_FORMAT.format(calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier)));
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str : PlaceholderAPI.setPlaceholders(player, str));
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix, CompletedLotterySixGame game) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix, game);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String str, LotterySix lotterySix, CompletedLotterySixGame game) {
        if (game == null) {
            return formatPlaceholders(player, str, lotterySix);
        }
        str = str
                .replace("{Date}", lotterySix.dateFormat.format(new Date(game.getDatetime())))
                .replace("{PrizePerBet}", lotterySix.pricePerBet + "")
                .replace("{TotalBets}", game.getTotalBets() + "")
                .replace("{TotalPrizes}", game.getTotalPrizes() + "")
                .replace("{FirstToThirdPlaceWinnersCount}", game.getWinnings().stream().filter(each -> each.getTier().ordinal() < 3).count() + "")
                .replace("{FirstNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(0)))
                .replace("{SecondNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(1)))
                .replace("{ThirdNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(2)))
                .replace("{FourthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(3)))
                .replace("{FifthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(4)))
                .replace("{SixthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(5)))
                .replace("{SpecialNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getSpecialNumber()));
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str
                    .replace("{" + prizeTier.name() + "Odds}", calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier) + "")
                    .replace("{" + prizeTier.name() + "Prize}", game.getPrizeForTier(prizeTier) + "")
                    .replace("{" + prizeTier.name() + "PrizeCount}", game.getWinnerCountForTier(prizeTier) + "");
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str : PlaceholderAPI.setPlaceholders(player, str));
    }

    public static double calculateOdds(int numberOfChoices, PrizeTier prizeTier) {
        switch (prizeTier) {
            case FIRST: {
                return 1.0 / probabilityFormula(numberOfChoices, 6);
            }
            case SECOND: {
                return probabilityFormula(6, 5) / probabilityFormula(numberOfChoices, 6);
            }
            case THIRD: {
                return probabilityFormula(6, 5) * probabilityFormula(42, 1) / probabilityFormula(numberOfChoices, 6);
            }
            case FOURTH: {
                return probabilityFormula(6, 4) * probabilityFormula(42, 1) / probabilityFormula(numberOfChoices, 6);
            }
            case FIFTH: {
                return probabilityFormula(6, 4) * probabilityFormula(42, 2) / probabilityFormula(numberOfChoices, 6);
            }
            case SIXTH: {
                return probabilityFormula(6, 3) * probabilityFormula(42, 2) / probabilityFormula(numberOfChoices, 6);
            }
            case SEVENTH: {
                return probabilityFormula(6, 3) * probabilityFormula(42, 3) / probabilityFormula(numberOfChoices, 6);
            }
            default: {
                throw new IllegalArgumentException("Unknown prize tier " + prizeTier);
            }
        }
    }

    public static double calculateOddsOneOver(int numberOfChoices, PrizeTier prizeTier) {
        double odds = calculateOdds(numberOfChoices, prizeTier);
        return 1.0 / odds;
    }

    private static double probabilityFormula(int a, int b) {
        double result = 1;
        for (; b > 0; a--, b--) {
            result *= (double) a / (double) b;
        }
        return result;
    }

}
