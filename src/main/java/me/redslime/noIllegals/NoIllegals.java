package me.redslime.noIllegals;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class NoIllegals extends JavaPlugin implements Listener {

    private static long lastRequestTime = 0;
    private boolean fixPotions, noIllegalBlocks, fixIllegals, fixOverstack, fixAttribute, fixUnbreakable, antichestnbt;
    private FileConfiguration config;
    private String webhookUrl;


    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        reloadConfigValues();
        System.out.println("[NoIllegals]: Enabled!");
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
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if(!player.hasPermission("illegal.bypass") && player.getGameMode() == GameMode.CREATIVE && antichestnbt){
            if (isContainer(item)) { // Use the isContainer method here
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place container blocks with NBT data!");
                sendAlert("Player " + player.getName() + " tried to place container item: " + item.getType().name());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        if(!event.getPlayer().hasPermission("illegal.bypass")){
            checkItem(event.getItem());
            if(!event.getMaterial().isAir()) {
                if (getConfig().getBoolean("noIllegalBlocks") && (event.getMaterial() == Material.BEDROCK || event.getMaterial() == Material.BARRIER || event.getMaterial() == Material.LIGHT || event.getMaterial() == Material.STRUCTURE_VOID || event.getMaterial() == Material.STRUCTURE_BLOCK || event.getMaterial() == Material.END_PORTAL_FRAME || event.getMaterial() == Material.SPAWNER)) {
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
        checkInventory(event.getInventory());
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
    private boolean isContainer(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            BlockState blockState = blockStateMeta.getBlockState();
            if (blockState instanceof InventoryHolder holder) {
                Inventory inventory = holder.getInventory();
                for (ItemStack content : inventory.getContents()) {
                    if (content != null && content.getType() != Material.AIR) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
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
        }

        if (noIllegalBlocks && (item.getType() == Material.BEDROCK || item.getType() == Material.BARRIER ||
                item.getType() == Material.LIGHT || item.getType() == Material.STRUCTURE_BLOCK ||
                item.getType() == Material.STRUCTURE_VOID || item.getType() == Material.SPAWNER ||
                item.getType() == Material.END_PORTAL_FRAME)) {
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
                player.sendMessage(str);
            }
        }
    }

    @Override
    public void onDisable() {
        System.out.println("[NoIllegals]: Disabled!");
    }
}