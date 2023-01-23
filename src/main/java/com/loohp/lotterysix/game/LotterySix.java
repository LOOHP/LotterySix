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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.game.completed.CompletedLotterySixGame;
import com.loohp.lotterysix.game.objects.BetNumbers;
import com.loohp.lotterysix.game.objects.CronExpression;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.MessageConsumer;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.playable.PlayableLotterySixGame;
import com.loohp.lotterysix.utils.ChatColorUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LotterySix implements AutoCloseable {

    private final TimerTask lotteryTask;
    private final File dataFolder;
    private final String configId;

    public String messageReloaded;
    public String messageNoPermission;
    public String messageNoConsole;
    public String messageInvalidUsage;
    public String messageNotEnoughMoney;
    public String messageBidPlaced;
    public String messageNoGameRunning;
    public String messageGameAlreadyRunning;
    public String messageGameLocked;

    public SimpleDateFormat dateFormat;

    public CronExpression runInterval;
    public TimeZone timezone;
    public long bidsAcceptDuration;
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
    public String[] guiYourBetsLotteryInfo;
    public String[] guiYourBetsNextPage;
    public String[] guiYourBetsPreviousPage;
    public String guiNewBetTitle;
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

    private volatile PlayableLotterySixGame currentGame;
    private volatile boolean gameLocked;
    private final List<CompletedLotterySixGame> completedGames;

    private final Consumer<Collection<PlayerWinnings>> givePrizesConsumer;
    private final Consumer<Collection<PlayerBets>> refundBetsConsumer;
    private final Predicate<PlayerBets> takeMoneyConsumer;
    private final Runnable lockRunnable;
    private final Supplier<Collection<UUID>> onlinePlayersSupplier;
    private final MessageConsumer messageSendingConsumer;
    private final MessageConsumer titleSendingConsumer;
    private final BiConsumer<UUID, BetNumbers> playerBetListener;
    private final Consumer<LotterySixAction> actionListener;

    public LotterySix(File dataFolder, String configId, Consumer<Collection<PlayerWinnings>> givePrizesConsumer, Consumer<Collection<PlayerBets>> refundBetsConsumer, Predicate<PlayerBets> takeMoneyConsumer, Runnable lockRunnable, Supplier<Collection<UUID>> onlinePlayersSupplier, MessageConsumer messageSendingConsumer, MessageConsumer titleSendingConsumer, BiConsumer<UUID, BetNumbers> playerBetListener, Consumer<LotterySixAction> actionListener) {
        this.dataFolder = dataFolder;
        this.configId = configId;
        this.givePrizesConsumer = givePrizesConsumer;
        this.refundBetsConsumer = refundBetsConsumer;
        this.takeMoneyConsumer = takeMoneyConsumer;
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

        new Timer().scheduleAtFixedRate(lotteryTask = new TimerTask() {
            private int counter = 0;
            @Override
            public void run() {
                if (currentGame == null) {
                    if (runInterval != null && runInterval.isSatisfiedBy(new Date())) {
                        startNewGame();
                        counter = 0;
                    }
                } else {
                    if (currentGame.getScheduledDateTime() <= System.currentTimeMillis()) {
                        runCurrentGame();
                    } else if (counter % announcerPeriodicMessageFrequency == 0) {
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            messageSendingConsumer.accept(uuid, announcerPeriodicMessageMessage, currentGame);
                        }
                    }
                }
                counter++;
            }
        }, 0, 1000);
    }

    @Override
    public void close() {
        lotteryTask.cancel();
        saveData(false);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public String getConfigId() {
        return configId;
    }

    public PlayableLotterySixGame getCurrentGame() {
        return currentGame;
    }

    public PlayableLotterySixGame startNewGame() {
        long now = LocalDateTime.now().atZone(timezone.toZoneId()).withNano(0).withSecond(0).toInstant().toEpochMilli();
        return startNewGame(now + bidsAcceptDuration);
    }

    public synchronized PlayableLotterySixGame startNewGame(long dateTime) {
        currentGame = PlayableLotterySixGame.createNewGame(this, Math.max(dateTime, System.currentTimeMillis()), completedGames.isEmpty() ? 0 : completedGames.get(0).getRemainingFunds());
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
        CompletedLotterySixGame completed = currentGame.runLottery(numberOfChoices, pricePerBet, lowestTopPlacesPrize, taxPercentage);
        completedGames.add(0, completed);
        currentGame = null;
        saveData(false);
        if (liveDrawAnnouncerEnabled) {
            new Timer().scheduleAtFixedRate(new TimerTask() {
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
                        this.cancel();
                        actionListener.accept(LotterySixAction.RUN_LOTTERY_FINISH);
                        return;
                    }
                    if (counter >= 0 && counter % liveDrawAnnouncerTimeBetween == 0) {
                        String message = liveDrawAnnouncerMessages.get(index++);
                        for (UUID uuid : onlinePlayersSupplier.get()) {
                            messageSendingConsumer.accept(uuid, message, completed);
                            if (liveDrawAnnouncerSendMessagesTitle) {
                                titleSendingConsumer.accept(uuid, message, completed);
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

    public void reloadConfig() {
        Config config = Config.getConfig(configId);
        config.reload();

        messageReloaded = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.Reloaded"));
        messageNoPermission = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoPermission"));
        messageNoConsole = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoConsole"));
        messageInvalidUsage = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.InvalidUsage"));
        messageNotEnoughMoney = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NotEnoughMoney"));
        messageBidPlaced = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.BidPlaced"));
        messageNoGameRunning = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.NoGameRunning"));
        messageGameAlreadyRunning = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameAlreadyRunning"));
        messageGameLocked = ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Messages.GameLocked"));

        String runInternalStr = config.getConfiguration().getString("LotterySix.RunInterval");
        try {
            String[] sections = runInternalStr.trim().split(" ");
            if (runInternalStr.equalsIgnoreCase("Never")) {
                runInterval = null;
            } else {
                String cron = "* " + sections[0] + " " + sections[1] + " ? " + sections[2] + " " + sections[3] + " " + sections[4];
                runInterval = new CronExpression(cron);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            runInterval = null;
        }
        if (runInterval != null) {
            timezone = TimeZone.getTimeZone(config.getConfiguration().getString("LotterySix.TimeZone"));
            runInterval.setTimeZone(timezone);
        }

        dateFormat = new SimpleDateFormat(ChatColorUtils.translateAlternateColorCodes('&', config.getConfiguration().getString("Formatting.Date")));
        dateFormat.setTimeZone(timezone);

        bidsAcceptDuration = config.getConfiguration().getLong("LotterySix.BidsAcceptDuration") * 1000;
        pricePerBet = config.getConfiguration().getLong("LotterySix.PricePerBet");
        numberOfChoices = Math.max(7, Math.min(54, config.getConfiguration().getInt("LotterySix.NumberOfChoices")));
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
        guiYourBetsLotteryInfo = config.getConfiguration().getStringList("GUI.YourBets.LotteryInfo").toArray(new String[0]);
        guiYourBetsNextPage = config.getConfiguration().getStringList("GUI.YourBets.NextPage").toArray(new String[0]);
        guiYourBetsPreviousPage = config.getConfiguration().getStringList("GUI.YourBets.PreviousPage").toArray(new String[0]);
        guiNewBetTitle = config.getConfiguration().getString("GUI.NewBet.Title");
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
