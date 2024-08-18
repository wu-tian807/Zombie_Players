package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.EnumTrainType;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.corosus.zombie_players.entity.ai.WorkInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class EventHandlerForge {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent event) {
		LivingEntity ent = event.getEntity();
		if (ent.level.isClientSide()) return;
		if (event.isCanceled()) return;

		if (ent instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) event.getEntity();

			if (!ConfigZombiePlayers.requiresDeathByZombieToSpawnZombiePlayer || event.getSource().getDirectEntity() instanceof Zombie) {
				ZombiePlayer.spawnInPlaceOfPlayer(player);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (ConfigZombiePlayers.automaticallyAddJoinedPlayersToNamesList) {
			if (!ConfigZombiePlayers.Spawning_playerNamesToUse.contains(event.getEntity().getDisplayName().getString())) {
				if (ConfigZombiePlayers.Spawning_playerNamesToUse.length() == 0) {
					ConfigZombiePlayers.Spawning_playerNamesToUse = event.getEntity().getDisplayName().getString();
				} else {
					ConfigZombiePlayers.Spawning_playerNamesToUse += ", " + event.getEntity().getDisplayName().getString();
				}
				ConfigMod.forceSaveAllFilesFromRuntimeSettings();
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND) {
			if (event.getEntity().getMainHandItem().getItem() == Items.GOLDEN_HOE) {
				if (Zombie_Players.getWorkAreaStage(event.getEntity()) == 1) {
					event.getEntity().getPersistentData().putInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE, 2);
					List<ZombiePlayer> listEnts = event.getLevel().getEntitiesOfClass(ZombiePlayer.class, new AABB(event.getEntity().blockPosition()).inflate(10, 5, 10));
					for (ZombiePlayer ent : listEnts) {
						if (ent.isCalm() && ent.getWorkInfo().isInAreaSetMode() && ent.getOwnerUUID().equals(event.getEntity().getUUID())) {
							ent.getWorkInfo().setWorkAreaPos1(event.getPos());
							event.getEntity().sendSystemMessage(Component.literal("First work area position set, right click second block with golden hoe"));
						}
					}
				} else if (Zombie_Players.getWorkAreaStage(event.getEntity()) == 2) {
					event.getEntity().getPersistentData().putInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE, 0);

					List<ZombiePlayer> listEnts = event.getLevel().getEntitiesOfClass(ZombiePlayer.class, new AABB(event.getEntity().blockPosition()).inflate(10, 5, 10));
					for (ZombiePlayer ent : listEnts) {
						if (ent.isCalm() && ent.getWorkInfo().isInAreaSetMode() && ent.getOwnerUUID().equals(event.getEntity().getUUID())) {
							AABB aabb = new AABB(
									Math.min(ent.getWorkInfo().getWorkAreaPos1().getX(), event.getPos().getX()),
									Math.min(ent.getWorkInfo().getWorkAreaPos1().getY(), event.getPos().getY()),
									Math.min(ent.getWorkInfo().getWorkAreaPos1().getZ(), event.getPos().getZ()),
									Math.max(ent.getWorkInfo().getWorkAreaPos1().getX(), event.getPos().getX()) + 1,
									Math.max(ent.getWorkInfo().getWorkAreaPos1().getY(), event.getPos().getY()) + 1,
									Math.max(ent.getWorkInfo().getWorkAreaPos1().getZ(), event.getPos().getZ()) + 1);
							ent.getWorkInfo().setPosWorkArea(aabb);
							ent.getWorkInfo().setInAreaSetMode(false);
							Vec3 center = ent.getWorkInfo().getPosWorkArea().getCenter();
							//ent.restrictTo(new BlockPos(center.x, center.y+1, center.z), (int) ent.getWorkInfo().getPosWorkArea().getSize());
							double size = Math.max(ent.getWorkInfo().getPosWorkArea().getXsize(), ent.getWorkInfo().getPosWorkArea().getZsize());
							ent.restrictTo(new BlockPos(center.x, center.y+1, center.z), (int) size);
							//this.setHomePosAndDistance(BlockPos.ZERO, -1, true);
							event.getEntity().sendSystemMessage(Component.literal("Zombie Player " + ent.getGameProfile().getName() + " work area set to " + aabb));
							event.setCanceled(true);
						}
					}
				}
			}

			trainZombiePlayer(event.getEntity(), event.getLevel(), event.getPos(), EnumTrainType.BLOCK_RIGHT_CLICK, event.getFace(), event.getHitVec());
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		trainZombiePlayer(event.getEntity(), event.getLevel(), event.getPos(), EnumTrainType.BLOCK_LEFT_CLICK, event.getFace(), null);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
		//trainZombiePlayer(event.getLevel(), event.getEntity().blockPosition(), EnumTrainType.ENTITY_RIGHT_CLICK);
	}

	public void trainZombiePlayerSetClick(Player player, Level level, BlockPos pos, EnumTrainType trainType) {
		List<ZombiePlayer> listEnts = level.getEntitiesOfClass(ZombiePlayer.class, new AABB(pos).inflate(10, 5, 10));
		for (ZombiePlayer ent : listEnts) {
			if (ent.isCalm() && ent.getWorkInfo().isInTrainingMode() && ent.getOwnerUUID().equals(player.getUUID())) {
				ent.getWorkInfo().setWorkClickLastObserved(trainType);
				player.sendSystemMessage(Component.literal("Zombie Player " + ent.getGameProfile().getName() + " set click to " + trainType));
			}
		}
	}

	public void trainZombiePlayer(Player player, Level level, BlockPos pos, EnumTrainType trainType, Direction direction, BlockHitResult blockHitResult) {
		List<ZombiePlayer> listEnts = level.getEntitiesOfClass(ZombiePlayer.class, new AABB(pos).inflate(10, 5, 10));
		for (ZombiePlayer ent : listEnts) {
			if (ent.isCalm() && ent.getWorkInfo().isInTrainingMode() && ent.getOwnerUUID().equals(player.getUUID())) {
				//ent.workClickLastObserved = trainType;
				ent.getWorkInfo().setWorkClickLastObserved(trainType);
				BlockState state = ent.level.getBlockState(pos);
				ent.getWorkInfo().setStateWorkLastObserved(state);
				ent.getWorkInfo().setWorkClickDirectionLastObserved(direction);
				ent.getWorkInfo().setItemNeededForWork(player.getMainHandItem());
				ent.getWorkInfo().setBlockHitResult(blockHitResult);
				player.sendSystemMessage(Component.literal("Zombie Player " + ent.getGameProfile().getName() + " observed " + state + " using " + player.getMainHandItem() + " click: " + trainType));
				//set a basic small work area and update restriction area if no work area set yet
				if (ent.getWorkInfo().getPosWorkArea() == WorkInfo.CENTER_ZERO) {
					AABB aabb = new AABB(
							pos.getX(),
							pos.getY(),
							pos.getZ(),
							pos.getX() + 1,
							pos.getY() + 1,
							pos.getZ() + 1);
					aabb = aabb.inflate(4, 2, 4);
					ent.getWorkInfo().setPosWorkArea(aabb);
					Vec3 center = ent.getWorkInfo().getPosWorkArea().getCenter();
					ent.restrictTo(new BlockPos(center.x, center.y+1, center.z), (int) ent.getWorkInfo().getPosWorkArea().getSize());
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onGameEvent(VanillaGameEvent event) {
		event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onSpawn(LivingSpawnEvent.CheckSpawn event) {
		if (event.getEntity() instanceof ZombiePlayer && event.getSpawnReason() == MobSpawnType.NATURAL) {
			if (!ConfigZombiePlayers.Spawning_spawnZombiePlayersNaturally) {
				event.setResult(Event.Result.DENY);
			}
		}
	}
}
