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

package com.loohp.lotterysix.proxy.bungee;

import com.google.gson.Gson;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.ILotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BetUnitType;
import com.loohp.lotterysix.game.objects.BossBarInfo;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerPreferenceKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.utils.ArrayUtils;
import com.loohp.lotterysix.utils.DataTypeIO;
import com.loohp.lotterysix.utils.SyncUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginMessageBungee implements Listener {

    private static final Gson GSON = new Gson();
    private static final byte[] EMPTY_DATA_ARRAY = new byte[0];

    private LotterySix instance;

    private final Map<Integer, ByteArrayOutputStream> incomingMessages;
    private final Random random;
    private final Map<ServerInfo, AtomicInteger> sequenceCounter;
    private final AtomicInteger lastReceivedSequence;
    private final Executor executor;

    private final Map<Integer, CompletableFuture<Boolean>> takeMoneyRequests;

    public PluginMessageBungee(LotterySix instance) {
        this.instance = instance;
        this.incomingMessages = new ConcurrentHashMap<>();
        this.random = new Random();
        this.sequenceCounter = new ConcurrentHashMap<>();
        this.takeMoneyRequests = new ConcurrentHashMap<>();
        this.lastReceivedSequence = new AtomicInteger();

        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setInstance(LotterySix instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onMessageReceive(PluginMessageEvent event) {
        try {
            if (!event.getTag().equals("lotterysix:main")) {
                return;
            }
            event.setCancelled(true);

            Server senderServer = (Server) event.getSender();

            byte[] packet = Arrays.copyOf(event.getData(), event.getData().length);
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
            ProxyServer.getInstance().getScheduler().runAsync(LotterySixBungee.plugin, () -> {
                try {
                    SyncUtils.blockUntilTrue(() -> sequenceId <= lastReceivedSequence.get() + 1, 250);
                    switch (packetId) {
                        case 0x00: { //Request Add Bet
                            PlayableLotterySixGame game = instance.getCurrentGame();
                            if (game != null) {
                                String name = DataTypeIO.readString(in, StandardCharsets.UTF_8);
                                UUID player = DataTypeIO.readUUID(in);
                                long bet = in.readLong();
                                BetUnitType type = BetUnitType.values()[in.readInt()];
                                int size = in.readInt();
                                List<BetNumbers> betNumbers = new ArrayList<>(size);
                                for (int i = 0; i < size; i++) {
                                    betNumbers.add(GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), BetNumbers.class));
                                }
                                int multipleDraw = in.readInt();
                                game.addBet(name, player, bet, type, betNumbers, multipleDraw);
                            }
                            break;
                        }
                        case 0x01: { //Respond take money result
                            int id = in.readInt();
                            CompletableFuture<Boolean> future = takeMoneyRequests.remove(id);
                            if (future != null) {
                                future.complete(in.readBoolean());
                            }
                            break;
                        }
                        case 0x02: { //Respond check past games
                            int size = in.read();
                            Set<UUID> gameIds = new HashSet<>(size);
                            for (int i = 0; i < size; i++) {
                                gameIds.add(DataTypeIO.readUUID(in));
                            }
                            respondPastGameSyncCheckResult(senderServer.getInfo(), gameIds);
                            break;
                        }
                        case 0x03: { //Update Player Preference
                            UUID player = DataTypeIO.readUUID(in);
                            PlayerPreferenceKey key = PlayerPreferenceKey.values()[in.readInt()];
                            Object value = key.getReader(DataTypeIO.readString(in, StandardCharsets.UTF_8));
                            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player);
                            lotteryPlayer.setPreference(key, value);
                            syncPlayerData(lotteryPlayer);
                            break;
                        }
                        case 0x04: { //Reset Player Preference
                            UUID player = DataTypeIO.readUUID(in);
                            PlayerPreferenceKey key = PlayerPreferenceKey.values()[in.readInt()];
                            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player);
                            lotteryPlayer.resetPreference(key);
                            syncPlayerData(lotteryPlayer);
                            break;
                        }
                        case 0x05: { //Update Player Stats
                            UUID player = DataTypeIO.readUUID(in);
                            PlayerStatsKey key = PlayerStatsKey.values()[in.readInt()];
                            Object value = GSON.fromJson(DataTypeIO.readString(in, StandardCharsets.UTF_8), key.getValueTypeClass());
                            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player);
                            lotteryPlayer.setStats(key, value);
                            syncPlayerData(lotteryPlayer);
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

    private void sendData(ServerInfo info, int packetId, byte[] data) {
        executor.execute(() -> {
            int sequenceId = sequenceCounter.computeIfAbsent(info, e -> new AtomicInteger()).getAndIncrement();
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
                    info.sendData("lotterysix:main", outputStream.toByteArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void updateCurrentGameData() {
        PlayableLotterySixGame game = instance.getCurrentGame();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            if (game == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeString(out, GSON.toJson(game), StandardCharsets.UTF_8);
            }
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x00, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateCurrentGameData(ServerInfo target) {
        PlayableLotterySixGame game = instance.getCurrentGame();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            if (game == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeString(out, GSON.toJson(game), StandardCharsets.UTF_8);
            }
            sendData(target, 0x00, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateLastResultData() {
        if (instance.getCompletedGames().isEmpty()) {
            return;
        }
        CompletedLotterySixGame game = instance.getCompletedGames().get(0);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, game.getGameId());
            DataTypeIO.writeString(out, GSON.toJson(game), StandardCharsets.UTF_8);
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x01, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFormattedTitle(ProxiedPlayer player, ILotterySixGame game, String title, int fadeIn, int stay, int fadeOut) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            if (game == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeUUID(out, game.getGameId());
            }
            DataTypeIO.writeUUID(out, player.getUniqueId());
            DataTypeIO.writeString(out, title, StandardCharsets.UTF_8);
            out.writeInt(fadeIn);
            out.writeInt(stay);
            out.writeInt(fadeOut);
            sendData(player.getServer().getInfo(), 0x02, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFormattedMessage(ProxiedPlayer player, ILotterySixGame game, String message, String hover) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            if (game == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeUUID(out, game.getGameId());
            }
            DataTypeIO.writeUUID(out, player.getUniqueId());
            DataTypeIO.writeString(out, message, StandardCharsets.UTF_8);
            DataTypeIO.writeString(out, hover, StandardCharsets.UTF_8);
            sendData(player.getServer().getInfo(), 0x03, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void giveMoney(ProxiedPlayer player, long amount) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            out.writeLong(amount);
            sendData(player.getServer().getInfo(), 0x04, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Boolean> takeMoney(ProxiedPlayer player, long amount) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);

            int id = random.nextInt();
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            out.writeInt(id);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            out.writeLong(amount);

            takeMoneyRequests.put(id, future);

            sendData(player.getServer().getInfo(), 0x05, outputStream.toByteArray());

            return future;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(false);
    }

    public void forceCloseAllGui() {
        for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
            sendData(info, 0x06, EMPTY_DATA_ARRAY);
        }
    }

    public void callLotterySixEvent(LotterySixAction action) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            out.writeInt(action.ordinal());
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x07, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void callPlayerBetEvent(ProxiedPlayer player, BetNumbers numbers, long price, AddBetResult result) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            out.writeLong(price);
            out.writeInt(result.ordinal());
            DataTypeIO.writeString(out, GSON.toJson(numbers), StandardCharsets.UTF_8);
            sendData(player.getServer().getInfo(), 0x08, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateLockState(boolean locked) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            out.writeBoolean(locked);
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x09, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
            sendData(info, 0x0A, EMPTY_DATA_ARRAY);
        }
    }

    public void requestPastGameSyncCheck(ServerInfo target) {
        sendData(target, 0x0B, EMPTY_DATA_ARRAY);
    }

    public void respondPastGameSyncCheckResult(ServerInfo target, Set<UUID> gameIds) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            List<CompletedLotterySixGame> games = new ArrayList<>();
            synchronized (instance.getCompletedGames().getIterateLock()) {
                for (CompletedLotterySixGame game : instance.getCompletedGames()) {
                    if (!gameIds.contains(game.getGameId())) {
                        games.add(game);
                    }
                }
            }
            out.writeInt(games.size());
            for (CompletedLotterySixGame game : games) {
                DataTypeIO.writeUUID(out, game.getGameId());
                DataTypeIO.writeString(out, GSON.toJson(game), StandardCharsets.UTF_8);
            }
            sendData(target, 0x0C, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openPlayMenu(ProxiedPlayer player) {
        openPlayMenu(player, null);
    }

    public void openPlayMenu(ProxiedPlayer player, String input) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            if (input == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeString(out, input, StandardCharsets.UTF_8);
            }
            sendData(player.getServer().getInfo(), 0x0D, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBetResult(ProxiedPlayer player, AddBetResult result, long price) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            out.writeInt(result.ordinal());
            out.writeLong(price);
            sendData(player.getServer().getInfo(), 0x0E, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncPlayerData(LotteryPlayer player) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getPlayer());
            DataTypeIO.writeString(out, GSON.toJson(player), StandardCharsets.UTF_8);
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x0F, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updater(ProxiedPlayer player) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            DataTypeIO.writeUUID(out, player.getUniqueId());
            sendData(player.getServer().getInfo(), 0x10, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateBossBar(BossBarInfo bossBarInfo, ILotterySixGame game) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outputStream);
            if (bossBarInfo.getMessage() == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeString(out, bossBarInfo.getMessage(), StandardCharsets.UTF_8);
            }
            DataTypeIO.writeString(out, bossBarInfo.getColor(), StandardCharsets.UTF_8);
            DataTypeIO.writeString(out, bossBarInfo.getStyle(), StandardCharsets.UTF_8);
            out.writeDouble(bossBarInfo.getProgress());
            if (game == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                DataTypeIO.writeUUID(out, game.getGameId());
            }
            for (ServerInfo info : ProxyServer.getInstance().getServers().values()) {
                sendData(info, 0x11, outputStream.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
