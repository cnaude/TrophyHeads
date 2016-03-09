package com.cnaude.trophyheads;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author cnaude
 */
public class TrophyHeads extends JavaPlugin implements Listener {

    public static String LOG_HEADER;
    static final Logger LOG = Logger.getLogger("Minecraft");
    private static Random randomGenerator;
    private File pluginFolder;
    private File configFile;
    private static final ArrayList<String> DEATH_TYPES = new ArrayList<>();
    private static boolean debugEnabled = false;
    private static boolean renameEnabled = false;
    private static boolean playerSkin = true;
    private static boolean sneakPunchInfo = true;
    private static boolean noBreak = true;
    private static final CaseInsensitiveMap<List<String>> ITEMS_REQUIRED = new CaseInsensitiveMap<>();
    private static final CaseInsensitiveMap<Integer> DROP_CHANCES = new CaseInsensitiveMap<>();
    private static final CaseInsensitiveMap<String> CUSTOM_SKINS = new CaseInsensitiveMap<>();
    private static final CaseInsensitiveMap<String> SKULL_MESSAGES = new CaseInsensitiveMap<>();
    private static final ArrayList<String> INFO_BLACKLIST = new ArrayList<>();
    private static Material renameItem = Material.PAPER;

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
        getCommand("headspawn").setExecutor(new HeadSpawnCommand(this));
        getCommand("headgive").setExecutor(new HeadGiveCommand(this));
        getCommand("trophyreload").setExecutor(new ReloadCommand(this));

