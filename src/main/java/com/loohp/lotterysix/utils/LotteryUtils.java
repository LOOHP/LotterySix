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
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.LazyReplaceString;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

public class LotteryUtils {

    public static final DecimalFormat ODDS_FORMAT = new DecimalFormat("0.##");
    public static final DecimalFormat BET_COUNT_FORMAT = new DecimalFormat("0.0");
    
    private static SimpleDateFormat dateFormat(LotterySix instance, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, instance.locale);
        format.setTimeZone(instance.timezone);
        return format;
    }

    public static String oneSignificantFigure(long value) {
        StringBuilder sb = new StringBuilder(Long.toString(value));
        for (int i = sb.charAt(0) == '-' ? 2 : 1; i < sb.length(); i++) {
            sb.setCharAt(i, '0');
        }
        return sb.toString();
    }

    public static long calculatePrice(BetNumbers numbers, LotterySix lotterySix) {
        return calculatePrice(numbers.getNumbers().size(), numbers.getBankersNumbers().size(), lotterySix.pricePerBet) * numbers.getSetsSize();
    }

    public static long calculatePrice(BetNumbersBuilder builder, LotterySix lotterySix) {
        if (!builder.completed()) {
            return 0;
        }
        return calculatePrice(builder.size(), builder.bankerSize(), lotterySix.pricePerBet) * builder.setsSize();
    }

    public static long calculatePrice(int size, int bankerSize, long pricePerBet) {
        return MathUtils.combinationsCount(size, bankerSize) * pricePerBet;
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String input, LotterySix lotterySix) {
        LazyReplaceString str = new LazyReplaceString(input)
                .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()))
                .replace("{Date}", () -> "-")
                .replace("{GameNumberRaw}", () -> "-")
                .replace("{GameNumber}", () -> "-")
                .replace("{SpecialName}", () -> "")
                .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "");
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str.replace("{" + prizeTier.name() + "Odds}", () -> {
                double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
            });
        }
        NumberStatistics stats = NumberStatistics.NOT_EVER_DRAWN;
        for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
            str = str.replace("{" + i + "LastDrawn}", () -> stats.isNotEverDrawn() ? lotterySix.guiNumberStatisticsNever : (stats.getLastDrawn() == 0 ? "-" : stats.getLastDrawn() + ""));
            str = str.replace("{" + i + "TimesDrawn}", () -> stats.getTimesDrawn() + "");
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str.toString() : PlaceholderAPI.setPlaceholders(player, str.toString()));
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix, PlayableLotterySixGame game) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix, game);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String input, LotterySix lotterySix, PlayableLotterySixGame game) {
        LazyReplaceString str;
        if (game == null) {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", "-")
                    .replaceAll("\\{Date_(.*?)}", result -> "-")
                    .replace("{GameNumberRaw}", "-")
                    .replace("{GameNumber}", "-")
                    .replace("{SpecialName}", "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()))
                    .replace("{TotalBets}", "-")
                    .replace("{CarryOverFund}", "-")
                    .replace("{PrizePool}", "-")
                    .replace("{BetPlayerNames}", "-");
            for (PrizeTier prizeTier : PrizeTier.values()) {
                str = str.replace("{" + prizeTier.name() + "Odds}", () -> {
                    double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                    return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                });
            }
            for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
                str = str.replace("{" + i + "LastDrawn}", "-");
                str = str.replace("{" + i + "TimesDrawn}", "-");
            }
        } else {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", () -> lotterySix.dateFormat.format(new Date(game.getDatetime())))
                    .replaceAll("\\{Date_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date(game.getDatetime())))
                    .replace("{GameNumberRaw}", () -> game.getGameNumber() + "")
                    .replace("{GameNumber}", () -> game.getGameNumber() + (game.hasSpecialName() ? " " + game.getSpecialName() : ""))
                    .replace("{SpecialName}", () -> game.hasSpecialName() ? game.getSpecialName() : "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()))
                    .replace("{TotalBets}", () -> StringUtils.formatComma(game.getTotalBets()))
                    .replace("{CarryOverFund}", () -> StringUtils.formatComma(game.getCarryOverFund(lotterySix.estimationRoundToNearest)))
                    .replace("{PrizePool}", () -> StringUtils.formatComma(game.estimatedPrizePool(lotterySix.maxTopPlacesPrize, lotterySix.taxPercentage, lotterySix.estimationRoundToNearest)))
                    .replace("{BetPlayerNames}", () -> chainPlayerBetNames(game.getBets()));
            for (PrizeTier prizeTier : PrizeTier.values()) {
                str = str.replace("{" + prizeTier.name() + "Odds}", () -> {
                    double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                    return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                });
            }
            for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
                NumberStatistics stats = game.getNumberStatistics(i);
                str = str.replace("{" + i + "LastDrawn}", () -> stats.isNotEverDrawn() ? lotterySix.guiNumberStatisticsNever : (stats.getLastDrawn() == 0 ? "-" : stats.getLastDrawn() + ""));
                str = str.replace("{" + i + "TimesDrawn}", () -> stats.getTimesDrawn() + "");
            }
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str.toString() : PlaceholderAPI.setPlaceholders(player, str.toString()));
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix, CompletedLotterySixGameIndex game) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix, game);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String input, LotterySix lotterySix, CompletedLotterySixGameIndex game) {
        LazyReplaceString str;
        if (game == null) {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", "-")
                    .replaceAll("\\{Date_(.*?)}", result -> "-")
                    .replace("{GameNumberRaw}", "-")
                    .replace("{GameNumber}", "-")
                    .replace("{SpecialName}", "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()));
            for (PrizeTier prizeTier : PrizeTier.values()) {
                str = str.replace("{" + prizeTier.name() + "Odds}", () -> {
                    double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                    return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                });
            }
        } else {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", () -> lotterySix.dateFormat.format(new Date(game.getDatetime())))
                    .replaceAll("\\{Date_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date(game.getDatetime())))
                    .replace("{GameNumberRaw}", () -> game.getGameNumber() + "")
                    .replace("{GameNumber}", () -> game.getGameNumber() + (game.hasSpecialName() ? " " + game.getSpecialName() : ""))
                    .replace("{SpecialName}", () -> game.hasSpecialName() ? game.getSpecialName() : "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()));
            for (PrizeTier prizeTier : PrizeTier.values()) {
                str = str.replace("{" + prizeTier.name() + "Odds}", () -> {
                    double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                    return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                });
            }
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str.toString() : PlaceholderAPI.setPlaceholders(player, str.toString()));
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix, CompletedLotterySixGame game) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix, game);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String input, LotterySix lotterySix, CompletedLotterySixGame game) {
        LazyReplaceString str;
        if (game == null) {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", "-")
                    .replaceAll("\\{Date_(.*?)}", result -> "-")
                    .replace("{GameNumberRaw}", "-")
                    .replace("{GameNumber}", "-")
                    .replace("{SpecialName}", "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()))
                    .replace("{TotalBets}", () -> "-")
                    .replace("{TotalPrizes}", () -> "-")
                    .replace("{LotteriesFundsRaised}", () -> "-")
                    .replace("{FirstNumber}", () -> "-")
                    .replace("{SecondNumber}", () -> "-")
                    .replace("{ThirdNumber}", () -> "-")
                    .replace("{FourthNumber}", () -> "-")
                    .replace("{FifthNumber}", () -> "-")
                    .replace("{SixthNumber}", () -> "-")
                    .replace("{FirstNumberOrdered}", "-")
                    .replace("{SecondNumberOrdered}", "-")
                    .replace("{ThirdNumberOrdered}", "-")
                    .replace("{FourthNumberOrdered}", "-")
                    .replace("{FifthNumberOrdered}", "-")
                    .replace("{SixthNumberOrdered}", "-")
                    .replace("{SpecialNumber}", "-")
                    .replace("{BetPlayerNames}", "-");
            for (PrizeTier prizeTier : PrizeTier.values()) {
                String prizeTierName = prizeTier.name();
                str = str
                        .replace("{" + prizeTier.name() + "Odds}", () -> {
                            double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                            return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                        })
                        .replace("{" + prizeTierName + "Prize}", "-")
                        .replace("{" + prizeTierName + "PrizeCount}", "-")
                        .replace("{" + prizeTierName + "PlayerNames}", "-");
            }
            for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
                str = str.replace("{" + i + "LastDrawn}", "-");
                str = str.replace("{" + i + "TimesDrawn}", "-");
            }
        } else {
            str = new LazyReplaceString(input)
                    .replace("{Now}", () -> lotterySix.dateFormat.format(new Date()))
                    .replaceAll("\\{Now_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date()))
                    .replace("{Date}", () -> lotterySix.dateFormat.format(new Date(game.getDatetime())))
                    .replaceAll("\\{Date_(.*?)}", result -> dateFormat(lotterySix, result.group(1)).format(new Date(game.getDatetime())))
                    .replace("{GameNumberRaw}", () -> game.getGameNumber() + "")
                    .replace("{GameNumber}", () -> game.getGameNumber() + (game.hasSpecialName() ? " " + game.getSpecialName() : ""))
                    .replace("{SpecialName}", () -> game.hasSpecialName() ? game.getSpecialName() : "")
                    .replace("{NumberOfChoices}", () -> lotterySix.numberOfChoices + "")
                    .replace("{PricePerBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet))
                    .replace("{PricePerPartialBet}", () -> StringUtils.formatComma(lotterySix.pricePerBet / BetUnitType.PARTIAL.getDivisor()))
                    .replace("{TotalBets}", () -> StringUtils.formatComma(game.getTotalBets()))
                    .replace("{TotalPrizes}", () -> StringUtils.formatComma(game.getTotalPrizes()))
                    .replace("{LotteriesFundsRaised}", () -> StringUtils.formatComma(game.getLotteriesFunds()))
                    .replace("{FirstNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(0)))
                    .replace("{SecondNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(1)))
                    .replace("{ThirdNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(2)))
                    .replace("{FourthNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(3)))
                    .replace("{FifthNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(4)))
                    .replace("{SixthNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(5)))
                    .replace("{FirstNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(0)))
                    .replace("{SecondNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(1)))
                    .replace("{ThirdNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(2)))
                    .replace("{FourthNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(3)))
                    .replace("{FifthNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(4)))
                    .replace("{SixthNumberOrdered}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(5)))
                    .replace("{SpecialNumber}", () -> ChatColorUtils.applyNumberColor(game.getDrawResult().getSpecialNumber()))
                    .replace("{BetPlayerNames}", () -> chainPlayerBetNames(game.getBets()));
            for (PrizeTier prizeTier : PrizeTier.values()) {
                String prizeTierName = prizeTier.name();
                str = str
                        .replace("{" + prizeTier.name() + "Odds}", () -> {
                            double odds = prizeTier.getWinningCriteria().oneOverProbability(lotterySix.numberOfChoices);
                            return Double.isFinite(odds) ? ODDS_FORMAT.format(odds) : "-";
                        })
                        .replace("{" + prizeTierName + "Prize}", () -> StringUtils.formatComma(game.getPrizeForTier(prizeTier)))
                        .replace("{" + prizeTierName + "PrizeCount}", () -> BET_COUNT_FORMAT.format(game.getWinnerCountForTier(prizeTier)) + "")
                        .replace("{" + prizeTierName + "PlayerNames}", () -> chainPlayerWinningsNames(game.getWinnings(prizeTier)));
            }
            for (int i = 1; i <= lotterySix.numberOfChoices; i++) {
                NumberStatistics stats = game.getNumberStatistics(i);
                str = str.replace("{" + i + "LastDrawn}", () -> stats.isNotEverDrawn() ? lotterySix.guiNumberStatisticsNever : (stats.getLastDrawn() == 0 ? "-" : stats.getLastDrawn() + ""));
                str = str.replace("{" + i + "TimesDrawn}", () -> stats.getTimesDrawn() + "");
            }
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str.toString() : PlaceholderAPI.setPlaceholders(player, str.toString()));
    }

    public static String chainPlayerBetNames(Collection<PlayerBets> bets) {
        return bets.stream().map(each -> each.getName()).distinct().collect(Collectors.joining(", "));
    }

    public static String chainPlayerWinningsNames(Collection<PlayerWinnings> winnings) {
        return winnings.stream().map(each -> each.getName()).distinct().collect(Collectors.joining(", "));
    }

}
