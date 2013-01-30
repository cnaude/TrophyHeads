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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author cnaude
 */
public class THMain extends JavaPlugin implements Listener {
    public static final String PLUGIN_NAME = "TrophyHeads";
    public static final String LOG_HEADER = "[" + PLUGIN_NAME + "]";
    static final Logger log = Logger.getLogger("Minecraft"); 
    private static Random randomGenerator;
        
    private File pluginFolder;
    private File configFile;
    
    private static int dropChance = 100;    
    
    @Override
    public void onEnable() {
        randomGenerator = new Random();
        pluginFolder = getDataFolder();
        configFile = new File(pluginFolder, "config.yml");
        createConfig();
        this.getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);   
    }
        
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {                                            
        if (randomGenerator.nextInt(100) > dropChance) {
            return;
        }
    
        Player player = event.getEntity();
        DamageCause dc = player.getLastDamageCause().getCause();
        Location loc = player.getLocation().clone();
        World world = loc.getWorld();
        String pName = player.getName(); 
        
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);        
        ItemMeta itemMeta = item.getItemMeta();
        ArrayList<String> itemDesc = new ArrayList<String>();
        itemMeta.setDisplayName("Head of " + pName);
        
        if (dc == DamageCause.ENTITY_ATTACK) {
            if(event.getEntity().getLastDamageCause().getEntity() instanceof Player){
                Player pk = (Player)event.getEntity().getKiller();   
                itemDesc.add("Killed by " + pk.getName());
            } else {
                Entity en = event.getEntity().getKiller();
                itemDesc.add("Killed by " + en.getType().getName());
            }
        } else {            
            itemDesc.add("Killed by " + dc.name());
        }
        itemMeta.setLore(itemDesc);
        item.setItemMeta(itemMeta);
        world.dropItemNaturally(loc,item); 
    }
    
    private void createConfig() {
        if (!pluginFolder.exists()) {
            try {
                pluginFolder.mkdir();
            } catch (Exception e) {
                logInfo("ERROR: " + e.getMessage());                
            }
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                logInfo("ERROR: " + e.getMessage());
            }
        }
    }
        
    private void loadConfig() {
        dropChance = getConfig().getInt("drop-chance");
        logInfo("Chance to drop head: " + dropChance + "%");
    }
            
    public void logInfo(String _message) {
        log.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logError(String _message) {
        log.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }
}
