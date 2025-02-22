package com.minecraftabnormals.neapolitan.common.entity;

import com.minecraftabnormals.neapolitan.common.entity.goals.*;
import com.minecraftabnormals.neapolitan.common.entity.util.ChimpanzeeAction;
import com.minecraftabnormals.neapolitan.common.entity.util.ChimpanzeeTypes;
import com.minecraftabnormals.neapolitan.common.item.MilkshakeItem;
import com.minecraftabnormals.neapolitan.core.other.NeapolitanConstants;
import com.minecraftabnormals.neapolitan.core.other.NeapolitanTags;
import com.minecraftabnormals.neapolitan.core.registry.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.controller.BodyController;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.*;
import net.minecraft.particles.*;
import net.minecraft.pathfinding.ClimberPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class ChimpanzeeEntity extends AnimalEntity implements IAngerable {
	private static final DataParameter<Integer> CHIMPANZEE_TYPE = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> ANGER_TIME = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> APE_MODE_TIME = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> HUNGER = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> DIRTINESS = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> PALENESS = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> LEFT_HAND_DYE_COLOR = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Integer> RIGHT_HAND_DYE_COLOR = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.INT);
	private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> IS_LEFT_HAND_DYED = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> IS_RIGHT_HAND_DYED = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Byte> ACTION = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.BYTE);
	private static final DataParameter<Byte> CLIMBING = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.BYTE);
	private static final DataParameter<Direction> FACING = EntityDataManager.defineId(ChimpanzeeEntity.class, DataSerializers.DIRECTION);

	private static final UUID SPEED_MODIFIER_SITTING_UUID = UUID.fromString("2EF64346-9E56-44E9-9574-1BF9FD6443CF");
	private static final AttributeModifier SPEED_MODIFIER_SITTING = new AttributeModifier(SPEED_MODIFIER_SITTING_UUID, "Sitting speed reduction", -0.75D, AttributeModifier.Operation.MULTIPLY_BASE);

	public static final EntitySize SITTING_DIMENSIONS = EntitySize.scalable(0.6F, 1.0F);

	private static final RangedInteger ANGER_RANGE = TickRangeConverter.rangeOfSeconds(20, 39);
	private UUID lastHurtBy;
	private int attackTimer;
	private int climbingStamina = 20 + this.random.nextInt(40);

	private boolean isLeader;
	private boolean lookingForBundle;

	@Nullable
	private ChimpanzeeEntity groomingTarget;
	@Nullable
	private ChimpanzeeEntity groomer;

	private float climbAnim;
	private float climbAnim0;

	private float sitAnim;
	private float sitAnim0;

	private int headShakeAnim;
	private int headShakeAnim0;

	public boolean isPartying = false;
	BlockPos jukeboxPosition;

	public ChimpanzeeEntity(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
		this.lookControl = new ChimpanzeeEntity.LookHelperController();
		this.setCanPickUpLoot(true);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new SwimGoal(this));
		this.goalSelector.addGoal(1, new ChimpSitGoal(this));
		this.goalSelector.addGoal(2, new ChimpGetScaredGoal(this, 1.25D));
		this.goalSelector.addGoal(3, new ChimpGrabBananaGoal(this, 1.25D));
		this.goalSelector.addGoal(4, new ChimpAttackGoal(this, 1.25D));
		this.goalSelector.addGoal(5, new ChimpPanicGoal(this, 1.25D));
		this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
		this.goalSelector.addGoal(7, new ChimpAvoidEntityGoal<>(this, PlantainSpiderEntity.class, 4.0F, 1.0D, 1.25D));
		this.goalSelector.addGoal(8, new ChimpOpenBunchGoal(this));
		this.goalSelector.addGoal(9, new ChimpEatBananaGoal(this));
		this.goalSelector.addGoal(10, new ChimpTemptBananaGoal(this, 1.25D));
		this.goalSelector.addGoal(11, new TemptGoal(this, 1.25D, Ingredient.of(NeapolitanTags.Items.CHIMPANZEE_FOOD), false));
		this.goalSelector.addGoal(12, new ChimpFollowParentGoal(this, 1.25D));
		this.goalSelector.addGoal(13, new ChimpShareBananaGoal(this, 1.0D));
		this.goalSelector.addGoal(14, new ChimpBeGroomedGoal(this));
		this.goalSelector.addGoal(15, new ChimpGroomGoal(this, 1.0D));
		this.goalSelector.addGoal(16, new ChimpFollowOthersGoal(this, 1.0D));
		this.goalSelector.addGoal(17, new ChimpShakeBundleGoal(this, 1.0D, 48, 16));
		this.goalSelector.addGoal(18, new ChimpPlayWithHelmetGoal(this));
		this.goalSelector.addGoal(19, new ChimpCryGoal(this));
		this.goalSelector.addGoal(20, new ChimpShakeHeadGoal(this));
		this.goalSelector.addGoal(21, new ChimpLookAtItemGoal(this));
		this.goalSelector.addGoal(22, new ChimpJumpOnBouncyGoal(this, 1.0D, 16));
		this.goalSelector.addGoal(23, new ChimpPlayNoteBlockGoal(this, 1.0D, 16));
		this.goalSelector.addGoal(24, new ChimpApeModeGoal(this, 1.0D));
		this.goalSelector.addGoal(25, new ChimpRandomWalkingGoal(this, 1.0D));
		this.goalSelector.addGoal(26, new LookAtGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.addGoal(27, new LookAtGoal(this, MobEntity.class, 6.0F));
		this.goalSelector.addGoal(28, new LookRandomlyGoal(this));

		this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::isAngryAt));
		this.targetSelector.addGoal(1, new ChimpHurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new ResetAngerGoal<>(this, true));
	}

	public static AttributeModifierMap.MutableAttribute registerAttributes() {
		return AnimalEntity.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 3.0D);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(CHIMPANZEE_TYPE, 0);
		this.entityData.define(ANGER_TIME, 0);
		this.entityData.define(APE_MODE_TIME, 0);
		this.entityData.define(HUNGER, 0);
		this.entityData.define(DIRTINESS, 0);
		this.entityData.define(PALENESS, 0);
		this.entityData.define(LEFT_HAND_DYE_COLOR, 0);
		this.entityData.define(RIGHT_HAND_DYE_COLOR, 0);
		this.entityData.define(SITTING, false);
		this.entityData.define(IS_LEFT_HAND_DYED, false);
		this.entityData.define(IS_RIGHT_HAND_DYED, false);
		this.entityData.define(ACTION, (byte) 0);
		this.entityData.define(CLIMBING, (byte) 0);
		this.entityData.define(FACING, Direction.DOWN);
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT compound) {
		super.addAdditionalSaveData(compound);
		this.addPersistentAngerSaveData(compound);
		compound.putInt("ChimpanzeeType", this.getChimpanzeeType());
		compound.putInt("ApeModeTime", this.getApeModeTime());
		compound.putInt("Hunger", this.getHunger());
		compound.putInt("Dirtiness", this.getDirtiness());
		compound.putInt("Paleness", this.getPaleness());
		compound.putByte("LeftHandDyeColor", (byte) this.getHandDyeColor(HandSide.LEFT).getId());
		compound.putByte("RightHandDyeColor", (byte) this.getHandDyeColor(HandSide.RIGHT).getId());
		compound.putBoolean("Sitting", this.isSitting());
		compound.putBoolean("IsLeftHandDyed", this.getHandDyed(HandSide.LEFT));
		compound.putBoolean("IsRightHandDyed", this.getHandDyed(HandSide.RIGHT));
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT compound) {
		super.readAdditionalSaveData(compound);
		this.readPersistentAngerSaveData((ServerWorld) this.level, compound);
		this.setChimpanzeeType(compound.getInt("ChimpanzeeType"));
		this.setApeModeTime(compound.getInt("ApeModeTime"));
		this.setHunger(compound.getInt("Hunger"));
		this.setDirtiness(compound.getInt("Dirtiness"));
		this.setPaleness(compound.getInt("Paleness"));
		this.setHandDyeColor(DyeColor.byId(compound.getInt("LeftHandDyeColor")), HandSide.LEFT);
		this.setHandDyeColor(DyeColor.byId(compound.getInt("RightHandDyeColor")), HandSide.RIGHT);
		this.setSitting(compound.getBoolean("Sitting"));
		this.setHandDyed(compound.getBoolean("IsLeftHandDyed"), HandSide.LEFT);
		this.setHandDyed(compound.getBoolean("IsRightHandDyed"), HandSide.RIGHT);
	}

	@Override
	protected BodyController createBodyControl() {
		return new ChimpanzeeEntity.BodyHelperController();
	}

	@Override
	protected PathNavigator createNavigation(World worldIn) {
		return new ClimberPathNavigator(this, worldIn);
	}

	// SOUND //

	@Override
	public int getAmbientSoundInterval() {
		return this.getApeModeTime() > 0 ? 20 : 120;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return (this.isAngry() || this.getApeModeTime() > 0) ? NeapolitanSounds.ENTITY_CHIMPANZEE_SCREAM.get() : NeapolitanSounds.ENTITY_CHIMPANZEE_AMBIENT.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return NeapolitanSounds.ENTITY_CHIMPANZEE_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NeapolitanSounds.ENTITY_CHIMPANZEE_DEATH.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(NeapolitanSounds.ENTITY_CHIMPANZEE_STEP.get(), 0.15F, 1.0F);
	}

	@Nullable
	@Override
	public SoundEvent getEatingSound(ItemStack itemStackIn) {
		return null;
	}

	public void playScreamSound() {
		this.playSound(NeapolitanSounds.ENTITY_CHIMPANZEE_SCREAM.get(), this.getSoundVolume(), this.getVoicePitch());
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	// AI //

	@Override
	protected void customServerAiStep() {
		this.updatePersistentAnger((ServerWorld) this.level, true);

		if (this.isAngry()) {
			this.lastHurtByPlayerTime = this.tickCount;
		}

		if (this.getApeModeTime() > 0 && this.getMoveControl().hasWanted() && !this.isSitting()) {
			double d0 = this.getMoveControl().getSpeedModifier();
			this.setSprinting(d0 >= 1.0D);
		} else {
			this.setSprinting(false);
		}

		super.customServerAiStep();
	}

	@Override
	public boolean doHurtTarget(Entity entityIn) {
		this.swingArms();
		this.level.broadcastEntityEvent(this, (byte) 4);
		float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float f1 = (int) f > 0 ? f / 2.0F + (float) this.random.nextInt((int) f) : f;
		float f2 = this.isChimpanzeeWeapon(this.getMainHandItem()) || this.isChimpanzeeWeapon(this.getOffhandItem()) ? f1 + 1.0F : f1;
		boolean flag = entityIn.hurt(DamageSource.mobAttack(this), f2);
		if (flag) {
			this.doEnchantDamageEffects(this, entityIn);
		}

		return flag;
	}

	public boolean isChimpanzeeWeapon(ItemStack stack) {
		Item item = stack.getItem();
		return item == Items.STICK || item == Items.BAMBOO;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		} else {
			if (this.canStandUp()) {
				this.setSitting(false);
			}
			return super.hurt(source, amount);
		}
	}

	@Override
	public void tick() {
		if (!this.level.isClientSide) {
			if (this.isDoingAction(ChimpanzeeAction.DEFAULT, ChimpanzeeAction.CLIMBING)) {
				this.setDefaultAction();
			}
		}

		super.tick();

		if (this.isAlive()) {
			if (!this.level.isClientSide) {
				this.handleClimbing();

				if (this.random.nextInt(60) == 0) {
					this.setLeader(this.shouldBeLeader());
				}

				if (this.random.nextInt(100) == 0 && (this.isInWaterRainOrBubble() || this.level.isRainingAt(this.blockPosition()))) {
					this.setHandDyed(false, this.random.nextBoolean() ? HandSide.LEFT : HandSide.RIGHT);
				}
			}

			this.spawnParticles();
		}

		this.climbAnim0 = this.climbAnim;
		if (this.isDoingAction(ChimpanzeeAction.CLIMBING)) {
			this.climbAnim = Math.min(this.climbAnim + 0.125F, 0.75F);
		} else if (this.isDoingAction(ChimpanzeeAction.HANGING, ChimpanzeeAction.SHAKING)) {
			this.climbAnim = Math.min(this.climbAnim + 0.125F, 1);
		} else {
			this.climbAnim = Math.max(this.climbAnim - 0.125F, 0);
		}

		this.sitAnim0 = this.sitAnim;
		if (this.isSitting()) {
			this.sitAnim = Math.min(this.sitAnim + 0.167F, 1);
		} else {
			this.sitAnim = Math.max(this.sitAnim - 0.167F, 0);
		}

		this.headShakeAnim0 = this.headShakeAnim;
		if (this.headShakeAnim > 0) {
			--this.headShakeAnim;
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackTimer > 0) {
			--this.attackTimer;
		}

		if (this.jukeboxPosition == null || !this.jukeboxPosition.closerThan(this.position(), 3.46D) || this.level.getBlockState(jukeboxPosition).getBlock() != Blocks.JUKEBOX) {
			this.isPartying = false;
			this.jukeboxPosition = null;
		}

		this.refreshDimensions();

		if (!this.level.isClientSide) {
			if (this.getApeModeTime() > 0) {
				this.setApeModeTime(this.getApeModeTime() - 1);
			}

			if (!this.isHungry() && this.getHunger() >= 0) {
				this.setHunger(this.getHunger() + 1);
				if (this.isHungry()) {
					this.setLeader(this.shouldBeLeader());
				}
			}

			if (!this.isDirty() && this.getDirtiness() >= 0) {
				this.setDirtiness(this.getDirtiness() + 1);
			}

			if (this.getPaleness() >= 0) {
				if (this.isInSunlight()) {
					this.setPaleness(this.getPaleness() - 1);
				} else if (!this.needsSunlight()) {
					this.setPaleness(this.getPaleness() + 1);
				}
			}

			if (this.shouldClimb() && this.verticalCollision) {
				if (--this.climbingStamina <= 0) {
					this.climbingStamina = -20;
				}
			} else if (this.onGround) {
				if (this.climbingStamina < 0) {
					++this.climbingStamina;
				} else {
					this.climbingStamina = 20 + this.random.nextInt(40);
				}
			}
		}
	}

	private void spawnParticles() {
		if (this.isDirty()) {
			if (this.tickCount % 6 == 0) {
				double d0 = ((double) this.random.nextFloat() + 1.0D) * 0.06D;
				double d1 = this.random.nextInt(360) - 360.0D;
				double d2 = ((double) this.random.nextFloat() + 1.0D) * 14.0D;
				d2 *= this.random.nextBoolean() ? 1.0D : -1.0D;

				level.addParticle(NeapolitanParticles.FLY.get(), this.getRandomX(0.5D), this.getEyeY() + this.random.nextDouble() * 0.2D + 0.3D, this.getRandomZ(0.5D), d0, d1, d2);
			}
		}

		if (this.isDoingAction(ChimpanzeeAction.EATING)) {
			ItemStack food = this.getSnack();
			if (this.tickCount % 10 == 0 && !food.isEmpty()) {
				if (this.level.isClientSide) {
					for (int i = 0; i < 6; ++i) {
						Vector3d vector3d = new Vector3d(((double) this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, ((double) this.random.nextFloat() - 0.5D) * 0.1D);
						vector3d = vector3d.xRot(-this.xRot * ((float) Math.PI / 180F));
						vector3d = vector3d.yRot(-this.yRot * ((float) Math.PI / 180F));
						double d0 = (double) (-this.random.nextFloat()) * 0.2D;
						Vector3d vector3d1 = new Vector3d(((double) this.random.nextFloat() - 0.5D) * 0.2D, d0, 0.6D * this.getScale() + ((double) this.random.nextFloat() - 0.5D) * 0.2D);
						vector3d1 = vector3d1.yRot(-this.yBodyRot * ((float) Math.PI / 180F));
						vector3d1 = vector3d1.add(this.getX(), this.getEyeY(), this.getZ());
						this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItemInHand(this.getSnackHand())), vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y + 0.05D, vector3d.z);
					}
				}

				this.playSound(NeapolitanSounds.ENTITY_CHIMPANZEE_EAT.get(), 0.25F + 0.5F * (float) this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			}
		} else if (this.isDoingAction(ChimpanzeeAction.CRYING)) {
			if (this.level.isClientSide) {
				if (this.tickCount % 2 == 0 && this.random.nextInt(4) > 0) {
					for (int i = 0; i < 2; ++i) {
						double d0 = i == 0 ? (double) (this.random.nextFloat()) * 0.15D + 0.1D : (double) (-this.random.nextFloat()) * 0.15D - 0.1D;
						double d1 = ((double) this.random.nextFloat()) * 0.1D + 0.15D;
						double d2 = i == 0 ? 0.15D : -0.15D;

						Vector3d vector3d = new Vector3d(d0, Math.random() * 0.2D + 0.1D, (double) (this.random.nextFloat()) * 0.2D + 0.1D);
						vector3d = vector3d.yRot(-this.yBodyRot * ((float) Math.PI / 180F));
						Vector3d vector3d1 = new Vector3d(d2, d1, 0.35D);
						vector3d1 = vector3d1.yRot(-this.yBodyRot * ((float) Math.PI / 180F));
						vector3d1 = vector3d1.add(this.getX(), this.getEyeY(), this.getZ());

						this.level.addParticle(NeapolitanParticles.TEAR.get(), vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y + 0.05D, vector3d.z);
					}
				}
			}
		}
	}

	private void handleClimbing() {
		this.setBesideClimbableBlock(this.horizontalCollision);

		if (this.isDoingAction(ChimpanzeeAction.CLIMBING)) {
			Direction newfacing = Direction.DOWN;

			for (Direction direction : Direction.Plane.HORIZONTAL) {
				Vector3d vector3d = this.collide(Vector3d.atLowerCornerOf(direction.getNormal()));

				if (Math.abs(vector3d.get(direction.getAxis())) <= 0.2D) {
					newfacing = direction;
					if (direction == this.entityData.get(FACING)) {
						break;
					}
				}
			}

			this.setFacing(newfacing);
		}
	}

	@Override
	public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getItemInHand(hand);

		if (!itemstack.isEmpty()) {
			if (itemstack.getItem() instanceof MilkshakeItem) {
				ActionResultType actionresulttype = itemstack.interactLivingEntity(player, this, hand);
				if (actionresulttype.consumesAction()) {
					return actionresulttype;
				}
			}

			if (!(this.isFood(itemstack) && !this.isHungry())) {
				if (this.getMainHandItem().isEmpty() || (this.isHungry() && this.isSnack(itemstack) && !this.isSnack(this.getMainHandItem()))) {
					if (!this.getMainHandItem().isEmpty()) {
						this.dropItem(this.getMainHandItem());
					}

					if (this.isSnack(itemstack)) {
						this.stopBeingAngry();
					}

					ItemStack itemstack1 = itemstack.copy();
					itemstack1.setCount(1);
					this.setItemInHand(Hand.MAIN_HAND, itemstack1);
					this.handDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
					this.usePlayerItem(player, itemstack);

					return ActionResultType.sidedSuccess(this.level.isClientSide);
				}
				return ActionResultType.PASS;
			}
		}

		return super.mobInteract(player, hand);
	}

	public void openBunch(Hand hand) {
		if (!this.level.isClientSide) {
			BananaPeelEntity bananapeel = NeapolitanEntities.BANANA_PEEL.get().create(this.level);
			bananapeel.moveTo(this.getX(), this.getEyeY(), this.getZ(), this.yRot, 0.0F);
			bananapeel.setDeltaMovement(this.random.nextDouble() * 0.4D - 0.2D, 0.4D, this.random.nextDouble() * 0.4D - 0.2D);
			this.level.addFreshEntity(bananapeel);

			this.setItemInHand(hand, new ItemStack(NeapolitanItems.BANANA.get()));
		}
	}

	public void eatSnack() {
		if (!this.getSnack().isEmpty()) {
			if (this.getSnack().getItem() == NeapolitanItems.BANANARROW.get()) {
				this.heal((float) NeapolitanItems.BANANA.get().getFoodProperties().getNutrition());
				this.hurt(DamageSource.GENERIC, 0.0F);
				this.setItemInHand(this.getSnackHand(), new ItemStack(Items.ARROW));
			} else {
				if (this.getSnack().isEdible()) {
					this.heal((float) this.getSnack().getItem().getFoodProperties().getNutrition());
				}
				this.setItemInHand(this.getSnackHand(), this.getSnack().finishUsingItem(this.level, this));
			}
		}
		this.setHunger(0);
	}

	@Override
	public void calculateEntityAnimation(LivingEntity entity, boolean isFlying) {
		this.animationSpeedOld = this.animationSpeed;
		double d0 = this.getX() - this.xo;
		double d1 = this.isDoingAction(ChimpanzeeAction.CLIMBING) ? this.getY() - this.yo : 0.0D;
		double d2 = this.getZ() - this.zo;
		float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;
		if (f > 1.0F) {
			f = 1.0F;
		}

		this.animationSpeed += (f - this.animationSpeed) * 0.4F;
		this.animationPosition += this.animationSpeed;
	}

	@Override
	public boolean onClimbable() {
		return this.isDoingAction(ChimpanzeeAction.DEFAULT, ChimpanzeeAction.CLIMBING) && this.isBesideClimbableBlock() && !this.isSitting() && this.climbingStamina > 0;
	}

	public boolean shouldClimb() {
		return !this.onGround && this.onClimbable();
	}

	@Override
	public boolean causeFallDamage(float distance, float damageMultiplier) {
		return false;
	}

	public boolean isBesideClimbableBlock() {
		return (this.entityData.get(CLIMBING) & 1) != 0;
	}

	public void setBesideClimbableBlock(boolean climbing) {
		byte b0 = this.entityData.get(CLIMBING);
		if (climbing) {
			b0 = (byte) (b0 | 1);
		} else {
			b0 = (byte) (b0 & -2);
		}

		this.entityData.set(CLIMBING, b0);
	}

	@Override
	public EntitySize getDimensions(Pose pose) {
		if (this.isSitting()) {
			return SITTING_DIMENSIONS.scale(this.getScale());
		} else {
			return super.getDimensions(pose);
		}
	}

	public boolean canStandUp() {
		EntitySize entitysize = this.getType().getDimensions();
		float f = entitysize.width / 2.0F;
		Vector3d vector3d = new Vector3d(this.getX() - (double) f, this.getY(), this.getZ() - (double) f);
		Vector3d vector3d1 = new Vector3d(this.getX() + (double) f, this.getY() + (double) entitysize.height, this.getZ() + (double) f);
		AxisAlignedBB axisalignedbb = new AxisAlignedBB(vector3d, vector3d1);
		return this.level.noCollision(this, axisalignedbb.deflate(1.0E-7D));
	}

	@Override
	public double getMyRidingOffset() {
		return this.isBaby() ? -0.05D : -0.3D;
	}

	@Override
	public boolean canTakeItem(ItemStack itemstackIn) {
		EquipmentSlotType equipmentslottype = MobEntity.getEquipmentSlotForItem(itemstackIn);
		if (!this.getItemBySlot(equipmentslottype).isEmpty()) {
			return false;
		} else {
			return equipmentslottype == EquipmentSlotType.MAINHAND && super.canTakeItem(itemstackIn);
		}
	}

	public int getApeModeTime() {
		return this.entityData.get(APE_MODE_TIME);
	}

	public void setApeModeTime(int time) {
		this.entityData.set(APE_MODE_TIME, time);
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		ItemStack heldstack = this.getItemBySlot(EquipmentSlotType.MAINHAND);
		return heldstack.isEmpty() || this.getItemValue(stack) > this.getItemValue(heldstack);
	}

	private int getItemValue(ItemStack stack) {
		if (this.isSnack(stack)) {
			return this.isHungry() ? 2 : 1;
		} else if (this.isFavoriteItem(stack)) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	protected void pickUpItem(ItemEntity itemEntity) {
		ItemStack itemstack = itemEntity.getItem();
		if (!this.isDoingAction(ChimpanzeeAction.LOOKING_AT_ITEM, ChimpanzeeAction.PLAYING_WITH_ITEM) && this.canHoldItem(itemstack)) {
			int i = itemstack.getCount();
			if (i > 1) {
				ItemEntity itementity = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), itemstack.split(i - 1));
				this.level.addFreshEntity(itementity);
			}

			this.dropItem(this.getMainHandItem());

			this.onItemPickup(itemEntity);
			this.setItemSlot(EquipmentSlotType.MAINHAND, itemstack.split(1));
			this.handDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
			this.take(itemEntity, itemstack.getCount());
			itemEntity.remove();

			if (this.isSnack(itemstack)) {
				this.stopBeingAngry();
			}
		}
	}

	public boolean isFavoriteItem(ItemStack stack) {
		return stack.getItem().is(NeapolitanTags.Items.CHIMPANZEE_FAVORITES);
	}

	public void throwHeldItem(Hand hand) {
		ItemStack stack = this.getItemInHand(hand);
		if (!stack.isEmpty() && !this.level.isClientSide) {
			Item item = stack.getItem();

			if (item instanceof ArrowItem) {
				ArrowItem arrowItem = (ArrowItem) item;
				List<Entity> list = this.level.getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), (entity) -> NeapolitanTags.EntityTypes.CHIMPANZEE_DART_TARGETS.contains(entity.getType()));
				Entity target = null;

				double maxValue = Double.MAX_VALUE;
				for (Entity entity : list) {
					double distance = this.distanceToSqr(entity);
					if (!(distance > maxValue)) {
						maxValue = distance;
						target = entity;
					}
				}

				if (target != null) {
					AbstractArrowEntity arrow = arrowItem.createArrow(level, stack, this);
					double d0 = target.getEyeY() - (double) 1.1F;
					double d1 = target.getX() - this.getX();
					double d2 = d0 - arrow.getY();
					double d3 = target.getZ() - this.getZ();
					float f = MathHelper.sqrt(d1 * d1 + d3 * d3) * 0.2F;
					this.lookAt(target, 90.0F, 90.0F);
					arrow.shoot(d1, d2 + (double) f, d3, 1.6F, 6.0F);
					this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
					this.level.addFreshEntity(arrow);
					this.setItemInHand(hand, ItemStack.EMPTY);

					this.swingArms();
					this.level.broadcastEntityEvent(this, (byte) 4);
				}

			} else {
				if (item instanceof DyeItem || (ModList.get().isLoaded(NeapolitanConstants.ENVIRONMENTAL) && item == ForgeRegistries.ITEMS.getValue(NeapolitanConstants.MUD_BALL))) {
					HandSide handside = hand == Hand.MAIN_HAND ? this.getMainArm() : this.getMainArm().getOpposite();
					this.setHandDyed(true, handside);
					this.setHandDyeColor(item instanceof DyeItem ? ((DyeItem) item).getDyeColor() : DyeColor.BROWN, handside);
				}

				ItemEntity itemEntity = new ItemEntity(this.level, this.getX() + this.getLookAngle().x * 0.2D, this.getY() + this.getBbHeight() * 0.625F, this.getZ() + this.getLookAngle().z * 0.2D, stack);
				Vector3d vector3d = new Vector3d(this.getLookAngle().x * 0.25D, 0.0D, this.getLookAngle().z * 0.25D);
				itemEntity.setDeltaMovement(vector3d);
				itemEntity.setPickUpDelay(40);
				itemEntity.setThrower(this.getUUID());
				this.level.addFreshEntity(itemEntity);
				this.setItemInHand(hand, ItemStack.EMPTY);

				this.swingArms();
				this.level.broadcastEntityEvent(this, (byte) 4);
			}
		}
	}

	public void dropItem(ItemStack itemStack) {
		ItemEntity itementity = new ItemEntity(this.level, this.getX(), this.getEyeY() - (double) 0.3F, this.getZ(), itemStack);
		itementity.setPickUpDelay(40);
		itementity.setThrower(this.getUUID());
		this.level.addFreshEntity(itementity);
	}

	public void spawnItemFromBucket(ItemStack itemStack, HandSide hand) {
		Vector3d vector3d = new Vector3d(hand == HandSide.LEFT ? 0.35D : -0.35D, 0.0D, 0.5D);
		vector3d = vector3d.yRot(-this.yBodyRot * ((float) Math.PI / 180F));

		ItemEntity itementity = new ItemEntity(this.level, this.getX() + vector3d.x * this.getScale(), this.getEyeY() - (double) 0.15F, this.getZ() + vector3d.z * this.getScale(), itemStack);
		itementity.setDeltaMovement(0.0D, 0.25D, 0.0D);
		itementity.setPickUpDelay(40);
		itementity.setThrower(this.getUUID());
		this.level.addFreshEntity(itementity);
	}

	public void setOffFirework(ItemStack itemStack, HandSide hand) {
		Vector3d vector3d = new Vector3d(hand == HandSide.LEFT ? 0.35D : -0.35D, 0.0D, 0.5D);
		vector3d = vector3d.yRot(-this.yBodyRot * ((float) Math.PI / 180F));

		FireworkRocketEntity fireworkrocketentity = new FireworkRocketEntity(this.level, this, this.getX() + vector3d.x * this.getScale(), this.getEyeY(), this.getZ() + vector3d.z * this.getScale(), itemStack);
		this.level.addFreshEntity(fireworkrocketentity);
	}

	// GROUP BEHAVIOR //

	public boolean isLeader() {
		return this.isLeader;
	}

	public void setLeader(boolean isLeaderIn) {
		this.isLeader = isLeaderIn;
	}

	public boolean isLookingForBundle() {
		return this.lookingForBundle;
	}

	public void setLookingForBundle(boolean lookingForBundleIn) {
		this.lookingForBundle = lookingForBundleIn;

		if (lookingForBundleIn) {
			this.setLeader(this.shouldBeLeader());
			Predicate<ChimpanzeeEntity> predicate = (chimpanzeeentity) -> {
				return chimpanzeeentity != this && chimpanzeeentity.getAge() >= 0;
			};
			List<ChimpanzeeEntity> list = this.level.getEntitiesOfClass(ChimpanzeeEntity.class, this.getBoundingBox().inflate(12.0D, 8.0D, 12.0D), predicate);

			for (ChimpanzeeEntity chimpanzeeentity : list) {
				chimpanzeeentity.setLeader(chimpanzeeentity.shouldBeLeader());
			}
		}
	}

	private boolean shouldBeLeader() {
		Predicate<ChimpanzeeEntity> predicate = (chimpanzeeentity) -> {
			return chimpanzeeentity != this && chimpanzeeentity.getAge() >= 0;
		};
		List<ChimpanzeeEntity> list = this.level.getEntitiesOfClass(ChimpanzeeEntity.class, this.getBoundingBox().inflate(8.0D, 8.0D, 8.0D), predicate);

		if (list.isEmpty()) {
			return false;
		}

		int chimpamount = list.size() + 1;
		int leaderamount = 0;

		for (ChimpanzeeEntity chimpanzeeentity : list) {
			if (chimpanzeeentity.isLeader()) {
				++leaderamount;
			}

			if (leaderamount * 4 >= chimpamount) {
				if (!(this.isLookingForBundle() && !chimpanzeeentity.isLookingForBundle())) {
					return false;
				}
			}
		}

		return true;
	}

	// SPAWNING //

	public static boolean canChimpanzeeSpawn(EntityType<ChimpanzeeEntity> entity, IWorld world, SpawnReason reason, BlockPos pos, Random random) {
		return world.getRawBrightness(pos, 0) > 8;
	}

	@Override
	public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
		spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
		this.setTypeForPosition(this, worldIn);
		this.setHunger(this.random.nextInt(4800));
		this.setDirtiness(this.random.nextInt(4800));
		this.populateDefaultEquipmentSlots(difficultyIn);
		return spawnDataIn;
	}

	public void setTypeForPosition(ChimpanzeeEntity entity, IWorld worldIn) {
		if (worldIn.getBiome(this.blockPosition()).getRegistryName().getPath().contains("rainforest")) {
			entity.setChimpanzeeType(ChimpanzeeTypes.RAINFOREST.getId());
		} else if (worldIn.getBiome(this.blockPosition()).getRegistryName().getPath().contains("bamboo")) {
			entity.setChimpanzeeType(ChimpanzeeTypes.BAMBOO.getId());
		} else {
			entity.setChimpanzeeType(ChimpanzeeTypes.JUNGLE.getId());
		}
	}

	protected void populateDefaultEquipmentSlots(DifficultyInstance difficultyIn) {
		if (this.random.nextFloat() < 0.1F) {
			float f = this.random.nextFloat();
			ItemStack itemstack;
			if (f < 0.6F) {
				if (this.level.getBiome(this.blockPosition()).getRegistryName().getPath().contains("bamboo")) {
					itemstack = new ItemStack(Items.BAMBOO);
				} else {
					itemstack = new ItemStack(Items.STICK);
				}
			} else {
				itemstack = new ItemStack(NeapolitanBlocks.BANANA_FROND.get());
			}

			this.setItemSlot(EquipmentSlotType.MAINHAND, itemstack);
		}

	}

	@Override
	public ChimpanzeeEntity getBreedOffspring(ServerWorld world, AgeableEntity ageableEntity) {
		ChimpanzeeEntity baby = NeapolitanEntities.CHIMPANZEE.get().create(world);
		baby.setChimpanzeeType(this.random.nextBoolean() ? this.getChimpanzeeType() : ((ChimpanzeeEntity) ageableEntity).getChimpanzeeType());
		return baby;
	}

	// DATA //

	public int getChimpanzeeType() {
		return this.entityData.get(CHIMPANZEE_TYPE);
	}

	public void setChimpanzeeType(int type) {
		this.entityData.set(CHIMPANZEE_TYPE, type);
	}

	public int getAttackTimer() {
		return this.attackTimer;
	}

	@Override
	public int getRemainingPersistentAngerTime() {
		return this.entityData.get(ANGER_TIME);
	}

	@Override
	public void setRemainingPersistentAngerTime(int time) {
		this.entityData.set(ANGER_TIME, time);
	}

	public ChimpanzeeEntity getGroomingTarget() {
		return this.groomingTarget;
	}

	public void setGroomingTarget(ChimpanzeeEntity target) {
		this.groomingTarget = target;
	}

	public ChimpanzeeEntity getGroomer() {
		return this.groomer;
	}

	public void setGroomer(ChimpanzeeEntity groomerIn) {
		this.groomer = groomerIn;
	}

	public Direction getFacing() {
		return this.entityData.get(FACING);
	}

	public void setFacing(Direction direction) {
		this.entityData.set(FACING, direction);
	}

	public boolean isSitting() {
		return this.entityData.get(SITTING);
	}

	public void setSitting(boolean sitting) {
		this.entityData.set(SITTING, sitting);
		ModifiableAttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (modifiableattributeinstance.getModifier(SPEED_MODIFIER_SITTING_UUID) != null) {
			modifiableattributeinstance.removeModifier(SPEED_MODIFIER_SITTING);
		}

		if (sitting) {
			modifiableattributeinstance.addTransientModifier(SPEED_MODIFIER_SITTING);
		}
	}

	public DyeColor getHandDyeColor(HandSide handSide) {
		return DyeColor.byId(handSide == HandSide.LEFT ? this.entityData.get(LEFT_HAND_DYE_COLOR) : this.entityData.get(RIGHT_HAND_DYE_COLOR));
	}

	public void setHandDyeColor(DyeColor dyeColor, HandSide handSide) {
		this.entityData.set(handSide == HandSide.LEFT ? LEFT_HAND_DYE_COLOR : RIGHT_HAND_DYE_COLOR, dyeColor.getId());
	}

	public boolean getHandDyed(HandSide handSide) {
		return handSide == HandSide.LEFT ? this.entityData.get(IS_LEFT_HAND_DYED) : this.entityData.get(IS_RIGHT_HAND_DYED);
	}

	public void setHandDyed(boolean dyed, HandSide handSide) {
		this.entityData.set(handSide == HandSide.LEFT ? IS_LEFT_HAND_DYED : IS_RIGHT_HAND_DYED, dyed);
	}

	@Override
	public UUID getPersistentAngerTarget() {
		return this.lastHurtBy;
	}

	@Override
	public void setPersistentAngerTarget(UUID target) {
		this.lastHurtBy = target;
	}

	@Override
	public void startPersistentAngerTimer() {
		this.setRemainingPersistentAngerTime(ANGER_RANGE.randomValue(this.random));
	}

	// NEED STUFF //

	public int getHunger() {
		return this.entityData.get(HUNGER);
	}

	public void setHunger(int amount) {
		this.entityData.set(HUNGER, amount);
	}

	public boolean isHungry() {
		return this.getHunger() >= 9600;
	}

	public boolean needsSnack() {
		return this.isHungry() && this.getSnack().isEmpty();
	}

	public Hand getSnackHand() {
		if (!this.getSnack(Hand.MAIN_HAND).isEmpty()) {
			return Hand.MAIN_HAND;
		} else {
			return Hand.OFF_HAND;
		}
	}

	public ItemStack getSnack() {
		if (!this.getSnack(Hand.MAIN_HAND).isEmpty()) {
			return this.getSnack(Hand.MAIN_HAND);
		} else if (!this.getSnack(Hand.OFF_HAND).isEmpty()) {
			return this.getSnack(Hand.OFF_HAND);
		}
		return ItemStack.EMPTY;
	}

	public ItemStack getSnack(Hand hand) {
		ItemStack snack = this.getItemInHand(hand);
		if (this.isSnack(snack)) {
			return snack;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.getItem().is(NeapolitanTags.Items.CHIMPANZEE_FOOD);
	}

	public boolean isSnack(ItemStack stack) {
		return stack.getItem().is(NeapolitanTags.Items.CHIMPANZEE_SNACKS);
	}

	public int getDirtiness() {
		return this.entityData.get(DIRTINESS);
	}

	public void setDirtiness(int amount) {
		this.entityData.set(DIRTINESS, amount);
	}

	public boolean isDirty() {
		return this.getDirtiness() >= 12000;
	}

	public void getCleaned() {
		this.setDirtiness(0);
		this.setHandDyed(false, HandSide.LEFT);
		this.setHandDyed(false, HandSide.RIGHT);
	}

	public int getPaleness() {
		return this.entityData.get(PALENESS);
	}

	public void setPaleness(int amount) {
		this.entityData.set(PALENESS, amount);
	}

	public boolean needsSunlight() {
		return this.getPaleness() >= 6000;
	}

	public float getVisiblePaleness() {
		return MathHelper.clamp((this.getPaleness() - 4800.0F) / 1200.0F, 0.0F, 1.0F);
	}

	public boolean isInSunlight() {
		BlockPos blockpos = this.getVehicle() instanceof BoatEntity ? (new BlockPos(this.getX(), (double) Math.round(this.getY()), this.getZ())).above() : new BlockPos(this.getX(), (double) Math.round(this.getY()), this.getZ());
		return this.level.getBrightness(LightType.SKY, blockpos) > 8;
	}

	// ACTION STUFF //

	public ChimpanzeeAction getAction() {
		return ChimpanzeeAction.byId(this.entityData.get(ACTION));
	}

	public boolean isDoingAction(ChimpanzeeAction... actions) {
		for (ChimpanzeeAction action : actions) {
			if (this.getAction() == action) {
				return true;
			}
		}

		return false;
	}

	public void setAction(ChimpanzeeAction action) {
		this.entityData.set(ACTION, (byte) action.getId());
	}

	public void setDefaultAction() {
		boolean flag = !this.isPassenger() && this.shouldClimb();

		if (flag) {
			this.setAction(ChimpanzeeAction.CLIMBING);
		} else {
			this.setAction(ChimpanzeeAction.DEFAULT);
		}
	}

	// ANIMATION //

	public boolean isPartying() {
		return this.isPartying;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void setRecordPlayingNearby(BlockPos pos, boolean isPartying) {
		this.jukeboxPosition = pos;
		this.isPartying = isPartying;
	}

	public boolean isMouthOpen() {
		if (this.isDoingAction(ChimpanzeeAction.EATING)) {
			return Math.sin(Math.PI * this.tickCount * 0.2D) > 0;
		} else if (this.isDoingAction(ChimpanzeeAction.CRYING, ChimpanzeeAction.PLAYING_WITH_ITEM, ChimpanzeeAction.PLAYING_WITH_HELMET, ChimpanzeeAction.JUMPING, ChimpanzeeAction.DRUMMING)) {
			return true;
		} else return this.getApeModeTime() > 0 || this.isAngry() || this.isHungry() || this.isPartying();
	}

	@OnlyIn(Dist.CLIENT)
	public float getClimbingAnim(float partialTicks) {
		return MathHelper.lerp(partialTicks, this.climbAnim0, this.climbAnim);
	}

	@OnlyIn(Dist.CLIENT)
	public float getSitAnim(float partialTicks) {
		return MathHelper.lerp(partialTicks, this.sitAnim0, this.sitAnim);
	}

	@OnlyIn(Dist.CLIENT)
	public float getHeadShakeAnim(float partialTicks) {
		return MathHelper.lerp(partialTicks, this.headShakeAnim0, this.headShakeAnim);
	}

	public void swingArms() {
		this.attackTimer = 10;
	}

	public void shakeHead() {
		this.headShakeAnim = 40;
		this.headShakeAnim0 = 40;
	}

	public void shakeHead(IParticleData particleData) {
		this.shakeHead();

		double d0 = this.random.nextGaussian() * 0.02D;
		double d1 = this.random.nextGaussian() * 0.02D;
		double d2 = this.random.nextGaussian() * 0.02D;

		this.level.addParticle(particleData, this.getX(), this.getY(1.0D), this.getZ(), d0, d1, d2);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			this.swingArms();
		} else if (id == 6) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_FRIEND.get());
		} else if (id == 7) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_SUN.get());
		} else if (id == 8) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_FOOD.get());
		} else {
			super.handleEntityEvent(id);
		}
	}

	// CONTROLLERS //

	class LookHelperController extends LookController {

		public LookHelperController() {
			super(ChimpanzeeEntity.this);
		}

		public void tick() {
			if (this.resetXRotOnTick()) {
				ChimpanzeeEntity.this.xRot = 0.0F;
			}

			if (this.hasWanted) {
				this.hasWanted = false;
				ChimpanzeeEntity.this.yHeadRot = this.rotateTowards(ChimpanzeeEntity.this.yHeadRot, this.getYRotD(), this.yMaxRotSpeed);
				ChimpanzeeEntity.this.xRot = this.rotateTowards(ChimpanzeeEntity.this.xRot, this.getXRotD(), this.xMaxRotAngle);
			} else {
				ChimpanzeeEntity.this.yHeadRot = this.rotateTowards(ChimpanzeeEntity.this.yHeadRot, ChimpanzeeEntity.this.yBodyRot, 10.0F);
			}

			Direction facing = ChimpanzeeEntity.this.getFacing();
			if (ChimpanzeeEntity.this.isDoingAction(ChimpanzeeAction.CLIMBING) && facing != Direction.DOWN) {
				ChimpanzeeEntity.this.yHeadRot = MathHelper.rotateIfNecessary(ChimpanzeeEntity.this.yHeadRot, facing.toYRot(), (float) ChimpanzeeEntity.this.getMaxHeadYRot());
			} else if (!ChimpanzeeEntity.this.getNavigation().isDone()) {
				ChimpanzeeEntity.this.yHeadRot = MathHelper.rotateIfNecessary(ChimpanzeeEntity.this.yHeadRot, ChimpanzeeEntity.this.yBodyRot, (float) ChimpanzeeEntity.this.getMaxHeadYRot());
			}
		}
	}

	class BodyHelperController extends BodyController {
		private int bodyRotationTickCounter;
		private Direction prevFacing;

		public BodyHelperController() {
			super(ChimpanzeeEntity.this);
		}

		public void clientTick() {
			super.clientTick();

			Direction facing = ChimpanzeeEntity.this.getFacing();

			if (facing != this.prevFacing || !ChimpanzeeEntity.this.isDoingAction(ChimpanzeeAction.CLIMBING)) {
				this.bodyRotationTickCounter = 10;
			}

			this.prevFacing = facing;

			if (facing != Direction.DOWN && ChimpanzeeEntity.this.isDoingAction(ChimpanzeeAction.CLIMBING)) {
				int i = this.bodyRotationTickCounter;
				float f = MathHelper.clamp((float) i / 10.0F, 0.0F, 1.0F);
				float f1 = 90.0F * f;
				ChimpanzeeEntity.this.yBodyRot = MathHelper.rotateIfNecessary(ChimpanzeeEntity.this.yBodyRot, facing.toYRot(), f1);

				if (this.bodyRotationTickCounter > 0) {
					--this.bodyRotationTickCounter;
				}
			} else if (facing != Direction.DOWN && ChimpanzeeEntity.this.isDoingAction(ChimpanzeeAction.CLIMBING)) {
				int i = this.bodyRotationTickCounter;
				float f = MathHelper.clamp((float) i / 10.0F, 0.0F, 1.0F);
				float f1 = 90.0F * f;
				ChimpanzeeEntity.this.yBodyRot = MathHelper.rotateIfNecessary(ChimpanzeeEntity.this.yBodyRot, facing.toYRot(), f1);

				if (this.bodyRotationTickCounter > 0) {
					--this.bodyRotationTickCounter;
				}
			}
		}
	}
}
