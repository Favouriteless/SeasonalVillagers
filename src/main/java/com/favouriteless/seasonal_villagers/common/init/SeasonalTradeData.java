package com.favouriteless.seasonal_villagers.common.init;

import com.favouriteless.seasonal_villagers.SeasonalVillagers;
import com.favouriteless.seasonal_villagers.core.util.reloadlisteners.SeasonalTradeReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid=SeasonalVillagers.MOD_ID, bus=Bus.FORGE)
public class SeasonalTradeData {

	public static final SeasonalTradeReloadListener SEASONAL_TRADES = new SeasonalTradeReloadListener("seasonal_trade_pools");

	@SubscribeEvent
	public static void addReloadListenerEvent(AddReloadListenerEvent event) {
		event.addListener(SEASONAL_TRADES);
	}

}
