package net.erutobusiness.zombiewarhorses;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * 敵対ゾンビ馬が全プレイヤーを狙うターゲットゴール。
 * canUse を !isTamed() でゲートするので、金ニンジンでテイムされた瞬間に自動で無効化＝温厚になる。
 */
public class HostileWhenWildGoal extends NearestAttackableTargetGoal<Player> {
    private final AbstractHorse horse;

    public HostileWhenWildGoal(AbstractHorse horse) {
        super(horse, Player.class, true);
        this.horse = horse;
    }

    @Override
    public boolean canUse() {
        return !this.horse.isTamed() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.horse.isTamed() && super.canContinueToUse();
    }
}
