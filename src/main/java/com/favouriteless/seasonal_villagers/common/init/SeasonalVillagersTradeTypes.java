package com.favouriteless.seasonal_villagers.common.init;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ConfiguredStructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecoration.Type;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SeasonalVillagersTradeTypes {

	public static class ItemsForItems implements ItemListing {

		private final ItemStack result;
		private final ItemStack currency;
		private final ItemStack currency2;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public ItemsForItems(ItemStack result, ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.result = result;
			this.currency = currency;
			this.currency2 = currency2;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		@Nullable
		@Override
		public MerchantOffer getOffer(Entity pTrader, Random pRandom) {
			return new MerchantOffer(currency, currency2, result, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static class SuspiciousStewForItems implements ItemListing {
		private final ItemStack currency;
		private final ItemStack currency2;
		private final MobEffect effect;
		private final int duration;
		private final int villagerXp;
		private final int maxUses;
		private final float priceMultiplier;

		public SuspiciousStewForItems(MobEffect pEffect, int pDuration, ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.effect = pEffect;
			this.duration = pDuration;
			this.currency = currency;
			this.currency2 = currency2;
			this.villagerXp = villagerXp;
			this.maxUses = maxUses;
			this.priceMultiplier = 0.05F;
		}

		@Nullable
		public MerchantOffer getOffer(Entity pTrader, Random pRandom) {
			ItemStack itemstack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
			SuspiciousStewItem.saveMobEffect(itemstack, this.effect, this.duration);
			return new MerchantOffer(currency, currency2, itemstack, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static class EnchantedItemForItems implements ItemListing {
		private final ItemStack result;
		private final ItemStack currency;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public EnchantedItemForItems(ItemStack result, ItemStack currency, int maxUses, int villagerXp, float priceMultiplier) {
			this.result = result;
			this.currency = currency;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			int i = 5 + pRand.nextInt(15);
			ItemStack itemstack = EnchantmentHelper.enchantItem(pRand, new ItemStack(this.result.getItem()), i, false);
			ItemStack cost = currency.copy();
			int j = Math.min(cost.getCount() + i, 64);
			cost.setCount(j);

			return new MerchantOffer(cost, itemstack, this.maxUses, this.villagerXp, this.priceMultiplier);
		}
	}

	public static class TippedArrowForItems implements ItemListing {
		private final ItemStack result;
		private final ItemStack currency;
		private final ItemStack currency2;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public TippedArrowForItems(ItemStack result, ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.result = result;
			this.currency = currency;
			this.currency2 = currency2;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			List<Potion> list = Registry.POTION.stream().filter((potion) -> !potion.getEffects().isEmpty() && PotionBrewing.isBrewablePotion(potion)).collect(Collectors.toList());
			Potion potion = list.get(pRand.nextInt(list.size()));
			ItemStack resultStack = PotionUtils.setPotion(new ItemStack(this.result.getItem(), this.result.getCount()), potion);


			return new MerchantOffer(resultStack, resultStack, currency2, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static class EnchantBookForItems implements ItemListing {
		private final Item currency;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public EnchantBookForItems(Item currency, int maxUses, int villagerXp, float priceMultiplier) {
			this.currency = currency;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			List<Enchantment> list = Registry.ENCHANTMENT.stream().filter(Enchantment::isTradeable).collect(Collectors.toList());
			Enchantment enchantment = list.get(pRand.nextInt(list.size()));
			int i = Mth.nextInt(pRand, enchantment.getMinLevel(), enchantment.getMaxLevel());
			ItemStack itemstack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, i));
			int j = 2 + pRand.nextInt(5 + i * 10) + 3 * i;
			if (enchantment.isTreasureOnly()) {
				j *= 2;
			}

			if (j > 64) {
				j = 64;
			}

			return new MerchantOffer(new ItemStack(currency, j), new ItemStack(Items.BOOK), itemstack, maxUses, villagerXp, priceMultiplier);
		}
	}

	public static class OceanTreasureMapForItems implements ItemListing {
		private final ItemStack currency;
		private final ItemStack currency2;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public OceanTreasureMapForItems(ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.currency = currency;
			this.currency2 = currency2;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		@Nullable
		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			if (!(pTrader.level instanceof ServerLevel)) {
				return null;
			} else {
				ServerLevel serverlevel = (ServerLevel)pTrader.level;
				BlockPos blockpos = serverlevel.findNearestMapFeature(ConfiguredStructureTags.ON_OCEAN_EXPLORER_MAPS, pTrader.blockPosition(), 100, true);
				if (blockpos != null) {
					ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), (byte)2, true, true);
					MapItem.renderBiomePreviewMap(serverlevel, itemstack);
					MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", MapDecoration.Type.MONUMENT);
					itemstack.setHoverName(new TranslatableComponent("filled_map.monument"));
					return new MerchantOffer(currency, currency2, itemstack, maxUses, villagerXp, priceMultiplier);
				} else {
					return null;
				}
			}
		}
	}

	public static class WoodlandTreasureMapForItems implements ItemListing {
		private final ItemStack currency;
		private final ItemStack currency2;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;

		public WoodlandTreasureMapForItems(ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.currency = currency;
			this.currency2 = currency2;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		@Nullable
		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			if (!(pTrader.level instanceof ServerLevel)) {
				return null;
			} else {
				ServerLevel serverlevel = (ServerLevel)pTrader.level;
				BlockPos blockpos = serverlevel.findNearestMapFeature(ConfiguredStructureTags.ON_WOODLAND_EXPLORER_MAPS, pTrader.blockPosition(), 100, true);
				if (blockpos != null) {
					ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), (byte)2, true, true);
					MapItem.renderBiomePreviewMap(serverlevel, itemstack);
					MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", Type.MANSION);
					itemstack.setHoverName(new TranslatableComponent("filled_map.mansion"));
					return new MerchantOffer(currency, currency2, itemstack, maxUses, villagerXp, priceMultiplier);
				} else {
					return null;
				}
			}
		}
	}

	public static class DyedArmorForItems implements ItemListing {
		private final Item item;
		private final ItemStack currency;
		private final ItemStack currency2;
		private final int maxUses;
		private final int villagerXp;
		private final float priceMultiplier;


		public DyedArmorForItems(Item item, ItemStack currency, ItemStack currency2, int maxUses, int villagerXp, float priceMultiplier) {
			this.item = item;
			this.currency = currency;
			this.currency2 = currency2;
			this.maxUses = maxUses;
			this.villagerXp = villagerXp;
			this.priceMultiplier = priceMultiplier;
		}

		public MerchantOffer getOffer(Entity pTrader, Random pRand) {
			ItemStack result = new ItemStack(item);
			if (item instanceof DyeableArmorItem) {
				List<DyeItem> list = Lists.newArrayList();
				list.add(getRandomDye(pRand));
				if (pRand.nextFloat() > 0.7F) {
					list.add(getRandomDye(pRand));
				}

				if (pRand.nextFloat() > 0.8F) {
					list.add(getRandomDye(pRand));
				}

				result = DyeableLeatherItem.dyeArmor(result, list);
			}

			return new MerchantOffer(currency, currency2, result, maxUses, villagerXp, priceMultiplier);
		}

		private static DyeItem getRandomDye(Random pRandom) {
			return DyeItem.byColor(DyeColor.byId(pRandom.nextInt(16)));
		}
	}
}

