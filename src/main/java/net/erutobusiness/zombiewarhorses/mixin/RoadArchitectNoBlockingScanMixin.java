package net.erutobusiness.zombiewarhorses.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * RoadArchitect 1.6.1 の「参加・チャンク生成のたびにサーバが固まる」対策。
 *
 * <p>症状: プレイヤーが参加すると {@code RoadPipelineController.onPlayerJoin} から村の探索が走り、
 * {@code StructureLocator.resolveCandidatesOnMainThread} が**メインスレッドで** {@code getChunk}
 * （＝未生成なら生成完了まで同期待ち）を呼ぶ。未探索方向では待ちが積み上がり、
 * 専用サーバ16GB・32コアでも RCON が応答しなくなる（2026-07-30 実測。jstack で確認）。
 * Chunky 事前生成中は同じ経路が {@code onChunkGenerated} から走り、60秒tickでwatchdogクラッシュした
 * （2026-07-29）。範囲縮小（initScanRadius 125→50）でも消えない。
 *
 * <p>対策: MOD自身が持っている「チャンクを読み込まない」経路へ倒す。
 * {@code resolveCandidatesOnMainThread(..., boolean allowChunkLoads)} の最後の引数を常に false にする。
 * MOD内には {@code "Skipping chunk load for candidate {} due to allowChunkLoads=false"} という
 * 分岐が元から用意されており、false のときは未生成チャンクの候補を単に飛ばす。
 *
 * <p>失うもの: **未生成領域にある村は道の接続対象から外れる**。
 * 当部は Chunky で事前生成してから公開する運用なので、生成済み範囲では従来どおり道が繋がる。
 * 「歩いて未踏方向へ出た瞬間に固まる」より、「未踏の村は後から道が付く」ほうが実害が小さい
 * （事前生成を進めれば対象に入る）。
 *
 * <p>remap=false: 対象は RoadArchitect 自身のメソッドで SRG マッピング対象外。
 * RoadArchitect 未導入なら注入対象が無く適用スキップ（非致命）。
 */
@Mixin(targets = "net.oxcodsnet.roadarchitect.util.StructureLocator", remap = false)
public class RoadArchitectNoBlockingScanMixin {

    /**
     * {@code resolveCandidatesOnMainThread} の allowChunkLoads を false に落とす。
     * private static メソッドの引数なので argsOnly + 型指定で最後の boolean を掴む。
     */
    @ModifyVariable(method = "resolveCandidatesOnMainThread", at = @At("HEAD"),
                    argsOnly = true, ordinal = 0, remap = false)
    private static boolean zombie_warhorses$neverLoadChunksOnMainThread(boolean allowChunkLoads) {
        return false;
    }
}
