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
import com.loohp.lotterysix.game.LotteryRegistry;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.CarryOverMode;
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
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.game.player.LotteryPlayerManager;
import com.loohp.lotterysix.utils.MathUtils;
import com.loohp.lotterysix.utils.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PlayableLotterySixGame implements ILotterySixGame {

    private static final Map<Object, Map<String, Object>> LOCKS_AND_FLAGS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final String DIRTY_FLAG = "dirty";

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <T, V> V getSharedLockOrFlag(T owner, String key, Supplier<V> constructor) {
        return (V) LOCKS_AND_FLAGS.computeIfAbsent(owner, k -> new ConcurrentHashMap<>()).computeIfAbsent(key, k -> constructor.get());
    }

    public static PlayableLotterySixGame createNewGame(LotterySix instance, long scheduledDateTime, String specialName, Map<Integer, NumberStatistics> numberStatistics, long carryOverFund, long lowestTopPlacesPrize, List<PlayerBets> placedBets) {
        GameNumber gameNumber = instance == null ? new GameNumber(Year.now(), 1) : instance.dateToGameNumber(scheduledDateTime);
        return new PlayableLotterySixGame(instance, UUID.randomUUID(), scheduledDateTime, gameNumber, specialName, numberStatistics, placedBets, lowestTopPlacesPrize, carryOverFund, true);
    }

    public static PlayableLotterySixGame createPresetGame(LotterySix instance, UUID gameId, GameNumber gameNumber, long scheduledDateTime, String specialName, Map<Integer, NumberStatistics> numberStatistics, long carryOverFund, long lowestTopPlacesPrize, List<PlayerBets> placedBets) {
        return new PlayableLotterySixGame(instance, gameId, scheduledDateTime, gameNumber, specialName, numberStatistics, placedBets, lowestTopPlacesPrize, carryOverFund, true);
    }

    private transient LotterySix instance;

    private final UUID gameId;
    private volatile long scheduledDateTime;
    private volatile GameNumber gameNumber;
    private volatile String specialName;
    private final ConcurrentHashMap<Integer, NumberStatistics> numberStatistics;
    private final ConcurrentHashMap<UUID, PlayerBets> bets;
    private volatile long carryOverFund;
    private volatile long lowestTopPlacesPrize;
    private volatile boolean valid;
    private volatile long lastSaved;

    private PlayableLotterySixGame(LotterySix instance, UUID gameId, long scheduledDateTime, GameNumber gameNumber, String specialName, Map<Integer, NumberStatistics> numberStatistics, List<PlayerBets> placedBets, long lowestTopPlacesPrize, long carryOverFund, boolean valid) {
        this.instance = instance;
        this.gameId = gameId;
        this.scheduledDateTime = scheduledDateTime;
        this.specialName = specialName;
        this.numberStatistics = new ConcurrentHashMap<>(numberStatistics);
        this.gameNumber = gameNumber;
        this.bets = new ConcurrentHashMap<>();
        for (PlayerBets bet : placedBets) {
            this.bets.put(bet.getBetId(), bet);
        }
        this.carryOverFund = carryOverFund;
        this.lowestTopPlacesPrize = lowestTopPlacesPrize;
        this.valid = valid;
        this.lastSaved = -1;
    }

    public AtomicBoolean getDirtyFlag() {
        return getSharedLockOrFlag(this, DIRTY_FLAG, () -> new AtomicBoolean(true));
    }

    public String toJson(Gson gson, boolean updateSaveTime) {
        if (updateSaveTime) {
            lastSaved = System.currentTimeMillis();
        }
        return gson.toJson(this);
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
        return specialName != null && !specialName.isEmpty();
    }

    public String getSpecialName() {
        return specialName;
    }

    public void setSpecialName(String specialName) {
        if (!Objects.equals(this.specialName, specialName)) {
            this.specialName = specialName;
            getDirtyFlag().set(true);
        }
    }

    @Override
    public String getDataFileName() {
        return "current.json";
    }

    public void setDatetime(long scheduledDateTime, GameNumber gameNumber) {
        if (this.scheduledDateTime != scheduledDateTime || !this.gameNumber.equals(gameNumber)) {
            this.scheduledDateTime = scheduledDateTime;
            this.gameNumber = gameNumber;
            getDirtyFlag().set(true);
        }
    }

    @Override
    public long getDatetime() {
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

    public void setNumberStatistics(Map<Integer, NumberStatistics> numberStatistics) {
        if (!this.numberStatistics.equals(numberStatistics)) {
            this.numberStatistics.clear();
            this.numberStatistics.putAll(numberStatistics);
            getDirtyFlag().set(true);
        }
    }

    public long getCarryOverFund(long rounding) {
        return MathUtils.followRound(rounding, carryOverFund);
    }

    public long getCarryOverFund() {
        return carryOverFund;
    }

    public void setCarryOverFund(long carryOverFund) {
        if (this.carryOverFund != carryOverFund) {
            this.carryOverFund = carryOverFund;
            getDirtyFlag().set(true);
        }
    }

    public long getLowestTopPlacesPrize() {
        return lowestTopPlacesPrize;
    }

    public void setLowestTopPlacesPrize(long lowestTopPlacesPrize) {
        if (this.lowestTopPlacesPrize != lowestTopPlacesPrize) {
            this.lowestTopPlacesPrize = lowestTopPlacesPrize;
            getDirtyFlag().set(true);
        }
    }

    public boolean isValid() {
        return valid;
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void markInvalid() {
        if (this.valid) {
            this.valid = false;
            getDirtyFlag().set(true);
        }
    }

    public void cancelGame() {
        this.valid = false;
        if (instance != null && !instance.backendBungeecordMode) {
            Map<UUID, List<PlayerBets>> multipleDrawBets = new HashMap<>();
            Map<LotteryPlayer, Boolean> affected = new HashMap<>();
            LotteryPlayerManager lotteryPlayerManager = instance.getLotteryPlayerManager();
            for (PlayerBets bet : bets.values()) {
                List<PlayerBets> playerBets = multipleDrawBets.computeIfAbsent(bet.getPlayer(), k -> new ArrayList<>());
                if (bet.isMultipleDraw()) {
                    playerBets.add(bet);
                } else {
                    LotteryPlayer lotteryPlayer = lotteryPlayerManager.getLotteryPlayer(bet.getPlayer());
                    lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - bet.getBet(), false);
                    lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + bet.getBet(), false);
                    lotteryPlayer.updateStats(PlayerStatsKey.NOTIFY_BALANCE_CHANGE, long.class, i -> i + bet.getBet(), false);
                    affected.put(lotteryPlayer, true);
                }
            }
            for (Map.Entry<UUID, List<PlayerBets>> entry : multipleDrawBets.entrySet()) {
                LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(entry.getKey());
                lotteryPlayer.setMultipleDrawPlayerBets(entry.getValue(), false);
                affected.putIfAbsent(lotteryPlayer, false);
            }
            for (Map.Entry<LotteryPlayer, Boolean> entry : affected.entrySet()) {
                LotteryPlayer lotteryPlayer = entry.getKey();
                lotteryPlayer.save();
                if (entry.getValue()) {
                    instance.notifyBalanceChangeConsumer(lotteryPlayer.getPlayer());
                }
            }
        }
    }

    public synchronized void invalidateBetsIf(Predicate<PlayerBets> predicate, boolean clearMultipleDraw) {
        List<PlayerBets> removedBets = new ArrayList<>();
        Iterator<PlayerBets> itr = bets.values().iterator();
        while (itr.hasNext()) {
            PlayerBets playerBet = itr.next();
            if (predicate.test(playerBet)) {
                itr.remove();
                removedBets.add(playerBet);
            }
        }
        getDirtyFlag().set(true);
        if (instance != null && !instance.backendBungeecordMode) {
            instance.requestSave(true);
            LotteryPlayerManager lotteryPlayerManager = instance.getLotteryPlayerManager();
            Set<LotteryPlayer> affected = new HashSet<>();
            for (PlayerBets bet : removedBets) {
                long total = clearMultipleDraw ? bet.getBet() * bet.getDrawsRemaining() : bet.getBet();
                LotteryPlayer lotteryPlayer = lotteryPlayerManager.getLotteryPlayer(bet.getPlayer());
                lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i - total, false);
                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + total, false);
                lotteryPlayer.updateStats(PlayerStatsKey.NOTIFY_BALANCE_CHANGE, long.class, i -> i + total, false);
                affected.add(lotteryPlayer);
            }
            for (LotteryPlayer lotteryPlayer : affected) {
                if (clearMultipleDraw) {
                    lotteryPlayer.setMultipleDrawPlayerBets(Collections.emptyList(), false);
                }
                lotteryPlayer.save();
                instance.notifyBalanceChangeConsumer(lotteryPlayer.getPlayer());
            }
            instance.getPlayerBetsInvalidateListener().accept(removedBets);
        }
    }

    public boolean hasBets() {
        return !bets.isEmpty();
    }

    public List<PlayerBets> getBets() {
        return bets.values().stream().sorted().collect(Collectors.toList());
    }

    public Set<UUID> getBetIds() {
        return Collections.unmodifiableSet(bets.keySet());
    }

    public PlayerBets getBet(UUID betId) {
        return bets.get(betId);
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        return bets.values().stream().filter(each -> each.getPlayer().equals(player)).sorted().collect(Collectors.toList());
    }

    public long getTotalBets() {
        return bets.values().stream().mapToLong(each -> each.getBet()).sum();
    }

    public AddBetResult addBet(String name, UUID player, long bet, BetUnitType unitType, BetNumbers chosenNumbers, int multipleDraw) {
        return addBet(new PlayerBets(name, player, System.currentTimeMillis(), System.nanoTime(), bet, unitType, chosenNumbers, multipleDraw));
    }

    public AddBetResult addBet(String name, UUID player, long betPerUnit, BetUnitType unitType, Collection<BetNumbers> chosenNumbers, int multipleDraw) {
        if (chosenNumbers.isEmpty()) {
            throw new IllegalArgumentException("chosenNumbers cannot be empty");
        }
        long now = System.currentTimeMillis();
        long nano = System.nanoTime();
        return addBet(name, player, chosenNumbers.stream().map(each -> new PlayerBets(name, player, now, nano, betPerUnit, unitType, each, multipleDraw)).collect(Collectors.toList()));
    }

    public AddBetResult addBet(PlayerBets bet) {
        return addBet(bet.getName(), bet.getPlayer(), Collections.singleton(bet));
    }

    private synchronized AddBetResult addBet(String name, UUID player, Collection<PlayerBets> bets) {
        long price = bets.stream().mapToLong(each -> each.getBet() * each.getMultipleDraw()).sum();
        if (instance != null && instance.isGameLocked()) {
            return betResult0(player, price, bets, AddBetResult.GAME_LOCKED);
        }
        if (instance != null && !instance.backendBungeecordMode) {
            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player);
            long totalBets = getPlayerBets(player).stream().mapToLong(each -> each.getBet()).sum() + price;
            long playerLimit = lotteryPlayer.getPreference(PlayerPreferenceKey.BET_LIMIT_PER_ROUND, long.class);
            if (playerLimit >= 0 && playerLimit < totalBets) {
                return betResult0(player, price, bets, AddBetResult.LIMIT_SELF);
            }
            long permissionLimit = instance.getPlayerBetLimit(player);
            if (permissionLimit >= 0 && permissionLimit < totalBets) {
                return betResult0(player, price, bets, AddBetResult.LIMIT_PERMISSION);
            }
            for (PlayerBets bet : bets) {
                BetNumbers numbers = bet.getChosenNumbers();
                if (numbers.isCombination() && MathUtils.combinationsCount(numbers.getNumbers().size(), numbers.getBankersNumbers().size()) > instance.maximumChancePerSelection) {
                    return betResult0(player, price, bets, AddBetResult.LIMIT_CHANCE_PER_SELECTION);
                }
            }
            if (price > lotteryPlayer.getStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class)) {
                return betResult0(player, price, bets, AddBetResult.NOT_ENOUGH_MONEY);
            }
            if (System.currentTimeMillis() < lotteryPlayer.getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class)) {
                return betResult0(player, price, bets, AddBetResult.ACCOUNT_SUSPENDED);
            }
            lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i - price, false);
            lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_BETS_PLACED, long.class, i -> i + price, false);
            lotteryPlayer.save();
        }
        for (PlayerBets bet : bets) {
            this.bets.put(bet.getBetId(), bet);
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
            if (result.isSuccess()) {
                for (PlayerBets playerBet : bets) {
                    instance.getConsoleMessageConsumer().accept(
                            playerBet.getName() + " (" + playerBet.getPlayer() + ") placed a bet worth $" + playerBet.getBet() * playerBet.getMultipleDraw() + " (" + playerBet.getType() + ") with type " +
                                    playerBet.getChosenNumbers().getType() + " [" + playerBet.getChosenNumbers().toString() + "] to game " + gameNumber + " (" + gameId + ") for " +
                                    playerBet.getMultipleDraw() + " games");
                }
            }
        }
        return result;
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
        PrizeCalculationMode prizeCalculationMode = instance == null ? PrizeCalculationMode.DEFAULT : instance.prizeCalculationMode;
        CarryOverMode carryOverMode = instance == null ? CarryOverMode.DEFAULT : instance.carryOverMode;
        return runLottery(maxNumber, pricePerBet, maxTopPlacesPrize, taxPercentage, prizeCalculationMode, carryOverMode);
    }

    public CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, long maxTopPlacesPrize, double taxPercentage, PrizeCalculationMode prizeCalculationMode, CarryOverMode carryOverMode) {
        SecureRandom random = new SecureRandom();
        List<Integer> num = random.ints(1, maxNumber + 1).distinct().limit(LotteryRegistry.NUMBERS_PER_BET + 1).boxed().collect(Collectors.toList());
        WinningNumbers winningNumbers = new WinningNumbers(num.subList(0, LotteryRegistry.NUMBERS_PER_BET), num.get(num.size() - 1));
        return runLottery(maxNumber, pricePerBet, maxTopPlacesPrize, taxPercentage, winningNumbers, prizeCalculationMode, carryOverMode);
    }

    public CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, long maxTopPlacesPrize, double taxPercentage, WinningNumbers winningNumbers) {
        PrizeCalculationMode prizeCalculationMode = instance == null ? PrizeCalculationMode.DEFAULT : instance.prizeCalculationMode;
        CarryOverMode carryOverMode = instance == null ? CarryOverMode.DEFAULT : instance.carryOverMode;
        return runLottery(maxNumber, pricePerBet, maxTopPlacesPrize, taxPercentage, winningNumbers, prizeCalculationMode, carryOverMode);
    }

    public synchronized CompletedLotterySixGame runLottery(int maxNumber, long pricePerBet, long maxTopPlacesPrize, double taxPercentage, WinningNumbers winningNumbers, PrizeCalculationMode prizeCalculationMode, CarryOverMode carryOverMode) {
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
        for (PlayerBets playerBets : bets.values()) {
            participants.add(playerBets.getPlayer());
            for (Iterator<Pair<PrizeTier, WinningCombination>> itr = winningNumbers.checkWinning(playerBets.getChosenNumbers()).iterator(); itr.hasNext();) {
                Pair<PrizeTier, WinningCombination> prizeTiersPair = itr.next();
                tiers.get(prizeTiersPair.getFirst()).add(Pair.of(playerBets, prizeTiersPair.getSecond()));
            }
        }
        if (instance != null) {
            new Thread(() -> {
                for (UUID player : participants) {
                    instance.getLotteryPlayerManager().getLotteryPlayer(player).updateStats(PlayerStatsKey.TOTAL_ROUNDS_PARTICIPATED, long.class, i -> i + 1);
                }
            }).start();
        }

        if (prizeCalculationMode.equals(PrizeCalculationMode.HKJC)) {
            return hkjcCalculation(pricePerBet, maxTopPlacesPrize, taxPercentage, tiers, winningNumbers, newNumberStats);
        }

        long totalBetsFund = getTotalBets();
        long totalPrize;
        switch (carryOverMode) {
            case DEFAULT: {
                totalPrize = (lowestTopPlacesPrize / 2) + carryOverFund + (long) Math.floor(totalBetsFund * (1.0 - taxPercentage));
                break;
            }
            case ONLY_TICKET_SALES: {
                totalPrize = lowestTopPlacesPrize + carryOverFund + (long) Math.floor(totalBetsFund * (1.0 - taxPercentage));
                break;
            }
            default: {
                throw new RuntimeException("Unknown carry over mode: " + carryOverMode);
            }
        }

        long lotteriesFunds = totalBetsFund - (long) Math.floor(totalBetsFund * (1.0 - taxPercentage));

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
            if (lowerTier.isVariableTier()) {
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

        if (instance == null || instance.retainLowestPrizeForTier) {
            prizeForTier.replaceAll((k, v) -> {
                if (v <= 0) {
                    return v;
                }
                return Math.max(v, instance.pricePerBet * k.getFixedPrizeMultiplier());
            });
            for (int u = 0; u < winnings.size(); u++) {
                PlayerWinnings playerWinnings = winnings.get(u);
                long currentPrize = playerWinnings.getWinnings();
                long rounded = prizeForTier.get(playerWinnings.getTier()) / playerWinnings.getWinningBet(bets).getType().getDivisor();
                if (currentPrize < rounded) {
                    winnings.set(u, playerWinnings.winnings(rounded));
                    carryOverNext -= (rounded - currentPrize);
                }
            }
            carryOverNext = Math.max(0, carryOverNext);
        }

        winnings.sort(Comparator.comparing((PlayerWinnings playerWinnings) -> playerWinnings.getTier()).thenComparing((PlayerWinnings playerWinnings) -> playerWinnings.getWinningBet(bets)));
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

        return new CompletedLotterySixGame(gameId, scheduledDateTime, gameNumber, specialName, winningNumbers, newNumberStats, pricePerBet, prizeForTier, winnings, bets, totalPrizes, carryOverNext, lotteriesFunds);
    }

    private synchronized CompletedLotterySixGame hkjcCalculation(long pricePerBet, long maxTopPlacesPrize, double taxPercentage, Map<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> tiers, WinningNumbers winningNumbers, Map<Integer, NumberStatistics> newNumberStats) {
        PrizeTier[] prizeTiers = PrizeTier.values();
        long totalBetsFund = getTotalBets();
        long totalFund = (long) Math.floor(totalBetsFund * (1.0 - taxPercentage));
        long totalFundForFourthToSeventh = (long) Math.floor((double) totalFund * 0.6);
        long totalFourthToSeventhFundRequired = 0;
        long lotteriesFunds = totalBetsFund - totalFund;

        Map<PrizeTier, Long> portionsFourthToSeventh = new EnumMap<>(PrizeTier.class);
        Map<PrizeTier, Double> unitsFourthToSeventh = new EnumMap<>(PrizeTier.class);
        for (Map.Entry<PrizeTier, List<Pair<PlayerBets, WinningCombination>>> entry : tiers.entrySet()) {
            PrizeTier prizeTier = entry.getKey();
            if (!prizeTier.isVariableTier() && !entry.getValue().isEmpty()) {
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
            if (!prizeTier.isVariableTier()) {
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
        long difference = Math.max(0, lowestTopPlacesPrize - (long) Math.floor(totalRemaining * 0.45));

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
                carryOverNext += Math.max(0, firstTierCarryOver - difference);
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

        if (instance == null || instance.retainLowestPrizeForTier) {
            prizeForTier.replaceAll((k, v) -> {
                if (v <= 0) {
                    return v;
                }
                if (k.equals(PrizeTier.FIRST)) {
                    return Math.max(v, instance.lowestTopPlacesPrize);
                }
                return Math.max(v, instance.pricePerBet * k.getFixedPrizeMultiplier());
            });
            for (int u = 0; u < winnings.size(); u++) {
                PlayerWinnings playerWinnings = winnings.get(u);
                long currentPrize = playerWinnings.getWinnings();
                long rounded = prizeForTier.get(playerWinnings.getTier()) / playerWinnings.getWinningBet(bets).getType().getDivisor();
                if (currentPrize < rounded) {
                    winnings.set(u, playerWinnings.winnings(rounded));
                    carryOverNext -= (rounded - currentPrize);
                }
            }
            carryOverNext = Math.max(0, carryOverNext);
        }

        winnings.sort(Comparator.comparing((PlayerWinnings playerWinnings) -> playerWinnings.getTier()).thenComparing((PlayerWinnings playerWinnings) -> playerWinnings.getWinningBet(bets)));
        this.valid = false;

        return new CompletedLotterySixGame(gameId, scheduledDateTime, gameNumber, specialName, winningNumbers, newNumberStats, pricePerBet, prizeForTier, winnings, bets, totalPrizes, carryOverNext, lotteriesFunds);
    }

}
