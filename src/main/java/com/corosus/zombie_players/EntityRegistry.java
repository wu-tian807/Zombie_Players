package com.corosus.zombie_players;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.server.ServerStartingEvent;

@Mod.EventBusSubscriber(modid = Zombie_Players.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistry {

    // DeferredRegister 用于注册实体
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Zombie_Players.MODID);

    // 注册 ZombiePlayer 实体
    public static final RegistryObject<EntityType<ZombiePlayer>> zombie_player = ENTITY_TYPES.register("zombie_player",
            () -> EntityType.Builder.of(ZombiePlayer::new, MobCategory.MONSTER)
                    .setShouldReceiveVelocityUpdates(false)
                    .setUpdateInterval(3)
                    .setTrackingRange(128)
                    .sized(0.6F, 1.95F)
                    .build(new ResourceLocation(Zombie_Players.MODID, "zombie_player").toString())
    );

    // 在主类构造函数中初始化注册
    public static void registerEntity() {
        ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    // 初始化实体属性
    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        event.put(zombie_player.get(), ZombiePlayer.createAttributes().build());
    }

}
