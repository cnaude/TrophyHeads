/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.cnaude.plugin.TrophyHeads;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author cnaude
 */
public class THMain extends JavaPlugin implements Listener {        
    public static String LOG_HEADER;
    static final Logger log = Logger.getLogger("Minecraft"); 
    private static Random randomGenerator;
        
    private File pluginFolder;
    private File configFile;
    
    private static int dropChance = 100;
    private static ArrayList<String> deathTypes = new ArrayList<String>();
    private static boolean debugEnabled = false;
    private static boolean playerSkin = true;
    private static boolean sneakPunchInfo = true;
    
    @Override
    public void onEnable() {
        LOG_HEADER = "[" + this.getName() + "]";
        randomGenerator = new Random();
        pluginFolder = getDataFolder();
        configFile = new File(pluginFolder, "config.yml");
        createConfig();
        this.getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);   
        getCommand("headspawn").setExecutor(this);
    }
        
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
                ((SkullMeta)itemMeta).setOwner(pName);
                item.setItemMeta(itemMeta);
                world.dropItemNaturally(loc,item); 
                
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                
            }
        } else {
            sender.sendMessage("Only a player can use this command!");
        }
        return true;
    }
    
    /*
    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {        
        if (!sneakPunchInfo) {            
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {            
            return;
        }
        Block block = player.getTargetBlock(null, 5);
        if (block == null) {
            block = event.getClickedBlock();
        }        
        if (block.getType() == Material.SKULL) {
            logDebug("Player sneak clicked on a valid Skull");
            org.bukkit.block.Skull skull = (org.bukkit.block.Skull)block;
            String pName = "Unknown";            
            try {
                logDebug("About to check for owner!");
                if (skull.hasOwner()) {
                    logDebug("Skull block has owner");
                    pName = skull.getOwner();
                } else {
                    logDebug("Skull block has NO owner");
                }
                player.sendMessage(ChatColor.YELLOW + "This head once belonged to " + ChatColor.RED + pName);
            }
            catch (Exception e) {
                logError(e.getMessage());
            }
            finally {
                logDebug("Finally!");
            }
        } else {
            logDebug("Player sneak clicked on an invalid Skull");
        }
    }
    */
    
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {        
        if (randomGenerator.nextInt(100) > dropChance) {
            return;
        }        
        Player player = (Player)event.getEntity();
        if (!player.hasPermission("trophyheads.drop")) {
            return;
        }
        DamageCause dc = player.getLastDamageCause().getCause();        
        logDebug("DamageCause: " + dc.toString());        
        
        if (deathTypes.contains(dc.toString())
                || deathTypes.contains("ALL")
                || (deathTypes.contains("PVP") && player.getKiller() instanceof Player)) {
            logDebug("Match: true");
            Location loc = player.getLocation().clone();
            World world = loc.getWorld();
            String pName = player.getName(); 

            ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);        
            ItemMeta itemMeta = item.getItemMeta();
            ArrayList<String> itemDesc = new ArrayList<String>();
            itemMeta.setDisplayName("Head of " + pName);
            itemDesc.add(event.getDeathMessage());
            itemMeta.setLore(itemDesc);            
            if (playerSkin) {
                ((SkullMeta)itemMeta).setOwner(pName);
            }
            item.setItemMeta(itemMeta);
            world.dropItemNaturally(loc,item); 
        } else {
            logDebug("Match: false");
        }
    }
    
    private void createConfig() {
        if (!pluginFolder.exists()) {
            try {
                pluginFolder.mkdir();
            } catch (Exception e) {
                logError(e.getMessage());                
            }
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }
    }
        
    private void loadConfig() {
        debugEnabled = getConfig().getBoolean("debug-enabled");
        logDebug("Debug enabled");
        dropChance = getConfig().getInt("drop-chance");
        logDebug("Chance to drop head: " + dropChance + "%");
        for (String type : getConfig().getStringList("death-types")) {
            deathTypes.add(type);                
            logDebug("Death type: " + type);
        }     
        playerSkin = getConfig().getBoolean("player-skin");
        logDebug("Player skins: " + playerSkin);
        sneakPunchInfo = getConfig().getBoolean("sneak-punch-info");
        logDebug("Sneak punch info: " + sneakPunchInfo);
    }
            
    public void logInfo(String _message) {
        log.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logError(String _message) {
        log.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }
    
    public void logDebug(String _message) {
        if (debugEnabled) {
            log.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, _message));
        }
    }
}
