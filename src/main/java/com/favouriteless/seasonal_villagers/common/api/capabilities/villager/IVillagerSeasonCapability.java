package com.favouriteless.seasonal_villagers.common.api.capabilities.villager;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IVillagerSeasonCapability extends INBTSerializable<CompoundTag> {
	void setSeasonValue(String value);

	String getSeasonValue();

	void setDayValue(int value);

	int getDayValue();
}