        if (renameEnabled) {
            ItemStack resultHead = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(resultHead);
            shapelessRecipe.addIngredient(1, Material.SKULL_ITEM);
            shapelessRecipe.addIngredient(1, renameItem);
            getServer().addRecipe(shapelessRecipe);
        }
    }

    public void reloadMainConfig(CommandSender sender) {
        reloadConfig();
        getConfig().options().copyDefaults(false);
        loadConfig();
        sender.sendMessage(ChatColor.GOLD + "[TrophyHeads] "
                + ChatColor.WHITE + "Configuration reloaded.");
    }

    public String getCustomSkullType(String name) {
        if (CUSTOM_SKINS.containsKey(name)) {
            return name;
        }
        return EntityType.UNKNOWN.toString();
    }

    @EventHandler
    public void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
        if (!renameEnabled) {
            return;
        }
        if (event.getRecipe() instanceof Recipe) {
            CraftingInventory ci = event.getInventory();
            ItemStack result = ci.getResult();
            if (result == null) {
                return;
            }
            if (result.getType().equals(Material.SKULL_ITEM)) {
                for (ItemStack i : ci.getContents()) {
                    if (i.getType().equals(Material.SKULL_ITEM)) {
                        if (i.getData().getData() != (byte) 3) {
                            ci.setResult(new ItemStack(Material.AIR));
                            return;
                        }
                    }
                }
                for (ItemStack i : ci.getContents()) {
                    if (i.hasItemMeta() && i.getType().equals(renameItem)) {
                        ItemMeta im = i.getItemMeta();
                        if (im.hasDisplayName()) {
                            ItemStack res = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
                            ItemMeta itemMeta = res.getItemMeta();
                            ((SkullMeta) itemMeta).setOwner(im.getDisplayName());
                            res.setItemMeta(itemMeta);
                            ci.setResult(res);
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (!sneakPunchInfo) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (!player.hasPermission("trophyheads.info")) {
            return;
        }
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            org.bukkit.block.Block block = event.getClickedBlock();
            logDebug("Left clicked: " + block.getType().name());
            if (block.getType().equals(Material.SKULL)) {
                BlockState bs = block.getState();
                logDebug("Block state: " + bs.toString());
                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) bs;
                String pName = "Unknown";
                String message;
                logDebug("Skull type: " + skull.getSkullType().name());
                if (skull.getSkullType().equals(SkullType.PLAYER)) {
                    if (skull.hasOwner()) {
                        pName = skull.getOwner();
                        if (CUSTOM_SKINS.containsValue(pName)) {
                            message = SKULL_MESSAGES.get(getCustomSkullType(pName));
                        } else {
                            message = SKULL_MESSAGES.get(EntityType.PLAYER.name());
                        }
                    } else {
                        message = SKULL_MESSAGES.get(EntityType.PLAYER.toString());
                    }
                } else if (skull.getSkullType().toString().equals(SkullType.CREEPER.toString())) {
                    message = SKULL_MESSAGES.get(EntityType.CREEPER.toString());
                } else if (skull.getSkullType().toString().equals(SkullType.SKELETON.toString())) {
                    message = SKULL_MESSAGES.get(EntityType.SKELETON.toString());
                } else if (skull.getSkullType().toString().equals(SkullType.WITHER.toString())) {
                    message = SKULL_MESSAGES.get(EntityType.WITHER.toString());
                } else if (skull.getSkullType().toString().equals(SkullType.ZOMBIE.toString())) {
                    message = SKULL_MESSAGES.get(EntityType.ZOMBIE.toString());
                } else {
                    message = SKULL_MESSAGES.get(EntityType.PLAYER.toString());
                }
                if (pName == null) {
                    pName = "Unknown";
                }
                if (message == null) {
                    message = "";
                }
                if (INFO_BLACKLIST.contains(pName.toLowerCase())) {
                    logDebug("Ignoring: " + pName);
                } else {
                    message = message.replaceAll("%%NAME%%", pName);
                    message = ChatColor.translateAlternateColorCodes('&', message);
                    player.sendMessage(message);
                }
                event.setCancelled(noBreak);
            }
        }
    }

    public boolean isValidItem(EntityType et, Material mat) {
        if (et == null || mat == null) {
            return false;
        }
        try {
            if (ITEMS_REQUIRED.containsKey(et.name())) {
                if (ITEMS_REQUIRED.get(et.name()).contains("ANY")) {
                    return true;
                }
                if (ITEMS_REQUIRED.get(et.name()).contains(String.valueOf(mat.getId()))) {
                    return true;
                } else {
                    for (String s : ITEMS_REQUIRED.get(et.name())) {
                        if (s.toUpperCase().equals(mat.toString())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logDebug("isValidItem: Catching exception: " + ex.getMessage()
                    + " [: " + et.name() + "] [" + mat.name() + "] [" + ITEMS_REQUIRED.size() + "]");
            return false;
        }
        return false;
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        Player player = (Player) event.getEntity();
        if (!player.hasPermission("trophyheads.drop")) {
            return;
        }
        if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(EntityType.PLAYER.toString())) {
            return;
        }

        boolean dropOkay = false;
        DamageCause dc;
        if (player.getLastDamageCause() != null) {
            dc = player.getLastDamageCause().getCause();
            logDebug("DamageCause: " + dc.toString());
        } else {
            logDebug("DamageCause: NULL");
            return;
        }

        if (DEATH_TYPES.contains(dc.toString())) {
            dropOkay = true;
        }
        if (DEATH_TYPES.contains("ALL")) {
            dropOkay = true;
        }

        if (player.getKiller() instanceof Player) {
            logDebug("Player " + player.getName() + " killed by another player. Checking if PVP is valid death type.");
            if (DEATH_TYPES.contains("PVP")) {
                dropOkay = isValidItem(EntityType.PLAYER, player.getKiller().getItemInHand().getType());
                logDebug("PVP is a valid death type. Killer's item in hand is valid? " + dropOkay);
            } else {
                logDebug("PVP is not a valid death type.");
            }
        }

        if (dropOkay) {
            logDebug("Match: true");
            Location loc = player.getLocation().clone();
            World world = loc.getWorld();
            String pName = player.getName();

            ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
            ItemMeta itemMeta = item.getItemMeta();
            ArrayList<String> itemDesc = new ArrayList<>();
            itemMeta.setDisplayName("Head of " + pName);
            itemDesc.add(event.getDeathMessage());
            itemMeta.setLore(itemDesc);
            if (playerSkin) {
                ((SkullMeta) itemMeta).setOwner(pName);
            }
            item.setItemMeta(itemMeta);
            world.dropItemNaturally(loc, item);
        } else {
            logDebug("Match: false");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        if (event.isCancelled()) {
            logDebug("TH: Block break cancel detected.");
            return;
        }
        logDebug("TH: No cancel detected.");
        org.bukkit.block.Block block = event.getBlock();
        if (event.getPlayer() instanceof Player) {
            if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                return;
            }
        }
        if (block.getType() == Material.SKULL) {
            org.bukkit.block.Skull skull = (org.bukkit.block.Skull) block.getState();
            if (skull.getSkullType().equals(SkullType.PLAYER)) {
                if (skull.hasOwner()) {
                    String pName = skull.getOwner();
                    if (CUSTOM_SKINS.containsValue(pName)) {
                        Location loc = block.getLocation().clone();
                        event.setCancelled(true);
                        block.setType(Material.AIR);
                        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
                        ItemMeta itemMeta = item.getItemMeta();
                        ((SkullMeta) itemMeta).setOwner(pName);
                        itemMeta.setDisplayName(ChatColor.GREEN + getCustomSkullType(pName) + " Head");
                        item.setItemMeta(itemMeta);

                        World world = loc.getWorld();
                        world.dropItemNaturally(loc, item);
                    }

                }
            }
        }
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        Player player;
        EntityType entityType = event.getEntityType();
        String entityName = entityType.name();
        Entity entity = event.getEntity();
        int skullType;
        boolean dropOkay;
        
        if (entityType.equals(EntityType.PLAYER)) {
            return;
        }
        
        Material material = Material.AIR;
        if (((LivingEntity) entity).getKiller() instanceof Player) {
            player = (Player) ((LivingEntity) entity).getKiller();
            material = player.getItemInHand().getType();
        }

        dropOkay = isValidItem(entityType, material);

        if (entityType.equals(EntityType.SKELETON)) {
            if (((Skeleton) entity).getSkeletonType().equals(SkeletonType.NORMAL)) {
                if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(entityType.name())) {
                    return;
                }
                skullType = 0;
            } else {
                return;
            }
        } else if (entityType.equals(EntityType.ZOMBIE)) {
            if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(entityType.name())) {
                return;
            }
            skullType = 2;
        } else if (entityType.equals(EntityType.CREEPER)) {
            if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(entityType.name())) {
                return;
            }
            skullType = 4;
        } else if (entityType.equals(EntityType.ENDER_DRAGON)) {
            if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(entityType.name())) {
                return;
            }
            skullType = 5;
        } else if (DROP_CHANCES.containsKey(entityType.name())) {
            if (randomGenerator.nextInt(100) >= DROP_CHANCES.get(entityType.name())) {
                return;
            }
            skullType = 3;
        } else {
            return;
        }

        if (!dropOkay) {
            return;
        }

        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) skullType);
        
        if (entityType.equals(EntityType.GUARDIAN)) {
            if (((Guardian) entity).isElder()) {
                entityName = "ElderGuardian";
            }
        }
        if (skullType == 3 || CUSTOM_SKINS.containsKey(entityName)) {
            logDebug("Dropping: [skin: " + CUSTOM_SKINS.get(entityName) + "] "
                    + "[etName: " + entityName + "] [etStr: " + entityType.toString() + "]");
            if (!CUSTOM_SKINS.get(entityType.name()).equalsIgnoreCase("@default")) {
                ItemMeta itemMeta = item.getItemMeta();
                ((SkullMeta) itemMeta).setOwner(CUSTOM_SKINS.get(entityName));
                itemMeta.setDisplayName(entityName + " Head");
                item.setItemMeta(itemMeta);
            }
        }

        Location loc = entity.getLocation().clone();
        World world = loc.getWorld();
        world.dropItemNaturally(loc, item);
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
            } catch (IOException e) {
                logError(e.getMessage());
            }
        }
    }

    private void loadConfig() {
        debugEnabled = getConfig().getBoolean("debug-enabled");
        logDebug("Debug enabled");

        DROP_CHANCES.put(EntityType.PLAYER.toString(), getConfig().getInt("drop-chance"));
        logDebug("Chance to drop head: " + DROP_CHANCES.get(EntityType.PLAYER.toString()) + "%");

        playerSkin = getConfig().getBoolean("player-skin");
        logDebug("Player skins: " + playerSkin);

        sneakPunchInfo = getConfig().getBoolean("sneak-punch-info");
        logDebug("Sneak punch info: " + sneakPunchInfo);

        noBreak = getConfig().getBoolean("sneak-punch-no-break");
        logDebug("Sneak punch no break: " + noBreak);

        List<String> pItems = getConfig().getStringList("items-required");
        if (pItems.isEmpty()) {
            pItems.add("ANY");
            pItems.add("276");
        }

        ITEMS_REQUIRED.put(EntityType.PLAYER.toString(), pItems);
        logDebug("Player items required: " + ITEMS_REQUIRED.get(EntityType.PLAYER.toString()));

        for (String monsterName : getConfig().getConfigurationSection("custom-heads").getKeys(false)) {
            logDebug("Entity Name: " + monsterName);
            String entityName;
            if (monsterName.equalsIgnoreCase("CaveSpider")) {
                entityName = "CAVE_SPIDER";
            } else if (monsterName.equalsIgnoreCase("Golem") || monsterName.equalsIgnoreCase("IronGolem")) {
                entityName = "IRON_GOLEM";
            } else if (monsterName.equalsIgnoreCase("MushroomCow")) {
                entityName = "MUSHROOM_COW";
            } else if (monsterName.equalsIgnoreCase("PigZombie")) {
                entityName = "PIG_ZOMBIE";
            } else if (monsterName.equalsIgnoreCase("LavaSlime")) {
                entityName = "MAGMA_CUBE";
            } else if (monsterName.equalsIgnoreCase("EnderDragon")) {
                entityName = "ENDER_DRAGON";
            } else if (monsterName.equalsIgnoreCase("ElderGuardian")) {
                entityName = "ELDERGUARDIAN";
            } else {
                entityName = monsterName;
            }

            logDebug("  Type: " + entityName);
            int dropChance = getConfig().getInt("custom-heads." + monsterName + ".drop-chance", 0);
            List<String> items = getConfig().getStringList("custom-heads." + monsterName + ".items-required");
            if (items.isEmpty()) {
                items.add("ANY");
                items.add("276");
            }
            String skin = getConfig().getString("custom-heads." + monsterName + ".skin", "MHF_" + monsterName);
            String message = getConfig().getString("custom-heads." + monsterName + ".message", "&eThis head once belonged to a &e" + monsterName + "&e.");

            DROP_CHANCES.put(entityName, dropChance);
            logDebug("  Chance to drop head: " + DROP_CHANCES.get(entityName) + "%");

            ITEMS_REQUIRED.put(entityName, items);
            logDebug("  Items required: " + ITEMS_REQUIRED.get(entityName));

            CUSTOM_SKINS.put(entityName, skin);
            logDebug("  Skin: " + CUSTOM_SKINS.get(entityName));

            SKULL_MESSAGES.put(entityName, message);
            logDebug("  Message: " + SKULL_MESSAGES.get(entityName));

        }

        SKULL_MESSAGES.put(EntityType.PLAYER.toString(), getConfig().getString("message"));

        renameEnabled = getConfig().getBoolean("rename-enabled");
        if (renameEnabled) {
            try {
                renameItem = Material.getMaterial(getConfig().getInt("rename-item"));
            } catch (Exception e) {
                renameItem = Material.PAPER;
            }
            logDebug("Rename recipe enabled: head + " + renameItem.toString());
        }
        DEATH_TYPES.addAll(getConfig().getStringList("death-types"));

        INFO_BLACKLIST.clear();
        for (String name : getConfig().getStringList("info-blacklist")) {
            INFO_BLACKLIST.add(name.toLowerCase());
            logDebug("Blacklisting: " + name.toLowerCase());
        }
    }

    public void logInfo(String _message) {
        LOG.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logError(String _message) {
        LOG.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logDebug(String _message) {
        if (debugEnabled) {
            LOG.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, _message));
        }
    }
}
