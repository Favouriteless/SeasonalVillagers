package com.favouriteless.seasonal_villagers.common.init;

import com.favouriteless.seasonal_villagers.SeasonalVillagers;
import com.favouriteless.seasonal_villagers.common.api.capabilities.villager.VillagerSeasonCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;

@EventBusSubscriber(modid=SeasonalVillagers.MOD_ID, bus=Bus.FORGE)
public class SeasonalVillagersEvents {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickEntity(EntityInteract event) {
		Entity target = event.getTarget();
		if(!target.getLevel().isClientSide) {
			if(target instanceof Villager villager) {
				ISeasonState seasonState = SeasonHelper.getSeasonState(villager.level);
				String currentSeason = seasonState.getSeason().toString();

				villager.getCapability(VillagerSeasonCapability.INSTANCE).ifPresent((cap) -> {

					if(!currentSeason.equals(cap.getSeasonValue()) || (cap.getDayValue() - seasonState.getDay()) > seasonState.getSeasonDuration()) {
						VillagerTrades.TRADES.putAll(SeasonalTradeData.SEASONAL_TRADES.SEASON_TO_TRADES.get(currentSeason.toLowerCase()));
						villager.setOffers(null);
						int startLevel = villager.getVillagerData().getLevel();
						for(int i = 0; i < startLevel-1; i++) {
							villager.getVillagerData().setLevel(i+1);
							villager.updateTrades();
						}
						cap.setSeasonValue(currentSeason);
						cap.setDayValue(seasonState.getDay());
					}
				});
			}
		}
	}

}
