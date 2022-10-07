package com.favouriteless.seasonal_villagers.common.init;

import com.favouriteless.seasonal_villagers.SeasonalVillagers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid=SeasonalVillagers.MOD_ID, bus=Bus.FORGE)
public class SeasonalVillagersEvents {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickEntity(EntityInteract event) {
		Entity target = event.getTarget();
		if(!target.getLevel().isClientSide) {
			if(target instanceof Villager villager) {
				
			}
		}
	}

}
