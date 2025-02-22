package com.minecraftabnormals.neapolitan.common.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Random;

public class HealingItem extends Item {
	private final float healAmount;

	public HealingItem(float healAmount, Properties builder) {
		super(builder);
		this.healAmount = healAmount;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		applyHealing(this.healAmount, worldIn, entityLiving);
		return super.finishUsingItem(stack, worldIn, entityLiving);
	}

	public static void applyHealing(float healAmount, IWorld world, LivingEntity entity) {
		entity.heal(healAmount);
		Random rand = entity.getRandom();
		if (world.isClientSide()) {
			int times = 2 * Math.round(healAmount);
			for (int i = 0; i < times; ++i) {
				double d0 = rand.nextGaussian() * 0.02D;
				double d1 = rand.nextGaussian() * 0.02D;
				double d2 = rand.nextGaussian() * 0.02D;
				world.addParticle(ParticleTypes.HEART, entity.getRandomX(1.0D), entity.getRandomY() + 0.5D, entity.getRandomZ(1.0D), d0, d1, d2);
			}
		}
	}
}
