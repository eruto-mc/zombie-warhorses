package net.erutobusiness.zombiewarhorses.client;

import net.erutobusiness.zombiewarhorses.ZombieWarhorses;
import net.erutobusiness.zombiewarhorses.net.DismountPacket;
import net.erutobusiness.zombiewarhorses.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Carry On がクライアントの handleKeybinds で Shift を消費し、馬の降車入力を潰す問題への対策（クライアント）。
 *
 * <p>生のキー状態 {@code keyShift.isDown()}（Carry On の consumeClick に影響されない物理キー状態）を毎tick監視し、
 * 馬系（{@link AbstractHorse}＝馬/ロバ/ラバ/スケルトン馬/ゾンビ馬）に騎乗中に Shift を新規押下した瞬間に
 * {@link DismountPacket} をサーバーへ送る。サーバー側で本人を stopRiding する。
 *
 * <p>Carry On がロードされていない環境ではバニラ降車も並行して働くが、stopRiding は二重でも無害（no-op）。
 */
@Mod.EventBusSubscriber(modid = ZombieWarhorses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DismountFixClient {

    private static boolean prevShift = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        // GUIを開いている間・プレイヤー不在時は無効（誤爆防止）
        if (p == null || mc.screen != null) {
            prevShift = false;
            return;
        }

        boolean shift = mc.options.keyShift.isDown();
        boolean justPressed = shift && !prevShift;
        prevShift = shift;

        // 馬系に騎乗中に Shift を新規押下 → サーバーへ降車要求
        if (justPressed && p.getVehicle() instanceof AbstractHorse) {
            Net.CH.sendToServer(new DismountPacket());
        }
    }

    private DismountFixClient() {}
}
