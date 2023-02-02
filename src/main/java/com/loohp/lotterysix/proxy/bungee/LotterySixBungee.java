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

import com.google.common.collect.Collections2;
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.IDedGame;
import com.loohp.lotterysix.game.objects.LotteryPlayer;
import com.loohp.lotterysix.game.objects.LotterySixAction;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.objects.betnumbers.BetNumbers;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

        instance = new LotterySix(false, getDataFolder(), CONFIG_ID, c -> givePrizes(c), c -> refundBets(c), (p, a) -> takeMoney(p, a), (uuid, permission) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                return player.hasPermission(permission);
            }
            return false;
        }, lock -> {
            pluginMessageBungee.updateLockState(lock);
        }, () -> Collections2.transform(getProxy().getPlayers(), p -> p.getUniqueId()), (uuid, message, game) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                sendFormattedMessage(player, game, message);
            }
        }, (uuid, message, game) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            if (player != null) {
                sendFormattedTitle(player, game, message, 10, 100, 20);
            }
        }, (uuid, result, price, bets) -> {
            ProxiedPlayer player = getProxy().getPlayer(uuid);
            pluginMessageBungee.addBetResult(player, result, price);
            for (PlayerBets bet : bets) {
                callPlayerBetEvent(player, bet.getChosenNumbers());
            }
        }, action -> {
            callLotterySixEvent(action);
            forceCloseAllGui();
        }, lotteryPlayer -> {
            pluginMessageBungee.syncPlayerData(lotteryPlayer);
        }, message -> {
            ProxyServer.getInstance().getConsole().sendMessage(message);
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

    public static void callPlayerBetEvent(ProxiedPlayer player, BetNumbers numbers) {
        pluginMessageBungee.updateCurrentGameData();
        pluginMessageBungee.callPlayerBetEvent(player, numbers);
    }

    public static void sendFormattedTitle(ProxiedPlayer player, IDedGame game, String title, int fadeIn, int stay, int fadeOut) {
        pluginMessageBungee.sendFormattedTitle(player, game, title, fadeIn, stay, fadeOut);
    }

    public static void sendFormattedMessage(ProxiedPlayer player, IDedGame game, String message) {
        pluginMessageBungee.sendFormattedMessage(player, game, message);
    }

    public static void givePrizes(Collection<PlayerWinnings> winnings) {
        Map<UUID, Long> transactions = new HashMap<>();
        for (PlayerWinnings winning : winnings) {
            transactions.merge(winning.getPlayer(), winning.getWinnings(), (a, b) -> a + b);
        }
        for (Map.Entry<UUID, Long> entry : transactions.entrySet()) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
            if (player == null) {
                instance.getPlayerPreferenceManager().getLotteryPlayer(entry.getKey()).updateStats(PlayerStatsKey.PENDING_TRANSACTION, long.class, i -> i + entry.getValue());
            } else {
                pluginMessageBungee.giveMoney(player, entry.getValue());
            }
        }
    }

    public static void refundBets(Collection<PlayerBets> bets) {
        Map<UUID, Long> transactions = new HashMap<>();
        for (PlayerBets bet : bets) {
            transactions.merge(bet.getPlayer(), bet.getBet(), (a, b) -> a + b);
        }
        for (Map.Entry<UUID, Long> entry : transactions.entrySet()) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
            if (player == null) {
                instance.getPlayerPreferenceManager().getLotteryPlayer(entry.getKey()).updateStats(PlayerStatsKey.PENDING_TRANSACTION, long.class, i -> i + entry.getValue());
            } else {
                pluginMessageBungee.giveMoney(player, entry.getValue());
            }
        }
    }

    public static boolean takeMoney(UUID uuid, long amount) {
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

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        getProxy().getScheduler().runAsync(this, () -> instance.getPlayerPreferenceManager().loadLotteryPlayer(player.getUniqueId(), true));
    }

    @EventHandler
    public void onConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (player.getServer() != null) {
                    LotteryPlayer lotteryPlayer = instance.getPlayerPreferenceManager().getLotteryPlayer(player.getUniqueId());
                    Long money = lotteryPlayer.getStats(PlayerStatsKey.PENDING_TRANSACTION, long.class);
                    if (money != null && money > 0) {
                        TextComponent textComponent = new TextComponent(instance.messagePendingUnclaimed);
                        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lotterysix pendingtransaction"));
                        player.sendMessage(textComponent);
                    }
                    pluginMessageBungee.updateCurrentGameData(event.getServer().getInfo());
                    pluginMessageBungee.requestPastGameSyncCheck(event.getServer().getInfo());
                    pluginMessageBungee.syncPlayerData(lotteryPlayer);
                    this.cancel();
                }
            }
        }, 0, 200);
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        getProxy().getScheduler().runAsync(this, () -> instance.getPlayerPreferenceManager().unloadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }

}
