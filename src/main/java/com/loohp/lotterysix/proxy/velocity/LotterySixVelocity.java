/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2024. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2024. Contributors
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

package com.loohp.lotterysix.proxy.velocity;

import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.ILotterySixGame;
import com.loohp.lotterysix.game.objects.AddBetResult;
import com.loohp.lotterysix.game.objects.BossBarInfo;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.utils.StringUtils;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LotterySixVelocity {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int BSTATS_PLUGIN_ID = 21787;
    public static final String CONFIG_ID = "config";
    
    public static ProxyServer proxyServer;
    public static LotterySixVelocity plugin = null;

    private static LotterySix instance;
    private static PluginMessageVelocity pluginMessageVelocity;
    
    private static volatile BossBarInfo latestBossBar = BossBarInfo.CLEAR;
    private static volatile ILotterySixGame latestBossBarGame = null;

    private final Logger logger;
    private final File dataFolder;
    private final Metrics.Factory metricsFactory;
    private VelocityPluginDescription description;

    @Inject
    public LotterySixVelocity(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataDirectory) {
        LotterySixVelocity.proxyServer = server;
        this.logger = logger;
        this.metricsFactory = metricsFactory;
        this.dataFolder = dataDirectory.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        plugin = this;

        JsonObject json = GSON.fromJson(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("velocity-plugin.json"), StandardCharsets.UTF_8), JsonObject.class);
        description = new VelocityPluginDescription(json);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        Metrics metrics = metricsFactory.make(this, BSTATS_PLUGIN_ID);
        proxyServer.getEventManager().register(this, new DebugVelocity());

        try {
            Config.loadConfig(CONFIG_ID, new File(getDataFolder(), "config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        CommandManager commandManager = proxyServer.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("lotterysix").aliases("ls", "lottery").plugin(this).build();
        commandManager.register(commandMeta, new CommandsVelocity());

        proxyServer.getEventManager().register(this, pluginMessageVelocity = new PluginMessageVelocity(null));
        proxyServer.getChannelRegistrar().register(LSChannelIdentifier.INSTANCE);

        instance = new LotterySix(false, getDataFolder(), CONFIG_ID, (p, a) -> takeMoneyOnline(p, a), (p, a) -> giveMoneyNow(p, a), p -> notifyOfflineBalanceChange(p), (uuid, permission) -> {
            return getServer().getPlayer(uuid).map(value -> value.hasPermission(permission)).orElse(false);
        }, lock -> {
            pluginMessageVelocity.updateLockState(lock);
        }, () -> Collections2.transform(getServer().getAllPlayers(), p -> p.getUniqueId()), (uuid, message, hover, game) -> {
            getServer().getPlayer(uuid).ifPresent(player -> sendFormattedMessage(player, game, message, hover));
        }, (uuid, message, hover, game) -> {
            getServer().getPlayer(uuid).ifPresent(value -> sendFormattedTitle(value, game, message, 10, 100, 20));
        }, (uuid, result, price, bets) -> {
            Optional<Player> player = getServer().getPlayer(uuid);
            if (player.isPresent()) {
                pluginMessageVelocity.addBetResult(player.get(), result, price);
                for (PlayerBets bet : bets) {
                    callPlayerBetEvent(player.get(), bet.getChosenNumbers(), price, result);
                }
            }
        }, playerBets -> {
            pluginMessageVelocity.updateCurrentGameData();
        }, action -> {
            callLotterySixEvent(action);
            forceCloseAllGui();
        }, lotteryPlayer -> {
            pluginMessageVelocity.syncPlayerData(lotteryPlayer);
        }, message -> {
            getServer().getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }, (bossBarInfo, game) -> {
            latestBossBar = bossBarInfo;
            latestBossBarGame = game;
            pluginMessageVelocity.updateBossBar(bossBarInfo, game);
        });
        instance.reloadConfig();

        pluginMessageVelocity.setInstance(instance);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pluginMessageVelocity.updateCurrentGameData();
                pluginMessageVelocity.updateLockState(instance.isGameLocked());
            }
        }, 0, 10000);

        getLogger().info(TextColor.GREEN + "[LotterySix] LotterySix (Velocity) has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        instance.close();
        getLogger().info(TextColor.RED + "[LotterySix] LotterySix (Velocity) has been disabled!");
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public Logger getLogger() {
        return logger;
    }

    public VelocityPluginDescription getDescription() {
        return description;
    }

    public ProxyServer getServer() {
        return proxyServer;
    }

    public static LotterySix getInstance() {
        return instance;
    }

    public static PluginMessageVelocity getPluginMessageHandler() {
        return pluginMessageVelocity;
    }

    public static void callLotterySixEvent(LotterySixAction action) {
        pluginMessageVelocity.updateCurrentGameData();
        pluginMessageVelocity.updateLastResultData();
        pluginMessageVelocity.updateLockState(LotterySixVelocity.getInstance().isGameLocked());
        pluginMessageVelocity.callLotterySixEvent(action);
    }

    public static void callPlayerBetEvent(Player player, BetNumbers numbers, long price, AddBetResult result) {
        pluginMessageVelocity.updateCurrentGameData();
        pluginMessageVelocity.callPlayerBetEvent(player, numbers, price, result);
    }

    public static void sendFormattedTitle(Player player, ILotterySixGame game, String title, int fadeIn, int stay, int fadeOut) {
        pluginMessageVelocity.sendFormattedTitle(player, game, title, fadeIn, stay, fadeOut);
    }

    public static void sendFormattedMessage(Player player, ILotterySixGame game, String message, String hover) {
        pluginMessageVelocity.sendFormattedMessage(player, game, message, hover);
    }

    public static boolean giveMoneyNow(UUID uuid, long amount) {
        Player player = proxyServer.getPlayer(uuid).orElse(null);
        if (player == null) {
            return false;
        }
        pluginMessageVelocity.giveMoney(player, amount);
        return true;
    }

    public static boolean takeMoneyOnline(UUID uuid, long amount) {
        Player player = proxyServer.getPlayer(uuid).orElse(null);
        if (player == null) {
            return false;
        } else {
            try {
                return pluginMessageVelocity.takeMoney(player, amount).get(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void forceCloseAllGui() {
        pluginMessageVelocity.forceCloseAllGui();
    }

    public static void notifyOfflineBalanceChange(UUID uuid) {
        notifyOfflineBalanceChange(instance.getLotteryPlayerManager().getLotteryPlayer(uuid));
    }

    public static void notifyOfflineBalanceChange(LotteryPlayer lotteryPlayer) {
        Player player = proxyServer.getPlayer(lotteryPlayer.getPlayer()).orElse(null);
        if (player != null) {
            Long money = lotteryPlayer.getStats(PlayerStatsKey.PENDING_TRANSACTION, long.class);
            boolean notifyAnyway = false;
            if (money != null && money > 0) {
                lotteryPlayer.setStats(PlayerStatsKey.PENDING_TRANSACTION, 0L);
                lotteryPlayer.updateStats(PlayerStatsKey.ACCOUNT_BALANCE, long.class, i -> i + money);
                notifyAnyway = true;
            }
            long changed = lotteryPlayer.getStats(PlayerStatsKey.NOTIFY_BALANCE_CHANGE, long.class);
            if (notifyAnyway || changed != 0) {
                lotteryPlayer.setStats(PlayerStatsKey.NOTIFY_BALANCE_CHANGE, 0L);
                pluginMessageVelocity.syncPlayerData(lotteryPlayer);
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(instance.messageNotifyBalanceChange.replace("{Amount}", StringUtils.formatComma(changed))));
            }
        }
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        proxyServer.getScheduler().buildTask(this, () -> instance.getLotteryPlayerManager().loadLotteryPlayer(player.getUniqueId(), true));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onConnected(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Optional<ServerConnection> opt = event.getPlayer().getCurrentServer();
                if (opt.isPresent()) {
                    LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
                    notifyOfflineBalanceChange(lotteryPlayer);
                    pluginMessageVelocity.updateCurrentGameData(opt.get().getServer());
                    pluginMessageVelocity.requestPastGameSyncCheck(opt.get().getServer());
                    pluginMessageVelocity.syncPlayerData(lotteryPlayer);
                    pluginMessageVelocity.updateBossBar(latestBossBar, latestBossBarGame);
                    this.cancel();
                }
            }
        }, 0, 200);
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        proxyServer.getScheduler().buildTask(this, () -> instance.getLotteryPlayerManager().unloadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }

}
