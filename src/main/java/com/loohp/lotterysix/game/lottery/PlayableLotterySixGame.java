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

import com.google.gson.Gson;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.CarryOverMode;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.Pair;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeCalculationMode;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningCombination;
import com.loohp.lotterysix.game.objects.WinningNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.utils.MathUtils;
import com.loohp.lotterysix.utils.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlayableLotterySixGame implements IDedGame {

    public static PlayableLotterySixGame createNewGame(LotterySix instance, long scheduledDateTime, String specialName, Map<Integer, NumberStatistics> numberStatistics, long carryOverFund, long lowestTopPlacesPrize) {
        return new PlayableLotterySixGame(instance, UUID.randomUUID(), scheduledDateTime, specialName, numberStatistics, lowestTopPlacesPrize, carryOverFund, true);
    }

    private transient LotterySix instance;
    private transient ReentrantReadWriteLock betsLock;
    private transient AtomicBoolean dirty;

    private final UUID gameId;
    private volatile long scheduledDateTime;
    private volatile GameNumber gameNumber;
    private volatile String specialName;
    private final ConcurrentHashMap<Integer, NumberStatistics> numberStatistics;
    private final LinkedHashMap<UUID, PlayerBets> bets;
    private volatile long carryOverFund;
    private volatile long lowestTopPlacesPrize;
    private volatile boolean valid;
    private volatile long lastSaved;

    private PlayableLotterySixGame(LotterySix instance, UUID gameId, long scheduledDateTime, String specialName, Map<Integer, NumberStatistics> numberStatistics, long lowestTopPlacesPrize, long carryOverFund, boolean valid) {
        this.instance = instance;
        this.gameId = gameId;
        this.scheduledDateTime = scheduledDateTime;
        this.specialName = specialName;
        this.numberStatistics = new ConcurrentHashMap<>(numberStatistics);
        this.gameNumber = instance.dateToGameNumber(scheduledDateTime);
        this.bets = new LinkedHashMap<>();
        this.carryOverFund = carryOverFund;
        this.lowestTopPlacesPrize = lowestTopPlacesPrize;
        this.valid = valid;
        this.lastSaved = -1;
    }
    
    private synchronized ReentrantReadWriteLock getBetsReadWriteLock() {
        if (betsLock == null) {
            return betsLock = new ReentrantReadWriteLock(true);
        }
        return betsLock;
    }

    public synchronized AtomicBoolean getDirtyFlag() {
        if (dirty == null) {
            return dirty = new AtomicBoolean(true);
        }
        return dirty;
    }

    public String toJson(Gson gson, boolean updateSaveTime) {
        getBetsReadWriteLock().readLock().lock();
        try {
            lastSaved = System.currentTimeMillis();
            return gson.toJson(this);
        } finally {
            getBetsReadWriteLock().readLock().unlock();
        }
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

    @Override
    public GameNumber getGameNumber() {
        return gameNumber;
    }

    public boolean hasSpecialName() {
        return specialName != null;
    }

    public String getSpecialName() {
        return specialName;
    }

    public void setSpecialName(String specialName) {
        this.specialName = specialName;
        getDirtyFlag().set(true);
    }

    public void setScheduledDateTime(long scheduledDateTime) {
        if (instance == null || !instance.backendBungeecordMode) {
            this.scheduledDateTime = scheduledDateTime;
            this.gameNumber = instance.dateToGameNumber(scheduledDateTime);
            getDirtyFlag().set(true);
        }
    }

    public long getScheduledDateTime() {
        return scheduledDateTime;
    }

    public NumberStatistics getNumberStatistics(int number) {
        if (numberStatistics == null) {
            return NumberStatistics.NOT_EVER_DRAWN;
        }
        return numberStatistics.getOrDefault(number, NumberStatistics.NOT_EVER_DRAWN);
    }

    public Map<Integer, NumberStatistics> getNumberStatistics() {
        return numberStatistics == null ? Collections.emptyMap() : Collections.unmodifiableMap(numberStatistics);
    }

    public long getCarryOverFund(long rounding) {
        return MathUtils.followRound(rounding, carryOverFund);
    }

    public long getCarryOverFund() {
        return carryOverFund;
    }

    public void setCarryOverFund(long carryOverFund) {
        if (instance == null || !instance.backendBungeecordMode) {
            this.carryOverFund = carryOverFund;
            getDirtyFlag().set(true);
        }
    }

    public long getLowestTopPlacesPrize() {
        return lowestTopPlacesPrize;
    }

    public void setLowestTopPlacesPrize(long lowestTopPlacesPrize) {
        if (instance == null || !instance.backendBungeecordMode) {
            this.lowestTopPlacesPrize = lowestTopPlacesPrize;
            getDirtyFlag().set(true);
        }
    }

    public boolean isValid() {
        return valid;
    }

    public void cancelGame() {
        this.valid = false;
        if (instance != null && !instance.backendBungeecordMode) {
            getBetsReadWriteLock().readLock().lock();
            try {
                instance.refundBets(bets.values());
                for (PlayerBets bet : bets.values()) {
                    instance.getPlayerPreferenceManager().getLotteryPlayer(bet.getPlayer()).updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - bet.getBet());
                }
            } finally {
                getBetsReadWriteLock().readLock().unlock();
            }
        }
    }

    public synchronized void invalidateBetsIf(Predicate<PlayerBets> predicate) {
        List<PlayerBets> removedBets = new ArrayList<>();
        getBetsReadWriteLock().writeLock().lock();
        try {
            Iterator<PlayerBets> itr = bets.values().iterator();
            while (itr.hasNext()) {
                PlayerBets playerBet = itr.next();
                if (predicate.test(playerBet)) {
                    itr.remove();
                    removedBets.add(playerBet);
                }
            }
            getDirtyFlag().set(true);
        } finally {
            getBetsReadWriteLock().writeLock().unlock();
        }
        if (instance != null && !instance.backendBungeecordMode) {
            instance.requestSave(true);
            instance.refundBets(removedBets);
            for (PlayerBets bet : removedBets) {
                instance.getPlayerPreferenceManager().getLotteryPlayer(bet.getPlayer()).updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - bet.getBet());
            }
            instance.getPlayerBetsInvalidateListener().accept(removedBets);
        }
    }

    public List<PlayerBets> getBets() {
        getBetsReadWriteLock().readLock().lock();
        try {
            return new ArrayList<>(bets.values());
        } finally {
            getBetsReadWriteLock().readLock().unlock();
        }
    }

    public long getTotalBets() {
        getBetsReadWriteLock().readLock().lock();
        try {
            return bets.values().stream().mapToLong(each -> each.getBet()).sum();
        } finally {
            getBetsReadWriteLock().readLock().unlock();
        }
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
        getBetsReadWriteLock().writeLock().lock();
        try {
            for (PlayerBets bet : bets) {
                this.bets.put(bet.getBetId(), bet);
            }
        } finally {
            getBetsReadWriteLock().writeLock().unlock();
        }
        getDirtyFlag().set(true);
        if (instance != null) {
            instance.requestSave(true);
            if (!instance.backendBungeecordMode && instance.announcerBetPlacedAnnouncementEnabled) {
                for (UUID uuid : instance.getOnlinePlayersSupplier().get()) {
                    instance.getMessageSendingConsumer().accept(uuid, instance.announcerBetPlacedAnnouncementMessage
                            .replace("{Player}", name).replace("{Price}", StringUtils.formatComma(price)), this);
                }
            }
        }
        return betResult0(player, price, bets, AddBetResult.SUCCESS);
    }

    private AddBetResult betResult0(UUID player, long price, Collection<PlayerBets> bets, AddBetResult result) {
        if (instance != null && !instance.backendBungeecordMode) {
            instance.getPlayerBetListener().accept(player, result, price, bets);
            for (PlayerBets playerBet : bets) {
                instance.getConsoleMessageConsumer().accept(
                        playerBet.getName() + " (" + playerBet.getPlayer() + ") placed a bet worth $" + playerBet.getBet() + " (" + playerBet.getType() + ") with type " +
                        playerBet.getChosenNumbers().getType() + " [" + playerBet.getChosenNumbers().toString() + "] to game " + gameNumber + " (" + gameId + ")");
            }
        }
        return result;
    }

    public boolean hasBets() {
        return !bets.isEmpty();
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        getBetsReadWriteLock().readLock().lock();
        try {
            return bets.values().stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList());
        } finally {
            getBetsReadWriteLock().readLock().unlock();
        }
    }

    public long estimatedPrizePool(long maxTopPlacesPrize, double taxPercentage, long rounding) {
        PrizeCalculationMode prizeCalculationMode = instance == null ? PrizeCalculationMode.DEFAULT : instance.prizeCalculationMode;
        if (prizeCalculationMode.equals(PrizeCalculationMode.HKJC)) {
            return MathUtils.followRound(rounding, Math.min(maxTopPlacesPrize, carryOverFund + lowestTopPlacesPrize));
        } else {
            CarryOverMode carryOverMode = instance == null ? CarryOverMode.DEFAULT : instance.carryOverMode;
            switch (carryOverMode) {
                case DEFAULT: {
                    return MathUtils.followRound(rounding, Math.min(maxTopPlacesPrize, Math.max(lowestTopPlacesPrize, (lowestTopPlacesPrize / 2) + carryOverFund + (long) Math.floor(getTotalBets() * (1.0 - taxPercentage)))));
                }
                case ONLY_TICKET_SALES: {
                    return MathUtils.followRound(rounding, Math.min(maxTopPlacesPrize, Math.max(lowestTopPlacesPrize, lowestTopPlacesPrize + carryOverFund + (long) Math.floor(getTotalBets() * (1.0 - taxPercentage)))));
                }
                default: {
                    throw new RuntimeException("Unknown carry over mode: " + carryOverMode);
                }
            }
        }
    }

    public CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, long maxTopPlacesPrize, double taxPercentage) {
        SecureRandom random = new SecureRandom();
        int[] num = random.ints(1, maxNumber + 1).distinct().limit(7).toArray();
        WinningNumbers winningNumbers = new WinningNumbers(num[0], num[1], num[2], num[3], num[4], num[5], num[6]);
        return runLottery(maxNumber, pricePerBet, maxTopPlacesPrize, taxPercentage, winningNumbers);
    }

    public synchronized CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, long maxTopPlacesPrize, double taxPercentage, WinningNumbers winningNumbers) {
        if (instance != null && instance.backendBungeecordMode) {
            throw new IllegalStateException("lottery cannot be run on backend server while on bungeecord mode");
        }

        Map<Integer, NumberStatistics> newNumberStats = new HashMap<>();
        for (int i = 1; i <= maxNumber; i++) {
            newNumberStats.put(i, getNumberStatistics(i).increment(winningNumbers.containsAnywhere(i)));
        }

        Map<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> tiers = new EnumMap<>(PrizeTier.class);
        for (PrizeTier prizeTier : PrizeTier.values()) {
            tiers.put(prizeTier, new ArrayList<>());
        }

        Set<UUID> participants = new HashSet<>();
        getBetsReadWriteLock().readLock().lock();
        try {
            for (PlayerBets playerBets : bets.values()) {
                participants.add(playerBets.getPlayer());
                List<Pair<PrizeTier, WinningCombination>> prizeTiersPairs = winningNumbers.checkWinning(playerBets.getChosenNumbers());
                for (Pair<PrizeTier, WinningCombination> prizeTiersPair : prizeTiersPairs) {
                    tiers.get(prizeTiersPair.getFirst()).add(Pair.of(playerBets, prizeTiersPair.getSecond()));
                }
            }
        } finally {
            getBetsReadWriteLock().readLock().unlock();
        }
        if (instance != null) {
            new Thread(() -> {
                for (UUID player : participants) {
                    instance.getPlayerPreferenceManager().getLotteryPlayer(player).updateStats(PlayerStatsKey.TOTAL_ROUNDS_PARTICIPATED, long.class, i -> i + 1);
                }
            }).start();
        }

        PrizeCalculationMode prizeCalculationMode = instance == null ? PrizeCalculationMode.DEFAULT : instance.prizeCalculationMode;
        if (prizeCalculationMode.equals(PrizeCalculationMode.HKJC)) {
            return hkjcCalculation(pricePerBet, maxTopPlacesPrize, taxPercentage, tiers, winningNumbers, newNumberStats);
        }

        CarryOverMode carryOverMode = instance == null ? CarryOverMode.DEFAULT : instance.carryOverMode;
        long totalPrize;
        switch (carryOverMode) {
            case DEFAULT: {
                totalPrize = (lowestTopPlacesPrize / 2) + carryOverFund + (long) Math.floor(getTotalBets() * (1.0 - taxPercentage));
                break;
            }
            case ONLY_TICKET_SALES: {
                totalPrize = lowestTopPlacesPrize + carryOverFund + (long) Math.floor(getTotalBets() * (1.0 - taxPercentage));
                break;
            }
            default: {
                throw new RuntimeException("Unknown carry over mode: " + carryOverMode);
            }
        }

        Map<PrizeTier, Long> prizeForTier = new EnumMap<>(PrizeTier.class);

        List<PlayerWinnings> winnings = new ArrayList<>();
        long totalPrizes = 0;
        long totalFourthToSeventh = 0;
        if (!tiers.get(PrizeTier.SEVENTH).isEmpty()) {
            prizeForTier.put(PrizeTier.SEVENTH, pricePerBet * PrizeTier.SEVENTH.getFixedPrizeMultiplier());
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.SEVENTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.SEVENTH, playerBets, pair.getSecond(), (pricePerBet * PrizeTier.SEVENTH.getFixedPrizeMultiplier()) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.SIXTH).isEmpty()) {
            prizeForTier.put(PrizeTier.SIXTH, pricePerBet * PrizeTier.SIXTH.getFixedPrizeMultiplier());
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.SIXTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.SIXTH, playerBets, pair.getSecond(), (pricePerBet * PrizeTier.SIXTH.getFixedPrizeMultiplier()) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.FIFTH).isEmpty()) {
            prizeForTier.put(PrizeTier.FIFTH, pricePerBet * PrizeTier.FIFTH.getFixedPrizeMultiplier());
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.FIFTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.FIFTH, playerBets, pair.getSecond(), (pricePerBet * PrizeTier.FIFTH.getFixedPrizeMultiplier()) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }
        if (!tiers.get(PrizeTier.FOURTH).isEmpty()) {
            prizeForTier.put(PrizeTier.FOURTH, pricePerBet * PrizeTier.FOURTH.getFixedPrizeMultiplier());
        }
        for (Pair<PlayerBets, WinningCombination> pair : tiers.get(PrizeTier.FOURTH)) {
            PlayerBets playerBets = pair.getFirst();
            PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), PrizeTier.FOURTH, playerBets, pair.getSecond(), (pricePerBet * PrizeTier.FOURTH.getFixedPrizeMultiplier()) / playerBets.getType().getDivisor());
            totalPrizes += playerWinnings.getWinnings();
            totalFourthToSeventh += playerWinnings.getWinnings();
            winnings.add(playerWinnings);
        }

        long totalRemaining = Math.min(maxTopPlacesPrize, Math.max(lowestTopPlacesPrize, totalPrize - totalFourthToSeventh));

        boolean thirdPlaceEmpty = tiers.get(PrizeTier.THIRD).isEmpty();
        boolean secondPlaceEmpty = tiers.get(PrizeTier.SECOND).isEmpty();
        boolean firstPlaceEmpty = tiers.get(PrizeTier.FIRST).isEmpty();

        double thirdTierWeightedWinners = Math.max(1, tiers.get(PrizeTier.THIRD).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum());
        double secondTierWeightedWinners = Math.max(1, tiers.get(PrizeTier.SECOND).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()) * 2.6;
        double totalWeightedWinners = thirdTierWeightedWinners + secondTierWeightedWinners;

        double carryOverPortion = 0;
        double thirdPortion = (thirdTierWeightedWinners / totalWeightedWinners) * 0.55;
        if (thirdPortion > 0.4) {
            carryOverPortion += (thirdPortion - 0.4);
            thirdPortion = 0.4;
        }

        double secondPortion = (secondTierWeightedWinners / totalWeightedWinners) * 0.55;
        if (secondPortion > 0.4) {
            carryOverPortion += (secondPortion - 0.4);
            secondPortion = 0.4;
        } else if (secondPortion < 0.15) {
            carryOverPortion -= (0.15 - secondPortion);
            secondPortion = 0.15;
        }

        long carryOverNext = (long) Math.floor(totalRemaining * carryOverPortion);

        long thirdTierPrizeTotal = (long) Math.floor(totalRemaining * thirdPortion);
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

        long secondTierPrizeTotal = (long) Math.floor(totalRemaining * secondPortion);
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

        PrizeTier[] prizeTiers = PrizeTier.values();
        long prize = firstTierPrizeTotal;
        for (int i = 0; i < prizeTiers.length - 1; i++) {
            PrizeTier prizeTier = prizeTiers[i];
            PrizeTier lowerTier = prizeTiers[i + 1];
            Long tierPrize = prizeForTier.get(prizeTier);
            if (tierPrize != null) {
                prize = tierPrize;
            }
            long lowerPrize = prizeForTier.getOrDefault(lowerTier, 0L);
            if (lowerPrize * prizeTier.getMinimumMultiplierFromLast() <= prize) {
                continue;
            }
            long newLowerPrize = prize / prizeTier.getMinimumMultiplierFromLast();
            prizeForTier.put(lowerTier, newLowerPrize);
            long prizeMoneyRemoved = 0;
            for (int u = 0; u < winnings.size(); u++) {
                PlayerWinnings playerWinnings = winnings.get(u);
                if (playerWinnings.getTier().equals(lowerTier)) {
                    long currentPrize = playerWinnings.getWinnings();
                    long newPrize = newLowerPrize / playerWinnings.getWinningBet(bets).getType().getDivisor();
                    prizeMoneyRemoved += (currentPrize - newPrize);
                    winnings.set(u, playerWinnings.winnings(newPrize));
                }
            }
            if (lowerTier.isTopTier()) {
                carryOverNext += prizeMoneyRemoved;
            }
            totalPrizes -= prizeMoneyRemoved;
        }

        prizeForTier.replaceAll((k, v) -> MathUtils.followRoundDown(pricePerBet, v));
        for (int u = 0; u < winnings.size(); u++) {
            PlayerWinnings playerWinnings = winnings.get(u);
            long currentPrize = playerWinnings.getWinnings();
            long rounded = prizeForTier.get(playerWinnings.getTier()) / playerWinnings.getWinningBet(bets).getType().getDivisor();
            if (currentPrize > rounded) {
                winnings.set(u, playerWinnings.winnings(rounded));
                carryOverNext += (currentPrize - rounded);
            }
        }

        this.valid = false;

        if (instance != null) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (instance.carryOverMode) {
                case ONLY_TICKET_SALES: {
                    long left = carryOverFund + (long) Math.floor(getTotalBets() * (1.0 - taxPercentage));
                    left = Math.round((double) left * ((double) carryOverNext / (double) totalRemaining));
                    carryOverNext = Math.max(0, Math.min(maxTopPlacesPrize - lowestTopPlacesPrize, left));
                    break;
                }
            }
        }

        return new CompletedLotterySixGame(gameId, scheduledDateTime, gameNumber, specialName, winningNumbers, newNumberStats, pricePerBet, prizeForTier, winnings, bets, totalPrizes, carryOverNext);
    }

    private synchronized CompletedLotterySixGame hkjcCalculation(long pricePerBet, long maxTopPlacesPrize, double taxPercentage, Map<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> tiers, WinningNumbers winningNumbers, Map<Integer, NumberStatistics> newNumberStats) {
        PrizeTier[] prizeTiers = PrizeTier.values();
        long totalFund = (long) Math.floor(getTotalBets() * (1.0 - taxPercentage));
        long totalFundForFourthToSeventh = (long) Math.floor((double) totalFund * 0.6);
        long totalFourthToSeventhFundRequired = 0;

        Map<PrizeTier, Long> portionsFourthToSeventh = new EnumMap<>(PrizeTier.class);
        Map<PrizeTier, Double> unitsFourthToSeventh = new EnumMap<>(PrizeTier.class);
        for (Map.Entry<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> entry : tiers.entrySet()) {
            PrizeTier prizeTier = entry.getKey();
            if (!prizeTier.isTopTier() && !entry.getValue().isEmpty()) {
                double unit = entry.getValue().stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum();
                unitsFourthToSeventh.put(prizeTier, unit);
                long portion = Math.round(BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(pricePerBet)).multiply(BigDecimal.valueOf(prizeTier.getFixedPrizeMultiplier())).doubleValue());
                totalFourthToSeventhFundRequired += portion;
                portionsFourthToSeventh.put(prizeTier, portion);
            }
        }

        boolean carryOverRequiredForFourthToSeventh = totalFourthToSeventhFundRequired > totalFundForFourthToSeventh;

        long total = totalFourthToSeventhFundRequired;
        long bigTotalFund = carryOverRequiredForFourthToSeventh ? totalFundForFourthToSeventh + carryOverFund : totalFundForFourthToSeventh;

        Map<PrizeTier, Long> maxPrizeInTier = new EnumMap<>(PrizeTier.class);
        for (Map.Entry<PrizeTier, Long> entry : portionsFourthToSeventh.entrySet()) {
            PrizeTier prizeTier = entry.getKey();
            if (!prizeTier.isTopTier()) {
                Double unit = unitsFourthToSeventh.get(prizeTier);
                if (unit == null) {
                    maxPrizeInTier.put(prizeTier, 0L);
                } else {
                    maxPrizeInTier.put(prizeTier, (long) Math.floor(bigTotalFund * ((double) entry.getValue() / (double) total) / unit));
                }
            }
        }

        Map<PrizeTier, Long> prizeForTier = new EnumMap<>(PrizeTier.class);
        List<PlayerWinnings> winnings = new ArrayList<>();
        long totalPrizes = 0;
        long totalFourthToSeventh = 0;
        for (int i = prizeTiers.length - 1; i >= 3; i--) {
            PrizeTier prizeTier = prizeTiers[i];
            if (!tiers.get(prizeTier).isEmpty()) {
                long prizePerBetForTier = Math.max(0, Math.min(pricePerBet * prizeTier.getFixedPrizeMultiplier(), maxPrizeInTier.get(prizeTier)));
                prizeForTier.put(prizeTier, prizePerBetForTier);
                for (Pair<PlayerBets, WinningCombination> pair : tiers.get(prizeTier)) {
                    PlayerBets playerBets = pair.getFirst();
                    PlayerWinnings playerWinnings = new PlayerWinnings(playerBets.getName(), playerBets.getPlayer(), prizeTier, playerBets, pair.getSecond(), prizePerBetForTier / playerBets.getType().getDivisor());
                    totalPrizes += playerWinnings.getWinnings();
                    totalFourthToSeventh += playerWinnings.getWinnings();
                    winnings.add(playerWinnings);
                }
            }
        }

        long carryOverRemaining = carryOverRequiredForFourthToSeventh ? carryOverFund - (long) Math.floor(totalFourthToSeventh - totalFundForFourthToSeventh) : carryOverFund;
        long totalRemaining = Math.max(0, totalFund - Math.min(totalFourthToSeventh, totalFundForFourthToSeventh));
        
        long lowestThirdTierPrize = 0;
        for (int i = 3; i < prizeTiers.length; i++) {
            PrizeTier prizeTier = prizeTiers[i];
            Long prize = prizeForTier.get(prizeTier);
            if (prize != null) {
                lowestThirdTierPrize = prize;
                for (int u = i - 1; u >= 2; u--) {
                    lowestThirdTierPrize *= prizeTiers[u].getMinimumMultiplierFromLast();
                }
                break;
            }
        }
        long lowestSecondTierPrize = lowestThirdTierPrize * PrizeTier.SECOND.getMinimumMultiplierFromLast();

        boolean thirdPlaceEmpty = tiers.get(PrizeTier.THIRD).isEmpty();
        boolean secondPlaceEmpty = tiers.get(PrizeTier.SECOND).isEmpty();
        boolean firstPlaceEmpty = tiers.get(PrizeTier.FIRST).isEmpty();

        double thirdTierWeightedWinners = Math.max(1, tiers.get(PrizeTier.THIRD).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum());
        double secondTierWeightedWinners = Math.max(1, tiers.get(PrizeTier.SECOND).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()) * 2.6;
        double totalWeightedWinners = thirdTierWeightedWinners + secondTierWeightedWinners;

        double carryOverPortion = 0;
        double thirdPortion = (thirdTierWeightedWinners / totalWeightedWinners) * 0.55;
        if (thirdPortion > 0.4) {
            carryOverPortion += (thirdPortion - 0.4);
            thirdPortion = 0.4;
        }

        double secondPortion = (secondTierWeightedWinners / totalWeightedWinners) * 0.55;
        if (secondPortion > 0.4) {
            carryOverPortion += (secondPortion - 0.4);
            secondPortion = 0.4;
        } else if (secondPortion < 0.15) {
            carryOverPortion -= (0.15 - secondPortion);
            secondPortion = 0.15;
        }

        long carryOverNext = (long) Math.floor(totalRemaining * carryOverPortion);
        
        long thirdTierPrizeTotal = (long) Math.floor(totalRemaining * thirdPortion);
        if (!thirdPlaceEmpty) {
            long thirdTierPrize = (long) Math.floor(thirdTierPrizeTotal / Math.max(1.0, tiers.get(PrizeTier.THIRD).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()));
            if (thirdTierPrize < lowestThirdTierPrize) {
                carryOverRemaining -= (lowestThirdTierPrize - thirdTierPrize);
                thirdTierPrize = lowestThirdTierPrize;
                if (carryOverRemaining < 0) {
                    thirdTierPrize += carryOverRemaining;
                    carryOverRemaining = 0;
                }
            }
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
        
        long secondTierPrizeTotal = (long) Math.floor(totalRemaining * secondPortion);
        if (!secondPlaceEmpty) {
            if (thirdPlaceEmpty) {
                if (firstPlaceEmpty) {
                    secondTierPrizeTotal += thirdTierPrizeTotal;
                } else {
                    secondTierPrizeTotal += thirdTierPrizeTotal * 0.25;
                }
            }
            long secondTierPrize = (long) Math.floor(secondTierPrizeTotal / Math.max(1.0, tiers.get(PrizeTier.SECOND).stream().mapToDouble(each -> each.getFirst().getType().getUnit()).sum()));
            if (secondTierPrize < lowestSecondTierPrize) {
                carryOverRemaining -= (lowestSecondTierPrize - secondTierPrize);
                secondTierPrize = lowestSecondTierPrize;
            }
            if (carryOverRemaining < 0) {
                secondTierPrize += carryOverRemaining;
                carryOverRemaining = 0;
            }
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
        
        long firstTierPrizeTotal = Math.min(maxTopPlacesPrize, carryOverRemaining + Math.max(lowestTopPlacesPrize, (long) Math.floor(totalRemaining * 0.45)));
        long firstTierCarryOver = 0;
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
                firstTierCarryOver += (firstTierPrize - firstTierTotal);
            }
        } else {
            firstTierCarryOver += firstTierPrizeTotal;
        }

        CarryOverMode carryOverMode = instance == null ? CarryOverMode.DEFAULT : instance.carryOverMode;
        switch (carryOverMode) {
            case DEFAULT: {
                carryOverNext += firstTierCarryOver;
                break;
            }
            case ONLY_TICKET_SALES: {
                carryOverNext += Math.max(0, firstTierCarryOver - lowestTopPlacesPrize);
                break;
            }
            default: {
                throw new RuntimeException("Unknown carry over mode: " + carryOverMode);
            }
        }

        long prize = firstTierPrizeTotal;
        for (int i = 0; i < prizeTiers.length - 1; i++) {
            PrizeTier prizeTier = prizeTiers[i];
            PrizeTier lowerTier = prizeTiers[i + 1];
            Long tierPrize = prizeForTier.get(prizeTier);
            if (tierPrize != null) {
                prize = tierPrize;
            }
            long lowerPrize = prizeForTier.getOrDefault(lowerTier, 0L);
            if (lowerPrize * prizeTier.getMinimumMultiplierFromLast() <= prize) {
                continue;
            }
            long newLowerPrize = prize / prizeTier.getMinimumMultiplierFromLast();
            prizeForTier.put(lowerTier, newLowerPrize);
            long prizeMoneyRemoved = 0;
            for (int u = 0; u < winnings.size(); u++) {
                PlayerWinnings playerWinnings = winnings.get(u);
                if (playerWinnings.getTier().equals(lowerTier)) {
                    long currentPrize = playerWinnings.getWinnings();
                    long newPrize = newLowerPrize / playerWinnings.getWinningBet(bets).getType().getDivisor();
                    prizeMoneyRemoved += (currentPrize - newPrize);
                    winnings.set(u, playerWinnings.winnings(newPrize));
                }
            }
            carryOverNext += prizeMoneyRemoved;
            totalPrizes -= prizeMoneyRemoved;
        }

        prizeForTier.replaceAll((k, v) -> MathUtils.followRoundDown(pricePerBet, v));
        for (int u = 0; u < winnings.size(); u++) {
            PlayerWinnings playerWinnings = winnings.get(u);
            long currentPrize = playerWinnings.getWinnings();
            long rounded = prizeForTier.get(playerWinnings.getTier()) / playerWinnings.getWinningBet(bets).getType().getDivisor();
            if (currentPrize > rounded) {
                winnings.set(u, playerWinnings.winnings(rounded));
                carryOverNext += (currentPrize - rounded);
            }
        }

        this.valid = false;

        return new CompletedLotterySixGame(gameId, scheduledDateTime, gameNumber, specialName, winningNumbers, newNumberStats, pricePerBet, prizeForTier, winnings, bets, totalPrizes, carryOverNext);
    }

}
