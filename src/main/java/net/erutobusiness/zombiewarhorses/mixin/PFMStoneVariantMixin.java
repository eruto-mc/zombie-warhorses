package net.erutobusiness.zombiewarhorses.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Paladin's Furniture (PFM) × Decorative Paths の紫黒（テクスチャ欠損）対策。
 *
 * <p>PFMは {@code StoneVariantRegistry.getVariantFromBlock(Block, ResourceLocation)} でブロック名を
 * 部分一致（"diorite"/"andesite"/"granite"等を contains）判定し、石マテリアルとして家具化する。
 * Decorative Paths の {@code diorite_path_14} 等は名前に石種を含むため拾われるが、パスブロックの特殊テクスチャを
 * PFMが解決できず、生成された家具（kitchen_counter/chair）が紫黒になる（PFM issue #215/#262 と同種）。
 *
 * <p>対策: namespace が {@code decorativepaths} のブロックは石マテリアルとして認識させない
 * （{@code Optional.empty()} を返す）。他MOD・バニラの家具生成は一切変えない外科的除外。
 * PFM未導入環境ではターゲット不在で本mixinは適用スキップ（非致命）。
 *
 * <p>remap=false: 対象はPFM自身のメソッドで、SRGマッピング対象外のため名前をそのまま使う。
 */
@Mixin(targets = "com.unlikepaladin.pfm.data.materials.StoneVariantRegistry", remap = false)
public class PFMStoneVariantMixin {

    @Inject(method = "getVariantFromBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void eruto$excludeDecorativePaths(Block block, ResourceLocation id, CallbackInfoReturnable<Optional<?>> cir) {
        if (id != null && "decorativepaths".equals(id.getNamespace())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
