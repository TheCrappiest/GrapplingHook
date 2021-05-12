package com.thecrappiest.grappling;

import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_16_R3.NBTTagCompound;

public class NBTUtil {

	public static NBTUtil getInstance() {
		return new NBTUtil();
	}

	public net.minecraft.server.v1_16_R3.ItemStack getNMSStack(ItemStack item) { return CraftItemStack.asNMSCopy(item);}
	public NBTTagCompound getNBTCompound(net.minecraft.server.v1_16_R3.ItemStack nmsStack) { return nmsStack.getTag(); }
	public ItemStack getNormalItem(net.minecraft.server.v1_16_R3.ItemStack nmsStack) { return CraftItemStack.asCraftMirror(nmsStack); }

	public boolean hasKey(ItemStack itemStack, String key) {
		net.minecraft.server.v1_16_R3.ItemStack nmsStack = getNMSStack(itemStack);
		if (nmsStack == null)
			return false;
		
		NBTTagCompound nbtTag = getNBTCompound(nmsStack);
		if (nbtTag == null)
			return false;
		
		return (Boolean) nbtTag.hasKey(key);
	}

	public ItemStack setNBTUses(ItemStack hook, int uses) {
		net.minecraft.server.v1_16_R3.ItemStack nmsStack = getNMSStack(hook);
		NBTTagCompound tagCompound = getNBTCompound(nmsStack) != null ? getNBTCompound(nmsStack) : new NBTTagCompound();
		
		tagCompound.setInt("GrapplingHook_Uses", uses);
		return getNormalItem(nmsStack);
	}

	public Integer getNBTUses(ItemStack hook) {
		net.minecraft.server.v1_16_R3.ItemStack nmsStack = getNMSStack(hook);
		NBTTagCompound tagCompound = getNBTCompound(nmsStack);

		if (tagCompound == null) {
			return 0;
		}

		return tagCompound.getInt("GrapplingHook_Uses");
	}

	public boolean containsNBTUses(ItemStack hook) {
		return hasKey(hook, "GrapplingHook_Uses");
	}

}
