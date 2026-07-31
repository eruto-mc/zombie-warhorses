package net.erutobusiness.zombiewarhorses.client;

import net.erutobusiness.zombiewarhorses.ZombieWarhorses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * dev専用スクショ自動化（zwh.screenshot=true の時だけ動作）。
 * 1回の起動で4状態を各1枚ずつ撮る:
 *   01 世界・鎧なし / 02 世界・鎧あり / 03 インベントリ空スロット / 04 インベントリ鎧装着
 *
 * 検証を「サバイバルの実操作」で行うため、GUIを直接開いたり slot に直挿ししない:
 *   - 騎乗 = 実際の右クリック相互作用（gameMode.interact → InteractPacket）
 *   - インベントリ = 騎乗中に E を押すのと同じ OPEN_INVENTORY コマンドパケット
 *   - 装備 = 金馬鎧を所持品からGUI内でシフトクリック（quick-move）で鎧スロットへ
 * これにより mixin の canWearArmor/isArmor 判定とスロットの mayPlace を実際に通す。
 */
@Mod.EventBusSubscriber(modid = ZombieWarhorses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScreenshotHarness {
    private static final boolean ENABLED = Boolean.getBoolean("zwh.screenshot");
    private static int t = 0;
    private static int horseId = -1;
    private static boolean done = false;
    private static boolean spawned, mounted, invOpened, placed, closed;
    // 馬とプレイヤー観察点の座標（spawn時に確定）
    private static double sx, sy, sz;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) { t = 0; return; }
        t++;
        final IntegratedServer server = mc.getSingleplayerServer();

        // フォーカス喪失で自動ポーズ→内蔵サーバー停止＋ポーズ画面が写り込むのを防ぐ
        if (t == 2) {
            mc.options.pauseOnLostFocus = false;
            if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        }

        // --- t=40: 掃除→観察点へ配置→金馬鎧を所持→凍結テイム馬をスポーン ---
        if (t == 40 && !spawned) {
            spawned = true;
            sx = mc.player.getX(); sz = mc.player.getZ(); sy = 130.0D;
            server.execute(() -> {
                ServerLevel lvl = server.overworld();
                lvl.setDayTime(6000L);
                for (Entity old : lvl.getAllEntities()) {            // 過去の試運転の残骸を掃除
                    if (old instanceof ZombieHorse) old.discard();
                }
                ServerPlayer sp = firstPlayer(server);
                if (sp != null) {
                    sp.getAbilities().mayfly = true; sp.getAbilities().flying = true; sp.onUpdateAbilities();
                    sp.teleportTo(lvl, sx, sy, sz, 0.0F, 8.0F);       // 馬の6ブロック手前・馬を向く
                    // 金馬鎧は「手に持たない」枠(所持品スロット9)へ。手に持つと右クリックが騎乗でなく直接装備になる
                    sp.getInventory().selected = 0;
                    sp.getInventory().items.set(0, ItemStack.EMPTY); // 手は空＝右クリックで騎乗
                    sp.getInventory().items.set(9, new ItemStack(Items.GOLDEN_HORSE_ARMOR));
                }
                ZombieHorse zh = EntityType.ZOMBIE_HORSE.create(lvl);
                if (zh == null) return;
                zh.moveTo(sx, sy, sz + 6.0D, 90.0F, 0.0F);
                zh.setYBodyRot(90.0F); zh.setYHeadRot(90.0F);
                zh.setNoGravity(true); zh.setNoAi(true); zh.setTamed(true);
                if (sp != null) zh.setOwnerUUID(sp.getUUID());
                lvl.addFreshEntity(zh);
                horseId = zh.getId();
            });
        }

        if (t == 88 && mc.screen instanceof PauseScreen) mc.setScreen(null);
        if (t == 90) grab(mc, "zwh_01_world_plain.png");             // 世界・鎧なし

        // --- t=95: 騎乗できる距離まで歩み寄る（馬の2ブロック手前）＋非スニーク ---
        if (t == 95) {
            server.execute(() -> {
                ServerPlayer sp = firstPlayer(server);
                if (sp != null) sp.teleportTo(server.overworld(), sx, sy, sz + 4.0D, 0.0F, 8.0F);
            });
            mc.player.setShiftKeyDown(false);
        }

        // --- t=100: 実際の右クリックで騎乗（InteractPacket） ---
        if (t == 100 && !mounted) {
            mounted = true;
            Entity e = mc.level.getEntity(horseId);
            if (e != null && mc.gameMode != null) {
                mc.gameMode.interact(mc.player, e, InteractionHand.MAIN_HAND);
            }
        }

        // --- t=120: 騎乗中に E（＝OPEN_INVENTORY コマンド。サーバが openCustomInventoryScreen を呼ぶ） ---
        if (t == 120 && !invOpened) {
            invOpened = true;
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(
                        mc.player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
            }
        }

        if (t == 118) ZombieWarhorses.LOGGER.info("[zwh] mounted? passenger={} vehicle={}",
                mc.player.isPassenger(), mc.player.getVehicle() == null ? "none" : mc.player.getVehicle().getType());
        if (t == 149) ZombieWarhorses.LOGGER.info("[zwh] screen={}",
                mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
        if (t == 150) grab(mc, "zwh_03_inv_empty.png");             // インベントリ・鎧スロット空

        // --- t=160: 所持品の金馬鎧をGUI内でシフトクリック→鎧スロットへ quick-move ---
        if (t == 160 && !placed) {
            placed = true;
            int slotIndex = findPlayerArmorSlot(mc);
            if (slotIndex >= 0 && mc.gameMode != null) {
                mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
                ZombieWarhorses.LOGGER.info("[zwh] shift-click armor from player slot {}", slotIndex);
            } else {
                ZombieWarhorses.LOGGER.warn("[zwh] armor not found in open menu (index={})", slotIndex);
            }
        }

        if (t == 210) grab(mc, "zwh_04_inv_armored.png");           // インベントリ・鎧装着

        // --- t=220: GUIを閉じる（実際のクローズ）→ t=225: 下馬＋観察点へ戻る ---
        if (t == 220 && !closed) { closed = true; mc.player.closeContainer(); }
        if (t == 225) {
            server.execute(() -> {
                ServerPlayer sp = firstPlayer(server);
                if (sp != null) {
                    sp.stopRiding();
                    sp.getAbilities().mayfly = true; sp.getAbilities().flying = true; sp.onUpdateAbilities();
                    sp.teleportTo(server.overworld(), sx, sy, sz, 0.0F, 8.0F);
                }
            });
        }

        if (t == 270) {                                             // 世界・鎧あり
            grab(mc, "zwh_02_world_armored.png");
            done = true;
            ZombieWarhorses.LOGGER.info("[zwh] ALL 4 SCREENSHOTS DONE");
        }
    }

    /** 開いている馬インベントリGUIの中で、プレイヤー所持品側にある金馬鎧のスロット番号を探す。 */
    private static int findPlayerArmorSlot(Minecraft mc) {
        if (mc.player == null || mc.player.containerMenu == null) return -1;
        var slots = mc.player.containerMenu.slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.container instanceof Inventory && s.getItem().is(Items.GOLDEN_HORSE_ARMOR)) return i;
        }
        return -1;
    }

    private static ServerPlayer firstPlayer(IntegratedServer server) {
        return server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                (Component c) -> ZombieWarhorses.LOGGER.info("[zwh] shot {} : {}", name, c.getString()));
        ZombieWarhorses.LOGGER.info("[zwh] GRAB {}", name);
    }
}
