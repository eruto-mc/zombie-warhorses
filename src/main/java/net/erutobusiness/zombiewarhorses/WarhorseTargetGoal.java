package net.erutobusiness.zombiewarhorses;

import java.util.UUID;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/**
 * テイム馬が近くの敵対Mob（Monster全般＝ゾンビ等）を自発的に狙うターゲットゴール。
 * 発動条件: テイム済み ＆ 飼い主が近く(OWNER_RANGE以内)にいる。
 *   → 放置/ログアウト中の馬が単独で敵に突撃して自滅する事故を防ぐ（飼い主の戦いに参加する形）。
 * バニラのテイム狼(OwnerHurtTargetGoal等)は飼い主の攻撃対象にだけ応じるが、
 *   本MODは「軍馬が周囲の敵に自発応戦」する仕様なので、飼い主近接を条件にして暴走だけ抑える。
 */
public class WarhorseTargetGoal extends NearestAttackableTargetGoal<Monster> {
    // 根拠: バニラのテイム狼は飼い主から12ブロック超で「離れすぎ」とみなしテレポートする
    //   （＝ペットと飼い主が"近い"と判定される正準距離）。それに合わせて交戦を飼い主12ブロック以内に限定。
    private static final double OWNER_RANGE = 12.0D;
    private final AbstractHorse horse;

    public WarhorseTargetGoal(AbstractHorse horse) {
        super(horse, Monster.class, true);
        this.horse = horse;
    }

    private boolean ownerNearby() {
        UUID id = this.horse.getOwnerUUID();
        if (id == null) return false;
        Player owner = this.horse.level().getPlayerByUUID(id);
        return owner != null && owner.isAlive()
                && owner.distanceToSqr(this.horse) <= OWNER_RANGE * OWNER_RANGE;
    }

    @Override
    public boolean canUse() {
        return this.horse.isTamed() && ownerNearby() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.horse.isTamed() && ownerNearby() && super.canContinueToUse();
    }
}
