package com.corosus.zombie_players;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Zombie_Players.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class WorldRegistry {

    //使用RegisterSpawnPlacementsEvent 注册玩家僵尸的生成
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event){
        event.register(EntityRegistry.zombie_player.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ZombiePlayer::canSpawn, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
