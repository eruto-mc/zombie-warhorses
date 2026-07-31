package net.erutobusiness.zombiewarhorses.net;

import net.erutobusiness.zombiewarhorses.ZombieWarhorses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String VER = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZombieWarhorses.MODID, "main"),
            () -> VER, VER::equals, VER::equals);

    public static void init() {
        CH.registerMessage(0, ArmorSyncPacket.class,
                ArmorSyncPacket::encode, ArmorSyncPacket::decode, ArmorSyncPacket::handle);
        // client→server: Carry On 併用時でも Shift で馬から降りるための降車要求
        CH.registerMessage(1, DismountPacket.class,
                DismountPacket::encode, DismountPacket::decode, DismountPacket::handle);
    }

    /** そのエンティティを追跡中の全クライアントへ送る（装着/取り外し時）。 */
    public static void sendToTracking(Entity e, ArmorSyncPacket p) {
        CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), p);
    }

    /** 追跡を開始した特定プレイヤーへ送る（StartTracking時＝変化直後/再ログイン対応）。 */
    public static void sendTo(ServerPlayer player, ArmorSyncPacket p) {
        CH.send(PacketDistributor.PLAYER.with(() -> player), p);
    }

    private Net() {}
}
