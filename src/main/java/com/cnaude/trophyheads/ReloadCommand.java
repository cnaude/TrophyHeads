/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cnaude.trophyheads;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author cnaude
 */
public class ReloadCommand implements CommandExecutor {

    private final TrophyHeads plugin;

    public ReloadCommand(TrophyHeads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender.hasPermission("trophyheads.reload")) {
            plugin.reloadMainConfig(sender);
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
        }
        return true;
    }
}
