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
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

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
		
		setSounds();
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
		if (getConfig().getBoolean("Hook.Unbreakable")) {
		    hookMeta.setUnbreakable(true);
		}
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
	
	public boolean canUserGrapple(Player player, Location location) {
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();

		boolean canBypass = WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()));
		if(canBypass) {
			return true;
		}else {
			return query.testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.USE);
		}
	}
	
	public void setSounds() {
		FileConfiguration c = getConfig();
		if(c.isSet("Usage-Sounds.Throw") && !c.getString("Usage-Sounds.Throw").equals("NONE")) {
			if(Sound.valueOf(c.getString("Usage-Sounds.Throw")) != null) {
				throwSound = Sound.valueOf(c.getString("Usage-Sounds.Throw"));
			}
		}
		if(c.isSet("Usage-Sounds.Reel") && !c.getString("Usage-Sounds.Reel").equals("NONE")) {
			if(Sound.valueOf(c.getString("Usage-Sounds.Reel")) != null) {
				reelSound = Sound.valueOf(c.getString("Usage-Sounds.Reel"));
			}
		}
		if(c.isSet("Usage-Sounds.Cooldown") && !c.getString("Usage-Sounds.Cooldown").equals("NONE")) {
			if(Sound.valueOf(c.getString("Usage-Sounds.Cooldown")) != null) {
				cooldownSound = Sound.valueOf(c.getString("Usage-Sounds.Cooldown"));
			}
		}
		if(c.isSet("Usage-Sounds.No-Permission") && !c.getString("Usage-Sounds.No-Permission").equals("NONE")) {
			if(Sound.valueOf(c.getString("Usage-Sounds.No-Permission")) != null) {
				nopermSound = Sound.valueOf(c.getString("Usage-Sounds.No-Permission"));
			}
		}
	}

	public Sound throwSound, reelSound, cooldownSound, nopermSound = null;
	
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
