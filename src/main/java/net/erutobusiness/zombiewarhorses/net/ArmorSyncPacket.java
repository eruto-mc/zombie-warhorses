package net.erutobusiness.zombiewarhorses.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * サーバー→クライアント: 指定エンティティの馬鎧を通知する。
 * 受信側はクライアント限定で ClientArmorCache に格納（DistExecutorでサーバー側classload回避）。
 */
public class ArmorSyncPacket {
    private final int entityId;
    private final ItemStack armor;

    public ArmorSyncPacket(int entityId, ItemStack armor) {
        this.entityId = entityId;
        this.armor = armor;
    }

    public static void encode(ArmorSyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeItem(p.armor);
    }

    public static ArmorSyncPacket decode(FriendlyByteBuf buf) {
        return new ArmorSyncPacket(buf.readVarInt(), buf.readItem());
    }

    public static void handle(ArmorSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.erutobusiness.zombiewarhorses.client.ClientArmorCache.put(p.entityId, p.armor)));
        ctx.get().setPacketHandled(true);
    }
}
