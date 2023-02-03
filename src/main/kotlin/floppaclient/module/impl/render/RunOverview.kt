package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Based of Skytils Dungeon Timer:
 * https://github.com/Skytils/SkytilsMod/blob/801bc0d29e0e50a85c3e672e7f54759e10acb558/src/main/kotlin/gg/skytils/skytilsmod/features/impl/dungeons/DungeonTimer.kt
 * @author Stivais
 */

object RunOverview : Module(
    "Run Overview",
    category = Category.RENDER,
    description = "Shows the time each phase took of a run."
) {

    private val showHud = BooleanSetting("Show HUD", enabled = true, description = "Render the splits on a hud")
    private val sendChat = BooleanSetting("Send Chat Message", enabled = false, description = "Send a message of split")
    private val floorSevenSplits = BooleanSetting("Custom F7 Splits", enabled = true, description = "Show Custom splits for Floor 7 and Master Floor 7")

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
    var p3TermStageTime = -1L
    var p3ClearTime = -1L
    var p4ClearTime = -1L
    var dungClearTime = -1L

    var terminalDone = false
    var termStage = 0

    var termStageDid = mutableListOf<String>()

    init {
        this.addSettings(
            showHud,
            sendChat,
            floorSevenSplits,
            xHud,
            yHud,
            scaleHud
        )
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(RunOverview)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(RunOverview)
        super.onDisable()
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons) return

        val formattedText = event.message.formattedText
        val text = StringUtils.stripControlCodes(event.message.unformattedText)

        when {

            formattedText.startsWith("§r§aDungeon starts in 1 second.§r") || formattedText.startsWith("§r§aDungeon starts in 1 second. Get ready!§r") && dungeonStart == -1L -> {
                dungeonStart = System.currentTimeMillis() + 1000
            }

            text.startsWith("The BLOOD DOOR has been opened!") || text.startsWith("[BOSS] The Watcher:") && bloodOpenTime == -1L -> {
                bloodOpenTime = System.currentTimeMillis()
                if (sendChat.enabled) modMessage("Blood opened at§c ${timeFormat(bloodOpenTime, dungeonStart)}")
            }

            formattedText.startsWith("§r§c[BOSS] The Watcher§r§f: You have proven yourself. You may pass.§r") && bloodClearTime == -1L -> {
                bloodClearTime = System.currentTimeMillis()
                if (sendChat.enabled) modMessage("Watcher cleared at§c ${timeFormat(bloodClearTime, bloodOpenTime)}")
            }

            text.startsWith("[BOSS]") && !text.startsWith("[BOSS] The Watcher:") && (!text.startsWith("[BOSS] Scarf") && !Utils.isFloor(
                2
            )) && (!text.startsWith("[BOSS] Bonzo") && !Utils.isFloor(1)) && bossEnterTime == -1L -> {
                bossEnterTime = System.currentTimeMillis()
                if (sendChat.enabled) modMessage("Portal entered at §c${timeFormat(bossEnterTime, bloodClearTime)}")
            }

            formattedText.contains("§r§c☠ §r§eDefeated §r") -> {
                dungClearTime = System.currentTimeMillis()
                if (dungeonStart != -1L) modMessage("Dungeon took ${timeFormat(dungClearTime, dungeonStart)}")
            }


            floorSevenSplits.enabled -> {
                when {
                    formattedText.startsWith("§r§cPathetic Maxor, just like expected.§r") && p1ClearTime == -1L -> {
                        p1ClearTime = System.currentTimeMillis()
                        if (sendChat.enabled) {
                            if (bossEnterTime == -1L) {
                                modMessage("Maxor finished at §cUnknown")
                            } else {
                                modMessage("Maxor finished at §c${timeFormat(p1ClearTime, bossEnterTime)}")
                            }
                        }
                    }

                    formattedText.endsWith("§r§cAt least my son died by your hands.§r") && p2ClearTime == -1L -> {
                        p2ClearTime = System.currentTimeMillis()
                        if (sendChat.enabled) modMessage("Storm finished at §c${timeFormat(p2ClearTime, p1ClearTime)}")
                    }

                    text.contains("activated a terminal! (") || text.contains("completed a device! (") && (formattedText.endsWith("(§c0§a/7)§r") || formattedText.endsWith("(§c7§a/7)§r")) -> {
                        terminalDone = true
                        if (sendChat.enabled) {
                            termStage += 1
                            if (p3TermStageTime == -1L) {
                                modMessage("$termStage took §c${timeFormat(System.currentTimeMillis(), p2ClearTime)}")
                                termStageDid.add("${timeFormat(System.currentTimeMillis(), p2ClearTime)}")
                            } else {
                                modMessage("$termStage took §c${timeFormat(System.currentTimeMillis(), p3TermStageTime)}")
                                termStageDid.add("${timeFormat(System.currentTimeMillis(), p3TermStageTime)}")
                            }
                            p3TermStageTime = System.currentTimeMillis()
                        }
                    }

                    text.startsWith("The Core entrance is opening!") && p3TermTime == -1L -> {
                        p3TermTime = System.currentTimeMillis()
                        if (sendChat.enabled) {
                            modMessage("Terminals finished at §c${timeFormat(p3TermTime, p1ClearTime)}")
                            if (terminalDone) {
                                termStageDid.add("${timeFormat(p3TermTime, p3TermStageTime)}")
                                modMessage("Terminals: $termStageDid")
                            }
                        }
                    }

                    formattedText.endsWith("§r§c....§r") && p3ClearTime == -1L -> {
                        p3ClearTime = System.currentTimeMillis()
                        if (sendChat.enabled) modMessage("Goldor finished at §c${timeFormat(p3ClearTime, p3TermTime)}")
                    }

                    formattedText.endsWith("§r§cAll this, for nothing...§r") && p4ClearTime == -1L -> {
                        p4ClearTime = System.currentTimeMillis()
                        if (sendChat.enabled) modMessage("Necron finished at §c${timeFormat(p4ClearTime, p3ClearTime)}")
                    }
                }
            }

            else -> return
        }
    }

    object RunOverview : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("Watcher Clear: 100.0"),
        mc.fontRendererObj.FONT_HEIGHT * 3 + 3,
        scaleHud
    ) {
        override fun renderHud() {

            fun renderLine(a: String, b: Int) {
                mc.fontRendererObj.drawString(a, 0, mc.fontRendererObj.FONT_HEIGHT * b + 1, 0xffffff)
            }

            val bloodOpen = "§c§lBlood Open§r: ${
                if (bloodOpenTime == -1L && dungeonStart != -1L) {
                    timeFormat(System.currentTimeMillis(), dungeonStart)
                } else {
                    timeFormat(bloodOpenTime, dungeonStart)
                }
            }"

            val bloodClear = "§c§lWatcher Clear§r: ${
                if (bloodClearTime == -1L) {
                    if (bloodOpenTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), bloodOpenTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(bloodClearTime, bloodOpenTime)
                }
            }"

            val bossEntry = "§c§lBoss entry§r: ${
                if (bossEnterTime == -1L) {
                    if (bloodClearTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), bloodClearTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(bossEnterTime, bloodClearTime)
                }
            }"

            val maxor = "§c§lMaxor§r: ${
                if (p1ClearTime == -1L) {
                    if (bossEnterTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), bossEnterTime))
                    } else {
                        if (dungeonStart != -1L) {
                            "0.0"
                        } else {
                            "Unknown"
                        }
                    }
                } else {
                    timeFormat(p1ClearTime, bossEnterTime)
                }
            }"

            val storm = "§c§lStorm§r: ${
                if (p2ClearTime == -1L) {
                    if (p1ClearTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), p1ClearTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(p2ClearTime, p1ClearTime)
                }
            }"

            val terminals = "§c§lTerminals§r: ${
                if (p3TermTime == -1L) {
                    if (p2ClearTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), p2ClearTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(p3TermTime, p2ClearTime)
                }
            }"

            val goldor = "§c§lGoldor§r: ${
                if (p3ClearTime == -1L) {
                    if (p2ClearTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), p3ClearTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(p3ClearTime, p3TermTime)
                }
            }"

            val necron = "§c§lNecron§r: ${
                if (p4ClearTime == -1L) {
                    if (p3ClearTime != -1L) {
                        (timeFormat(System.currentTimeMillis(), p3ClearTime))
                    } else {
                        "0.0"
                    }
                } else {
                    timeFormat(p4ClearTime, p3ClearTime)
                }
            }"

            val bossKill: String

            if (floorSevenSplits.enabled && Utils.inF7Boss()) {
                bossKill = "§c§lWither King§r: ${
                    if (dungClearTime == -1L) {
                        if (p4ClearTime != -1L) {
                            (timeFormat(System.currentTimeMillis(), p4ClearTime))
                        } else {
                            "0.0"
                        }
                    } else {
                        timeFormat(dungClearTime, p4ClearTime)
                    }
                }"
            } else {
                bossKill = "§c§lBoss§r: ${
                    if (dungClearTime == -1L) {
                        if (bossEnterTime != -1L) {
                            (timeFormat(System.currentTimeMillis(), bossEnterTime))
                        } else {
                            "0.0"
                        }
                    } else {
                        timeFormat(dungClearTime, bossEnterTime)
                    }
                }"
            }

            if (inDungeons && showHud.enabled) {

                renderLine(bloodOpen, 0) // blood open time
                renderLine(bloodClear, 1) // blood clear time
                this.width = mc.fontRendererObj.getStringWidth(bloodClear)

                renderLine(bossEntry, 2) // boss entry
                if (floorSevenSplits.enabled && Utils.isFloor(7)) {
                    if (!Utils.inF7Boss()) return

                    renderLine(maxor, 3) // maxor clear time
                    renderLine(storm, 4) // storm clear time
                    renderLine(terminals, 5) // terminal time
                    renderLine(goldor, 6) // goldor clear time
                    renderLine(necron, 7) // necron clear time

                    if (Utils.isInM7()) {

                        renderLine(bossKill, 8) //wither king/kill time
                        this.height = mc.fontRendererObj.FONT_HEIGHT * 8 + 1
                    }
                } else {
                    renderLine(bossKill, 3)
                    this.height = mc.fontRendererObj.FONT_HEIGHT * 3 + 1
                }
            }
            super.renderHud()
        }
    }
    private fun timeFormat(a: Long, b: Long): String {
        val seconds = ((a - b).toDouble() / 10).roundToInt().toDouble() / 100
        return if (seconds >= 60) {
            "${floor(seconds / 60).toInt()}m ${((seconds % 60) * 100).roundToInt().toDouble() / 100}s"
        } else {
            "${seconds}s"
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
        p3TermStageTime = -1L
        p3ClearTime = -1L
        p4ClearTime = -1L
        dungClearTime = -1L

        terminalDone = false

        termStage = 0
        termStageDid.clear()
    }
}