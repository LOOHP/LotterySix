/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

import com.google.common.collect.Collections2;
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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LotterySixBungee extends Plugin implements Listener {

    public static final int BSTATS_PLUGIN_ID = 17556;
    public static final String CONFIG_ID = "config";

    public static LotterySixBungee plugin;

    private static LotterySix instance;
    private static PluginMessageBungee pluginMessageBungee;

    private static volatile BossBarInfo latestBossBar = BossBarInfo.CLEAR;
    private static volatile ILotterySixGame latestBossBarGame = null;

    @Override
    public void onEnable() {
        plugin = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerListener(this, new DebugBungee());

        try {
            Config.loadConfig(CONFIG_ID, new File(getDataFolder(), "config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        getProxy().getPluginManager().registerCommand(this, new CommandsBungee());

        getProxy().getPluginManager().registerListener(this, pluginMessageBungee = new PluginMessageBungee(null));
        getProxy().registerChannel("lotterysix:main");

        instance = new LotterySix(false, getDataFolder(), CONFIG_ID, (p, a) -> takeMoneyOnline(p, a), (p, a) -> giveMoneyNow(p, a), p -> notifyOfflineBalanceChange(p), (uuid, permission) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                return player.hasPermission(permission);
            }
            return false;
        }, lock -> {
            pluginMessageBungee.updateLockState(lock);
        }, () -> Collections2.transform(getProxy().getPlayers(), p -> p.getUniqueId()), (uuid, message, hover, game) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                sendFormattedMessage(player, game, message, hover);
            }
        }, (uuid, message, hover, game) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                sendFormattedTitle(player, game, message, 10, 100, 20);
            }
        }, (uuid, result, price, bets) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            pluginMessageBungee.addBetResult(player, result, price);
            for (PlayerBets bet : bets) {
                callPlayerBetEvent(player, bet.getChosenNumbers(), price, result);
            }
        }, playerBets -> {
            pluginMessageBungee.updateCurrentGameData();
        }, action -> {
            callLotterySixEvent(action);
            forceCloseAllGui();
        }, lotteryPlayer -> {
            pluginMessageBungee.syncPlayerData(lotteryPlayer);
        }, message -> {
            ProxyServer.getInstance().getConsole().sendMessage(message);
        }, (bossBarInfo, game) -> {
            latestBossBar = bossBarInfo;
            latestBossBarGame = game;
            pluginMessageBungee.updateBossBar(bossBarInfo, game);
        });
        instance.reloadConfig();

        pluginMessageBungee.setInstance(instance);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pluginMessageBungee.updateCurrentGameData();
                pluginMessageBungee.updateLockState(instance.isGameLocked());
            }
        }, 0, 10000);

        getProxy().getConsole().sendMessage(ChatColor.GREEN + "[LotterySix] LotterySix (Bungeecord) has been Enabled!");
    }

    @Override
    public void onDisable() {
        instance.close();
        getProxy().getConsole().sendMessage(ChatColor.RED + "[LotterySix] LotterySix (Bungeecord) has been Disabled!");
    }

    public static LotterySix getInstance() {
        return instance;
    }

    public static PluginMessageBungee getPluginMessageHandler() {
        return pluginMessageBungee;
    }

    public static void callLotterySixEvent(LotterySixAction action) {
        pluginMessageBungee.updateCurrentGameData();
        pluginMessageBungee.updateLastResultData();
        pluginMessageBungee.updateLockState(LotterySixBungee.getInstance().isGameLocked());
        pluginMessageBungee.callLotterySixEvent(action);
    }

    public static void callPlayerBetEvent(ProxiedPlayer player, BetNumbers numbers, long price, AddBetResult result) {
        pluginMessageBungee.updateCurrentGameData();
        pluginMessageBungee.callPlayerBetEvent(player, numbers, price, result);
    }

    public static void sendFormattedTitle(ProxiedPlayer player, ILotterySixGame game, String title, int fadeIn, int stay, int fadeOut) {
        pluginMessageBungee.sendFormattedTitle(player, game, title, fadeIn, stay, fadeOut);
    }

    public static void sendFormattedMessage(ProxiedPlayer player, ILotterySixGame game, String message, String hover) {
        pluginMessageBungee.sendFormattedMessage(player, game, message, hover);
    }

    public static boolean giveMoneyNow(UUID uuid, long amount) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player == null) {
            return false;
        }
        pluginMessageBungee.giveMoney(player, amount);
        return true;
    }

    public static boolean takeMoneyOnline(UUID uuid, long amount) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player == null) {
            return false;
        } else {
            try {
                return pluginMessageBungee.takeMoney(player, amount).get(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void forceCloseAllGui() {
        pluginMessageBungee.forceCloseAllGui();
    }

    public static void notifyOfflineBalanceChange(UUID uuid) {
        notifyOfflineBalanceChange(instance.getLotteryPlayerManager().getLotteryPlayer(uuid));
    }

    public static void notifyOfflineBalanceChange(LotteryPlayer lotteryPlayer) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(lotteryPlayer.getPlayer());
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
                pluginMessageBungee.syncPlayerData(lotteryPlayer);
                player.sendMessage(instance.messageNotifyBalanceChange.replace("{Amount}", StringUtils.formatComma(changed)));
            }
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        getProxy().getScheduler().runAsync(this, () -> instance.getLotteryPlayerManager().loadLotteryPlayer(player.getUniqueId(), true));
    }

    @EventHandler
    public void onConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (player.getServer() != null) {
                    LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().getLotteryPlayer(player.getUniqueId());
                    notifyOfflineBalanceChange(lotteryPlayer);
                    pluginMessageBungee.updateCurrentGameData(event.getServer().getInfo());
                    pluginMessageBungee.requestPastGameSyncCheck(event.getServer().getInfo());
                    pluginMessageBungee.syncPlayerData(lotteryPlayer);
                    pluginMessageBungee.updateBossBar(latestBossBar, latestBossBarGame);
                    this.cancel();
                }
            }
        }, 0, 200);
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        getProxy().getScheduler().runAsync(this, () -> instance.getLotteryPlayerManager().unloadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }

}
