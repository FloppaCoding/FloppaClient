package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.dungeon.RunInformation
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.RegisterHudElement
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.LocationManager
import floppaclient.utils.LocationManager.inDungeons
import floppaclient.utils.Utils.timeFormat
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Similar to Skytils Necron phase Timer
 * @link https://github.com/Skytils/SkytilsMod/blob/801bc0d29e0e50a85c3e672e7f54759e10acb558/src/main/kotlin/gg/skytils/skytilsmod/features/impl/dungeons/DungeonTimer.kt
 * @author Stivais
 */
object RunOverview : Module(
    "Run Overview",
    category = Category.RENDER,
    description = "Shows how much time it took to complete stages in dungeons."
) {
    private val showHud: Boolean by BooleanSetting("Show HUD", default = true, description = "Render the splits in a hud.")
    private val chatMessage: Boolean by BooleanSetting("Chat Message", default = false, description = "Shows a message of whenever a phase is completed.")
    private val floorSevenSplits: Boolean by BooleanSetting("Custom F7 Splits", default = true, description = "Show Custom splits for Floor 7 and Master Floor 7.")

    var dungeonStart = -1L
    var bloodOpenTime = -1L
    var bloodClearTime = -1L
    var bossEnterTime = -1L
    var p1ClearTime = -1L
    var p2ClearTime = -1L
    var p3TermTime = -1L
    var p3ClearTime = -1L
    var p4ClearTime = -1L
    var dungClearTime = -1L

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons) return

        val formattedText = event.message.formattedText
        val text = StringUtils.stripControlCodes(event.message.unformattedText)

        when {
            formattedText.startsWith("§r§aDungeon starts in 1 second.§r") || formattedText.startsWith("§r§aDungeon starts in 1 second. Get ready!§r") && dungeonStart == -1L ->
                dungeonStart = System.currentTimeMillis() + 1000

            text.startsWith("The BLOOD DOOR has been opened!") || text.startsWith("[BOSS] The Watcher:") && bloodOpenTime == -1L -> {
                bloodOpenTime = System.currentTimeMillis()
                if (chatMessage) modMessage("Blood opened at§c ${timeFormat(bloodOpenTime - dungeonStart)}")
            }

            formattedText.startsWith("§r§c[BOSS] The Watcher§r§f: You have proven yourself. You may pass.§r") && bloodClearTime == -1L -> {
                bloodClearTime = System.currentTimeMillis()
                if (chatMessage) modMessage("Watcher cleared at§c ${timeFormat(bloodClearTime - bloodOpenTime)}")
            }

            text.startsWith("[BOSS]") && !text.startsWith("[BOSS] The Watcher:") && (!text.startsWith("[BOSS] Scarf") && !RunInformation.isInFloor(2))
                    && (!text.startsWith("[BOSS] Bonzo") && !RunInformation.isInFloor(1)) && bossEnterTime == -1L -> {
                bossEnterTime = System.currentTimeMillis()
                if (chatMessage) modMessage("Portal entered at §c${timeFormat(bossEnterTime - bloodClearTime)}")
            }

            formattedText.contains("§r§c☠ §r§eDefeated §r") -> {
                dungClearTime = System.currentTimeMillis()
                if (dungeonStart != -1L) modMessage("Dungeon took ${timeFormat(dungClearTime - dungeonStart)}")
            }

            floorSevenSplits && RunInformation.isInFloor(7) -> {
                when {
                    formattedText.endsWith("§r§cPathetic Maxor, just like expected.§r") && p1ClearTime == -1L -> {
                        p1ClearTime = System.currentTimeMillis()
                        if (chatMessage) modMessage("Maxor finished at §c${timeFormat(p1ClearTime - bossEnterTime)}")
                    }

                    formattedText.endsWith("§r§cAt least my son died by your hands.§r") && p2ClearTime == -1L -> {
                        p2ClearTime = System.currentTimeMillis()
                        if (chatMessage) modMessage("Storm finished at §c${timeFormat(p2ClearTime - p1ClearTime)}")
                    }

                    text.startsWith("The Core entrance is opening!") && p3TermTime == -1L -> {
                        p3TermTime = System.currentTimeMillis()
                        if (chatMessage) modMessage("Terminals finished at §c${timeFormat(p3TermTime - p1ClearTime)}")
                    }

                    text.startsWith("[BOSS] Goldor: ....") && p3ClearTime == -1L -> {
                        p3ClearTime = System.currentTimeMillis()
                        if (chatMessage) modMessage("Goldor finished at §c${timeFormat(p3ClearTime - p3TermTime)}")
                    }

                    formattedText.endsWith("§r§cAll this, for nothing...§r") && p4ClearTime == -1L -> {
                        p4ClearTime = System.currentTimeMillis()
                        if (chatMessage) modMessage("Necron finished at §c${timeFormat(p4ClearTime - p3ClearTime)}")
                    }
                }
            }
        }
    }

    @RegisterHudElement
    object RunOverviewHUD : HudElement(this, 3, 200,
        mc.fontRendererObj.getStringWidth("Watcher Clear: 100.0"),
        mc.fontRendererObj.FONT_HEIGHT * 3 + 3,
    ) {
        override fun renderHud() {
            if (inDungeons && showHud) {
                val bloodOpen = Pair(bloodOpenTime, dungeonStart)
                val bloodClear = Pair(bloodClearTime, bloodOpenTime)
                val bossEntry = Pair(bossEnterTime, bloodClearTime)
                val maxorKill = Pair(p1ClearTime, bossEnterTime)
                val stormKill = Pair(p2ClearTime, p1ClearTime)
                val terminals = Pair(p3TermTime, p2ClearTime)
                val goldorKill = Pair(p3ClearTime, p3TermTime)
                val necronKill = Pair(p4ClearTime, p3ClearTime)

                // add check for in boss
                val bossKill: Pair<Long, Long> = if (floorSevenSplits) {
                    Pair(dungClearTime, p4ClearTime)
                } else Pair(dungClearTime, bossEnterTime)

                renderLine("§c§lBlood Clear", bloodOpen, 0)    // blood open time
                renderLine("§c§lWatcher Clear", bloodClear, 1)
                renderLine("§c§lBoss Entry", bossEntry, 2)

                if (floorSevenSplits && RunInformation.isInFloor(7)) { // add another check for inside of boss
                    renderLine("§d§lMaxor", maxorKill, 4)
                    renderLine("§b§lStorm", stormKill, 5)
                    renderLine("§e§lTerminals", terminals, 6)
                    renderLine("§6§lGoldor", goldorKill, 7)
                    renderLine("§4§lNecron", necronKill, 8)
                    renderLine("§7§lWither King", bossKill, 9)
                    this.height = mc.fontRendererObj.FONT_HEIGHT * 9 + 9
                } else {
                    renderLine("§c§lBoss Kill", bossKill, 3)
                    this.height = mc.fontRendererObj.FONT_HEIGHT * 3 + 3
                }
            }
        }

        private fun renderLine(string: String, pair: Pair<Long, Long>, int: Int) {
            val text = "$string§r: ${
                if (pair.first == -1L) {
                    if (pair.second != -1L) timeFormat(System.currentTimeMillis() - pair.second)
                    else "0.0s"
                } else timeFormat(pair.first - pair.second)
            }"
            mc.fontRendererObj.drawString(text, 0, mc.fontRendererObj.FONT_HEIGHT * int + 1, 0xffffff)
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        dungeonStart = -1L
        bloodOpenTime = -1L
        bloodClearTime = -1L
        bossEnterTime = -1L
        p1ClearTime = -1L
        p2ClearTime = -1L
        p3TermTime = -1L
        p3ClearTime = -1L
        p4ClearTime = -1L
        dungClearTime = -1L
    }
}