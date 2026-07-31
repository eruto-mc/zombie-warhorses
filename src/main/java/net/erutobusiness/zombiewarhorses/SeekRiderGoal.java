package net.erutobusiness.zombiewarhorses;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;

/**
 * 【2026-07-28 リデザイン】旧 RideZombieHorseGoal は「全ゾンビが毎tick範囲走査」で激重＝サーバハングの原因だった。
 *
 * <p>責務を反転し、希少な {@link ZombieHorse} 側に持たせる: 騎乗者のいない敵対（未テイム）ゾンビ馬だけが、
 * {@link #SCAN_INTERVAL} tick ごとにだけ近くの騎乗可能なゾンビを1体探し、寄って行って乗せる。
 * これで走査回数が O(ゾンビ数) → O(ゾンビ馬数) に激減し、さらに間引きで実質ほぼ無負荷になる。
 * ゾンビ馬は変化でしか生まれず数が少ないため、範囲(16)は維持しても問題ない。
 */
public class SeekRiderGoal extends Goal {
    private static final double SEARCH_RANGE = 16.0D;
    private static final int SCAN_INTERVAL = 60;    // 3秒に1回だけ重い走査（大幅に間引く）
    private static final double MOUNT_DIST_SQR = 4.0D;

    private final ZombieHorse horse;
    private int cooldown = 0;
    private Zombie rider;

    public SeekRiderGoal(ZombieHorse horse) {
        this.horse = horse;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 敵対・無人のゾンビ馬だけがライダーを探す（テイム済み/騎乗済みは対象外）
        if (this.horse.isTamed() || this.horse.isVehicle()) return false;
        // 間引き: SCAN_INTERVAL tick に1回だけ getEntitiesOfClass を走らせる
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = SCAN_INTERVAL;
        this.rider = findRider();
        return this.rider != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.rider != null && this.rider.isAlive() && !this.rider.isPassenger()
                && !this.horse.isTamed() && !this.horse.isVehicle();
    }

    private Zombie findRider() {
        List<Zombie> list = this.horse.level().getEntitiesOfClass(
                Zombie.class,
                this.horse.getBoundingBox().inflate(SEARCH_RANGE),
                z -> z.isAlive() && !z.isBaby() && !z.isPassenger() && !(z instanceof ZombifiedPiglin));
        Zombie nearest = null;
        double best = Double.MAX_VALUE;
        for (Zombie z : list) {
            double d = this.horse.distanceToSqr(z);
            if (d < best) {
                best = d;
                nearest = z;
            }
        }
        return nearest;
    }

    @Override
    public void start() {
        if (this.rider != null) this.horse.getNavigation().moveTo(this.rider, 1.15D);
    }

    @Override
    public void stop() {
        this.rider = null;
        this.horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.rider == null) return;
        this.horse.getLookControl().setLookAt(this.rider, 30.0F, 30.0F);
        if (this.horse.distanceToSqr(this.rider) <= MOUNT_DIST_SQR) {
            this.rider.startRiding(this.horse);
        } else {
            this.horse.getNavigation().moveTo(this.rider, 1.15D);
        }
    }
}
