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

package com.loohp.lotterysix.pluginmessaging;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.events.LotterySixEvent;
import com.loohp.lotterysix.events.PlayerBetEvent;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.GameNumber;
import com.loohp.lotterysix.game.lottery.ILotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.NumberStatistics;
import com.loohp.lotterysix.game.objects.Pair;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersBuilder;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbersType;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.objects.Scheduler;
import com.loohp.lotterysix.utils.ArrayUtils;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.DataTypeIO;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.StringUtils;
import com.loohp.lotterysix.utils.TitleUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginMessageHandler implements PluginMessageListener {

    private static final Gson GSON = new Gson();

    private static <T> Gson gsonOfInstance(T instance) {
        return GSON.newBuilder().registerTypeAdapter(instance.getClass(), (InstanceCreator<T>) type -> instance).create();
    }

    private final LotterySix instance;

    private final Map<Integer, ByteArrayOutputStream> incomingMessages;
    private final Random random;
    private final AtomicInteger sequenceCounter;
    private final AtomicInteger lastReceivedSequence;
    private final Executor executor;

    public PluginMessageHandler(LotterySix instance) {
        this.instance = instance;
        this.incomingMessages = new ConcurrentHashMap<>();
        this.random = new Random();
        this.sequenceCounter = new AtomicInteger();
        this.lastReceivedSequence = new AtomicInteger();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] data) {
        try {
            if (!channel.equals("lotterysix:main")) {
                return;
            }

            byte[] packet = Arrays.copyOf(data, data.length);
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet));

            int sequenceId = inputStream.readInt();
            int packetId = inputStream.readShort();
            boolean end = inputStream.readBoolean();
            ByteArrayOutputStream buffer = incomingMessages.get(sequenceId);
            if (buffer == null) {
                incomingMessages.putIfAbsent(sequenceId, buffer = new ByteArrayOutputStream());
            }
            byte[] b = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(b, 0, b.length)) != -1) {
                buffer.write(b, 0, nRead);
            }

            if (!end) {
                return;
            }
            incomingMessages.remove(sequenceId);

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer.toByteArray()));

            executor.execute(() -> {
                try {
                    switch (packetId) {
                        case 0x00: { // Update Current Game Data
                            if (in.readBoolean()) {
                                UUID gameId = DataTypeIO.readUUID(in);
                                long dateTime = in.readLong();
                                GameNumber gameNumber = DataTypeIO.readGameNumber(in);
                                String specialName = in.readBoolean() ? DataTypeIO.readString(in, StandardCharsets.UTF_8) : null;
                                int mapSize = in.readInt();
                                Map<Integer, NumberStatistics> numberStatistics = new HashMap<>(mapSize);
                                for (int i = 0; i < mapSize; i++) {
                                    numberStatistics.put((int) in.readByte(), DataTypeIO.readNumberStatistics(in));
                                }
                                long carryOverFund = in.readLong();
                                long lowestTopPlacesPrize = in.readLong();
                                boolean isValid = in.readBoolean();

                                int listSize = in.readInt();
                                Set<UUID> betIds = new HashSet<>(listSize);
                                for (int i = 0; i < listSize; i++) {
                                    betIds.add(DataTypeIO.readUUID(in));
                                }

                                PlayableLotterySixGame currentGame = instance.getCurrentGame();
                                if (currentGame == null || !currentGame.getGameId().equals(gameId) || (isValid && !currentGame.isValid())) {
                                    currentGame = PlayableLotterySixGame.createPresetGame(instance, gameId, gameNumber, dateTime, specialName, numberStatistics, carryOverFund, lowestTopPlacesPrize, Collections.emptyList());
                                    if (!isValid) {
                                        currentGame.markInvalid();
                                    }
                                    instance.setCurrentGame(currentGame);
                                } else {
                                    currentGame.setDatetime(dateTime, gameNumber);
                                    currentGame.setSpecialName(specialName);
                                    currentGame.setNumberStatistics(numberStatistics);
                                    currentGame.setCarryOverFund(carryOverFund);
                                    currentGame.setLowestTopPlacesPrize(lowestTopPlacesPrize);
                                    if (!isValid) {
                                        currentGame.markInvalid();
                                    }
                                }

                                betIds.removeAll(currentGame.getBetIds());
                                if (!betIds.isEmpty()) {
                                    requestCurrentGameBets(gameId, betIds);
                                }
                            } else {
                                if (instance.getCurrentGame() != null) {
                                    instance.getCurrentGame().markInvalid();
                                }
                                instance.setCurrentGame(null);
                            }
                            instance.requestSave(true);
                            break;
                        }
                        case 0x01: { // Update Last Game Data
                            CompletedLotterySixGame lastGame = null;
                            if (!instance.getCompletedGames().isEmpty()) {
                                lastGame = instance.getCompletedGames().get(0);
                            }
                            UUID gameId = DataTypeIO.readUUID(in);
                            if (lastGame != null && lastGame.getGameId().equals(gameId)) {
                                gsonOfInstance(lastGame).fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), CompletedLotterySixGame.class);
                                instance.getCompletedGames().setGameDirty(lastGame.getGameId());
                            } else {
                                CompletedLotterySixGame game = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), CompletedLotterySixGame.class);
                                instance.setLastGame(game);
                            }
                            instance.requestSave(false);
                            break;
                        }
                        case 0x02: { // Send Formatted Title
                            UUID gameId = in.readBoolean() ? DataTypeIO.readUUID(in) : null;
                            UUID uuid = DataTypeIO.readUUID(in);
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                String message = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                int fadeIn = in.readInt();
                                int stay = in.readInt();
                                int fadeOut = in.readInt();
                                ILotterySixGame game = gameId == null ? null : instance.getGame(gameId);
                                if (game instanceof PlayableLotterySixGame) {
                                    message = LotteryUtils.formatPlaceholders(player, message, instance, (PlayableLotterySixGame) game);
                                } else if (game instanceof CompletedLotterySixGame) {
                                    message = LotteryUtils.formatPlaceholders(player, message, instance, (CompletedLotterySixGame) game);
                                }
                                TitleUtils.sendTitle(player, ChatColorUtils.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message)), "", fadeIn, stay, fadeOut);
                            }
                            break;
                        }
                        case 0x03: { // Send Formatted Message
                            UUID gameId = in.readBoolean() ? DataTypeIO.readUUID(in) : null;
                            UUID uuid = DataTypeIO.readUUID(in);
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                String message = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                String hover = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                ILotterySixGame game = gameId == null ? null : instance.getGame(gameId);
                                if (game instanceof PlayableLotterySixGame) {
                                    message = LotteryUtils.formatPlaceholders(player, message, instance, (PlayableLotterySixGame) game);
                                    if (!hover.isEmpty()) {
                                        hover = LotteryUtils.formatPlaceholders(player, hover, instance, (PlayableLotterySixGame) game);
                                    }
                                } else if (game instanceof CompletedLotterySixGame) {
                                    message = LotteryUtils.formatPlaceholders(player, message, instance, (CompletedLotterySixGame) game);
                                    if (!hover.isEmpty()) {
                                        hover = LotteryUtils.formatPlaceholders(player, hover, instance, (CompletedLotterySixGame) game);
                                    }
                                }
                                TextComponent textComponent = new TextComponent(ChatColorUtils.translateAlternateColorCodes('&', message));
                                if (!hover.isEmpty()) {
                                    //noinspection deprecation
                                    textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {new TextComponent(ChatColorUtils.translateAlternateColorCodes('&', hover))}));
                                }
                                player.spigot().sendMessage(textComponent);
                            }
                            break;
                        }
                        case 0x04: { // Give Money
                            UUID uuid = DataTypeIO.readUUID(in);
                            long amount = in.readLong();
                            LotterySixPlugin.giveMoneyNow(uuid, amount);
                            break;
                        }
                        case 0x05: { // Take Money
                            int id = in.readInt();
                            UUID uuid = DataTypeIO.readUUID(in);
                            long amount = in.readLong();
                            respondTakeMoneyRequest(id, LotterySixPlugin.takeMoneyOnline(uuid, amount));
                            break;
                        }
                        case 0x06: { // Force Close GUIs
                            Scheduler.runTask(LotterySixPlugin.plugin, () -> LotterySixPlugin.forceCloseAllGui());
                            break;
                        }
                        case 0x07: { // Call LotterySix Event
                            LotterySixAction action = LotterySixAction.values()[in.readInt()];
                            Bukkit.getPluginManager().callEvent(new LotterySixEvent(LotterySixPlugin.getInstance(), action));
                            break;
                        }
                        case 0x08: { // Call Player Bet Event
                            UUID uuid = DataTypeIO.readUUID(in);
                            LotteryPlayer player = instance.getLotteryPlayerManager().getLotteryPlayer(uuid);
                            if (player != null) {
                                long price = in.readLong();
                                AddBetResult result = AddBetResult.values()[in.readInt()];
                                BetNumbers numbers = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), BetNumbers.class);
                                Bukkit.getPluginManager().callEvent(new PlayerBetEvent(player, numbers, price, result));
                            }
                            break;
                        }
                        case 0x09: { // Update Lock State
                            boolean lock = in.readBoolean();
                            instance.setGameLocked(lock);
                            break;
                        }
                        case 0x0A: { // Reload Config
                            instance.reloadConfig();
                            if (LotterySixPlugin.discordSRVHook != null) {
                                LotterySixPlugin.discordSRVHook.reload();
                            }
                            if (LotterySixPlugin.activeBossBar != null) {
                                Scheduler.runTask(LotterySixPlugin.plugin, () -> {
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (!LotterySixPlugin.activeBossBar.getPlayers().contains(player)) {
                                            LotterySixPlugin.activeBossBar.addPlayer(player);
                                        }
                                    }
                                });
                            }
                            break;
                        }
                        case 0x0B: { // Past Games Sync Check
                            respondPastGameSyncCheck();
                            break;
                        }
                        case 0x0C: { // Past Games Sync Check Missing
                            boolean shouldSave = false;
                            int size = in.readInt();
                            if (size > 0) {
                                for (int i = 0; i < size; i++) {
                                    UUID gameId = DataTypeIO.readUUID(in);
                                    Optional<CompletedLotterySixGameIndex> optGame;
                                    synchronized (instance.getCompletedGames().getIterateLock()) {
                                        optGame = instance.getCompletedGames().indexStream().filter(each -> each.getGameId().equals(gameId)).findFirst();
                                    }
                                    if (optGame.isPresent()) {
                                        CompletedLotterySixGame game = instance.getCompletedGames().get(optGame.get());
                                        gsonOfInstance(game).fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), CompletedLotterySixGame.class);
                                        instance.getCompletedGames().setGameDirty(game.getGameId());
                                    } else {
                                        instance.getCompletedGames().add(GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), CompletedLotterySixGame.class));
                                    }
                                }
                                if (instance.getCompletedGames().size() > 0) {
                                    instance.getCompletedGames().indexSort(Comparator.reverseOrder());
                                }
                                shouldSave = true;
                            }
                            size = in.readInt();
                            if (size > 0) {
                                Set<UUID> notExist = new HashSet<>();
                                for (int i = 0; i < size; i++) {
                                    notExist.add(DataTypeIO.readUUID(in));
                                }
                                synchronized (instance.getCompletedGames().getIterateLock()) {
                                    Iterator<CompletedLotterySixGameIndex> itr = instance.getCompletedGames().indexIterator();
                                    while (itr.hasNext()) {
                                        CompletedLotterySixGameIndex gameIndex = itr.next();
                                        if (notExist.contains(gameIndex.getGameId())) {
                                            itr.remove();
                                        }
                                    }
                                }
                                shouldSave = true;
                            }
                            if (shouldSave) {
                                instance.requestSave(false);
                            }
                            break;
                        }
                        case 0x0D: { // Open Main Menu
                            UUID uuid = DataTypeIO.readUUID(in);
                            int interactionId = in.readInt();
                            String input = in.readBoolean() ? DataTypeIO.readString(in, StandardCharsets.UTF_8) : null;
                            Scheduler.runTask(LotterySixPlugin.plugin, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) {
                                    if (input == null) {
                                        LotterySixPlugin.getGuiProvider().getMainMenu(player).show(player);
                                    } else {
                                        PlayableLotterySixGame game = instance.getCurrentGame();
                                        if (game != null) {
                                            Pair<Stream<? extends BetNumbersBuilder>, BetNumbersType> pair = BetNumbersBuilder.fromString(1, instance.numberOfChoices, input);
                                            if (pair == null) {
                                                player.sendMessage(instance.messageInvalidBetNumbers);
                                            } else {
                                                LotterySixPlugin.getGuiProvider().getNumberConfirm(player, game, pair.getFirst().map(e -> e.build()).collect(Collectors.toList()), pair.getSecond()).show(player);
                                            }
                                        } else {
                                            player.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                                        }
                                    }
                                }
                                inventoryOpenedResponse(interactionId);
                            }, Bukkit.getPlayer(uuid));
                            break;
                        }
                        case 0x0E: { // Respond Add Bet Result
                            UUID uuid = DataTypeIO.readUUID(in);
                            AddBetResult result = AddBetResult.values()[in.readInt()];
                            long price = in.readLong();
                            Scheduler.runTask(LotterySixPlugin.plugin, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) {
                                    switch (result) {
                                        case SUCCESS: {
                                            player.sendMessage(instance.messageBetPlaced.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case GAME_LOCKED: {
                                            player.sendMessage(instance.messageGameLocked.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case NOT_ENOUGH_MONEY: {
                                            player.sendMessage(instance.messageNotEnoughMoney.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case LIMIT_SELF: {
                                            player.sendMessage(instance.messageBetLimitReachedSelf.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case LIMIT_PERMISSION: {
                                            player.sendMessage(instance.messageBetLimitReachedPermission.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case LIMIT_CHANCE_PER_SELECTION: {
                                            player.sendMessage(instance.messageBetLimitMaximumChancePerSelection.replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                        case ACCOUNT_SUSPENDED: {
                                            long time = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                    }
                                    if (result.isSuccess()) {
                                        Scheduler.runTaskLater(LotterySixPlugin.plugin, () -> LotterySixPlugin.getGuiProvider().checkReopen(player), 5);
                                    }
                                }
                            }, Bukkit.getPlayer(uuid));
                            break;
                        }
                        case 0x0F: { // Update Player Preference & Stats
                            UUID uuid = DataTypeIO.readUUID(in);
                            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(uuid);

                            JsonObject json = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), JsonObject.class);

                            Map<PlayerPreferenceKey, Object> preferences = new EnumMap<>(PlayerPreferenceKey.class);
                            JsonObject preferencesJson = json.getAsJsonObject("preferences");

                            for (PlayerPreferenceKey key : PlayerPreferenceKey.values()) {
                                JsonElement element = preferencesJson.get(key.name());
                                if (element != null) {
                                    preferences.put(key, GSON.fromJson(element, key.getValueTypeClass()));
                                }
                            }

                            JsonObject statsJson = json.getAsJsonObject("stats");
                            Map<PlayerStatsKey, Object> stats = new EnumMap<>(PlayerStatsKey.class);

                            for (PlayerStatsKey key : PlayerStatsKey.values()) {
                                JsonElement element = statsJson.get(key.name());
                                if (element != null) {
                                    stats.put(key, GSON.fromJson(element, key.getValueTypeClass()));
                                }
                            }

                            List<PlayerBets> multipleDrawPlayerBets = GSON.fromJson(json.getAsJsonArray("multipleDrawPlayerBets"), new TypeToken<ArrayList<PlayerBets>>(){}.getType());

                            lotteryPlayer.bulkSet(preferences, stats, multipleDrawPlayerBets);
                            break;
                        }
                        case 0x10: { // Call Updater
                            UUID uuid = DataTypeIO.readUUID(in);
                            Scheduler.runTask(LotterySixPlugin.plugin, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) {
                                    Bukkit.dispatchCommand(player, "lotterysix update");
                                }
                            });
                            break;
                        }
                        case 0x11: { // Update BossBar
                            if (LotterySixPlugin.activeBossBar != null) {
                                String nullableMessage = in.readBoolean() ? DataTypeIO.readString(in, StandardCharsets.UTF_8) : null;
                                String color = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                String style = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                double progress = in.readDouble();
                                UUID gameId = in.readBoolean() ? DataTypeIO.readUUID(in) : null;
                                Scheduler.runTask(LotterySixPlugin.plugin, () -> {
                                    String message = nullableMessage;
                                    if (message == null) {
                                        LotterySixPlugin.activeBossBar.setVisible(false);
                                        return;
                                    }
                                    LotterySixPlugin.activeBossBar.setVisible(true);
                                    LotterySixPlugin.activeBossBar.setProgress(progress);
                                    LotterySixPlugin.activeBossBar.setColor(BarColor.valueOf(color));
                                    LotterySixPlugin.activeBossBar.setStyle(BarStyle.valueOf(style));
                                    ILotterySixGame game = gameId == null ? null : instance.getGame(gameId);
                                    if (game instanceof PlayableLotterySixGame) {
                                        message = LotteryUtils.formatPlaceholders(null, message, instance, (PlayableLotterySixGame) game);
                                    } else if (game instanceof CompletedLotterySixGame) {
                                        message = LotteryUtils.formatPlaceholders(null, message, instance, (CompletedLotterySixGame) game);
                                    }
                                    LotterySixPlugin.activeBossBar.setTitle(message);
                                });
                            }
                            break;
                        }
                        case 0x12: { // Current Game Bet Response
                            UUID gameId = DataTypeIO.readUUID(in);
                            PlayableLotterySixGame game = instance.getCurrentGame();
                            if (game != null && game.getGameId().equals(gameId)) {
                                int size = in.readInt();
                                for (int i = 0; i < size; i++) {
                                    PlayerBets bet = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), PlayerBets.class);
                                    if (game.getBet(bet.getBetId()) == null) {
                                        game.addBet(bet);
                                    }
                                }
                            }
                            break;
                        }
                    }
                    lastReceivedSequence.updateAndGet(i -> Math.max(i, sequenceId));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendData(int packetId, byte[] data) {
        Scheduler.runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.isEmpty()) {
                return;
            }
            Player player = players.stream().skip(random.nextInt(players.size())).findAny().get();
            int sequenceId = sequenceCounter.getAndIncrement();
            try {
                byte[][] dataArray = ArrayUtils.divideArray(data, 32700);

                for (int i = 0; i < dataArray.length; i++) {
                    byte[] chunk = dataArray[i];

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(outputStream);
                    out.writeInt(sequenceId);

                    out.writeShort(packetId);
                    out.writeBoolean(i == (dataArray.length - 1));

                    out.write(chunk);
                    player.sendPluginMessage(LotterySixPlugin.plugin, "lotterysix:main", outputStream.toByteArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void requestAddBet(String name, UUID player, long bet, BetUnitType unitType, BetNumbers chosenNumbers, int multipleDraw) {
        requestAddBet(name, player, bet, unitType, Collections.singleton(chosenNumbers), multipleDraw);
    }

    public void requestAddBet(String name, UUID player, long bet, BetUnitType unitType, Collection<BetNumbers> chosenNumbers, int multipleDraw) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeString(out, name, StandardCharsets.UTF_8);
            DataTypeIO.writeUUID(out, player);
            out.writeLong(bet);
            out.writeInt(unitType.ordinal());
            out.writeInt(chosenNumbers.size());
            for (BetNumbers numbers : chosenNumbers) {
                DataTypeIO.writeString(out, GSON.toJson(numbers), StandardCharsets.UTF_8);
            }
            out.writeInt(multipleDraw);
            sendData(0x00, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void respondTakeMoneyRequest(int id, boolean result) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            out.writeInt(id);
            out.writeBoolean(result);
            sendData(0x01, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void respondPastGameSyncCheck() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            synchronized (instance.getCompletedGames().getIterateLock()) {
                out.writeInt(instance.getCompletedGames().size());
                for (CompletedLotterySixGameIndex gameIndex : instance.getCompletedGames().indexIterable()) {
                    DataTypeIO.writeUUID(out, gameIndex.getGameId());
                }
            }
            sendData(0x02, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerPreference(LotteryPlayer player, PlayerPreferenceKey key, Object value) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getPlayer());
            out.writeInt(key.ordinal());
            DataTypeIO.writeString(out, value.toString(), StandardCharsets.UTF_8);
            sendData(0x03, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetPlayerPreference(LotteryPlayer player, PlayerPreferenceKey key) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getPlayer());
            out.writeInt(key.ordinal());
            sendData(0x04, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerStats(LotteryPlayer player, PlayerStatsKey key, Object value) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getPlayer());
            out.writeInt(key.ordinal());
            DataTypeIO.writeString(out, GSON.toJson(value, key.getValueTypeClass()), StandardCharsets.UTF_8);
            sendData(0x05, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestCurrentGameBets(UUID gameId, Set<UUID> betIds) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, gameId);
            out.writeInt(betIds.size());
            for (UUID betId : betIds) {
                DataTypeIO.writeUUID(out, betId);
            }
            sendData(0x06, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void inventoryOpenedResponse(int interactionId) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            out.writeInt(interactionId);
            sendData(0x07, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
