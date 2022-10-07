package com.favouriteless.seasonal_villagers.common.api.capabilities.villager;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class VillagerSeasonCapability implements IVillagerSeasonCapability {

	public static final Capability<IVillagerSeasonCapability> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
	private String seasonValue;
	private int dayValue;

	@Override
	public String getSeasonValue() {
		return seasonValue;
	}

	@Override
	public void setDayValue(int value) {
		dayValue = value;
	}

	@Override
	public int getDayValue() {
		return dayValue;
	}

	@Override
	public void setSeasonValue(String value) {
		this.seasonValue = value;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		if(seasonValue != null) nbt.putString("season", seasonValue);
		nbt.putInt("day", dayValue);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		if(nbt.contains("season")) this.setSeasonValue(nbt.getString("season"));
		if(nbt.contains("day")) this.setDayValue(nbt.getInt("day"));
	}

}
