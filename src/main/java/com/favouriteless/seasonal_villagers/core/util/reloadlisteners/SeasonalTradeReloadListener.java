package com.favouriteless.seasonal_villagers.core.util.reloadlisteners;

import com.favouriteless.seasonal_villagers.SeasonalVillagers;
import com.google.common.collect.Maps;
import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerTrades.EmeraldForItems;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.npc.VillagerTrades.ItemsForEmeralds;
import net.minecraft.world.entity.npc.VillagerTrades.SuspiciousStewForEmerald;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SeasonalTradeReloadListener extends SimpleJsonResourceReloadListener {

	public static final Gson GSON = new Gson();

	public final Map<VillagerProfession, Int2ObjectMap<ItemListing[]>> SPRING_TRADES = Maps.newHashMap();
	public final Map<VillagerProfession, Int2ObjectMap<ItemListing[]>> SUMMER_TRADES = Maps.newHashMap();
	public final Map<VillagerProfession, Int2ObjectMap<ItemListing[]>> AUTUMN_TRADES = Maps.newHashMap();
	public final Map<VillagerProfession, Int2ObjectMap<ItemListing[]>> WINTER_TRADES = Maps.newHashMap();

	public final Map<String, Map<VillagerProfession, Int2ObjectMap<ItemListing[]>>> SEASON_TO_TRADES = Util.make(Maps.newHashMap(), (map) -> {
		map.put("spring", SPRING_TRADES);
		map.put("summer", SUMMER_TRADES);
		map.put("autumn", AUTUMN_TRADES);
		map.put("winter", WINTER_TRADES);
	});

	public SeasonalTradeReloadListener(String directory) {
		super(GSON, directory);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {

		jsonMap.forEach((resourceLocation, jsonElement) -> {
			try {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String season = resourceLocation.getPath();

				if(SEASON_TO_TRADES.containsKey(season)) {
					Map<VillagerProfession, Int2ObjectMap<ItemListing[]>> seasonTrades = SEASON_TO_TRADES.get(season);

					if(jsonObject.has("pools")) {
						JsonArray pools = jsonObject.get("pools").getAsJsonArray();
						pools.forEach(poolEntry -> {
							JsonObject entry = poolEntry.getAsJsonObject();
							VillagerProfession profession = ForgeRegistries.PROFESSIONS.getValue(new ResourceLocation(entry.get("profession").getAsString()));
							if(profession == null)
								return;
							if(!entry.has("trades"))
								return;
							Int2ObjectMap<ItemListing[]> tradeMap = getTradeMap(entry.getAsJsonArray("trades"));

							seasonTrades.put(profession, tradeMap);

							VillagerTrades.TRADES.put(profession, tradeMap);
						});
					}
				}

			} catch (IllegalArgumentException | JsonParseException jsonparseexception) {
				SeasonalVillagers.LOGGER.error("Parsing error loading villager trades {}: {}", resourceLocation, jsonparseexception.getMessage());
			}
		});
	}

	public Int2ObjectMap<ItemListing[]> getTradeMap(JsonArray tradeArray) {
		Int2ObjectMap<List<ItemListing>> tradeMap = new Int2ObjectOpenHashMap<>();
		tradeArray.forEach(tradeEntry -> {
			JsonObject trade = tradeEntry.getAsJsonObject();

			if(!trade.has("type"))
				return;

			String type = trade.get("type").getAsString();

			int level = 0;
			if(trade.has("level"))
				level = trade.get("level").getAsInt();

			ItemListing listing = switch(type) {
				case "emeralds_for_items" -> new EmeraldForItems(
						Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("item").getAsString()))),
						trade.get("cost").getAsInt(),
						trade.get("maxUses").getAsInt(),
						trade.get("villagerXp").getAsInt());
				case "items_for_emeralds" -> new ItemsForEmeralds(
						Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("item").getAsString()))),
						trade.get("cost").getAsInt(),
						trade.get("maxUses").getAsInt(),
						trade.get("villagerXp").getAsInt());
				case "stew_for_emeralds" -> new SuspiciousStewForEmerald(
						Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(trade.get("effect").getAsString()))),
						trade.get("duration").getAsInt(),
						trade.get("villagerXp").getAsInt());
				default -> null;
			};

			if(listing == null)
				return;
			if(!tradeMap.containsKey(level))
				tradeMap.put(level, new ArrayList<>());

			tradeMap.get(level).add(listing);
		});


		Int2ObjectMap<ItemListing[]> finalTradeMap = new Int2ObjectOpenHashMap<>();
		for(int i : tradeMap.keySet()) {
			List<ItemListing> tradeList = tradeMap.get(i);
			ItemListing[] finalTradeArray = new ItemListing[tradeList.size()];
			for(int j = 0; j < finalTradeArray.length; j++) {
				finalTradeArray[j] = tradeList.get(j);
			}

			finalTradeMap.put(i, finalTradeArray);
		}
		return finalTradeMap;
	}

}