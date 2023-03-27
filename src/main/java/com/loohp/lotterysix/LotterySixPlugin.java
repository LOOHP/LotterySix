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

package com.loohp.lotterysix;

import com.google.common.collect.Collections2;
import com.loohp.lotterysix.config.Config;
import com.loohp.lotterysix.debug.Debug;
import com.loohp.lotterysix.discordsrv.DiscordSRVHook;
import com.loohp.lotterysix.events.LotterySixEvent;
import com.loohp.lotterysix.events.PlayerBetEvent;
import com.loohp.lotterysix.game.LotterySix;
import com.loohp.lotterysix.game.lottery.CompletedLotterySixGame;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerStatsKey;
import com.loohp.lotterysix.game.player.LotteryPlayer;
import com.loohp.lotterysix.metrics.Charts;
import com.loohp.lotterysix.metrics.Metrics;
import com.loohp.lotterysix.placeholderapi.LotteryPlaceholders;
import com.loohp.lotterysix.pluginmessaging.PluginMessageHandler;
import com.loohp.lotterysix.updater.Updater;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import com.loohp.lotterysix.utils.MCVersion;
import com.loohp.lotterysix.utils.StringUtils;
import com.loohp.lotterysix.utils.TitleUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class LotterySixPlugin extends JavaPlugin implements Listener {

    public static final int BSTATS_PLUGIN_ID = 17516;
    public static final String CONFIG_ID = "config";

    public static LotterySixPlugin plugin;

    public static String exactMinecraftVersion;
    public static MCVersion version;

    public static DiscordSRVHook discordSRVHook = null;
    public static boolean hasFloodgate = false;

    private static PluginMessageHandler pluginMessageHandler;

    private static LotterySix instance;
    private static LotteryPluginGUI guiProvider;
    private static Economy econ = null;
    private static Permission perms = null;

    public static BossBar activeBossBar;

    static {
        try {
            activeBossBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
        } catch (Throwable e) {
            activeBossBar = null;
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);

        exactMinecraftVersion = Bukkit.getVersion().substring(Bukkit.getVersion().indexOf("(") + 5, Bukkit.getVersion().indexOf(")"));
        version = MCVersion.fromPackageName(getServer().getClass().getPackage().getName());

        if (!version.isSupported()) {
            getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.RED + "[LotterySix] This version of minecraft is unsupported! (" + version.toString() + ")");
        }

        getServer().getPluginManager().registerEvents(new Debug(), this);
        getServer().getPluginManager().registerEvents(this, this);

        try {
            Config.loadConfig(CONFIG_ID, new File(getDataFolder(), "config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), true);
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        econ = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        perms = getServer().getServicesManager().getRegistration(Permission.class).getProvider();

        getCommand("lotterysix").setExecutor(new Commands());

        instance = new LotterySix(true, getDataFolder(), CONFIG_ID, (p, a) -> takeMoneyOnline(p, a), (p, a) -> giveMoneyNow(p, a), p -> notifyOfflineBalanceChange(p), (uuid, permission) -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                return player.getPlayer().hasPermission(permission);
            }
            return perms.playerHas(Bukkit.getWorlds().get(0).getName(), Bukkit.getOfflinePlayer(uuid), permission);
        }, lock -> {
            if (lock) {
                forceCloseAllGui();
            }
        }, () -> Collections2.transform(Bukkit.getOnlinePlayers(), p -> p.getUniqueId()), (uuid, message, hover, game) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
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
        }, (uuid, message, hover, game) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (game instanceof PlayableLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (PlayableLotterySixGame) game);
                } else if (game instanceof CompletedLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (CompletedLotterySixGame) game);
                }
                TitleUtils.sendTitle(player, ChatColorUtils.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message)), "", 10, 100, 20);
            }
        }, (uuid, result, price, bets) -> {
            for (PlayerBets bet : bets) {
                Bukkit.getPluginManager().callEvent(new PlayerBetEvent(instance.getLotteryPlayerManager().getLotteryPlayer(uuid), bet.getChosenNumbers(), price, result));
            }
        }, playerBets -> {

        }, action -> {
            Bukkit.getPluginManager().callEvent(new LotterySixEvent(instance, action));
            forceCloseAllGui();
        }, lotteryPlayer -> {
        }, message -> {
            Bukkit.getConsoleSender().sendMessage(message);
        }, (bossBarInfo, game) -> {
            if (activeBossBar == null) {
                return;
            }
            String message = bossBarInfo.getMessage();
            if (message == null) {
                activeBossBar.setVisible(false);
                return;
            }
            activeBossBar.setVisible(true);
            activeBossBar.setProgress(bossBarInfo.getProgress());
            activeBossBar.setColor(BarColor.valueOf(bossBarInfo.getColor()));
            activeBossBar.setStyle(BarStyle.valueOf(bossBarInfo.getStyle()));
            if (game instanceof PlayableLotterySixGame) {
                message = LotteryUtils.formatPlaceholders(null, message, instance, (PlayableLotterySixGame) game);
            } else if (game instanceof CompletedLotterySixGame) {
                message = LotteryUtils.formatPlaceholders(null, message, instance, (CompletedLotterySixGame) game);
            }
            activeBossBar.setTitle(message);
        });
        instance.reloadConfig();

        if (instance.backendBungeecordMode) {
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[LotterySix] Registering Plugin Messaging Channels for Bungeecord...");
            getServer().getMessenger().registerOutgoingPluginChannel(this, "lotterysix:main");
            getServer().getMessenger().registerIncomingPluginChannel(this, "lotterysix:main", pluginMessageHandler = new PluginMessageHandler(instance));
        }

        getServer().getPluginManager().registerEvents(guiProvider = new LotteryPluginGUI(this), this);

        Charts.setup(metrics);

        new LotteryPlaceholders().register();

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            discordSRVHook = new DiscordSRVHook();
            getServer().getPluginManager().registerEvents(discordSRVHook, this);
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "[LotterySix] LotterySix has hooked into DiscordSRV!");
        }

        if (getServer().getPluginManager().isPluginEnabled("floodgate")) {
            hasFloodgate = true;
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "[LotterySix] LotterySix has hooked into Floodgate!");
        }

        if (instance.updaterEnabled) {
            getServer().getPluginManager().registerEvents(new Updater(), this);
        }

        if (activeBossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!activeBossBar.getPlayers().contains(player)) {
                    activeBossBar.addPlayer(player);
                }
            }
        }

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[LotterySix] LotterySix has been Enabled!");
    }

    @Override
    public void onDisable() {
        instance.close();
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[LotterySix] LotterySix has been Disabled!");
    }

    public static LotterySix getInstance() {
        return instance;
    }

    public static LotteryPluginGUI getGuiProvider() {
        return guiProvider;
    }

    public static PluginMessageHandler getPluginMessageHandler() {
        return pluginMessageHandler;
    }

    public static Economy getEcon() {
        return econ;
    }

    public static Permission getPerms() {
        return perms;
    }

    public static boolean giveMoneyNow(UUID uuid, long amount) {
        return econ.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount).transactionSuccess();
    }

    public static boolean takeMoneyOnline(UUID uuid, long amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return false;
        }
        if (!instance.allowLoans) {
            double currentBalance = econ.getBalance(player);
            if (currentBalance - amount < 0) {
                return false;
            }
        }
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static void forceCloseAllGui() {
        runOrScheduleSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                guiProvider.forceClose(player);
            }
        });
    }

    public static void runOrScheduleSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(LotterySixPlugin.plugin, runnable);
        }
    }

    public static void notifyOfflineBalanceChange(UUID uuid) {
        notifyOfflineBalanceChange(instance.getLotteryPlayerManager().getLotteryPlayer(uuid));
    }

    public static void notifyOfflineBalanceChange(LotteryPlayer lotteryPlayer) {
        Player player = Bukkit.getPlayer(lotteryPlayer.getPlayer());
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
                player.sendMessage(instance.messageNotifyBalanceChange.replace("{Amount}", StringUtils.formatComma(changed)));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (activeBossBar != null) {
            activeBossBar.addPlayer(event.getPlayer());
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            LotteryPlayer lotteryPlayer = instance.getLotteryPlayerManager().loadLotteryPlayer(event.getPlayer().getUniqueId(), true);
            if (!instance.backendBungeecordMode) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> notifyOfflineBalanceChange(lotteryPlayer), 20);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (activeBossBar != null) {
            activeBossBar.removePlayer(event.getPlayer());
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> instance.getLotteryPlayerManager().unloadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }
}
