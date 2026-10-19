package net.erutobusiness.zombiewarhorses;

import com.mojang.logging.LogUtils;
import net.erutobusiness.zombiewarhorses.net.Net;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
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
        LOGGER.info("[zombie_warhorses] loaded (zombie horse armour handled by {})",
                vanillaBackportHandlesArmor() ? "Vanilla Backport" : "this mod");
    }

    private static Boolean vbArmor;

    /**
     * Vanilla Backport 1.2 以降はゾンビ馬に本物の鎧枠を持たせる（保存は "ArmorItem"、胸の装備へ反映、
     * 防御の属性、描画まで自前）。そのときは本MODの鎧の処理（NBT 退避・属性・描画レイヤー・同期）を止め、
     * 防御が二重に足され・鎧が二重に描かれるのを防ぐ。1.1.7.x には鎧枠が無いので本MODが受け持つ。
     */
    public static boolean vanillaBackportHandlesArmor() {
        if (vbArmor == null) {
            vbArmor = ModList.get().getModContainerById("vanillabackport")
                    .map(c -> c.getModInfo().getVersion().compareTo(new DefaultArtifactVersion("1.2")) >= 0)
                    .orElse(false);
        }
        return vbArmor;
    }
}
