package com.thecrappiest.grappling;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GrapplingListener implements Listener {

	public final GrapplingHook gh;

	public GrapplingListener(GrapplingHook gh) {
		this.gh = gh;
		Bukkit.getPluginManager().registerEvents(this, gh);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHookReel(PlayerFishEvent event) {
		Player player = event.getPlayer();
		PlayerInventory inv = player.getInventory();
		Location loc = player.getLocation();
		Location hookLoc = event.getHook().getLocation();
		UUID id = player.getUniqueId();
		State state = event.getState();

		EquipmentSlot hand = inv.getItemInMainHand().getType().equals(Material.FISHING_ROD) ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
		ItemStack rod = hand == EquipmentSlot.HAND ? inv.getItemInMainHand() : inv.getItemInOffHand();

		NBTUtil nbt = NBTUtil.getInstance();

		FileConfiguration config = gh.getConfig();

		boolean onCooldown = gh.cooldownTimes.containsKey(id);
		boolean isHook = nbt.containsNBTUses(rod);
		boolean bypassCooldown = player.hasPermission(config.getString("Permissions.Bypass-Cooldown"));

		if (state == State.FISHING) {
			if (onCooldown && isHook && !bypassCooldown) {
				event.setCancelled(true);
				player.sendMessage(gh.color(config.getString("Cooldown.Message")));
			}
			return;
		}

		if (!isHook) return;
		if (state == State.CAUGHT_FISH) event.getCaught().remove();
		if (config.getInt("Cooldown.Ticks") >= 0 && !bypassCooldown) gh.cooldownTimes.put(id, config.getInt("Cooldown.Ticks"));
		if (gh.cooldownTask == null) gh.startTask();

		if (!player.hasPermission(config.getString("Permissions.Use"))) {
			event.setCancelled(true);
			player.sendMessage(gh.color(config.getString("No-Permission")));
			return;
		}

		int uses = nbt.getNBTUses(rod);
		if (uses == 1) {
			if (config.getBoolean("Break-Hook")) {
				if (hand == EquipmentSlot.HAND) {
					inv.setItemInMainHand(null);
				} else {
					inv.setItemInOffHand(null);
				}
			}
		}

		if (config.getBoolean("Attach-Block")) {
			Block closestAttached = gh.hooksClosestBlock(hookLoc);
			if (closestAttached == null && hookLoc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) return;
		}

		if (uses > 1) {
			rod = nbt.setNBTUses(rod, uses - 1);

			ItemMeta rodMeta = rod.getItemMeta();
			List<String> lore = new ArrayList<>();
			config.getStringList("Hook.Lore").forEach(line -> {

				String edit = line;
				if (uses > -1) {
					edit = edit.replace("%uses%", String.valueOf(uses - 1));
				} else {
					edit = edit.replace("%uses%", config.getString("Hook.Unlimited-String"));
				}

				lore.add(gh.color(edit));

			});
			rodMeta.setLore(lore);
			rod.setItemMeta(rodMeta);

			if (hand == EquipmentSlot.HAND) {
				inv.setItemInMainHand(rod);
			} else {
				inv.setItemInOffHand(rod);
			}
		}

		if (config.getBoolean("Teleport-To")) {
			Block hookBlock = hookLoc.getBlock();
			Location aboveHookLoc = hookBlock.getLocation().clone().add(0.500, 0, 0.500);
			aboveHookLoc.setYaw(loc.getYaw());
			aboveHookLoc.setPitch(loc.getPitch());
			Block below = hookBlock.getRelative(BlockFace.DOWN);
			if (below.getType() != Material.AIR) {
				player.teleport(aboveHookLoc);
			} else {
				Block closestAttached = gh.hooksClosestBlock(hookLoc);

				if (closestAttached != null) {
					if (closestAttached.getRelative(BlockFace.UP).getType() == Material.AIR && closestAttached
							.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.AIR) {
						Location aboveClosest = closestAttached.getLocation().clone().add(0.500, 1, 0.500);
						aboveClosest.setYaw(loc.getYaw());
						aboveClosest.setPitch(loc.getPitch());
						player.teleport(aboveClosest);
						return;
					}
				}

				player.teleport(aboveHookLoc);
			}
			return;
		}

		Vector toHook = hookLoc.toVector().subtract(loc.toVector());
		if (!config.getBoolean("Momentum.Adaptable")) {
			Vector up = loc.getDirection().clone().add(new Vector(0, config.getDouble("Momentum.Upwards"), 0));
			Vector forwards = toHook.clone().normalize().multiply(config.getDouble("Momentum.Forward"));

			player.setVelocity(forwards.add(up));
		} else {
			double multiplier = config.getDouble("Momentum.Multiplier");

			if (hookLoc.getBlockY() <= loc.getBlockY() && hookLoc.getBlockY() > loc.getBlockY() - 4) {
				player.setVelocity(toHook.clone().normalize().setY(.1).multiply(multiplier + 2));
			} else {
				player.setVelocity(toHook.clone().normalize().multiply(multiplier));
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent event) {
		if (!gh.getConfig().getBoolean("Force-Give.Enabled")) return;

		Player p = event.getPlayer();
		int slot = gh.getConfig().getInt("Force-Give.Slot");
		int uses = gh.getConfig().getInt("Force-Give.Uses");

		ItemStack hook = gh.generateHook(uses);
		new BukkitRunnable() {
			public void run() {
				p.getInventory().setItem(slot, hook);
			}
		}.runTaskLater(gh, gh.getConfig().getInt("Force-Give.Delay"));
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Item item = event.getItemDrop();
		NBTUtil nbt = NBTUtil.getInstance();

		if (!nbt.containsNBTUses(item.getItemStack())) return;
		if (!gh.getConfig().getBoolean("Force-Give.Dropable"))
			event.setCancelled(true);
	}

}
