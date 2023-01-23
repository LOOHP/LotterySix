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

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.DARK_GREEN + "LotterySix written by LOOHP!");
            sender.sendMessage(ChatColor.GOLD + "You are running LotterySix version: " + LotterySixPlugin.plugin.getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("lotterysix.reload")) {
                LotterySixPlugin.getInstance().reloadConfig();
                sender.sendMessage(LotterySixPlugin.getInstance().messageReloaded);
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("play")) {
            if (sender.hasPermission("lotterysix.play")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (sender instanceof Player) {
                    LotterySixPlugin.getGuiProvider().getMainMenu().show((Player) sender);
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoConsole);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (sender.hasPermission("lotterysix.start")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    if (args.length > 1) {
                        try {
                            LotterySixPlugin.getInstance().startNewGame(Long.parseLong(args[1]));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(LotterySixPlugin.getInstance().messageInvalidUsage);
                        }
                    } else {
                        LotterySixPlugin.getInstance().startNewGame();
                    }
                } else {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameAlreadyRunning);
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("run")) {
            if (sender.hasPermission("lotterysix.run")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                } else {
                    LotterySixPlugin.getInstance().runCurrentGame();
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("cancel")) {
            if (sender.hasPermission("lotterysix.cancel")) {
                if (LotterySixPlugin.getInstance().isGameLocked()) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageGameLocked);
                    return true;
                }
                if (LotterySixPlugin.getInstance().getCurrentGame() == null) {
                    sender.sendMessage(LotterySixPlugin.getInstance().messageNoGameRunning);
                } else {
                    LotterySixPlugin.getInstance().cancelCurrentGame();
                }
            } else {
                sender.sendMessage(LotterySixPlugin.getInstance().messageNoPermission);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> tab = new LinkedList<>();

        switch (args.length) {
            case 0:
                if (sender.hasPermission("lotterysix.reload")) {
                    tab.add("reload");
                }
                if (sender.hasPermission("lotterysix.play")) {
                    tab.add("play");
                }
                if (sender.hasPermission("lotterysix.start")) {
                    tab.add("start");
                }
                if (sender.hasPermission("lotterysix.run")) {
                    tab.add("run");
                }
                if (sender.hasPermission("lotterysix.cancel")) {
                    tab.add("cancel");
                }
                return tab;
            case 1:
                if (sender.hasPermission("lotterysix.reload")) {
                    if ("reload".startsWith(args[0].toLowerCase())) {
                        tab.add("reload");
                    }
                }
                if (sender.hasPermission("lotterysix.play")) {
                    if ("play".startsWith(args[0].toLowerCase())) {
                        tab.add("play");
                    }
                }
                if (sender.hasPermission("lotterysix.start")) {
                    if ("start".startsWith(args[0].toLowerCase())) {
                        tab.add("start");
                    }
                }
                if (sender.hasPermission("lotterysix.run")) {
                    if ("run".startsWith(args[0].toLowerCase())) {
                        tab.add("run");
                    }
                }
                if (sender.hasPermission("lotterysix.cancel")) {
                    if ("cancel".startsWith(args[0].toLowerCase())) {
                        tab.add("cancel");
                    }
                }
                return tab;
            default:
                return tab;
        }
    }

}
