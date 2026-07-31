package net.erutobusiness.zombiewarhorses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.erutobusiness.zombiewarhorses.ModEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * ゾンビ馬にバニラの馬鎧スロットを開放する。
 *  - canWearArmor → true（インベントリGUIに鎧枠が出る／鎧スロットが機能する）
 *  - isArmor → 馬鎧を受け入れる
 *  - containerChanged → 鎧枠の変更を自作の同期/属性/永続化へ橋渡し
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Inject(method = "canWearArmor", at = @At("HEAD"), cancellable = true)
    private void zwh$canWearArmor(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ZombieHorse) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isArmor", at = @At("HEAD"), cancellable = true)
    private void zwh$isArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ZombieHorse && stack.getItem() instanceof HorseArmorItem) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "containerChanged", at = @At("TAIL"))
    private void zwh$containerChanged(Container container, CallbackInfo ci) {
        if ((Object) this instanceof ZombieHorse zh) {
            ModEvents.onZombieHorseArmorChanged(zh);
        }
    }
}
