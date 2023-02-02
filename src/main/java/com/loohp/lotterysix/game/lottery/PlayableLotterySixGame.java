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
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.Pair;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningCombination;
import com.loohp.lotterysix.game.objects.WinningNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayableLotterySixGame implements IDedGame {

    public static PlayableLotterySixGame createNewGame(LotterySix instance, long scheduledDateTime, long carryOverFund, long lowestTopPlacesPrize) {
        return new PlayableLotterySixGame(instance, UUID.randomUUID(), scheduledDateTime, lowestTopPlacesPrize, carryOverFund, true);
    }

    private transient LotterySix instance;

    private final UUID gameId;
    private volatile long scheduledDateTime;
    private final Map<UUID, PlayerBets> bets;
    private final long carryOverFund;
    private volatile long lowestTopPlacesPrize;
    private volatile boolean valid;

    private PlayableLotterySixGame(LotterySix instance, UUID gameId, long scheduledDateTime, long lowestTopPlacesPrize, long carryOverFund, boolean valid) {
        this.instance = instance;
        this.gameId = gameId;
        this.scheduledDateTime = scheduledDateTime;
        this.bets = new LinkedHashMap<>();
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

    @Override
    public UUID getGameId() {
        return gameId;
    }

    public void setScheduledDateTime(long scheduledDateTime) {
        if (instance == null || !instance.backendBungeecordMode) {
            this.scheduledDateTime = scheduledDateTime;
        }
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
        if (instance == null || !instance.backendBungeecordMode) {
            this.lowestTopPlacesPrize = lowestTopPlacesPrize;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void cancelGame() {
        this.valid = false;
        if (instance != null && !instance.backendBungeecordMode) {
            instance.refundBets(bets.values());
            for (PlayerBets bet : bets.values()) {
                instance.getPlayerPreferenceManager().getLotteryPlayer(bet.getPlayer()).updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - bet.getBet());
            }
        }
    }

    public Collection<PlayerBets> getBets() {
        return Collections.unmodifiableCollection(bets.values());
    }

    public long getTotalBets() {
        return bets.values().stream().mapToLong(each -> each.getBet()).sum();
    }

    public AddBetResult addBet(String name, UUID player, long bet, BetUnitType unitType, BetNumbers chosenNumbers) {
        return addBet(new PlayerBets(name, player, System.currentTimeMillis(), bet, unitType, chosenNumbers));
    }

    public AddBetResult addBet(String name, UUID player, long betPerUnit, BetUnitType unitType, Collection<BetNumbers> chosenNumbers) {
        if (chosenNumbers.isEmpty()) {
            throw new IllegalArgumentException("chosenNumbers cannot be empty");
        }
        long now = System.currentTimeMillis();
        return addBet(name, player, chosenNumbers.stream().map(each -> new PlayerBets(name, player, now, betPerUnit, unitType, each)).collect(Collectors.toList()));
    }

    public AddBetResult addBet(PlayerBets bet) {
        return addBet(bet.getName(), bet.getPlayer(), Collections.singleton(bet));
    }

    private synchronized AddBetResult addBet(String name, UUID player, Collection<PlayerBets> bets) {
        long price = bets.stream().mapToLong(each -> each.getBet()).sum();
        if (instance.isGameLocked()) {
            return betResult0(player, price, bets, AddBetResult.GAME_LOCKED);
        }
        if (instance != null && !instance.backendBungeecordMode) {
            LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(player);
            long totalBets = getPlayerBets(player).stream().mapToLong(each -> each.getBet()).sum() + price;
            long playerLimit = lotteryPlayer.getPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND, long.class);
            if (playerLimit >= 0 && playerLimit < totalBets) {
                return betResult0(player, price, bets, AddBetResult.LIMIT_SELF);
            }
            long permissionLimit = instance.getPlayerBetLimit(player);
            if (permissionLimit >= 0 && permissionLimit < totalBets) {
                return betResult0(player, price, bets, AddBetResult.LIMIT_PERMISSION);
            }
            if (!instance.takeMoney(player, price)) {
                return betResult0(player, price, bets, AddBetResult.NOT_ENOUGH_MONEY);
            }
            lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i + price);
        }
        for (PlayerBets bet : bets) {
            this.bets.put(bet.getBetId(), bet);
        }
        if (instance != null) {
            instance.saveData(true);
            if (!instance.backendBungeecordMode && instance.announcerBetPlacedAnnouncementEnabled) {
                for (UUID uuid : instance.getOnlinePlayersSupplier().get()) {
                    instance.getMessageSendingConsumer().accept(uuid, instance.announcerBetPlacedAnnouncementMessage
                            .replace("{Player}", name).replace("{Price}", price + ""), this);
                }
            }
        }
        return betResult0(player, price, bets, AddBetResult.SUCCESS);
    }

    private AddBetResult betResult0(UUID player, long price, Collection<PlayerBets> bets, AddBetResult result) {
        if (instance != null && !instance.backendBungeecordMode) {
            instance.getPlayerBetListener().accept(player, result, price, bets);
        }
        return result;
    }

    public boolean hasBets() {
        return !bets.isEmpty();
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        return Collections.unmodifiableList(bets.values().stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public long estimatedPrizePool(double taxPercentage) {
        return Math.max(lowestTopPlacesPrize, carryOverFund + (long) Math.floor(bets.values().stream().mapToLong(each -> each.getBet()).sum() * (1.0 - taxPercentage)));
    }

    public synchronized CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, double taxPercentage) {
        if (instance != null && instance.backendBungeecordMode) {
            throw new IllegalStateException("lottery cannot be run on backend server while on bungeecord mode");
        }
        SecureRandom random = new SecureRandom();
        int[] num = random.ints(1, maxNumber + 1).distinct().limit(7).toArray();
        WinningNumbers winningNumbers = new WinningNumbers(num[0], num[1], num[2], num[3], num[4], num[5], num[6]);
        Map<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> tiers = new EnumMap<>(PrizeTier.class);
        for (PrizeTier prizeTier : PrizeTier.values()) {
            tiers.put(prizeTier, new ArrayList<>());
        }
        long totalPrize = carryOverFund + (long) Math.floor(bets.values().stream().mapToLong(each -> each.getBet()).sum() * (1.0 - taxPercentage));
        Set<UUID> participants = new HashSet<>();
        for (PlayerBets playerBets : bets.values()) {
            participants.add(playerBets.getPlayer());
            List<Pair<PrizeTier, WinningCombination>> prizeTiersPairs = winningNumbers.checkWinning(playerBets.getChosenNumbers());
            for (Pair<PrizeTier, WinningCombination> prizeTiersPair : prizeTiersPairs) {
                tiers.get(prizeTiersPair.getFirst()).add(Pair.of(playerBets, prizeTiersPair.getSecond()));
            }
        }
        if (instance != null) {
            new Thread(() -> {
                for (UUID player : participants) {
                    instance.getPlayerPreferenceManager().getLotteryPlayer(player).updateStats(PlayerStatsKey.TOTAL_ROUNDS_PARTICIPATED, long.class, i -> i + 1);
                }
            }).start();
        }
        Map<PrizeTier, Long> prizeForTier = new EnumMap<>(PrizeTier.class);

        List<PlayerWinnings> winnings = new ArrayList<>();
        long totalPrizes = 0;
        long totalFourthToSeventh = 0;
        if (!tiers.get(PrizeTier.SEVENTH).isEmpty()) {
            prizeForTier.put(PrizeTier.SEVENTH, pricePerBet * 4);
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.SEVENTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.SEVENTH, playerBets, pair.getSecond(), (pricePerBet * 4) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.SIXTH).isEmpty()) {
            prizeForTier.put(PrizeTier.SIXTH, pricePerBet * 32);
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.SIXTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.SIXTH, playerBets, pair.getSecond(), (pricePerBet * 32) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.FIFTH).isEmpty()) {
            prizeForTier.put(PrizeTier.FIFTH, pricePerBet * 64);
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.FIFTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.FIFTH, playerBets, pair.getSecond(), (pricePerBet * 64) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.FOURTH).isEmpty()) {
            prizeForTier.put(PrizeTier.FOURTH, pricePerBet * 960);
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.FOURTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.FOURTH, playerBets, pair.getSecond(), (pricePerBet * 960) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }

        long totalRemaining = Math.max(lowestTopPlacesPrize, totalPrize - totalFourthToSeventh);

        boolean thirdPlaceEmpty = tiers.get(PrizeTier.THIRD).isEmpty();
        boolean secondPlaceEmpty = tiers.get(PrizeTier.SECOND).isEmpty();
        boolean firstPlaceEmpty = tiers.get(PrizeTier.FIRST).isEmpty();

        long carryOverNext = 0;

        long thirdTierPrizeTotal = (long) Math.floor(totalRemaining * 0.4);
        if (!thirdPlaceEmpty) {
            long thirdTierPrize = (long) Math.floor(thirdTierPrizeTotal / Math.max(1.0, tiers.get(PrizeTier.THIRD).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()));
            prizeForTier.put(PrizeTier.THIRD, thirdTierPrize);
            long thirdTierTotal = 0;
            for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.THIRD)) {
                PlayerBets playerBets = pair.getFirst();
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.THIRD, playerBets, pair.getSecond(), thirdTierPrize / playerBets.getType().getDivisor());
                totalPrizes += playerWinnings.getWinnings();
                thirdTierTotal += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
            if (thirdTierPrize > thirdTierTotal) {
                carryOverNext += (thirdTierPrize - thirdTierTotal);
            }
        } else if (firstPlaceEmpty && secondPlaceEmpty) {
            carryOverNext += thirdTierPrizeTotal;
        }

        long secondTierPrizeTotal = (long) Math.floor(totalRemaining * 0.15);
        if (!secondPlaceEmpty) {
            if (thirdPlaceEmpty) {
                if (firstPlaceEmpty) {
                    secondTierPrizeTotal += thirdTierPrizeTotal;
                } else {
                    secondTierPrizeTotal += thirdTierPrizeTotal * 0.25;
                }
            }
            long secondTierPrize = (long) Math.floor(secondTierPrizeTotal / Math.max(1.0, tiers.get(PrizeTier.SECOND).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()));
            prizeForTier.put(PrizeTier.SECOND, secondTierPrize);
            long secondTierTotal = 0;
            for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.SECOND)) {
                PlayerBets playerBets = pair.getFirst();
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.SECOND, playerBets, pair.getSecond(), secondTierPrize / playerBets.getType().getDivisor());
                totalPrizes += playerWinnings.getWinnings();
                secondTierTotal += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
            if (secondTierPrize > secondTierTotal) {
                carryOverNext += (secondTierPrize - secondTierTotal);
            }
        } else {
            carryOverNext += secondTierPrizeTotal;
        }

        long firstTierPrizeTotal = (long) Math.floor(totalRemaining * 0.45);
        if (!firstPlaceEmpty) {
            if (thirdPlaceEmpty) {
                if (secondPlaceEmpty) {
                    firstTierPrizeTotal += thirdTierPrizeTotal;
                } else {
                    firstTierPrizeTotal += thirdTierPrizeTotal * 0.75;
                }
            }
            long firstTierPrize = (long) Math.floor(firstTierPrizeTotal / Math.max(1.0, tiers.get(PrizeTier.FIRST).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()));
            prizeForTier.put(PrizeTier.FIRST, firstTierPrize);
            long firstTierTotal = 0;
            for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.FIRST)) {
                PlayerBets playerBets = pair.getFirst();
                PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.FIRST, playerBets, pair.getSecond(), firstTierPrize / playerBets.getType().getDivisor());
                totalPrizes += playerWinnings.getWinnings();
                firstTierTotal += playerWinnings.getWinnings();
                winnings.add(playerWinnings);
            }
            if (firstTierPrize > firstTierTotal) {
                carryOverNext += (firstTierPrize - firstTierTotal);
            }
        } else {
            carryOverNext += firstTierPrizeTotal;
        }

        this.valid = false;

        return new CompletedLotterySixGame(gameId, scheduledDateTime, winningNumbers, pricePerBet, prizeForTier, winnings, bets, totalPrizes, (long) Math.floor(carryOverNext * (1 - taxPercentage)));
    }

}
