package net.erutobusiness.zombiewarhorses.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * ゾンビ馬に、NBTに保持した馬鎧を描画する。バニラの HorseArmorLayer 相当を
 * ゾンビ馬向けに用意（ゾンビ馬のレンダラーには鎧描画層が無いため）。
 */
public class ZombieHorseArmorLayer extends RenderLayer<AbstractHorse, HorseModel<AbstractHorse>> {
    private final HorseModel<AbstractHorse> armorModel;

    public ZombieHorseArmorLayer(RenderLayerParent<AbstractHorse, HorseModel<AbstractHorse>> parent,
                                 HorseModel<AbstractHorse> armorModel) {
        super(parent);
        this.armorModel = armorModel;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buf, int light, AbstractHorse horse,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!(horse instanceof ZombieHorse)) return;
        ItemStack armor = ClientArmorCache.get(horse.getId());
        if (!(armor.getItem() instanceof HorseArmorItem hai)) return;

        this.getParentModel().copyPropertiesTo(this.armorModel);
        this.armorModel.prepareMobModel(horse, limbSwing, limbSwingAmount, partialTick);
        this.armorModel.setupAnim(horse, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(hai.getTexture()));
        this.armorModel.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
