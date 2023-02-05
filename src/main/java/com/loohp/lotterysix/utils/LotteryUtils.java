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
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class LotteryUtils {

    public static final BigInteger SIX_FACTORIAL = BigInteger.valueOf(720);
    public static final DecimalFormat ODDS_FORMAT = new DecimalFormat("0.##");
    public static final DecimalFormat BET_COUNT_FORMAT = new DecimalFormat("0.#");

    public static String oneSignificantFigure(long value) {
        StringBuilder sb = new StringBuilder(Long.toString(value));
        for (int i = sb.charAt(0) == '-' ? 2 : 1; i < sb.length(); i++) {
            sb.setCharAt(i, '0');
        }
        return sb.toString();
    }

    public static BigInteger factorial(long number) {
        return LongStream.rangeClosed(1, number).mapToObj(i -> BigInteger.valueOf(i)).reduce(BigInteger.ONE, (x, y) -> x.multiply(y));
    }

    public static long calculatePrice(BetNumbers numbers, LotterySix lotterySix) {
        return calculatePrice(numbers.getType(), numbers.getNumbers().size(), numbers.getBankersNumbers().size(), lotterySix.pricePerBet);
    }

    public static long calculatePrice(BetNumbersBuilder builder, LotterySix lotterySix) {
        return calculatePrice(builder.getType(), builder.size(), builder.getType().equals(BetNumbersType.BANKER) ? ((BetNumbersBuilder.BankerBuilder) builder).bankerSize() : 0, lotterySix.pricePerBet);
    }

    public static long calculatePrice(BetNumbersType type, int size, int bankerSize, long pricePerBet) {
        long permutations = 0;
        switch (type) {
            case SINGLE:
            case RANDOM: {
                permutations = 1;
                break;
            }
            case MULTIPLE: {
                permutations = factorial(size).divide((factorial(size - 6).multiply(SIX_FACTORIAL))).longValue();
                break;
            }
            case BANKER: {
                permutations = factorial(size).divide(factorial(size - (6 - bankerSize)).multiply(factorial(6 - bankerSize))).longValue();
                break;
            }
        }
        return permutations * pricePerBet;
    }

    public static String[] formatPlaceholders(OfflinePlayer player, String[] str, LotterySix lotterySix) {
        String[] array = new String[str.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = formatPlaceholders(player, str[i], lotterySix);
        }
        return array;
    }

    public static String formatPlaceholders(OfflinePlayer player, String str, LotterySix lotterySix) {
        str = str.replace("{PricePerBet}", StringUtils.formatComma(lotterySix.pricePerBet));
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str.replace("{" + prizeTier.name() + "Odds}", ODDS_FORMAT.format(calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier)));
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
                .replace("{GameNumber}", game.getGameNumber() + "")
                .replace("{PricePerBet}", StringUtils.formatComma(lotterySix.pricePerBet))
                .replace("{TotalBets}", StringUtils.formatComma(game.getTotalBets()))
                .replace("{PrizePool}", StringUtils.formatComma(game.estimatedPrizePool(lotterySix.taxPercentage)));
        if (str.contains("{BetPlayerNames}")) {
            str = str.replace("{BetPlayerNames}", chainPlayerBetNames(game.getBets()));
        }
        for (PrizeTier prizeTier : PrizeTier.values()) {
            str = str.replace("{" + prizeTier.name() + "Odds}", ODDS_FORMAT.format(calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier)));
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
                .replace("{GameNumber}", game.getGameNumber() + "")
                .replace("{PricePerBet}", StringUtils.formatComma(lotterySix.pricePerBet))
                .replace("{TotalBets}", StringUtils.formatComma(game.getTotalBets()))
                .replace("{TotalPrizes}", StringUtils.formatComma(game.getTotalPrizes()))
                .replace("{FirstToThirdPlaceWinnersCount}", game.getWinnings().stream().filter(each -> each.getTier().ordinal() < 3).count() + "")
                .replace("{FirstNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(0)))
                .replace("{SecondNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(1)))
                .replace("{ThirdNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(2)))
                .replace("{FourthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(3)))
                .replace("{FifthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(4)))
                .replace("{SixthNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumber(5)))
                .replace("{FirstNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(0)))
                .replace("{SecondNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(1)))
                .replace("{ThirdNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(2)))
                .replace("{FourthNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(3)))
                .replace("{FifthNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(4)))
                .replace("{SixthNumberOrdered}", ChatColorUtils.applyNumberColor(game.getDrawResult().getNumberOrdered(5)))
                .replace("{SpecialNumber}", ChatColorUtils.applyNumberColor(game.getDrawResult().getSpecialNumber()));
        if (str.contains("{BetPlayerNames}")) {
            str = str.replace("{BetPlayerNames}", chainPlayerBetNames(game.getBets()));
        }
        for (PrizeTier prizeTier : PrizeTier.values()) {
            String prizeTierName = prizeTier.name();
            str = str
                    .replace("{" + prizeTierName + "Odds}", calculateOddsOneOver(lotterySix.numberOfChoices, prizeTier) + "")
                    .replace("{" + prizeTierName + "Prize}", StringUtils.formatComma(game.getPrizeForTier(prizeTier)))
                    .replace("{" + prizeTierName + "PrizeCount}", BET_COUNT_FORMAT.format(game.getWinnerCountForTier(prizeTier)) + "");
            if (str.contains("{" + prizeTierName + "PlayerNames}")) {
                str = str.replace("{" + prizeTierName + "PlayerNames}", chainPlayerWinningsNames(game.getWinnings(prizeTier)));
            }
        }
        return ChatColorUtils.translateAlternateColorCodes('&', player == null ? str : PlaceholderAPI.setPlaceholders(player, str));
    }

    public static String chainPlayerBetNames(Collection<PlayerBets> bets) {
        return bets.stream().map(each -> each.getName()).distinct().collect(Collectors.joining(", "));
    }

    public static String chainPlayerWinningsNames(Collection<PlayerWinnings> winnings) {
        return winnings.stream().map(each -> each.getName()).distinct().collect(Collectors.joining(", "));
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
