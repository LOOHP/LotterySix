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
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.MessageConsumer;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

    public SimpleDateFormat dateFormat;

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
    public String guiConfirmNewBetTitle;
    public String[] guiConfirmNewBetLotteryInfo;
    public String[] guiConfirmNewBetConfirm;
    public String[] guiConfirmNewBetCancel;

    public String announcerPeriodicMessageMessage;
    public int announcerPeriodicMessageFrequency;
    public boolean announcerPeriodicMessageOneMinuteBefore;
    public String announcerDrawCancelledMessage;
    public boolean liveDrawAnnouncerEnabled;
    public boolean liveDrawAnnouncerSendMessagesTitle;
    public int liveDrawAnnouncerTimeBetween;
    public List<String> liveDrawAnnouncerPreMessage;
    public List<String> liveDrawAnnouncerMessages;
    public List<String> liveDrawAnnouncerPostMessages;

    public String discordSRVDrawResultAnnouncementChannel;
    public String discordSRVDrawResultAnnouncementTitle;
    public String discordSRVDrawResultAnnouncementDescription;
    public String discordSRVDrawResultAnnouncementThumbnailURL;
    public String discordSRVSlashCommandsGlobalMessagesNotLinked;
    public boolean discordSRVSlashCommandsViewPastDrawEnabled;
    public String discordSRVSlashCommandsViewPastDrawDescription;
    public String discordSRVSlashCommandsViewPastDrawNoResults;
    public String discordSRVSlashCommandsViewPastDrawYourBets;
    public String discordSRVSlashCommandsViewPastDrawNoWinnings;
    public String discordSRVSlashCommandsViewPastDrawThumbnailURL;
    public boolean discordSRVSlashCommandsViewCurrentBetsEnabled;
    public String discordSRVSlashCommandsViewCurrentBetsDescription;
    public String discordSRVSlashCommandsViewCurrentBetsTitle;
    public String discordSRVSlashCommandsViewCurrentBetsNoBets;
    public String discordSRVSlashCommandsViewCurrentBetsNoGame;
    public String discordSRVSlashCommandsViewCurrentBetsThumbnailURL;

    public Map<String, Long> playerBetLimit;

    private final LotteryPlayerManager playerPreferenceManager;

    private volatile PlayableLotterySixGame currentGame;
    private volatile boolean gameLocked;
    private final List<CompletedLotterySixGame> completedGames;

    private final Consumer<Collection<PlayerWinnings>> givePrizesConsumer;
    private final Consumer<Collection<PlayerBets>> refundBetsConsumer;
    private final Predicate<PlayerBets> takeMoneyConsumer;
    private final BiPredicate<UUID, String> hasPermissionPredicate;
    private final Runnable lockRunnable;
    private final Supplier<Collection<UUID>> onlinePlayersSupplier;
    private final MessageConsumer messageSendingConsumer;
    private final MessageConsumer titleSendingConsumer;
    private final BiConsumer<UUID, BetNumbers> playerBetListener;
    private final Consumer<LotterySixAction> actionListener;

    public LotterySix(File dataFolder, String configId, Consumer<Collection<PlayerWinnings>> givePrizesConsumer, Consumer<Collection<PlayerBets>> refundBetsConsumer, Predicate<PlayerBets> takeMoneyConsumer, BiPredicate<UUID, String> hasPermissionPredicate, Runnable lockRunnable, Supplier<Collection<UUID>> onlinePlayersSupplier, MessageConsumer messageSendingConsumer, MessageConsumer titleSendingConsumer, BiConsumer<UUID, BetNumbers> playerBetListener, Consumer<LotterySixAction> actionListener) {
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
        this.completedGames = new ArrayList<>();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        reloadConfig();
        loadData();

        this.playerPreferenceManager = new LotteryPlayerManager(this);

        new Timer().scheduleAtFixedRate(lotteryTask = new TimerTask() {
            private int counter = 0;
            private int saveInterval = 0;
            @Override
            public void run() {
                if (currentGame == null) {
                    if (runInterval != null && CronUtils.satisfyByCurrentMinute(runInterval, timezone)) {

                        startNewGame();
                        counter = 0;
                    }
                } else {
                    if (currentGame.getScheduledDateTime() <= System.currentTimeMillis()) {
                        runCurrentGame();
                    } else if (counter % announcerPeriodicMessageFrequency == 0) {
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            if (!playerPreferenceManager.getLotteryPlayer(uuid).getPreference(PlayerPreferenceKey.HIDE_PERIODIC_ANNOUNCEMENTS, boolean.class)) {
                                messageSendingConsumer.accept(uuid, announcerPeriodicMessageMessage, currentGame);
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
        }, 0, 1000);
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

    public PlayableLotterySixGame getCurrentGame() {
        return currentGame;
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
        currentGame = PlayableLotterySixGame.createNewGame(this, Math.max(dateTime, System.currentTimeMillis()), completedGames.isEmpty() ? 0 : completedGames.get(0).getRemainingFunds(), lowestTopPlacesPrize);
        saveData(true);
        actionListener.accept(LotterySixAction.START);
        return currentGame;
    }

    public synchronized CompletedLotterySixGame runCurrentGame() {
        if (currentGame.getBets().isEmpty()) {
            cancelCurrentGame();
            return null;
        }
        setGameLocked(true);
        actionListener.accept(LotterySixAction.RUN_LOTTERY_BEGIN);
        if (liveDrawAnnouncerEnabled) {
            for (UUID uuid : onlinePlayersSupplier.get()) {
                for (String message : liveDrawAnnouncerPreMessage) {
                    messageSendingConsumer.accept(uuid, message, currentGame);
                }
            }
        }
        CompletedLotterySixGame completed = currentGame.runLottery(numberOfChoices, pricePerBet, taxPercentage);
        completedGames.add(0, completed);
        currentGame = null;
        saveData(false);
        if (liveDrawAnnouncerEnabled) {
            new Timer().scheduleAtFixedRate(announcementTask = new TimerTask() {
                private int counter = -liveDrawAnnouncerTimeBetween;
                private int index = 0;
                @Override
                public void run() {
                    if (index >= liveDrawAnnouncerMessages.size()) {
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            for (String message : liveDrawAnnouncerPostMessages) {
                                messageSendingConsumer.accept(uuid, message, completed);
                            }
                        }
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
            setGameLocked(false);
            actionListener.accept(LotterySixAction.RUN_LOTTERY_FINISH);
        }
        return completed;
    }

    public synchronized PlayableLotterySixGame cancelCurrentGame() {
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

    public List<CompletedLotterySixGame> getCompletedGames() {
        return completedGames;
    }

    public void givePrizes(Collection<PlayerWinnings> winnings) {
        givePrizesConsumer.accept(winnings);
    }

    public void refundBets(Collection<PlayerBets> bets) {
        refundBetsConsumer.accept(bets);
    }

    public boolean takeMoney(PlayerBets bet) {
        return takeMoneyConsumer.test(bet);
    }

    public boolean isGameLocked() {
        return gameLocked;
    }

    public void setGameLocked(boolean gameLocked) {
        this.gameLocked = gameLocked;
        lockRunnable.run();
    }

    public BiConsumer<UUID, BetNumbers> getPlayerBetListener() {
        return playerBetListener;
    }

    public Consumer<LotterySixAction> getActionListener() {
        return actionListener;
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

        String runInternalStr = config.getConfiguration().getString("LotterySix.RunInterval");
        if (runInternalStr.equalsIgnoreCase("Never")) {
            runInterval = null;
        } else {
            runInterval = CronUtils.PARSER.parse(runInternalStr);
        }
        timezone = TimeZone.getTimeZone(config.getConfiguration().getString("LotterySix.TimeZone"));

        dateFormat = new SimpleDateFormat(ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Formatting.Date")));
        dateFormat.setTimeZone(timezone);

        betsAcceptDuration = config.getConfiguration().getLong("LotterySix.BetsAcceptDuration") * 1000;
        if (runInterval == null) {
            betsAcceptDuration = Math.abs(betsAcceptDuration);
        }
        pricePerBet = config.getConfiguration().getLong("LotterySix.PricePerBet");
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
        guiConfirmNewBetTitle = config.getConfiguration().getString("GUI.ConfirmNewBet.Title");
        guiConfirmNewBetLotteryInfo = config.getConfiguration().getStringList("GUI.ConfirmNewBet.LotteryInfo").toArray(new String[0]);
        guiConfirmNewBetConfirm = config.getConfiguration().getStringList("GUI.ConfirmNewBet.Confirm").toArray(new String[0]);
        guiConfirmNewBetCancel = config.getConfiguration().getStringList("GUI.ConfirmNewBet.Cancel").toArray(new String[0]);

        announcerPeriodicMessageMessage = config.getConfiguration().getString("Announcer.PeriodicMessage.Message");
        announcerPeriodicMessageFrequency = config.getConfiguration().getInt("Announcer.PeriodicMessage.Frequency");
        announcerPeriodicMessageOneMinuteBefore = config.getConfiguration().getBoolean("Announcer.PeriodicMessage.OneMinuteBefore");
        announcerDrawCancelledMessage = config.getConfiguration().getString("Announcer.DrawCancelledMessage");
        liveDrawAnnouncerEnabled = config.getConfiguration().getBoolean("Announcer.LiveDrawAnnouncer.Enabled");
        liveDrawAnnouncerSendMessagesTitle = config.getConfiguration().getBoolean("Announcer.LiveDrawAnnouncer.SendMessagesTitle");
        liveDrawAnnouncerTimeBetween = config.getConfiguration().getInt("Announcer.LiveDrawAnnouncer.TimeBetween");
        liveDrawAnnouncerPreMessage = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PreMessage");
        liveDrawAnnouncerMessages = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.Messages");
        liveDrawAnnouncerPostMessages = config.getConfiguration().getStringList("Announcer.LiveDrawAnnouncer.PostMessages");

        discordSRVDrawResultAnnouncementChannel = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.Channel");
        discordSRVDrawResultAnnouncementTitle = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.Title");
        discordSRVDrawResultAnnouncementDescription = String.join("\n", config.getConfiguration().getStringList("DiscordSRV.DrawResultAnnouncement.Description"));
        discordSRVDrawResultAnnouncementThumbnailURL = config.getConfiguration().getString("DiscordSRV.DrawResultAnnouncement.ThumbnailURL");
        discordSRVSlashCommandsGlobalMessagesNotLinked = config.getConfiguration().getString("DiscordSRV.SlashCommands.Global.Messages.NotLinked");
        discordSRVSlashCommandsViewPastDrawEnabled = config.getConfiguration().getBoolean("DiscordSRV.SlashCommands.ViewPastDraw.Enabled");
        discordSRVSlashCommandsViewPastDrawDescription = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.Description");
        discordSRVSlashCommandsViewPastDrawNoResults = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.NoResults");
        discordSRVSlashCommandsViewPastDrawYourBets = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.YourBets");
        discordSRVSlashCommandsViewPastDrawNoWinnings = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.NoWinnings");
        discordSRVSlashCommandsViewPastDrawThumbnailURL = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewPastDraw.ThumbnailURL");
        discordSRVSlashCommandsViewCurrentBetsEnabled = config.getConfiguration().getBoolean("DiscordSRV.SlashCommands.ViewCurrentBets.Enabled");
        discordSRVSlashCommandsViewCurrentBetsDescription = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.Description");
        discordSRVSlashCommandsViewCurrentBetsTitle = config.getConfiguration().getString("DiscordSRV.SlashCommands.ViewCurrentBets.Title");
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
        for (File file : lotteryDataFolder.listFiles()) {
            if (file.getName().endsWith(".json")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                    if (file.equals(currentGameFile)) {
                        currentGame = gson.fromJson(reader, PlayableLotterySixGame.class);
                        currentGame.setInstance(this);
                    } else {
                        completedGames.add(gson.fromJson(reader, CompletedLotterySixGame.class));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        completedGames.sort(Comparator.comparing((CompletedLotterySixGame game) -> game.getDatetime()).reversed());
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
            for (CompletedLotterySixGame completedLotterySixGame : completedGames) {
                File file = new File(lotteryDataFolder, new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_zzz").format(new Date(completedLotterySixGame.getDatetime())) + ".json");
                if (!file.exists()) {
                    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                        pw.println(gson.toJson(completedLotterySixGame));
                        pw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
