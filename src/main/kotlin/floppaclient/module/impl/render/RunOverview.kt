package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.dungeon.RunInformation
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.RegisterHudElement
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils.timeFormat
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Based on the [Skytils Dungeon Timer](https://github.com/Skytils/SkytilsMod/blob/801bc0d29e0e50a85c3e672e7f54759e10acb558/src/main/kotlin/gg/skytils/skytilsmod/features/impl/dungeons/DungeonTimer.kt).
 *
 * @author Stivais
 */
object RunOverview : Module(
    "Run Overview",
    category = Category.RENDER,
    description = "Shows how much time your party needed to complete individual stages / splits of a dungeon run. "
) {

    private val showHud = BooleanSetting("Show HUD", default = true, description = "Render the splits in a hud.")
    private val chatMessage = BooleanSetting("Chat Message", default = false, description = "Shows a message of whenever a phase is completed.")
    private val floorSevenSplits = BooleanSetting("Custom F7 Splits", default = true, description = "Show Custom splits for Floor 7 and Master Floor 7.")

    private val xHud = NumberSetting("x", default = 3.0, visibility = Visibility.HIDDEN)
    private val yHud = NumberSetting("y", default = 150.0, visibility = Visibility.HIDDEN)
    private val scaleHud = NumberSetting("scale", 1.0, 0.1, 4.0, 0.01, Visibility.HIDDEN)

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

    init {
        this.addSettings(
            showHud,
            chatMessage,
            floorSevenSplits,
            xHud,
            yHud,
            scaleHud
        )
    }

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
                if (chatMessage.enabled) modMessage("Blood opened at§c ${timeFormat(bloodOpenTime - dungeonStart)}")
            }

            formattedText.startsWith("§r§c[BOSS] The Watcher§r§f: You have proven yourself. You may pass.§r") && bloodClearTime == -1L -> {
                bloodClearTime = System.currentTimeMillis()
                if (chatMessage.enabled) modMessage("Watcher cleared at§c ${timeFormat(bloodClearTime - bloodOpenTime)}")
            }

            text.startsWith("[BOSS]") && !text.startsWith("[BOSS] The Watcher:") && (!text.startsWith("[BOSS] Scarf") && !RunInformation.isInFloor(2))
                    && (!text.startsWith("[BOSS] Bonzo") && !RunInformation.isInFloor(1)) && bossEnterTime == -1L -> {
                bossEnterTime = System.currentTimeMillis()
                if (chatMessage.enabled) modMessage("Portal entered at §c${timeFormat(bossEnterTime - bloodClearTime)}")
            }

            formattedText.contains("§r§c☠ §r§eDefeated §r") -> {
                dungClearTime = System.currentTimeMillis()
                if (dungeonStart != -1L) modMessage("Dungeon took ${timeFormat(dungClearTime - dungeonStart)}")
            }

            floorSevenSplits.enabled -> {
                when {
                    formattedText.endsWith("§r§cPathetic Maxor, just like expected.§r") && p1ClearTime == -1L -> {
                        p1ClearTime = System.currentTimeMillis()
                        if (chatMessage.enabled) modMessage("Maxor finished at §c${timeFormat(p1ClearTime - bossEnterTime)}")
                    }

                    formattedText.endsWith("§r§cAt least my son died by your hands.§r") && p2ClearTime == -1L -> {
                        p2ClearTime = System.currentTimeMillis()
                        if (chatMessage.enabled) modMessage("Storm finished at §c${timeFormat(p2ClearTime - p1ClearTime)}")
                    }

                    text.startsWith("The Core entrance is opening!") && p3TermTime == -1L -> {
                        p3TermTime = System.currentTimeMillis()
                        if (chatMessage.enabled) modMessage("Terminals finished at §c${timeFormat(p3TermTime - p1ClearTime)}")
                    }

                    text.startsWith("[BOSS] Goldor: ....") && p3ClearTime == -1L -> {
                        p3ClearTime = System.currentTimeMillis()
                        if (chatMessage.enabled) modMessage("Goldor finished at §c${timeFormat(p3ClearTime - p3TermTime)}")
                    }

                    formattedText.endsWith("§r§cAll this, for nothing...§r") && p4ClearTime == -1L -> {
                        p4ClearTime = System.currentTimeMillis()
                        if (chatMessage.enabled) modMessage("Necron finished at §c${timeFormat(p4ClearTime - p3ClearTime)}")
                    }
                }
            }
        }
    }

    @RegisterHudElement
    object RunOverviewHUD : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("Watcher Clear: 100.0"),
        mc.fontRendererObj.FONT_HEIGHT * 3 + 3,
        scaleHud
    ) {
        override fun renderHud() {
            if (inDungeons && showHud.enabled) {
                val bloodOpen = Pair(bloodOpenTime, dungeonStart)
                val bloodClear = Pair(bloodClearTime, bloodOpenTime)
                val bossEntry = Pair(bossEnterTime, bloodClearTime)
                val maxorKill = Pair(p1ClearTime, bossEnterTime)
                val stormKill = Pair(p2ClearTime, p1ClearTime)
                val terminals = Pair(p3TermTime, p2ClearTime)
                val goldorKill = Pair(p3ClearTime, p3TermTime)
                val necronKill = Pair(p4ClearTime, p3ClearTime)

                // add check for in boss
                val bossKill: Pair<Long, Long> = if (floorSevenSplits.enabled) {
                    Pair(dungClearTime, p4ClearTime)
                } else Pair(dungClearTime, bossEnterTime)

                renderLine2("Blood Clear", bloodOpen, 0)    // blood open time
                renderLine2("Watcher Clear", bloodClear, 1)
                renderLine2("Boss Entry", bossEntry, 2)

                if (floorSevenSplits.enabled) { // add another check for inside of boss
                    renderLine2("Maxor", maxorKill, 4)
                    renderLine2("Storm", stormKill, 5)
                    renderLine2("Terminals", terminals, 6)
                    renderLine2("Goldor", goldorKill, 7)
                    renderLine2("Necron", necronKill, 8)

                    renderLine2("Wither King", bossKill, 9)
                    this.height = mc.fontRendererObj.FONT_HEIGHT * 9 + 9
                } else {
                    renderLine2("Boss Kill", bossKill, 3)
                    this.height = mc.fontRendererObj.FONT_HEIGHT * 3 + 3
                }
            }
        }

        private fun renderLine2(string: String, pair: Pair<Long, Long>, int: Int) {
            val text = "§c§l$string§r: ${
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