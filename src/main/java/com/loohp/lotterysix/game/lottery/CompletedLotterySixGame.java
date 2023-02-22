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
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.PrizeTier;
import com.loohp.lotterysix.game.objects.WinningNumbers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompletedLotterySixGame implements IDedGame {

    public static final Comparator<PlayerWinnings> PLAYER_WINNINGS_COMPARATOR = Comparator.comparing(playerWinnings -> playerWinnings.getTier());

    private transient Map<PrizeTier, List<PlayerWinnings>> winnersByTierCache;
    private transient Map<PrizeTier, Double> winnerCountForTierCache;

    private final UUID gameId;
    private final long datetime;
    private final GameNumber gameNumber;
    private final String specialName;
    private final WinningNumbers drawResult;
    private final Map<Integer, NumberStatistics> numberStatistics;
    private final long pricePerBet;
    private final Map<PrizeTier, Long> prizeForTier;
    private final List<PlayerWinnings> winners;
    private final Map<UUID, PlayerBets> bets;
    private final long totalPrizes;
    private final long remainingFunds;

    public CompletedLotterySixGame(UUID gameId, long datetime, GameNumber gameNumber, String specialName, WinningNumbers drawResult, Map<Integer, NumberStatistics> numberStatistics, long pricePerBet, Map<PrizeTier, Long> prizeForTier, List<PlayerWinnings> winners, Map<UUID, PlayerBets> bets, long totalPrizes, long remainingFunds) {
        this.gameId = gameId;
        this.datetime = datetime;
        this.gameNumber = gameNumber;
        this.specialName = specialName;
        this.drawResult = drawResult;
        this.numberStatistics = numberStatistics;
        this.pricePerBet = pricePerBet;
        this.prizeForTier = prizeForTier;
        this.winners = Collections.unmodifiableList(winners);
        this.bets = Collections.unmodifiableMap(bets);
        this.totalPrizes = totalPrizes;
        this.remainingFunds = remainingFunds;
    }

    private synchronized void cacheWinnersByTier() {
        if (winnersByTierCache == null) {
            winnersByTierCache = new HashMap<>();
            for (PlayerWinnings winnings : winners) {
                winnersByTierCache.computeIfAbsent(winnings.getTier(), k -> new ArrayList<>()).add(winnings);
            }
        }
    }

    private synchronized void cacheWinnerCountForTier() {
        if (winnersByTierCache == null) {
            winnerCountForTierCache = new HashMap<>();
            for (PrizeTier prizeTier : PrizeTier.values()) {
                winnerCountForTierCache.put(prizeTier, getWinnings(prizeTier).stream().mapToDouble(each -> each.getWinningBet(this).getType().getUnit()).sum());
            }
        }
    }

    @Override
    public UUID getGameId() {
        return gameId;
    }

    @Override
    public GameNumber getGameNumber() {
        return gameNumber;
    }

    public String getDataFileName() {
        return datetime + ".json";
    }

    public CompletedLotterySixGameIndex toGameIndex() {
        return new CompletedLotterySixGameIndex(gameId, datetime, gameNumber, drawResult, specialName);
    }

    public long getDatetime() {
        return datetime;
    }

    public boolean hasSpecialName() {
        return specialName != null && !specialName.isEmpty();
    }

    public String getSpecialName() {
        return specialName;
    }

    public WinningNumbers getDrawResult() {
        return drawResult;
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

    public long getPricePerBet(BetUnitType type) {
        return pricePerBet / type.getDivisor();
    }

    public List<PlayerWinnings> getWinnings() {
        return winners;
    }

    public List<PlayerWinnings> getWinnings(PrizeTier prizeTier) {
        cacheWinnersByTier();
        return Collections.unmodifiableList(winnersByTierCache.getOrDefault(prizeTier, Collections.emptyList()));
    }

    public Collection<PlayerBets> getBets() {
        return bets.values();
    }

    public PlayerBets getBet(UUID betId) {
        return bets.get(betId);
    }

    public long getTotalBets() {
        return bets.values().stream().mapToLong(each -> each.getBet()).sum();
    }

    public List<PlayerWinnings> getPlayerWinnings(UUID player) {
        return Collections.unmodifiableList(winners.stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public List<PlayerWinnings> getSortedPlayerWinnings(UUID player) {
        return Collections.unmodifiableList(winners.stream().filter(each -> each.getPlayer().equals(player)).sorted(PLAYER_WINNINGS_COMPARATOR).collect(Collectors.toList()));
    }

    public List<PlayerBets> getPlayerBets(UUID player) {
        return Collections.unmodifiableList(bets.values().stream().filter(each -> each.getPlayer().equals(player)).collect(Collectors.toList()));
    }

    public long getTotalPrizes() {
        return totalPrizes;
    }

    public long getPrizeForTier(PrizeTier prizeTier) {
        return prizeForTier.getOrDefault(prizeTier, 0L);
    }

    public double getWinnerCountForTier(PrizeTier prizeTier) {
        cacheWinnerCountForTier();
        return winnerCountForTierCache.get(prizeTier);
    }

    public long getRemainingFunds() {
        return remainingFunds;
    }

    public void givePrizesAndUpdateStats(LotterySix instance) {
        instance.givePrizes(winners);
        new Thread(() -> {
            Map<UUID, Long> transactions = new HashMap<>();
            Map<UUID, PrizeTier> prizeTiers = new HashMap<>();
            for (PlayerWinnings winning : winners) {
                transactions.merge(winning.getPlayer(), winning.getWinnings(), (a, b) -> a + b);
                prizeTiers.merge(winning.getPlayer(), winning.getTier(), (a, b) -> a.ordinal() < b.ordinal() ? a : b);
            }
            for (Map.Entry<UUID, Long> entry : transactions.entrySet()) {
                LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(entry.getKey());
                PrizeTier prizeTier = prizeTiers.get(entry.getKey());
                lotteryPlayer.updateStats(PlayerStatsKey.TOTAL_WINNINGS, long.class, i -> i + entry.getValue());
                lotteryPlayer.updateStats(PlayerStatsKey.HIGHEST_WON_TIER, PrizeTier.class, t -> t == null || prizeTier.ordinal() < t.ordinal(), prizeTier);
            }
        }).start();
    }

}
