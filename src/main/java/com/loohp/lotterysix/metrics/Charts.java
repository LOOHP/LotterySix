/*
 * This file is part of InteractiveChat.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
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

package com.loohp.lotterysix.metrics;

import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PrizeTier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.concurrent.Callable;

public class Charts {

    private static Optional<CompletedLotterySixGame> getLatestGame() {
        List<CompletedLotterySixGame> games = LotterySixPlugin.getInstance().getCompletedGames();
        return games.isEmpty() ? Optional.empty() : Optional.of(games.get(0));
    }

    private static Optional<PlayableLotterySixGame> getCurrentGame() {
        return Optional.ofNullable(LotterySixPlugin.getInstance().getCurrentGame());
    }

    public static void setup(Metrics metrics) {

        metrics.addCustomChart(new Metrics.SingleLineChart("first_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.FIRST))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("second_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.SECOND))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("third_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.THIRD))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("fourth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.FOURTH))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("fifth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.FIFTH))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("sixth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.SIXTH))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("seventh_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> (int) Math.ceil(e.getWinnerCountForTier(PrizeTier.SEVENTH))).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.AdvancedPie("most_popular_number", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> valueMap = new HashMap<>();
                if (getLatestGame().isPresent()) {
                    CompletedLotterySixGame game = getLatestGame().get();
                    for (PrimitiveIterator.OfInt itr = game.getBets().stream().flatMapToInt(each -> each.getChosenNumbers().getAllNumbers().stream().mapToInt(i -> i)).iterator(); itr.hasNext();) {
                        valueMap.merge(itr.nextInt() + "", 1, (a, b) -> a + b);
                    }
                }
                return valueMap;
            }
        }));

        metrics.addCustomChart(new Metrics.AdvancedPie("most_popular_winning_number", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> valueMap = new HashMap<>();
                if (getLatestGame().isPresent()) {
                    CompletedLotterySixGame game = getLatestGame().get();
                    for (int i : game.getDrawResult().getNumbers()) {
                        valueMap.put(i + "", 1);
                    }
                    valueMap.put(game.getDrawResult().getSpecialNumber() + "", 1);
                }
                return valueMap;
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("lotterysix_played", new Callable<Integer>() {
            private long lastCall = System.currentTimeMillis();
            @Override
            public Integer call() throws Exception {
                int counts = (int) LotterySixPlugin.getInstance().getCompletedGames().indexStream().filter(each -> each.getDatetime() >= lastCall).count();
                lastCall = System.currentTimeMillis();
                return counts;
            }
        }));

        metrics.addCustomChart(new Metrics.AdvancedPie("most_popular_bet_entry_method", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> valueMap = new HashMap<>();
                if (getLatestGame().isPresent()) {
                    CompletedLotterySixGame game = getLatestGame().get();
                    for (PlayerBets playerBets : game.getBets()) {
                        valueMap.merge(playerBets.getChosenNumbers().getType().name(), 1, (a, b) -> a + b);
                    }
                }
                return valueMap;
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("on-going_lottery_six_games", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getCurrentGame().map(each -> 1).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SimplePie("number_of_choices", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return LotterySixPlugin.getInstance().numberOfChoices + "";
            }
        }));

        metrics.addCustomChart(new Metrics.SimplePie("price_per_unit_bet", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return LotterySixPlugin.getInstance().pricePerBet + "";
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("global_total_current_turnover", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return (int) Math.min(getCurrentGame().map(each -> each.getTotalBets()).orElse(0L), Integer.MAX_VALUE);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("global_total_previous_winnings", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return (int) Math.min(getLatestGame().map(each -> each.getWinnings().stream().mapToLong(w -> w.getWinnings()).sum()).orElse(0L), Integer.MAX_VALUE);
            }
        }));

    }

}
