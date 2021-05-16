package com.thecrappiest.grappling;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GrapplingCommands implements CommandExecutor {

	GrapplingHook gh = GrapplingHook.getInstance();

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("grappling")) {
			FileConfiguration config = gh.getConfig();
			if (args.length == 0 || args.length == 1) {
				if (args.length == 1) {
					if (args[0].equalsIgnoreCase("reload")) {
						if (sender.hasPermission(config.getString("Permissions.Reload"))) {
							gh.reloadConfig();
							gh.setSounds();
							sender.sendMessage(gh.color("&aReloaded grapplinghook config."));
						} else {
							sender.sendMessage(gh.color(config.getString("No-Permission")));
						}
					}
				} else {
					if (sender.hasPermission(config.getString("Permissions.Help"))) {
						for (String line : config.getStringList("Help")) {
							sender.sendMessage(gh.color(line));
						}
					} else {
						sender.sendMessage(gh.color(config.getString("No-Permission")));
					}
				}
			}

			if (args.length >= 2 && sender.hasPermission(config.getString("Permissions.Give"))
					&& args[0].equalsIgnoreCase("give")) {
				Player target = Bukkit.getPlayerExact(args[1]);

				int edituses = 5;
				if (args.length == 3) {
					if (gh.isNumber(args[2])) {
						edituses = Integer.valueOf(args[2]);
					}

					if (edituses <= 0) {
						edituses = -1;
					}
				}

				int uses = edituses;
				if (target != null) {
					ItemStack hook = gh.generateHook(uses);

					if (target.getInventory().firstEmpty() == -1) {
						target.getWorld().dropItemNaturally(target.getLocation(), hook);
					} else {
						target.getInventory().addItem(hook);
					}

				} else {
					sender.sendMessage(gh.color(
							"&cUnable to locate player with that username. Make sure they are online and try again."));
				}
			} else if (args.length >= 2 && !sender.hasPermission(config.getString("Permissions.Give"))
					&& args[0].equalsIgnoreCase("give")) {
				sender.sendMessage(gh.color(config.getString("No-Permission")));
			}

			return true;
		}
		return false;
	}

}
