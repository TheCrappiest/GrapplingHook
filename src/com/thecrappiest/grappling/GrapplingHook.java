package com.thecrappiest.grappling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;

public class GrapplingHook extends JavaPlugin {

	public static GrapplingHook instance;

	public void onEnable() {
		instance = this;

		getCommand("grappling").setExecutor(new GrapplingCommands());
		new GrapplingListener(this);

		if (!getConfig().isSet("Help")) {
			getConfig().options().copyDefaults(true);
			saveConfig();
		}

		reloadConfig();
	}

	public void onDisable() {
		reloadConfig();
	}

	public static GrapplingHook getInstance() {
		return instance;
	}

	public String color(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public boolean isNumber(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	public ItemStack generateHook(int uses) {
		ItemStack hook = new ItemStack(Material.FISHING_ROD);
		ItemMeta hookMeta = hook.getItemMeta();

		List<String> lore = new ArrayList<>();

		getConfig().getStringList("Hook.Lore").forEach(line -> {

			String edit = line;
			if (uses > -1) {
				edit = edit.replace("%uses%", String.valueOf(uses));
			} else {
				edit = edit.replace("%uses%", getConfig().getString("Hook.Unlimited-String"));
			}

			lore.add(color(edit));

		});

		hookMeta.setDisplayName(color(getConfig().getString("Hook.Name")));
		hookMeta.setLore(lore);
		hookMeta.setUnbreakable(true);
		if (getConfig().getBoolean("Hook.Hide-Unbreakable")) {
			hookMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		}
		hook.setItemMeta(hookMeta);

		hook = NBTUtil.getInstance().setNBTUses(hook, uses);

		return hook;
	}

	public Block hooksClosestBlock(Location hookLoc) {
		Block hookBlock = hookLoc.getBlock();
		BlockFace[] blockFaces = { BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH };
		Map<Block, Double> distances = new HashMap<>();

		for (BlockFace bf : blockFaces) {
			Block relative = hookBlock.getRelative(bf);
			if (relative.getType() != Material.AIR) {
				distances.put(relative, relative.getLocation().distance(hookLoc));
			}
		}

		if (!distances.isEmpty()) {
			return (Block) sortMapLow(distances).entrySet().iterator().next().getKey();
		}

		return null;
	}

	public Map<Object, Object> sortMapLow(Map<Block, Double> map) {
		return map.entrySet().stream().sorted((Map.Entry.<Block, Double>comparingByValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public Map<UUID, Integer> cooldownTimes = new HashMap<>();
	public BukkitTask cooldownTask = null;

	public void startTask() {
		cooldownTask = new BukkitRunnable() {
			public void run() {

				cooldownTimes.replaceAll((id, time) -> time - 1);
				cooldownTimes.values().removeIf(time -> time <= 0);

				if (cooldownTimes.isEmpty()) {
					cooldownTask = null;
					cancel();
				}
			}
		}.runTaskTimer(this, 0, 1);
	}

}
