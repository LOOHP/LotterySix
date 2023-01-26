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

package com.loohp.lotterysix.game.lottery;

import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningNumbers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayableLotterySixGame {

    public static PlayableLotterySixGame createNewGame(LotterySix instance, long scheduledDateTime, long carryOverFund, long lowestTopPlacesPrize) {
        return new PlayableLotterySixGame(instance, UUID.randomUUID(), scheduledDateTime, new ArrayList<>(), carryOverFund, lowestTopPlacesPrize, true);
    }

    private transient LotterySix instance;

    private final UUID gameId;
    private long scheduledDateTime;
    private final List<PlayerBets> bets;
    private final long carryOverFund;
    private long lowestTopPlacesPrize;
    private volatile boolean valid;

    private PlayableLotterySixGame(LotterySix instance, UUID gameId, long scheduledDateTime, List<PlayerBets> bets, long lowestTopPlacesPrize, long carryOverFund, boolean valid) {
        this.instance = instance;
        this.gameId = gameId;
        this.scheduledDateTime = scheduledDateTime;
        this.bets = bets;
        this.carryOverFund = carryOverFund;
        this.lowestTopPlacesPrize = lowestTopPlacesPrize;
        this.valid = valid;
    }

    public LotterySix getInstance() {
        return instance;
    }

    public void setInstance(LotterySix instance) {
        this.instance = instance;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setScheduledDateTime(long scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public long getScheduledDateTime() {
        return scheduledDateTime;
    }

    public long getCarryOverFund() {
        return carryOverFund;
    }

    public long getLowestTopPlacesPrize() {
        return lowestTopPlacesPrize;
    }

    public void setLowestTopPlacesPrize(long lowestTopPlacesPrize) {
        this.lowestTopPlacesPrize = lowestTopPlacesPrize;
    }

    public boolean isValid() {
        return valid;
    }

    public void cancelGame() {
        this.valid = false;
        if (instance != null) {
            instance.refundBets(bets);
            for (PlayerBets bet : bets) {
                instance.getPlayerPreferenceManager().getLotteryPlayer(bet.getPlayer()).updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - bet.getBet());
            }
        }
    }

    public List<PlayerBets> getBets() {
        return Collections.unmodifiableList(bets);
    }

    public long getTotalBets() {
        return bets.stream().mapToLong(each -> each.getBet()).sum();
    }

    public boolean addBet(UUID player, long bet, BetNumbers chosenNumbers) {
        return addBet(new PlayerBets(player, bet, chosenNumbers));
    }

    public synchronized boolean addBet(PlayerBets bet) {
        if (instance.isGameLocked()) {
            return false;
        }
        if (instance != null) {
            instance.getPlayerBetListener().accept(bet.getPlayer(), bet.getChosenNumbers());
            if (!instance.takeMoney(bet)) {
                return false;
            }
            instance.getPlayerPreferenceManager().getLotteryPlayer(bet.getPlayer()).updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i + bet.getBet());
        }
        bets.add(bet);
        if (instance != null) {
            instance.saveData(true);
        }
        return true;
    }

    public boolean hasBets() {
        return !bets.isEmpty();
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        return Collections.unmodifiableList(bets.stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public long estimatedPrizePool(double taxPercentage) {
        return Math.max(lowestTopPlacesPrize, carryOverFund + (long) Math.floor(bets.stream().mapToLong(each -> each.getBet()).count() * (1.0 - taxPercentage)));
    }

    public synchronized CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, double taxPercentage) {
        long now = System.currentTimeMillis();
        SecureRandom random = new SecureRandom();
        int[] num = random.ints(1, maxNumber + 1).distinct().limit(7).toArray();
        WinningNumbers winningNumbers = new WinningNumbers(num[0], num[1], num[2], num[3], num[4], num[5], num[6]);
        Map<PrizeTier, List<PlayerBets>> tiers = new EnumMap<>(PrizeTier.class);
        for (PrizeTier prizeTier : PrizeTier.values()) {
            tiers.put(prizeTier, new ArrayList<>());
        }
        long totalPrize = carryOverFund + (long) Math.floor(bets.stream().mapToLong(each -> each.getBet()).count() * (1.0 - taxPercentage));
        Set<UUID> participants = new HashSet<>();
        for (PlayerBets playerBets : bets) {
            participants.add(playerBets.getPlayer());
            PrizeTier prizeTier = winningNumbers.checkWinning(playerBets.getChosenNumbers());
            if (prizeTier != null) {
                tiers.get(prizeTier).add(playerBets);
            }
        }
        if (instance != null) {
            new Thread(() -> {
                for (UUID player : participants) {
                    instance.getPlayerPreferenceManager().getLotteryPlayer(player).updateStats(PlayerStatsKey.TOTAL_ROUNDS_PARTICIPATED, long.class, i -> i + 1);
                }
            }).start();
        }
        List<PlayerWinnings> winnings = new ArrayList<>();
        long totalPrizes = 0;
        long totalFourthToSeventh = 0;
        for (PlayerBets playerBets : tiers.get(PrizeTier.SEVENTH)) {
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.SEVENTH, playerBets, pricePerBet * 4);
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        for (PlayerBets playerBets : tiers.get(PrizeTier.SIXTH)) {
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.SIXTH, playerBets, pricePerBet * 32);
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        for (PlayerBets playerBets : tiers.get(PrizeTier.FIFTH)) {
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.FIFTH, playerBets, pricePerBet * 64);
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        for (PlayerBets playerBets : tiers.get(PrizeTier.FOURTH)) {
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.FOURTH, playerBets, pricePerBet * 960);
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        long totalRemaining = Math.max(lowestTopPlacesPrize, totalPrize - totalFourthToSeventh);

        long thirdTierPrizeTotal = (long) Math.floor(totalRemaining * 0.4);
        if (!tiers.get(PrizeTier.THIRD).isEmpty()) {
            long thirdTierPrize = (long) Math.floor(thirdTierPrizeTotal / (double) tiers.get(PrizeTier.THIRD).size());
            for (PlayerBets playerBets : tiers.get(PrizeTier.THIRD)) {
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.THIRD, playerBets, thirdTierPrize);
                totalPrizes += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
        }

        long carryOverNext = 0;

        long secondTierPrizeTotal = (long) Math.floor(totalRemaining * 0.15);
        if (!tiers.get(PrizeTier.SECOND).isEmpty()) {
            long secondTierPrize = (long) Math.floor(secondTierPrizeTotal / (double) tiers.get(PrizeTier.SECOND).size());
            for (PlayerBets playerBets : tiers.get(PrizeTier.SECOND)) {
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.SECOND, playerBets, secondTierPrize);
                totalPrizes += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
        } else {
            carryOverNext += secondTierPrizeTotal;
        }

        long firstTierPrizeTotal = (long) Math.floor(totalRemaining * 0.45);

        if (!tiers.get(PrizeTier.FIRST).isEmpty()) {
            long firstTierPrize = (long) Math.floor(firstTierPrizeTotal / (double) tiers.get(PrizeTier.FIRST).size());
            for (PlayerBets playerBets : tiers.get(PrizeTier.FIRST)) {
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getPlayer(), PrizeTier.FIRST, playerBets, firstTierPrize);
                totalPrizes += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
        } else {
            carryOverNext += secondTierPrizeTotal;
        }

        this.valid = false;

        if (instance != null) {
            instance.givePrizes(winnings);
            new Thread(() -> {
                for (PlayerWinnings winning : winnings) {
                    LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(winning.getPlayer());
                    lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_WINNINGS, long.class, i -> i + winning.getWinnings());
                    lotteryPlayer.updateStats(PlayerStatsKey.HIGHEST_WON_TIER, PrizeTier.class, t -> t == null || winning.getTier().ordinal() < t.ordinal(), winning.getTier());
                }
            }).start();
        }

        return new CompletedLotterySixGame(gameId, now, winningNumbers, winnings, bets, totalPrizes, (long) Math.floor(carryOverNext * (1 - taxPercentage)));
    }

}
