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
import com.loohp.lotterysix.game.objects.PrizeTier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class Charts {

    private static Optional<CompletedLotterySixGame> getLatestGame() {
        List<CompletedLotterySixGame> games = LotterySixPlugin.getInstance().getCompletedGames();
        return games.isEmpty() ? Optional.empty() : Optional.of(games.get(0));
    }

    public static void setup(Metrics metrics) {

        metrics.addCustomChart(new Metrics.SingleLineChart("first_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.FIRST)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("second_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.SECOND)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("third_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.THIRD)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("fourth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.FOURTH)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("fifth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.FIFTH)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("sixth_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.SIXTH)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.SingleLineChart("seventh_tier_winners", new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return getLatestGame().map(e -> e.getWinnerCountForTier(PrizeTier.SEVENTH)).orElse(0);
            }
        }));

        metrics.addCustomChart(new Metrics.AdvancedPie("most_popular_number", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() throws Exception {
                Map<String, Integer> valueMap = new HashMap<>();
                if (getLatestGame().isPresent()) {
                    CompletedLotterySixGame game = getLatestGame().get();
                    int[] numbers = game.getBets().stream().flatMapToInt(each -> each.getChosenNumbers().stream()).toArray();
                    for (int i = 0; i < LotterySixPlugin.getInstance().numberOfChoices; i++) {
                        int finalI = i;
                        valueMap.put(i + "", (int) Arrays.stream(numbers).filter(u -> u == finalI).count());
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
                    for (int i : game.getDrawResult()) {
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
                int counts = (int) LotterySixPlugin.getInstance().getCompletedGames().stream().filter(each -> each.getDatetime() >= lastCall).count();
                lastCall = System.currentTimeMillis();
                return counts;
            }
        }));

    }

}
