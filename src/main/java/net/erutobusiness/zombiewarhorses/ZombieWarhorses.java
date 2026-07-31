package net.erutobusiness.zombiewarhorses;

import com.mojang.logging.LogUtils;
import net.erutobusiness.zombiewarhorses.net.Net;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Zombie Warhorses — メインエントリ。
 * 実処理は {@link ModEvents} の Forge イベントハンドラ側。
 */
@Mod(ZombieWarhorses.MODID)
public class ZombieWarhorses {
    public static final String MODID = "zombie_warhorses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZombieWarhorses() {
        Net.init();
        MinecraftForge.EVENT_BUS.register(new ModEvents());
        LOGGER.info("[zombie_warhorses] loaded");
    }
}
