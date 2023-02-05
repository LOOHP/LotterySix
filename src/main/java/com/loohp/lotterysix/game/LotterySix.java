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

package com.loohp.lotterysix.game;

import com.cronutils.model.Cron;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.lottery.IDedGame;
import com.loohp.lotterysix.game.lottery.LazyCompletedLotterySixGameList;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.BetResultConsumer;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.MessageConsumer;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.player.LotteryPlayerManager;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.CronUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LotterySix implements AutoCloseable {

    private final TimerTask lotteryTask;
    private volatile TimerTask announcementTask;
    private final File dataFolder;
    private final String configId;

    public String messageReloaded;
    public String messageNoPermission;
    public String messageNoConsole;
    public String messageInvalidUsage;
    public String messageNotEnoughMoney;
    public String messageBetPlaced;
    public String messageNoGameRunning;
    public String messageGameAlreadyRunning;
    public String messageGameLocked;
    public String messagePreferenceUpdated;
    public String messageGameStarted;
    public String messageGameSettingsUpdated;
    public String messageBetLimitReachedSelf;
    public String messageBetLimitReachedPermission;
    public String messagePlayerNotFound;
    public String messagePendingUnclaimed;
    public String messagePendingClaimed;
    public String messageGameNumberNotFound;

    public String explanationMessage;
    public String explanationURL;
    public String[] explanationGUIItem;

    public Locale locale;
    public SimpleDateFormat dateFormat;

    public boolean updaterEnabled;

    public boolean backendBungeecordMode;

    public Cron runInterval;
    public TimeZone timezone;
    public long betsAcceptDuration;
    public long pricePerBet;
    public int numberOfChoices;
    public long lowestTopPlacesPrize;
    public double taxPercentage;

    public String guiMainMenuTitle;
    public String[] guiMainMenuCheckPastResults;
    public String[] guiMainMenuNoLotteryGamesScheduled;
    public String[] guiMainMenuCheckOwnBets;
    public String[] guiMainMenuPlaceNewBets;
    public String guiLastResultsTitle;
    public String[] guiLastResultsLotteryInfo;
    public String[] guiLastResultsYourBets;
    public String guiLastResultsNoWinnings;
    public String[] guiLastResultsNothing;
    public String[] guiLastResultsLookupHistoricGames;
    public String guiGameNumberInputTitle;
    public String guiYourBetsTitle;
    public String[] guiYourBetsNothing;
    public String guiSelectNewBetTypeTitle;
    public String[] guiSelectNewBetTypeSingle;
    public String[] guiSelectNewBetTypeMultiple;
    public String[] guiSelectNewBetTypeBanker;
    public String[] guiSelectNewBetTypeRandom;
    public String guiNewBetSingleTitle;
    public String guiNewBetMultipleTitle;
    public String guiNewBetBankerTitle;
    public String[] guiNewBetFinish;
    public String[] guiNewBetFinishBankers;
    public String guiRandomEntryCountTitle;
    public String guiRandomEntryCountValue;
    public String guiConfirmNewBetTitle;
    public String[] guiConfirmNewBetLotteryInfo;
    public String[] guiConfirmNewBetConfirm;
    public String[] guiConfirmNewBetPartialConfirm;
    public String[] guiConfirmNewBetCancel;

    public String announcerPeriodicMessageMessage;
    public String announcerPeriodicMessageHover;
    public int announcerPeriodicMessageFrequency;
    public boolean announcerPeriodicMessageOneMinuteBefore;
    public String announcerDrawCancelledMessage;
    public boolean announcerBetPlacedAnnouncementEnabled;
    public String announcerBetPlacedAnnouncementMessage;
    public boolean liveDrawAnnouncerEnabled;
    public boolean liveDrawAnnouncerSendMessagesTitle;
    public int liveDrawAnnouncerTimeBetween;
    public List<String> liveDrawAnnouncerPreMessage;
    public List<String> liveDrawAnnouncerPreMessageHover;
    public List<String> liveDrawAnnouncerMessages;
    public List<String> liveDrawAnnouncerPostMessages;
    public List<String> liveDrawAnnouncerPostMessagesHover;

    public String discordSRVDrawResultAnnouncementChannel;
    public String discordSRVDrawResultAnnouncementTitle;
    public String discordSRVDrawResultAnnouncementDescription;
    public String discordSRVDrawResultAnnouncementThumbnailURL;
    public String discordSRVSlashCommandsGlobalMessagesNotLinked;
    public String discordSRVSlashCommandsGlobalLabelsGameNumberName;
    public String discordSRVSlashCommandsGlobalLabelsGameNumberDescription;
    public boolean discordSRVSlashCommandsViewPastDrawEnabled;
    public String discordSRVSlashCommandsViewPastDrawDescription;
    public String discordSRVSlashCommandsViewPastDrawNoResults;
    public String discordSRVSlashCommandsViewPastDrawYourBets;
    public String discordSRVSlashCommandsViewPastDrawNoWinnings;
    public String discordSRVSlashCommandsViewPastDrawThumbnailURL;
    public boolean discordSRVSlashCommandsViewCurrentBetsEnabled;
    public String discordSRVSlashCommandsViewCurrentBetsDescription;
    public String discordSRVSlashCommandsViewCurrentBetsTitle;
    public List<String> discordSRVSlashCommandsViewCurrentBetsSubTitle;
    public String discordSRVSlashCommandsViewCurrentBetsNoBets;
    public String discordSRVSlashCommandsViewCurrentBetsNoGame;
    public String discordSRVSlashCommandsViewCurrentBetsThumbnailURL;

    public Map<String, Long> playerBetLimit;

    private final LotteryPlayerManager playerPreferenceManager;

    private volatile PlayableLotterySixGame currentGame;
    private volatile boolean gameLocked;
    private final LazyCompletedLotterySixGameList completedGames;

    private final Consumer<Collection<PlayerWinnings>> givePrizesConsumer;
    private final Consumer<Collection<PlayerBets>> refundBetsConsumer;
    private final BiPredicate<UUID, Long> takeMoneyConsumer;
    private final BiPredicate<UUID, String> hasPermissionPredicate;
    private final Consumer<Boolean> lockRunnable;
    private final Supplier<Collection<UUID>> onlinePlayersSupplier;
    private final MessageConsumer messageSendingConsumer;
    private final MessageConsumer titleSendingConsumer;
    private final BetResultConsumer playerBetListener;
    private final Consumer<LotterySixAction> actionListener;
    private final Consumer<LotteryPlayer> lotteryPlayerUpdateListener;
    private final Consumer<String> consoleMessageConsumer;

    public LotterySix(boolean isBackend, File dataFolder, String configId, Consumer<Collection<PlayerWinnings>> givePrizesConsumer, Consumer<Collection<PlayerBets>> refundBetsConsumer, BiPredicate<UUID, Long> takeMoneyConsumer, BiPredicate<UUID, String> hasPermissionPredicate, Consumer<Boolean> lockRunnable, Supplier<Collection<UUID>> onlinePlayersSupplier, MessageConsumer messageSendingConsumer, MessageConsumer titleSendingConsumer, BetResultConsumer playerBetListener, Consumer<LotterySixAction> actionListener, Consumer<LotteryPlayer> lotteryPlayerUpdateListener, Consumer<String> consoleMessageConsumer) {
        this.dataFolder = dataFolder;
        this.configId = configId;
        this.givePrizesConsumer = givePrizesConsumer;
        this.refundBetsConsumer = refundBetsConsumer;
        this.takeMoneyConsumer = takeMoneyConsumer;
        this.hasPermissionPredicate = hasPermissionPredicate;
        this.lockRunnable = lockRunnable;
        this.onlinePlayersSupplier = onlinePlayersSupplier;
        this.messageSendingConsumer = messageSendingConsumer;
        this.titleSendingConsumer = titleSendingConsumer;
        this.playerBetListener = playerBetListener;
        this.actionListener = actionListener;
        this.lotteryPlayerUpdateListener = lotteryPlayerUpdateListener;
        this.consoleMessageConsumer = consoleMessageConsumer;

        File lotteryDataFolder = new File(getDataFolder(), "data");
        lotteryDataFolder.mkdirs();
        this.completedGames = new LazyCompletedLotterySixGameList(lotteryDataFolder);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        reloadConfig();
        loadData();

        backendBungeecordMode = isBackend && Config.getConfig(configId).getConfiguration().getBoolean("Bungeecord");

        this.playerPreferenceManager = new LotteryPlayerManager(this);

        new Timer().scheduleAtFixedRate(lotteryTask = new TimerTask() {
            private int counter = 0;
            private int saveInterval = 0;
            private boolean oneMinuteAnnounced = false;
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (!backendBungeecordMode) {
                    if (currentGame == null) {
                        if (runInterval != null && CronUtils.satisfyByCurrentMinute(runInterval, timezone)) {
                            startNewGame();
                            counter = 0;
                        }
                    } else {
                        boolean announce = false;
                        if (currentGame.getScheduledDateTime() <= now) {
                            runCurrentGame();
                        } else if (counter % announcerPeriodicMessageFrequency == 0) {
                            announce = true;
                        } else {
                            if ((currentGame.getScheduledDateTime() / 1000) - (now / 1000) <= 60) {
                                if (!oneMinuteAnnounced) {
                                    oneMinuteAnnounced = true;
                                    announce = true;
                                }
                            } else {
                                if (oneMinuteAnnounced) {
                                    oneMinuteAnnounced = false;
                                }
                            }
                        }
                        if (announce) {
                            for (UUID uuid : onlinePlayersSupplier.get()) {
                                if (!playerPreferenceManager.getLotteryPlayer(uuid).getPreference(PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, boolean.class)) {
                                    messageSendingConsumer.accept(uuid, announcerPeriodicMessageMessage, announcerPeriodicMessageHover, currentGame);
                                }
                            }
                        }
                    }
                }
                if (saveInterval % 30 == 0) {
                    saveData(true);
                }
                saveInterval++;
                counter++;
            }
        }, 5000, 1000);
    }

    @Override
    public void close() {
        lotteryTask.cancel();
        if (announcementTask != null) {
            announcementTask.cancel();
        }
        saveData(false);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public String getConfigId() {
        return configId;
    }

    public LotteryPlayerManager getPlayerPreferenceManager() {
        return playerPreferenceManager;
    }

    public IDedGame getGame(UUID uuid) {
        if (currentGame != null && currentGame.getGameId().equals(uuid)) {
            return currentGame;
        }
        synchronized (completedGames.getIterateLock()) {
            for (CompletedLotterySixGame completedLotterySixGame : completedGames) {
                if (completedLotterySixGame.getGameId().equals(uuid)) {
                    return completedLotterySixGame;
                }
            }
        }
        return null;
    }

    public PlayableLotterySixGame getCurrentGame() {
        return currentGame;
    }

    public GameNumber dateToGameNumber(long time) {
        Year year = Year.from(ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), timezone.toZoneId()));
        if (completedGames.isEmpty()) {
            return new GameNumber(year, 1);
        } else {
            CompletedLotterySixGameIndex gameIndex = completedGames.getIndex(0);
            if (gameIndex.getGameNumber() == null || !gameIndex.getGameNumber().getYear().equals(year)) {
                return new GameNumber(year, 1);
            }
            return new GameNumber(year, gameIndex.getGameNumber().getNumber() + 1);
        }
    }

    public PlayableLotterySixGame startNewGame() {
        ZonedDateTime now = CronUtils.getNow(timezone);
        long endTime = now.withNano(0).toInstant().toEpochMilli() + betsAcceptDuration;
        if (runInterval != null && betsAcceptDuration < 0) {
            ZonedDateTime dateTime = CronUtils.getNextExecution(runInterval, now);
            if (dateTime != null) {
                endTime = dateTime.toInstant().toEpochMilli() + betsAcceptDuration;
            }
        }
        return startNewGame(endTime);
    }

    public synchronized PlayableLotterySixGame startNewGame(long dateTime) {
        if (backendBungeecordMode) {
            throw new IllegalStateException("method cannot be ran on backend server while on bungeecord mode");
        }
        currentGame = PlayableLotterySixGame.createNewGame(this, Math.max(dateTime, System.currentTimeMillis()), completedGames.isEmpty() ? 0 : completedGames.get(0).getRemainingFunds(), lowestTopPlacesPrize);
        saveData(true);
        actionListener.accept(LotterySixAction.START);
        return currentGame;
    }

    protected synchronized CompletedLotterySixGame runCurrentGame() {
        if (backendBungeecordMode) {
            throw new IllegalStateException("method cannot be ran on backend server while on bungeecord mode");
        }
        if (!currentGame.hasBets()) {
            cancelCurrentGame();
            return null;
        }
        setGameLocked(true);
        actionListener.accept(LotterySixAction.RUN_LOTTERY_BEGIN);
        if (liveDrawAnnouncerEnabled) {
            for (UUID uuid : onlinePlayersSupplier.get()) {
                for (int i = 0; i < liveDrawAnnouncerPreMessage.size(); i++) {
                    String hover = i < liveDrawAnnouncerPreMessageHover.size() ? liveDrawAnnouncerPreMessageHover.get(i) : "";
                    messageSendingConsumer.accept(uuid, liveDrawAnnouncerPreMessage.get(i), hover, currentGame);
                }
            }
        }
        consoleMessageConsumer.accept("Calculating Lottery Wins, this might take a while...");
        long start = System.currentTimeMillis();
        CompletedLotterySixGame completed = currentGame.runLottery(numberOfChoices, pricePerBet, taxPercentage);
        long end = System.currentTimeMillis();
        consoleMessageConsumer.accept("Lottery Wins Calculation Completed! (" + (end - start) + "ms)");
        completedGames.add(0, completed);
        currentGame = null;
        saveData(false);
        actionListener.accept(LotterySixAction.RUN_LOTTERY_INTERNAL_DRAWN);
        if (liveDrawAnnouncerEnabled) {
            new Timer().scheduleAtFixedRate(announcementTask = new TimerTask() {
                private int counter = -liveDrawAnnouncerTimeBetween;
                private int index = 0;
                @Override
                public void run() {
                    if (index >= liveDrawAnnouncerMessages.size()) {
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            for (int i = 0; i < liveDrawAnnouncerPostMessages.size(); i++) {
                                String hover = i < liveDrawAnnouncerPostMessagesHover.size() ? liveDrawAnnouncerPostMessagesHover.get(i) : "";
                                messageSendingConsumer.accept(uuid, liveDrawAnnouncerPostMessages.get(i), hover, completed);
                            }
                        }
                        completed.givePrizesAndUpdateStats(LotterySix.this);
                        setGameLocked(false);
                        announcementTask = null;
                        this.cancel();
                        actionListener.accept(LotterySixAction.RUN_LOTTERY_FINISH);
                        return;
                    }
                    if (counter >= 0 && counter % liveDrawAnnouncerTimeBetween == 0) {
                        String message = liveDrawAnnouncerMessages.get(index++);
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            messageSendingConsumer.accept(uuid, message, completed);
                            if (liveDrawAnnouncerSendMessagesTitle) {
                                if (!playerPreferenceManager.getLotteryPlayer(uuid).getPreference(PlayerPreferenceKey.HIDE_TITLES, boolean.class)) {
                                    titleSendingConsumer.accept(uuid, message, completed);
                                }
                            }
                        }
                    }
                    counter++;
                }
            }, 0, 1000);
        } else {
            completed.givePrizesAndUpdateStats(this);
            setGameLocked(false);
            actionListener.accept(LotterySixAction.RUN_LOTTERY_FINISH);
        }
        return completed;
    }

    public synchronized PlayableLotterySixGame cancelCurrentGame() {
        if (backendBungeecordMode) {
            throw new IllegalStateException("method cannot be ran on backend server while on bungeecord mode");
        }
        for (UUID uuid : onlinePlayersSupplier.get()) {
            messageSendingConsumer.accept(uuid, announcerDrawCancelledMessage, currentGame);
        }
        currentGame.cancelGame();
        PlayableLotterySixGame game = currentGame;
        currentGame = null;
        saveData(true);
        actionListener.accept(LotterySixAction.CANCEL);
        return game;
    }

    @Deprecated
    public void setCurrentGame(PlayableLotterySixGame currentGame) {
        this.currentGame = currentGame;
    }

    @Deprecated
    public void setLastGame(CompletedLotterySixGame game) {
        Iterator<CompletedLotterySixGame> itr = completedGames.iterator();
        while (itr.hasNext()) {
            CompletedLotterySixGame completedLotterySixGame = itr.next();
            if (completedLotterySixGame.getDatetime() > game.getDatetime()) {
                itr.remove();
            } else {
                break;
            }
        }
        completedGames.add(0, game);
    }

    public LazyCompletedLotterySixGameList getCompletedGames() {
        return completedGames;
    }

    public void givePrizes(Collection<PlayerWinnings> winnings) {
        givePrizesConsumer.accept(winnings);
    }

    public void refundBets(Collection<PlayerBets> bets) {
        refundBetsConsumer.accept(bets);
    }

    public boolean takeMoney(UUID player, long amount) {
        return takeMoneyConsumer.test(player, amount);
    }

    public boolean isGameLocked() {
        return gameLocked;
    }

    public void setGameLocked(boolean gameLocked) {
        this.gameLocked = gameLocked;
        lockRunnable.accept(gameLocked);
    }

    public Supplier<Collection<UUID>> getOnlinePlayersSupplier() {
        return onlinePlayersSupplier;
    }

    public MessageConsumer getMessageSendingConsumer() {
        return messageSendingConsumer;
    }

    public MessageConsumer getTitleSendingConsumer() {
        return titleSendingConsumer;
    }

    public BetResultConsumer getPlayerBetListener() {
        return playerBetListener;
    }

    public Consumer<LotterySixAction> getActionListener() {
        return actionListener;
    }

    public Consumer<LotteryPlayer> getLotteryPlayerUpdateListener() {
        return lotteryPlayerUpdateListener;
    }

    public long getPlayerBetLimit(UUID uuid) {
        if (hasPermissionPredicate.test(uuid, "lotterysix.betlimit.unlimited")) {
            return -1;
        }
        long limit = Long.MIN_VALUE;
        for (Map.Entry<String, Long> entry : playerBetLimit.entrySet()) {
            if (hasPermissionPredicate.test(uuid, "lotterysix.betlimit." + entry.getKey())) {
                long value = entry.getValue();
                if (value < 0) {
                    return -1;
                } else if (value > limit) {
                    limit = value;
                }
            }
        }
        if (limit == Long.MIN_VALUE) {
            return playerBetLimit.getOrDefault("default", -1L);
        }
        return limit;
    }

    public void reloadConfig() {
        Config config = Config.getConfig(configId);
        config.reload();

        messageReloaded = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.Reloaded"));
        messageNoPermission = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoPermission"));
        messageNoConsole = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoConsole"));
        messageInvalidUsage = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.InvalidUsage"));
        messageNotEnoughMoney = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NotEnoughMoney"));
        messageBetPlaced = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.BetPlaced"));
        messageNoGameRunning = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoGameRunning"));
        messageGameAlreadyRunning = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameAlreadyRunning"));
        messageGameLocked = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameLocked"));
        messagePreferenceUpdated = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.PreferenceUpdated"));
        messageGameStarted = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameStarted"));
        messageGameSettingsUpdated = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameSettingsUpdated"));
        messageBetLimitReachedSelf = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.BetLimitReachedSelf"));
        messageBetLimitReachedPermission = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.BetLimitReachedPermission"));
        messagePlayerNotFound = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.PlayerNotFound"));
        messagePendingUnclaimed = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.PendingUnclaimed"));
        messagePendingClaimed = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.PendingClaimed"));
        messageGameNumberNotFound = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameNumberNotFound"));

        explanationMessage = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Explanation.Message"));
        explanationURL = config.getConfiguration().getString("Explanation.URL");
        explanationGUIItem = config.getConfiguration().getStringList("Explanation.GUIItem").stream().map(each -> ChatColorUtils.translateAlternateColorCodes('&', each)).toArray(String[]::new);

        String runInternalStr = config.getConfiguration().getString("LotterySix.RunInterval");
        if (runInternalStr.equalsIgnoreCase("Never")) {
            runInterval = null;
        } else {
            runInterval = CronUtils.PARSER.parse(runInternalStr);
        }
        timezone = TimeZone.getTimeZone(config.getConfiguration().getString("LotterySix.TimeZone"));

        String[] localeStr = config.getConfiguration().getString("Formatting.Locale").split("_");
        locale = new Locale(localeStr[0], localeStr[1]);
        dateFormat = new SimpleDateFormat(ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Formatting.Date")), locale);
        dateFormat.setTimeZone(timezone);

        updaterEnabled = config.getConfiguration().getBoolean("Options.Updater");

        betsAcceptDuration = config.getConfiguration().getLong("LotterySix.BetsAcceptDuration") * 1000;
        if (runInterval == null) {
            betsAcceptDuration = Math.abs(betsAcceptDuration);
        }
        pricePerBet = config.getConfiguration().getLong("LotterySix.PricePerBet");
        if (pricePerBet % 2 != 0) {
            pricePerBet += 1;
        }
        numberOfChoices = Math.max(7, Math.min(49, config.getConfiguration().getInt("LotterySix.NumberOfChoices")));
        lowestTopPlacesPrize = config.getConfiguration().getLong("LotterySix.LowestTopPlacesPrize");
        taxPercentage = config.getConfiguration().getDouble("LotterySix.TaxPercentage");

        guiMainMenuTitle = config.getConfiguration().getString("GUI.MainMenu.Title");
        guiMainMenuCheckPastResults = config.getConfiguration().getStringList("GUI.MainMenu.CheckPastResults").toArray(new String[0]);
        guiMainMenuNoLotteryGamesScheduled = config.getConfiguration().getStringList("GUI.MainMenu.NoLotteryGamesScheduled").toArray(new String[0]);
        guiMainMenuCheckOwnBets = config.getConfiguration().getStringList("GUI.MainMenu.CheckOwnBets").toArray(new String[0]);
        guiMainMenuPlaceNewBets = config.getConfiguration().getStringList("GUI.MainMenu.PlaceNewBets").toArray(new String[0]);
        guiLastResultsTitle = config.getConfiguration().getString("GUI.LastResults.Title");
        guiLastResultsLotteryInfo = config.getConfiguration().getStringList("GUI.LastResults.LotteryInfo").toArray(new String[0]);
        guiLastResultsYourBets = config.getConfiguration().getStringList("GUI.LastResults.YourBets").toArray(new String[0]);
        guiLastResultsNoWinnings = config.getConfiguration().getString("GUI.LastResults.NoWinnings");
        guiLastResultsNothing = config.getConfiguration().getStringList("GUI.LastResults.Nothing").toArray(new String[0]);
        guiLastResultsLookupHistoricGames = config.getConfiguration().getStringList("GUI.LastResults.LookupHistoricGames").toArray(new String[0]);
        guiGameNumberInputTitle = config.getConfiguration().getString("GUI.GameNumberInput.Title");
        guiYourBetsTitle = config.getConfiguration().getString("GUI.YourBets.Title");
        guiYourBetsNothing = config.getConfiguration().getStringList("GUI.YourBets.Nothing").toArray(new String[0]);
        guiSelectNewBetTypeTitle = config.getConfiguration().getString("GUI.SelectNewBetType.Title");
        guiSelectNewBetTypeSingle = config.getConfiguration().getStringList("GUI.SelectNewBetType.Single").toArray(new String[0]);
        guiSelectNewBetTypeMultiple = config.getConfiguration().getStringList("GUI.SelectNewBetType.Multiple").toArray(new String[0]);
        guiSelectNewBetTypeBanker = config.getConfiguration().getStringList("GUI.SelectNewBetType.Banker").toArray(new String[0]);
        guiSelectNewBetTypeRandom = config.getConfiguration().getStringList("GUI.SelectNewBetType.Random").toArray(new String[0]);
        guiNewBetSingleTitle = config.getConfiguration().getString("GUI.NewBet.SingleTitle");
        guiNewBetMultipleTitle = config.getConfiguration().getString("GUI.NewBet.MultipleTitle");
        guiNewBetBankerTitle = config.getConfiguration().getString("GUI.NewBet.BankerTitle");
        guiNewBetFinish = config.getConfiguration().getStringList("GUI.NewBet.Finish").toArray(new String[0]);
        guiNewBetFinishBankers = config.getConfiguration().getStringList("GUI.NewBet.FinishBankers").toArray(new String[0]);
        guiRandomEntryCountTitle = config.getConfiguration().getString("GUI.RandomEntryCount.Title");
        guiRandomEntryCountValue = config.getConfiguration().getString("GUI.RandomEntryCount.Value");
        guiConfirmNewBetTitle = config.getConfiguration().getString("GUI.ConfirmNewBet.Title");
        guiConfirmNewBetLotteryInfo = config.getConfiguration().getStringList("GUI.ConfirmNewBet.LotteryInfo").toArray(new String[0]);
        guiConfirmNewBetConfirm = config.getConfiguration().getStringList("GUI.ConfirmNewBet.Confirm").toArray(new String[0]);
        guiConfirmNewBetPartialConfirm = config.getConfiguration().getStringList("GUI.ConfirmNewBet.PartialConfirm").toArray(new String[0]);
        guiConfirmNewBetCancel = config.getConfiguration().getStringList("GUI.ConfirmNewBet.Cancel").toArray(new String[0]);

        announcerPeriodicMessageMessage = config.getConfiguration().getString("Announcer.PeriodicMessage.Message");
        announcerPeriodicMessageHover = config.getConfiguration().getString("Announcer.PeriodicMessage.Hover");
        announcerPeriodicMessageFrequency = config.getConfiguration().getInt("Announcer.PeriodicMessage.Frequency");
        announcerPeriodicMessageOneMinuteBefore = config.getConfiguration().getBoolean("Announcer.PeriodicMessage.OneMinuteBefore");
        announcerDrawCancelledMessage = config.getConfiguration().getString("Announcer.DrawCancelledMessage");
        announcerBetPlacedAnnouncementEnabled = config.getConfiguration().getBoolean("Announcer.BetPlacedAnnouncement.Enabled");
        announcerBetPlacedAnnouncementMessage = config.getConfiguration().getString("Announcer.BetPlacedAnnouncement.Message");
        liveDrawAnnouncerEnabled = config.getConfiguration().getBoolean("Announcer.LiveDrawAnnouncer.Enabled");
        liveDrawAnnouncerSendMessagesTitle = config.getConfiguration().getBoolean("Announcer.LiveDrawAnnouncer.SendMessagesTitle");
        liveDrawAnnouncerTimeBetween = config.getConfiguration().getInt("Announcer.LiveDrawAnnouncer.TimeBetween");
        liveDrawAnnouncerPreMessage = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PreMessage");
        liveDrawAnnouncerPreMessageHover = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PreMessageHover");
        liveDrawAnnouncerMessages = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.Messages");
        liveDrawAnnouncerPostMessages = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PostMessages");
        liveDrawAnnouncerPostMessagesHover = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PostMessagesHover");

        discordSRVDrawResultAnnouncementChannel = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.Channel");
        discordSRVDrawResultAnnouncementTitle = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.Title");
        discordSRVDrawResultAnnouncementDescription = String.join("\n", config.getConfiguration().getStringList("DiscordSRV.DrawResultAnnouncement.Description"));
        discordSRVDrawResultAnnouncementThumbnailURL = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.ThumbnailURL");
        discordSRVSlashCommandsGlobalMessagesNotLinked = config.getConfiguration().getString("DiscordSRV.SlashCommands.Global.Messages.NotLinked");
        discordSRVSlashCommandsGlobalLabelsGameNumberName = config.getConfiguration().getString("DiscordSRV.SlashCommands.Global.Labels.GameNumber.Name");
        discordSRVSlashCommandsGlobalLabelsGameNumberDescription = config.getConfiguration().getString("DiscordSRV.SlashCommands.Global.Labels.GameNumber.Description");
        discordSRVSlashCommandsViewPastDrawEnabled = config.getConfiguration().getBoolean("DiscordSRV.SlashCommands.ViewPastDraw.Enabled");
        discordSRVSlashCommandsViewPastDrawDescription = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.Description");
        discordSRVSlashCommandsViewPastDrawNoResults = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.NoResults");
        discordSRVSlashCommandsViewPastDrawYourBets = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.YourBets");
        discordSRVSlashCommandsViewPastDrawNoWinnings = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.NoWinnings");
        discordSRVSlashCommandsViewPastDrawThumbnailURL = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.ThumbnailURL");
        discordSRVSlashCommandsViewCurrentBetsEnabled = config.getConfiguration().getBoolean("DiscordSRV.SlashCommands.ViewCurrentBets.Enabled");
        discordSRVSlashCommandsViewCurrentBetsDescription = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.Description");
        discordSRVSlashCommandsViewCurrentBetsTitle = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.Title");
        discordSRVSlashCommandsViewCurrentBetsSubTitle = config.getConfiguration().getStringList("DiscordSRV.SlashCommands.ViewCurrentBets.SubTitle");
        discordSRVSlashCommandsViewCurrentBetsNoBets = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.NoBets");
        discordSRVSlashCommandsViewCurrentBetsNoGame = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.NoGame");
        discordSRVSlashCommandsViewCurrentBetsThumbnailURL = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.ThumbnailURL");

        playerBetLimit = new HashMap<>();
        for (String group : config.getConfiguration().getConfigurationSection("Restrictions.BetLimitPerRound").getKeys(false)) {
            playerBetLimit.put(group, config.getConfiguration().getLong("Restrictions.BetLimitPerRound." + group));
        }
    }

    public synchronized void loadData() {
        completedGames.clear();
        File lotteryDataFolder = new File(getDataFolder(), "data");
        lotteryDataFolder.mkdirs();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File currentGameFile = new File(lotteryDataFolder, "current.json");
        if (currentGameFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(currentGameFile.toPath()), StandardCharsets.UTF_8))) {
                currentGame = gson.fromJson(reader, PlayableLotterySixGame.class);
                currentGame.setInstance(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File completedGameFile = new File(lotteryDataFolder, "completed.json");
        if (completedGameFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(completedGameFile.toPath()), StandardCharsets.UTF_8))) {
                JsonArray array = gson.fromJson(reader, JsonArray.class);
                for (JsonElement element : array) {
                    CompletedLotterySixGameIndex gameIndex = gson.fromJson(element.getAsJsonObject(), CompletedLotterySixGameIndex.class);
                    if (new File(lotteryDataFolder, gameIndex.getDataFileName()).exists()) {
                        completedGames.add(gameIndex);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!completedGames.isEmpty()) {
                //noinspection ResultOfMethodCallIgnored
                completedGames.get(0);
            }
        } else {
            for (File file : lotteryDataFolder.listFiles()) {
                if (file.getName().endsWith(".json") && !file.equals(currentGameFile) && !file.equals(completedGameFile)) {
                    CompletedLotterySixGame game = null;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                        game = gson.fromJson(reader, CompletedLotterySixGame.class);
                        completedGames.add(game);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (game != null) {
                        file.renameTo(new File(lotteryDataFolder, game.getDataFileName()));
                    }
                }
            }
            completedGames.sort(Comparator.comparing((CompletedLotterySixGame game) -> game.getDatetime()).reversed());
            saveData(false);
        }
    }

    public synchronized void saveData(boolean onlyCurrent) {
        File lotteryDataFolder = new File(getDataFolder(), "data");
        lotteryDataFolder.mkdirs();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File currentGameFile = new File(lotteryDataFolder, "current.json");
        if (currentGame != null) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(currentGameFile.toPath()), StandardCharsets.UTF_8))) {
                pw.println(gson.toJson(currentGame));
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (currentGameFile.exists()) {
            currentGameFile.delete();
        }
        if (!onlyCurrent) {
            File completedGameFile = new File(lotteryDataFolder, "completed.json");
            JsonArray array = new JsonArray();
            synchronized (completedGames.getIterateLock()) {
                for (CompletedLotterySixGameIndex gameIndex : completedGames.indexIterable()) {
                    array.add(gson.toJsonTree(gameIndex));
                }
            }
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(completedGameFile.toPath()), StandardCharsets.UTF_8))) {
                pw.println(gson.toJson(array));
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (completedGames.getIterateLock()) {
                for (CompletedLotterySixGameIndex gameIndex : completedGames.indexIterable()) {
                    File file = new File(lotteryDataFolder, gameIndex.getDataFileName());
                    if (!file.exists()) {
                        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                            pw.println(gson.toJson(completedGames.get(gameIndex)));
                            pw.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
