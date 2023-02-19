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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.loohp.lotterysix.LotterySixPlugin;
import com.loohp.lotterysix.events.LotterySixEvent;
import com.loohp.lotterysix.events.PlayerBetEvent;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGameIndex;
import com.loohp.lotterysix.game.lottery.IDedGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginMessageHandler implements PluginMessageListener {

    private static final Gson GSON = new Gson();
    private static final byte[] EMPTY_DATA_ARRAY = new byte[0];

    private static <T> Gson gsonOfInstance(T instance) {
        return new GsonBuilder().registerTypeAdapter(instance.getClass(), (InstanceCreator<T>) type -> instance).create();
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
                                if (instance.getCurrentGame() == null) {
                                    PlayableLotterySixGame game = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), PlayableLotterySixGame.class);
                                    instance.setCurrentGame(game);
                                } else {
                                    gsonOfInstance(instance.getCurrentGame()).fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), PlayableLotterySixGame.class);
                                }
                            } else {
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
                                IDedGame game = gameId == null ? null : instance.getGame(gameId);
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
                                IDedGame game = gameId == null ? null : instance.getGame(gameId);
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
                            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> LotterySixPlugin.forceCloseAllGui());
                            break;
                        }
                        case 0x07: { // Call LotterySix Event
                            LotterySixAction action = LotterySixAction.values()[in.readInt()];
                            Bukkit.getPluginManager().callEvent(new LotterySixEvent(LotterySixPlugin.getInstance(), action));
                            break;
                        }
                        case 0x08: { // Call Player Bet Event
                            UUID uuid = DataTypeIO.readUUID(in);
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                BetNumbers numbers = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), BetNumbers.class);
                                Bukkit.getPluginManager().callEvent(new PlayerBetEvent(player, numbers));
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
                            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> {
                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    if (!LotterySixPlugin.activeBossBar.getPlayers().contains(player)) {
                                        LotterySixPlugin.activeBossBar.addPlayer(player);
                                    }
                                }
                            });
                            break;
                        }
                        case 0x0B: { // Past Games Sync Check
                            respondPastGameSyncCheck();
                            break;
                        }
                        case 0x0C: { // Past Games Sync Check Missing
                            int size = in.readInt();
                            for (int i = 0; i < size; i++) {
                                instance.getCompletedGames().add(GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), CompletedLotterySixGame.class));
                            }
                            if (size > 0) {
                                instance.getCompletedGames().indexSort(Comparator.comparing((CompletedLotterySixGameIndex gameIndex) -> gameIndex.getDatetime()).reversed());
                            }
                            break;
                        }
                        case 0x0D: { // Open Main Menu
                            UUID uuid = DataTypeIO.readUUID(in);
                            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) {
                                    LotterySixPlugin.getGuiProvider().getMainMenu(player).show(player);
                                }
                            });
                            break;
                        }
                        case 0x0E: { // Respond Add Bet Result
                            UUID uuid = DataTypeIO.readUUID(in);
                            AddBetResult result = AddBetResult.values()[in.readInt()];
                            long price = in.readLong();
                            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> {
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
                                        case ACCOUNT_SUSPENDED: {
                                            long time = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId()).getPreference(PlayerPreferenceKey.SUSPEND_ACCOUNT_UNTIL, long.class);
                                            player.sendMessage(instance.messageBettingAccountSuspended.replace("{Date}", instance.dateFormat.format(new Date(time))).replace("{Price}", StringUtils.formatComma(price)));
                                            break;
                                        }
                                    }
                                    if (result.isSuccess()) {
                                        Bukkit.getScheduler().runTaskLater(LotterySixPlugin.plugin, () -> LotterySixPlugin.getGuiProvider().checkReopen(player), 5);
                                    }
                                }
                            });
                            break;
                        }
                        case 0x0F: { // Update Player Preference & Stats
                            UUID uuid = DataTypeIO.readUUID(in);
                            LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(uuid);

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

                            lotteryPlayer.bulkSet(preferences, stats);
                            break;
                        }
                        case 0x10: { // Call Updater
                            UUID uuid = DataTypeIO.readUUID(in);
                            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> {
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
                                Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, () -> {
                                    String message = nullableMessage;
                                    if (message == null) {
                                        LotterySixPlugin.activeBossBar.setVisible(false);
                                        return;
                                    }
                                    LotterySixPlugin.activeBossBar.setVisible(true);
                                    LotterySixPlugin.activeBossBar.setProgress(progress);
                                    LotterySixPlugin.activeBossBar.setColor(BarColor.valueOf(color));
                                    LotterySixPlugin.activeBossBar.setStyle(BarStyle.valueOf(style));
                                    IDedGame game = gameId == null ? null : instance.getGame(gameId);
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
        Bukkit.getScheduler().runTaskAsynchronously(LotterySixPlugin.plugin, () -> {
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

    public void requestAddBet(String name, UUID player, long bet, BetUnitType unitType, BetNumbers chosenNumbers) {
        requestAddBet(name, player, bet, unitType, Collections.singleton(chosenNumbers));
    }

    public void requestAddBet(String name, UUID player, long bet, BetUnitType unitType, Collection<BetNumbers> chosenNumbers) {
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

}
