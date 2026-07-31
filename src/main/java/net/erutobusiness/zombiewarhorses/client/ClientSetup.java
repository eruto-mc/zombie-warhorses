package net.erutobusiness.zombiewarhorses.client;

import net.erutobusiness.zombiewarhorses.ZombieWarhorses;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ゾンビ馬のレンダラーに馬鎧描画レイヤーを後付けする（クライアント・MODバス）。
 * mixin 不要で Forge のイベントだけで実現。
 */
@Mod.EventBusSubscriber(modid = ZombieWarhorses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?> renderer = event.getRenderer(EntityType.ZOMBIE_HORSE);
        if (renderer instanceof LivingEntityRenderer lr) {
            HorseModel armorModel = new HorseModel(event.getEntityModels().bakeLayer(ModelLayers.HORSE_ARMOR));
            lr.addLayer(new ZombieHorseArmorLayer(lr, armorModel));
        }
    }
}
