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
public class HeadGiveCommand implements CommandExecutor {

    final TrophyHeads plugin;

    public HeadGiveCommand(TrophyHeads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender.hasPermission("trophyheads.give")) {
            if (args.length >= 2) {
                Player player = null;
                String pName = args[1];
                for (Player pl : plugin.getServer().getOnlinePlayers()) {
                    if (args[0].equalsIgnoreCase(pl.getName())) {
                        player = pl;
                    }
                }
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE
                            + args[0] + ChatColor.RED + " not found!");
                    return true;
                }

                int count = 1;
                if (args.length == 3) {
                    if (args[1].matches("\\d+")) {
                        count = Integer.parseInt(args[1]);
                    }
                }
                ItemStack item = new ItemStack(Material.SKULL_ITEM, count, (byte) 3);
                Location loc = player.getLocation().clone();
                World world = loc.getWorld();
                ItemMeta itemMeta = item.getItemMeta();
                ((SkullMeta) itemMeta).setOwner(pName);
                item.setItemMeta(itemMeta);
                plugin.logDebug("Skull: " + item.toString());
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
                sender.sendMessage("Usage: /headgive <player> <skull name> <count>");
            }

        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");

        }
        return true;
    }
}
