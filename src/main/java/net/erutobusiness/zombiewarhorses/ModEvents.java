package net.erutobusiness.zombiewarhorses;

import java.util.UUID;

import net.erutobusiness.zombiewarhorses.net.ArmorSyncPacket;
import net.erutobusiness.zombiewarhorses.net.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 状態遷移:
 *   野生馬 ─(テイム)→ テイム馬（飼い主が近い時、近くの敵対Mobに応戦）
 *          ─(ゾンビ系にkill)→ 敵対ゾンビ馬（全員に敵対／ゾンビが騎乗しに来る）
 *   敵対ゾンビ馬 ─(金ニンジンでバニラ馬式テイム)→ 温厚な騎乗マウント
 *
 * 装備: 変化時に鞍をNBT退避し、テイム時に再装着。
 *   馬鎧は、Vanilla Backport 1.2 以降があればその鎧枠へ入れる（保存・防御・描画は VB）。
 *   無ければNBT保持のまま防御力を属性で付与＋描画レイヤー(client)で表示（ゾンビ馬に鎧スロットが無いための代替）。
 * 変化した馬には消えない印を付ける（VB 1.2 のゾンビ馬は離れると消えるため）。
 */
public class ModEvents {

    private static final double HORSE_ATTACK = 4.0D;
    private static final double ZH_ATTACK = 5.0D;
    private static final int TAME_TEMPER_GAIN = 10;
    static final String NBT_SADDLE = "zwh_saddle";
    static final String NBT_ARMOR = "zwh_armor";
    private static final UUID ARMOR_MOD_ID = UUID.fromString("6f3b6d2a-6c2e-4f1a-9d3e-2a1b7c8d9e01");

    // --- 生成時にAIゴールを付与（tame状態でcanUse切替） ---
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity ent = event.getEntity();

