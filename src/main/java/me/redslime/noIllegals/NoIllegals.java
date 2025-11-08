package me.redslime.noIllegals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.lushplugins.pluginupdater.api.updater.Updater;

import com.google.gson.Gson;

public final class NoIllegals extends JavaPlugin implements Listener {

    private static long lastRequestTime = 0;
    private boolean fixPotions, noIllegalBlocks, fixIllegals, fixOverstack, fixAttribute, fixUnbreakable, antichestnbt, creativechestlock, nospawneggs;
    private FileConfiguration config;
    private String webhookUrl;


    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfigValues();
        System.out.println("[NoIllegals]: Enabled!");
        autoUpdate();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("illegal").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                if (!player.hasPermission("illegal.cmd")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    return false;
                }
                reloadConfig();
                reloadConfigValues();
                player.sendMessage(ChatColor.GREEN + "Reloaded Config!");
                return true;
            }
            return false;
        });
    }

    private void reloadConfigValues() {
        webhookUrl = config.getString("webhook-url");
        fixPotions = config.getBoolean("fixPotions");
        noIllegalBlocks = config.getBoolean("noIllegalBlocks");
        fixIllegals = config.getBoolean("fixIllegals");
        fixOverstack = config.getBoolean("fixOverstack");
        fixAttribute = config.getBoolean("fixAttribute");
        fixUnbreakable = config.getBoolean("fixUnbreakable");
        antichestnbt = config.getBoolean("antichestnbt");
        creativechestlock = config.getBoolean("creativechestlock");
        nospawneggs = config.getBoolean("nospawneggs");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if(!player.hasPermission("illegal.bypass") && player.getGameMode() == GameMode.CREATIVE && antichestnbt){
            if (event.getBlock().getState() instanceof Container container) {
                if (!container.getInventory().isEmpty()){            
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot place container blocks with NBT data!");
                    alertUsers("Player " + player.getName() + " tried to place container item with inventory data!:" + item.getType().name());
                    sendAlert("Player " + player.getName() + " tried to place container item with inventory data!:" + item.getType().name());

                }
            }
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event){
        if(event.getSpawnReason() == SpawnReason.SPAWNER_EGG){
            event.setCancelled(true);
            alertUsers("A mob was spawned due to a spawn egg! This was prevented.");
            sendAlert("A mob was spawned due to a spawn egg! This was prevented.");
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        if(!event.getPlayer().hasPermission("illegal.bypass")){
            checkItem(event.getItem());
            if(!event.getMaterial().isAir()) {
                if (getConfig().getBoolean("noIllegalBlocks") && (event.getMaterial() == Material.BEDROCK || event.getMaterial() == Material.BARRIER || event.getMaterial() == Material.LIGHT || event.getMaterial() == Material.STRUCTURE_VOID || event.getMaterial() == Material.STRUCTURE_BLOCK || event.getMaterial() == Material.END_PORTAL_FRAME || event.getMaterial() == Material.SPAWNER || event.getMaterial() == Material.REINFORCED_DEEPSLATE || event.getMaterial() == Material.COMMAND_BLOCK_MINECART)) {
                    event.setCancelled(true);
                }
            }
            if (event.getItem() == null && event.getMaterial().isAir()) return;
            if(event.getMaterial() == Material.POTION || event.getMaterial() == Material.SPLASH_POTION || event.getMaterial() == Material.LINGERING_POTION){
                if(event.getItem().getItemMeta() != null && event.getItem().hasItemMeta()){
                    PotionMeta potionMeta = (PotionMeta) event.getItem().getItemMeta();
                    if (potionMeta != null && potionMeta.hasCustomEffects() && fixPotions) {
                        sendAlert("Illegal potion " + event.getItem().getType().name() + " removed.");
                        alertUsers(ChatColor.GREEN + "Illegal potion " + event.getItem().getType().name() + " removed.");
                        event.getItem().setAmount(0);
                        event.setCancelled(true);
                    }
                }
            }
            if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem().toString().contains("SPAWN_EGG") && event.getClickedBlock().getType() == Material.SPAWNER && !event.getPlayer().hasPermission("illegal.bypass")){
                event.setCancelled(true);
                alertUsers(event.getPlayer().getName() + "tried to use a spawn egg with a spawner!");
                sendAlert(event.getPlayer().getName() + "tried to use a spawn egg with a spawner!");
            }
        }
    }

    @EventHandler
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        List<PotionEffect> allEffects = new ArrayList<>();

        PotionType baseType = arrow.getBasePotionType();
        if (baseType != null) {
            allEffects.addAll(baseType.getPotionEffects());
        }

        allEffects.addAll(arrow.getCustomEffects());

        for (PotionEffect effect : allEffects) {
            if (effect.getAmplifier() > 1) {
                arrow.removeCustomEffect(effect.getType());
                arrow.addCustomEffect(new PotionEffect(
                        effect.getType(),
                        effect.getDuration(),
                        1,
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon()
                ), true);
            }
        }
    }


    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event){
        if(event.getPlayer().getGameMode() == GameMode.CREATIVE && creativechestlock && !event.getPlayer().hasPermission("illegal.bypass")){
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to drop items in creative!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player plr){
            if(!plr.hasPermission("illegal.bypass")){
                checkItem(event.getCurrentItem());
            }
        } else {
            checkItem(event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if(event.getPlayer().getGameMode() == GameMode.CREATIVE && !event.getPlayer().hasPermission("illegal.bypass")){
            event.getPlayer().sendMessage(ChatColor.RED + "You cant open containers in creative!");
            event.setCancelled(true);
            return;
        }
        if(!event.getPlayer().hasPermission("illegal.bypass")){
            checkInventory(event.getInventory());
        }
    }

    private void checkInventory(Inventory inventory) {
        if(inventory.getHolder() instanceof Player plr){
            if(plr.hasPermission("illegal.bypass")){
                return;
            }
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                if (checkItem(item)) {
                    inventory.setItem(i, item);
                }
            }
        }
    }

    private boolean checkItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.AIR) return false;
        boolean modified = false;

        if (fixPotions && (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            if (potionMeta != null && potionMeta.hasCustomEffects()) {
                sendAlert("Illegal potion " + item.getType().name() + " removed.");
                alertUsers(ChatColor.GREEN + "Illegal potion " + item.getType().name() + " removed.");
                item.setAmount(0);
                modified = true;
            }

            if(item.getType() == Material.TIPPED_ARROW){
                if(item.hasItemMeta()){
                    PotionMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasCustomEffects()){
                        sendAlert("Illegal potion arrow " + item.getType().name() + " removed.");
                        alertUsers(ChatColor.GREEN + "Illegal potion arrow " + item.getType().name() + " removed.");
                        item.setAmount(0);
                        modified = true;
                    }
                }
            }
        }
        
        if(nospawneggs && item.getType().toString().contains("SPAWN_EGG")){
            alertUsers("Illegal Spawn Egg " + item.getType().name() + " Has been Removed.");
            sendAlert("Illegal Spawn Egg " + item.getType().name() + " Has been Removed.");
            item.setAmount(0);
            modified = true;
        }

        if (noIllegalBlocks && (item.getType() == Material.BEDROCK || item.getType() == Material.BARRIER ||
                item.getType() == Material.LIGHT || item.getType() == Material.STRUCTURE_BLOCK ||
                item.getType() == Material.STRUCTURE_VOID || item.getType() == Material.SPAWNER ||
                item.getType() == Material.END_PORTAL_FRAME || 
                item.getType() == Material.REINFORCED_DEEPSLATE || 
                item.getType() == Material.COMMAND_BLOCK_MINECART)) {
            sendAlert("Illegal item " + item.getType().name() + " removed.");
            alertUsers(ChatColor.GREEN + "Illegal item " + item.getType().name() + " removed.");
            item.setAmount(0);
            modified = true;
        }

        if (fixIllegals) {
            for (Enchantment ench : item.getEnchantments().keySet()) {
                int level = item.getEnchantmentLevel(ench);
                if (level > ench.getMaxLevel()) {
                    sendAlert("Illegal enchantment " + ench + "(" + level + ") fixed.");
                    alertUsers(ChatColor.GREEN + "Illegal enchantment " + ench + "(" + level + ") fixed.");
                    item.addUnsafeEnchantment(ench, ench.getMaxLevel());
                    modified = true;
                }
                if (!ench.canEnchantItem(item)) {
                    sendAlert("Illegal enchantment " + ench + "(" + level + ") removed.");
                    alertUsers(ChatColor.GREEN + "Illegal enchantment " + ench + "(" + level + ") removed.");
                    item.removeEnchantment(ench);
                    modified = true;
                }
            }

            if(item.getType() == Material.ENCHANTED_BOOK){
                if(item.hasItemMeta()){
                    EnchantmentStorageMeta meta = item.getItemMeta();
                    Map<Enchantment, Integer> enchants = meta.getStoredEnchants();
                    
                    for (Map.Entry<Enchantment, Integer> enchant : stored.entrySet()) {
                        int level = enchant.getValue();
                        if(level > enchant.getKey().getMaxLevel()){
                            sendAlert("Illegal enchantment book " + ench + "(" + level + ") fixed.");
                            alertUsers(ChatColor.GREEN + "Illegal enchantment book " + ench + "(" + level + ") fixed.");
                            meta.removeStoredEnchant(enchant);
                            meta.addStoredEnchant(enchant.getKey(), enchant.getKey().getMaxLevel(), true);
                            modified = true;
                        }
                    }
                }
            }
        }

        if (fixOverstack && item.getAmount() > item.getMaxStackSize()) {
            sendAlert("Overstacked item " + item.getType().name() + " fixed.");
            alertUsers(ChatColor.GREEN + "Overstacked item " + item.getType().name() + " fixed.");
            item.setAmount(item.getMaxStackSize());
            modified = true;
        }

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (fixAttribute && meta.hasAttributeModifiers()) {
                    meta.getAttributeModifiers().clear();
                    item.setItemMeta(meta);
                    sendAlert("Attribute modifiers removed from " + item.getType().name() + ".");
                    alertUsers(ChatColor.GREEN + "Attribute modifiers removed from " + item.getType().name() + ".");
                    modified = true;
                }
                if (fixUnbreakable && meta.isUnbreakable()) {
                    meta.setUnbreakable(false);
                    item.setItemMeta(meta);
                    sendAlert("Unbreakable tag removed from " + item.getType().name() + ".");
                    alertUsers(ChatColor.GREEN + "Unbreakable tag removed from " + item.getType().name() + ".");
                    modified = true;
                }
            }
        }
        return modified;
    }

    public void sendAlert(String message) {
        if(!Objects.equals(webhookUrl, "YOUR_DISCORD_WEBHOOK_URL_HERE") && !webhookUrl.isEmpty()) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastRequestTime < 1000) {
                try {
                    long sleepTime = 1000 - (currentTime - lastRequestTime);
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Bukkit.getLogger().warning("Rate limit sleep interrupted: " + e.getMessage());
                }
            }
            try {
                URL url = new URL(Objects.requireNonNull(getConfig().getString("webhook-url")));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                Map<String, String> data = new HashMap<>();
                data.put("content", message);
                String json = new Gson().toJson(data);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

            } catch (IOException e) {
                Bukkit.getLogger().warning("Failed to send webhook message: " + e.getMessage());
            }
            lastRequestTime = System.currentTimeMillis();
        }
    }

    public void alertUsers(String str){
        for (Player player : Bukkit.getOnlinePlayers()) {
            if(player.hasPermission("illegal.alerts")){
                player.sendMessage(ChatColor.GREEN + str);
            }
        }
    }

    public void autoUpdate(){
        Updater updater = new Updater.Builder(getPlugin(NoIllegals.class))
        .modrinth("5qvP2NRZ", false) // Updater platform(s)
        .build();
        if (updater.isAlreadyDownloaded() || !updater.isUpdateAvailable()) {
            Bukkit.getLogger().info("It looks like there is no new update available!");
            return;
        }

        updater.attemptDownload().thenAccept(success -> {
            if (success) {
                Bukkit.getLogger().info("Successfully updated plugin, restart the server to apply changes!");
            } else {
                Bukkit.getLogger().info("Failed to update plugin!");
            }
        });
    }

    @Override
    public void onDisable() {
        System.out.println("[NoIllegals]: Disabled!");
    }
}