package net.erutobusiness.zombiewarhorses.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.ItemStack;

/**
 * クライアント側で「エンティティID → 装備中の馬鎧」を保持する。
 * サーバーの persistentData はクライアントに同期されないため、専用パケットで受け取ってここに入れ、
 * 描画レイヤー(ZombieHorseArmorLayer)がここから読む。
 */
public class ClientArmorCache {
    private static final Map<Integer, ItemStack> MAP = new ConcurrentHashMap<>();

    public static void put(int entityId, ItemStack armor) {
        if (armor == null || armor.isEmpty()) {
            MAP.remove(entityId);
        } else {
            MAP.put(entityId, armor);
        }
    }

    public static ItemStack get(int entityId) {
        return MAP.getOrDefault(entityId, ItemStack.EMPTY);
    }

    public static void remove(int entityId) {
        MAP.remove(entityId);
    }
}
