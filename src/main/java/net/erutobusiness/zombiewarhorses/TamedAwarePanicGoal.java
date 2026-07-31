package net.erutobusiness.zombiewarhorses;

import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/**
 * バニラの逃走AIを置き換える版。テイム済みの間は逃げない（＝戦う軍馬になる）。
 * 野生馬はこれまで通り逃走する。
 */
public class TamedAwarePanicGoal extends PanicGoal {
    private final AbstractHorse horse;

    public TamedAwarePanicGoal(AbstractHorse horse, double speedModifier) {
        super(horse, speedModifier);
        this.horse = horse;
    }

    @Override
    public boolean canUse() {
        return !this.horse.isTamed() && super.canUse();
    }
}
