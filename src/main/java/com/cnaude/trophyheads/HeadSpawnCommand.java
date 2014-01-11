/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cnaude.trophyheads;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 *
 * @author cnaude
 */
public class HeadSpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("trophyheads.spawn")) {
                String pName = player.getName();
                int count = 1;
                if (args.length >= 1) {
                    pName = args[0];
                    if (args.length == 2) {
                        if (args[1].matches("\\d+")) {
                            count = Integer.parseInt(args[1]);
                        }
                    }
                }
                ItemStack item = new ItemStack(Material.SKULL_ITEM, count, (byte) 3);
                Location loc = player.getLocation().clone();
                World world = loc.getWorld();
                ItemMeta itemMeta = item.getItemMeta();
                ((SkullMeta) itemMeta).setOwner(pName);
                item.setItemMeta(itemMeta);
                if (player.getInventory().firstEmpty() > -1) {
                    player.sendMessage("Placed " + ChatColor.GOLD
                            + pName + "'s head " + ChatColor.RESET + " in your inventory.");
                    player.getInventory().setItem(player.getInventory().firstEmpty(), item);
                } else {
                    player.sendMessage("Dropped " + ChatColor.GOLD
                            + pName + "'s head" + ChatColor.RESET
                            + " on the ground because your inventory was full.");
                    world.dropItemNaturally(loc, item);
                }

            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");

            }
        } else {
            sender.sendMessage(ChatColor.RED + "Only a player can use this command!");
        }
        return true;
    }
}
