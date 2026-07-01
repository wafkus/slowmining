package com.example.slowmining;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlowMiningPlugin extends JavaPlugin implements Listener {

    // Konfiguracja: typ bloku -> poziom Mining Fatigue (amplifier)
    private final Map<Material, Integer> slowBlocks = new HashMap<>();
    private boolean ignoreIfCreative = true;

    // Sledzenie kopiacych graczy
    private final Map<UUID, Block> diggingBlock = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    // Jak dlugo (w tickach) trzyma sie efekt zanim zostanie odswiezony.
    // 20 tickow = 1 sekunda. Odswiezamy co 10 tickow, efekt trwa 60 tickow -
    // zapas na wypadek lagow.
    private static final int EFFECT_DURATION_TICKS = 60;
    private static final long REFRESH_PERIOD_TICKS = 10L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SlowMining wlaczony. Skonfigurowane bloki: " + slowBlocks.size());
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        diggingBlock.clear();
    }

    private void loadConfigValues() {
        slowBlocks.clear();
        reloadConfig();
        ConfigurationSection section = getConfig().getConfigurationSection("blocks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    getLogger().warning("Nieznany material w config.yml: " + key);
                    continue;
                }
                int amplifier = section.getInt(key + ".amplifier", 1);
                slowBlocks.put(material, Math.max(0, amplifier));
            }
        }
        ignoreIfCreative = getConfig().getBoolean("ignore-if-creative", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("slowmining")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                loadConfigValues();
                sender.sendMessage("§a[SlowMining] Konfiguracja przeladowana. Bloki: " + slowBlocks.size());
                return true;
            }
            sender.sendMessage("§eUzycie: /slowmining reload");
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Integer amplifier = slowBlocks.get(block.getType());
        if (amplifier == null) {
            return;
        }
        if (ignoreIfCreative && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        startSlowing(player, block, amplifier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        stopSlowing(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopSlowing(event.getPlayer());
    }

    private void startSlowing(Player player, Block block, int amplifier) {
        UUID uuid = player.getUniqueId();

        applyFatigue(player, amplifier);
        diggingBlock.put(uuid, block);

        // Jesli zadanie odswiezajace juz dziala dla tego gracza, nie tworz drugiego.
        if (activeTasks.containsKey(uuid)) {
            return;
        }

        BukkitTask task = getServer().getScheduler().runTaskTimer(this, () -> {
            Block currentTarget = diggingBlock.get(uuid);
            if (currentTarget == null || !player.isOnline()) {
                stopSlowing(player);
                return;
            }

            Block lookingAt = player.getTargetBlockExact(6);
            boolean stillDiggingSameBlock =
                    lookingAt != null
                            && lookingAt.getWorld().equals(currentTarget.getWorld())
                            && lookingAt.getX() == currentTarget.getX()
                            && lookingAt.getY() == currentTarget.getY()
                            && lookingAt.getZ() == currentTarget.getZ()
                            && lookingAt.getType() == currentTarget.getType();

            if (!stillDiggingSameBlock) {
                stopSlowing(player);
                return;
            }

            applyFatigue(player, amplifier);
        }, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);

        activeTasks.put(uuid, task);
    }

    private void applyFatigue(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_DIGGING,
                EFFECT_DURATION_TICKS,
                amplifier,
                true,   // ambient
                false,  // particles
                false   // icon
        ));
    }

    private void stopSlowing(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        diggingBlock.remove(uuid);
        if (player.isOnline() && player.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {
            player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
        }
    }
}