        if (ent instanceof ZombieHorse zh) {
            if (!hasZwhGoal(zh)) {   // EntityJoinLevelEvent が同一entに複数回発火してもゴールを重複付与しない
                // 騎乗ロジックは「全ゾンビが探す」のではなく希少なゾンビ馬側に集約（SeekRiderGoal＝間引き済み）
                zh.goalSelector.addGoal(3, new SeekRiderGoal(zh));
                zh.goalSelector.addGoal(4, new FixedMeleeGoal(zh, 1.4D, ZH_ATTACK));
                zh.targetSelector.addGoal(2, new HostileWhenWildGoal(zh));
            }
            CompoundTag pd = zh.getPersistentData();
            if (ZombieWarhorses.vanillaBackportHandlesArmor()) {
                // VB 1.2 以降: 鎧は VB が保存・反映する。本MODが前に退避した鎧があれば VB の枠へ移して、
                // 本MODの退避と防御の属性は消す（残すと防御が二重になる）
                if (pd.contains(NBT_ARMOR)) {
                    if (zh.getSlot(401).get().isEmpty()) {
                        zh.getSlot(401).set(ItemStack.of(pd.getCompound(NBT_ARMOR)));
                    }
                    pd.remove(NBT_ARMOR);
                }
                AttributeInstance inst = zh.getAttribute(Attributes.ARMOR);
                if (inst != null) inst.removeModifier(ARMOR_MOD_ID);
            } else {
                // 保持していた鎧を鎧スロット(401)へ復元（GUI/描画/防御へ反映。set→containerChanged→同期）
                if (pd.contains(NBT_ARMOR)) {
                    zh.getSlot(401).set(ItemStack.of(pd.getCompound(NBT_ARMOR)));
                }
                refreshArmorAttribute(zh);
            }
        } else if (ent.getClass() == Horse.class) {
            Horse h = (Horse) ent;
            if (hasZwhGoal(h)) return;
            h.goalSelector.addGoal(4, new FixedMeleeGoal(h, 1.3D, HORSE_ATTACK));
            h.targetSelector.addGoal(2, new WarhorseTargetGoal(h));
            h.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
            h.goalSelector.addGoal(1, new TamedAwarePanicGoal(h, 1.2D));
        }
        // ゾンビ側にはゴールを付けない（旧 RideZombieHorseGoal は撤去。負荷は SeekRiderGoal に集約＝O(ゾンビ数)→O(ゾンビ馬数)）
    }

    /** EntityJoinLevelEvent の複数回発火によるゴール重複付与を防ぐガード（本MODのゴールが既にあるか）。 */
    private static boolean hasZwhGoal(Mob mob) {
        return mob.goalSelector.getAvailableGoals().stream().anyMatch(w -> {
                    Goal g = w.getGoal();
                    return g instanceof SeekRiderGoal || g instanceof FixedMeleeGoal || g instanceof TamedAwarePanicGoal;
                })
                || mob.targetSelector.getAvailableGoals().stream().anyMatch(w -> {
                    Goal g = w.getGoal();
                    return g instanceof WarhorseTargetGoal || g instanceof HostileWhenWildGoal;
                });
    }

    // --- テイム馬がゾンビ系に殺されたら敵対ゾンビ馬に変化（装備を保持） ---
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) return;
        Level level = horse.level();
        if (level.isClientSide()) return;
        if (!horse.isTamed()) return;

        Entity killer = event.getSource().getEntity();
        // 村人をゾンビ化させる系（ゾンビ/ハスク/ドラウド/ゾンビ村人）のみ。ゾンビピグリンは村人を変えないので除外。
        boolean undeadKiller = (killer instanceof Zombie z && !(z instanceof ZombifiedPiglin))
                || killer instanceof ZombieHorse;
        if (!undeadKiller) return;

        ZombieHorse zh = EntityType.ZOMBIE_HORSE.create(level);
        if (zh == null) return;
        zh.moveTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), horse.getXRot());

        // 鞍は NBT に退避（テイム時に再装着）
        ItemStack saddle = horse.getSlot(400).get();
        if (!saddle.isEmpty()) zh.getPersistentData().put(NBT_SADDLE, saddle.save(new CompoundTag()));
        ItemStack armor = horse.getSlot(401).get();
        if (!armor.isEmpty()) {
            if (ZombieWarhorses.vanillaBackportHandlesArmor()) {
                // VB 1.2 以降: VB の鎧枠へ直接入れる（胸の装備になるので、鎧を着た馬は昼に燃えない）
                zh.getSlot(401).set(armor.copy());
            } else {
                zh.getPersistentData().put(NBT_ARMOR, armor.save(new CompoundTag()));
            }
        }
        // 失った馬は「群れの乗り物」として残す。VB 1.2 のゾンビ馬は離れると消える作りなので、消えない印を付ける
        zh.setPersistenceRequired();

        level.addFreshEntity(zh); // onJoin が敵対AI（と、VB が無いときは鎧の属性）を付与
        level.playSound(null, zh.getX(), zh.getY(), zh.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    // --- ゾンビ馬への右クリ操作: 未テイム=金ニンジンでテイム / テイム済み=馬鎧の着脱 ---
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof ZombieHorse zh)) return;
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        // テイム済みはバニラ操作に任せる: 右クリで騎乗／スニーク右クリでインベントリ（mixinで鎧スロットが出る）
        if (zh.isTamed()) {
            return;
        }

        // 未テイム: 金ニンジンでバニラ馬式テイム（治療＝元馬復元はしない）
        if (!stack.is(Items.GOLDEN_CARROT)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!player.getAbilities().instabuild) stack.shrink(1);

        int maxTemper = zh.getMaxTemper();
        if (maxTemper > 0 && zh.getRandom().nextInt(maxTemper) < zh.getTemper()) {
            // 成功: 騎乗ゾンビを振り落とし＋テイム＋鞍復元
            zh.ejectPassengers();
            zh.setTamed(true);
            zh.setOwnerUUID(player.getUUID());
            zh.setTemper(maxTemper);
            CompoundTag pd = zh.getPersistentData();
            if (pd.contains(NBT_SADDLE)) {
                zh.getSlot(400).set(ItemStack.of(pd.getCompound(NBT_SADDLE)));
                pd.remove(NBT_SADDLE);
            }
            zh.level().broadcastEntityEvent(zh, (byte) 7); // ハート
        } else {
            zh.modifyTemper(TAME_TEMPER_GAIN);
            zh.level().broadcastEntityEvent(zh, (byte) 6); // 煙（失敗）
        }
    }

    // --- クライアントが馬を追跡開始したら、その馬の鎧を本人に同期（変化直後/再ログイン対応） ---
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (ZombieWarhorses.vanillaBackportHandlesArmor()) return; // 描画は VB が受け持つ
        if (!(event.getTarget() instanceof ZombieHorse zh)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack armor = getStoredArmor(zh);
        if (!armor.isEmpty()) {
            Net.sendTo(sp, new ArmorSyncPacket(zh.getId(), armor));
        }
    }

    /**
     * ゾンビ馬の鎧スロット(401)が変化した時に呼ばれる（mixin: AbstractHorse.containerChanged 経由）。
     * AbstractHorse は slot1 を保存しないため、永続化は自前NBTへ。さらにクライアント同期＋防御属性を更新。
     */
    public static void onZombieHorseArmorChanged(ZombieHorse zh) {
        if (zh.level().isClientSide()) return;
        if (ZombieWarhorses.vanillaBackportHandlesArmor()) return; // 保存・同期・防御は VB が受け持つ
        ItemStack armor = zh.getSlot(401).get();
        if (armor.isEmpty()) {
            zh.getPersistentData().remove(NBT_ARMOR);
        } else {
            zh.getPersistentData().put(NBT_ARMOR, armor.save(new CompoundTag()));
        }
        Net.sendToTracking(zh, new ArmorSyncPacket(zh.getId(), armor)); // 描画用同期
        refreshArmorAttribute(zh);
    }

    /** 鎧スロット(401)の内容に応じて ARMOR 属性の一時modifierを張り直す（ゾンビ馬はバニラが armor 属性を付けないため自前）。 */
    private static void refreshArmorAttribute(AbstractHorse h) {
        AttributeInstance inst = h.getAttribute(Attributes.ARMOR);
        if (inst == null) return;
        inst.removeModifier(ARMOR_MOD_ID);
        ItemStack armor = getStoredArmor(h);
        if (armor.getItem() instanceof HorseArmorItem hai) {
            inst.addTransientModifier(new AttributeModifier(
                    ARMOR_MOD_ID, "zwh_armor", hai.getProtection(), AttributeModifier.Operation.ADDITION));
        }
    }

    /** 現在の鎧（鎧スロット401が正）。 */
    public static ItemStack getStoredArmor(AbstractHorse h) {
        return h.getSlot(401).get();
    }
}
