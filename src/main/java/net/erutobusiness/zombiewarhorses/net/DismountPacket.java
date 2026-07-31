package net.erutobusiness.zombiewarhorses.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * クライアント→サーバー: 馬に乗っている本人の「降車したい」要求。
 *
 * <p>背景: Carry On はクライアントの {@code Minecraft.handleKeybinds()} に mixin して Shift を
 * 自前の拾い上げ処理に消費する。バニラの馬の降車はサーバー側処理（クライアントが送る入力パケットの
 * shiftKeyDown を見て発火）なので、Carry On が Shift を握ると降車入力が届かず降りられなくなる。
 *
 * <p>対策: {@link net.erutobusiness.zombiewarhorses.client.DismountFixClient} が生のキー状態
 * （{@code keyShift.isDown()}＝Carry On の消費に影響されない）を監視し、馬騎乗中に Shift を新規押下したら
 * このパケットを送信。サーバーで本人の {@code stopRiding()} を実行する。Carry On 本体は一切いじらない。
 */
public class DismountPacket {

    public DismountPacket() {}

    public static void encode(DismountPacket p, FriendlyByteBuf buf) {
        // ペイロードなし（降車したいという合図のみ）
    }

    public static DismountPacket decode(FriendlyByteBuf buf) {
        return new DismountPacket();
    }

    public static void handle(DismountPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            // 本人が何かに騎乗中のときだけ降ろす（自分自身の降車なので不正利用の余地なし）。
            if (sp != null && sp.isPassenger()) {
                sp.stopRiding();
            }
        });
        c.setPacketHandled(true);
    }
}
