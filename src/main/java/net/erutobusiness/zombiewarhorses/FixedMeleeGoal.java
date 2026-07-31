package net.erutobusiness.zombiewarhorses;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * 馬/ゾンビ馬は ATTACK_DAMAGE 属性を持たないため、doHurtTarget() は使えない。
 * MeleeAttackGoal の経路探索はそのまま使い、命中処理だけ固定ダメージで上書きする。
 * ターゲットの選定は TargetGoal 側（WarhorseTargetGoal / HostileWhenWildGoal）が行う。
 */
public class FixedMeleeGoal extends MeleeAttackGoal {
    private final float damage;

    public FixedMeleeGoal(PathfinderMob mob, double speedModifier, double damage) {
        super(mob, speedModifier, true);
        this.damage = (float) damage;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTargetSqr) {
        double reach = this.getAttackReachSqr(target);
        if (distToTargetSqr <= reach && this.getTicksUntilNextAttack() <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            target.hurt(this.mob.damageSources().mobAttack(this.mob), this.damage);
        }
    }
}
