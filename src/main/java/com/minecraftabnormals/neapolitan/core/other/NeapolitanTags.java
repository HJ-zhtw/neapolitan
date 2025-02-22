package com.minecraftabnormals.neapolitan.core.other;

import com.minecraftabnormals.neapolitan.core.Neapolitan;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;

public class NeapolitanTags {
	public static class Blocks {
		public static final ITag<Block> VANILLA_PLANTABLE_ON = BlockTags.bind(id("vanilla_plantable_on"));
		public static final ITag<Block> UNAFFECTED_BY_MINT = BlockTags.bind(id("unaffected_by_mint"));
		public static final ITag<Block> CHIMPANZEE_JUMPING_BLOCKS = BlockTags.bind(id("chimpanzee_jumping_blocks"));
	}

	public static class Items {
		public static final ITag<Item> CHIMPANZEE_FOOD = ItemTags.bind(id("chimpanzee_food"));
		public static final ITag<Item> CHIMPANZEE_SNACKS = ItemTags.bind(id("chimpanzee_snacks"));
		public static final ITag<Item> CHIMPANZEE_APE_MODE_ITEMS = ItemTags.bind(id("chimpanzee_ape_mode_items"));
		public static final ITag<Item> CHIMPANZEE_SHAKEABLE_BUCKETS = ItemTags.bind(id("chimpanzee_shakeable_buckets"));
		public static final ITag<Item> CHIMPANZEE_FAVORITES = ItemTags.bind(id("chimpanzee_favorites"));
		public static final ITag<Item> HIDES_CHIMPANZEE_EARS = ItemTags.bind(id("hides_chimpanzee_ears"));
	}

	public static class EntityTypes {
		public static final ITag<EntityType<?>> MILKABLE = EntityTypeTags.bind(id("milkable"));
		public static final ITag<EntityType<?>> UNAFFECTED_BY_SLIPPING = EntityTypeTags.bind(id("unaffected_by_slipping"));
		public static final ITag<EntityType<?>> UNAFFECTED_BY_HARMONY = EntityTypeTags.bind(id("unaffected_by_harmony"));
		public static final ITag<EntityType<?>> SCARES_CHIMPANZEES = EntityTypeTags.bind(id("scares_chimpanzees"));
		public static final ITag<EntityType<?>> CHIMPANZEE_DART_TARGETS = EntityTypeTags.bind(id("chimpanzee_dart_targets"));
	}

	private static String id(String string) {
		return Neapolitan.MOD_ID + ":" + string;
	}
}
