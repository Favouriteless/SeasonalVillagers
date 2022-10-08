package com.favouriteless.seasonal_villagers.core.util.reloadlisteners;

import com.favouriteless.seasonal_villagers.SeasonalVillagers;
import com.favouriteless.seasonal_villagers.common.init.SeasonalVillagersTradeTypes.*;
import com.google.common.collect.Maps;
import com.google.gson.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

	public final Map<String, Function<JsonObject, ItemListing>> TYPE_FACTORIES = Util.make(Maps.newHashMap(), (map) -> {
		map.put("items", SeasonalTradeReloadListener::getItemsForItems);
		map.put("suspicious_stew", SeasonalTradeReloadListener::getStewForItems);
		map.put("enchanted_item", SeasonalTradeReloadListener::getEnchantedItemForItems);
		map.put("enchant_book", SeasonalTradeReloadListener::getEnchantBookForItems);
		map.put("tipped_arrow", SeasonalTradeReloadListener::getTippedArrowForItems);
		map.put("ocean_treasure_map", SeasonalTradeReloadListener::getOceanMapForItems);
		map.put("woodland_treasure_map", SeasonalTradeReloadListener::getWoodlandMapForItems);
		map.put("dyed_armor", SeasonalTradeReloadListener::getDyedArmorForItems);
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

			if(!trade.has("type") || !TYPE_FACTORIES.containsKey(trade.get("type").getAsString()))
				return;
			if(!trade.has("level"))
				return;

			String type = trade.get("type").getAsString();
			int level = trade.get("level").getAsInt();
			ItemListing listing = TYPE_FACTORIES.get(type).apply(trade);

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


	public static void checkMandatoryResult(JsonObject trade) {
		if(!trade.has("item"))
			throw new JsonParseException("Trade missing result item");
		if(!trade.has("count"))
			throw new JsonParseException("Trade missing result count");
	}

	private static void checkMandatoryCurrency(JsonObject trade) {
		if(!trade.has("currency"))
			throw new JsonParseException("Trade missing currency item");
		if(!trade.has("cost"))
			throw new JsonParseException("Trade missing cost");
	}

	public static ItemListing getItemsForItems(JsonObject trade) {
		checkMandatoryResult(trade);
		checkMandatoryCurrency(trade);

		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("item").getAsString()));
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int count = trade.get("count").getAsInt();
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new ItemsForItems(new ItemStack(item, count), new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new ItemsForItems(new ItemStack(item, count), new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static ItemListing getStewForItems(JsonObject trade) {
		checkMandatoryCurrency(trade);
		if(!trade.has("effect"))
			throw new JsonParseException("Stew trade missing effect");
		if(!trade.has("duration"))
			throw new JsonParseException("Stew trade missing effect duration");

		MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(trade.get("effect").getAsString()));
		int duration = trade.get("duration").getAsInt();
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new SuspiciousStewForItems(effect, duration, new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new SuspiciousStewForItems(effect, duration, new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static ItemListing getEnchantedItemForItems(JsonObject trade) {
		checkMandatoryCurrency(trade);
		if(!trade.has("item"))
			throw new JsonParseException("Trade missing result item");

		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("item").getAsString()));
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		return new EnchantedItemForItems(new ItemStack(item, 1), new ItemStack(currency, cost), maxUses, villagerXp, priceMultiplier);
	}

	public static ItemListing getTippedArrowForItems(JsonObject trade) {
		checkMandatoryCurrency(trade);
		if(!trade.has("count"))
			throw new JsonParseException("Trade missing result count");

		int count = trade.get("count").getAsInt();
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new TippedArrowForItems(new ItemStack(Items.TIPPED_ARROW, count), new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new TippedArrowForItems(new ItemStack(Items.TIPPED_ARROW, count), new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}	}

	public static ItemListing getEnchantBookForItems(JsonObject trade) {
		if(!trade.has("currency"))
			throw new JsonParseException("Trade missing currency item");
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.2F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();


		return new EnchantBookForItems(currency, maxUses, villagerXp, priceMultiplier);
	}

	public static ItemListing getOceanMapForItems(JsonObject trade) {
		checkMandatoryCurrency(trade);

		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new OceanTreasureMapForItems(new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new OceanTreasureMapForItems(new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static ItemListing getWoodlandMapForItems(JsonObject trade) {
		checkMandatoryCurrency(trade);

		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new WoodlandTreasureMapForItems(new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new WoodlandTreasureMapForItems(new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static ItemListing getDyedArmorForItems(JsonObject trade) {
		if(!trade.has("item"))
			throw new JsonParseException("Trade missing result item");
		checkMandatoryCurrency(trade);

		Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("item").getAsString()));
		Item currency = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency").getAsString()));
		int cost = trade.get("cost").getAsInt();

		int maxUses = 12;
		int villagerXp = 1;
		float priceMultiplier = 0.05F;

		if(trade.has("maxUses"))
			maxUses = trade.get("maxUses").getAsInt();
		if(trade.has("villagerXp"))
			villagerXp = trade.get("villagerXp").getAsInt();
		if(trade.has("priceMultiplier"))
			priceMultiplier = trade.get("priceMultiplier").getAsFloat();

		if(trade.has("currency2") && trade.has("cost2")) {
			Item currency2 = ForgeRegistries.ITEMS.getValue(new ResourceLocation(trade.get("currency2").getAsString()));
			int cost2 = trade.get("cost2").getAsInt();
			return new DyedArmorForItems(item, new ItemStack(currency, cost), new ItemStack(currency2, cost2), maxUses, villagerXp, priceMultiplier);
		}
		else {
			return new DyedArmorForItems(item, new ItemStack(currency, cost), ItemStack.EMPTY, maxUses, villagerXp, priceMultiplier);
		}
	}

}


























