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
import com.loohp.lotterysix.game.objects.PlayerBets;
import com.loohp.lotterysix.game.objects.PlayerWinnings;
import com.loohp.lotterysix.game.lottery.PlayableLotterySixGame;
import com.loohp.lotterysix.metrics.Charts;
import com.loohp.lotterysix.metrics.Metrics;
import com.loohp.lotterysix.placeholderapi.LotteryPlaceholders;
import com.loohp.lotterysix.utils.ChatColorUtils;
import com.loohp.lotterysix.utils.LotteryUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class LotterySixPlugin extends JavaPlugin implements Listener {

    public static final int BSTATS_PLUGIN_ID = 17516;
    public static final String CONFIG_ID = "config";

    public static LotterySixPlugin plugin;

    public static DiscordSRVHook discordSRVHook = null;

    private static LotterySix instance;
    private static LotteryPluginGUI guiProvider;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        plugin = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        getServer().getPluginManager().registerEvents(new Debug(), this);
        getServer().getPluginManager().registerEvents(this, this);

        try {
            Config.loadConfig(CONFIG_ID, new File(getDataFolder(), "config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), getClass().getClassLoader().getResourceAsStream("config.yml"), true);
        } catch (IOException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp.getProvider();

        getCommand("lotterysix").setExecutor(new Commands());

        instance = new LotterySix(getDataFolder(), CONFIG_ID, c -> givePrizes(c), c -> refundBets(c), b -> takeMoney(b), () -> forceCloseAllGui(), () -> Collections2.transform(Bukkit.getOnlinePlayers(), p -> p.getUniqueId()), (uuid, message, game) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (game instanceof PlayableLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (PlayableLotterySixGame) game);
                } else if (game instanceof CompletedLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (CompletedLotterySixGame) game);
                }
                player.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', message));
            }
        }, (uuid, message, game) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (game instanceof PlayableLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (PlayableLotterySixGame) game);
                } else if (game instanceof CompletedLotterySixGame) {
                    message = LotteryUtils.formatPlaceholders(player, message, instance, (CompletedLotterySixGame) game);
                }
                player.sendTitle(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, message)), "", 10, 100, 20);
            }
        }, (uuid, numbers) -> {
            Bukkit.getPluginManager().callEvent(new PlayerBetEvent(Bukkit.getPlayer(uuid), numbers));
        }, action -> {
            Bukkit.getPluginManager().callEvent(new LotterySixEvent(instance, action));
        });
        instance.reloadConfig();
        guiProvider = new LotteryPluginGUI(this);

        Charts.setup(metrics);

        new LotteryPlaceholders().register();

        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            getServer().getPluginManager().registerEvents(discordSRVHook = new DiscordSRVHook(), this);
            getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "[LotterySix] LotterySix has hooked into DiscordSRV!");
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

    public static Economy getEcon() {
        return econ;
    }

    public static void givePrizes(Collection<PlayerWinnings> winnings) {
        for (PlayerWinnings winning : winnings) {
            econ.depositPlayer(Bukkit.getOfflinePlayer(winning.getPlayer()), winning.getWinnings());
        }
    }

    public static void refundBets(Collection<PlayerBets> bets) {
        for (PlayerBets bet : bets) {
            econ.depositPlayer(Bukkit.getOfflinePlayer(bet.getPlayer()), bet.getBet());
        }
    }

    public static boolean takeMoney(PlayerBets bet) {
        return econ.withdrawPlayer(Bukkit.getOfflinePlayer(bet.getPlayer()), bet.getBet()).transactionSuccess();
    }

    public static void forceCloseAllGui() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            guiProvider.forceClose(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> instance.getPlayerPreferenceManager().loadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> instance.getPlayerPreferenceManager().unloadLotteryPlayer(event.getPlayer().getUniqueId(), true));
    }
}
