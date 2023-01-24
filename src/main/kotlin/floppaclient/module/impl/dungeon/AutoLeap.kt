package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils
import floppaclient.utils.Utils.inF7Boss
import floppaclient.utils.Utils.isHoldingOneOf
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils.rightClick
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ContainerChest
import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AutoLeap : Module(
    "Fast Leap",
    category = Category.DUNGEON,
    description = "Instantly leaps to the Selected teammate on leap left click."
){
    private val brTarget: SelectorSetting
    private val brTargetName = StringSetting("Br Target Name", description = "Name of the player you want to target during blood rush. This is only active when Custom is selected for Br target.")
    private val disableAfterBr = BooleanSetting("Disable after Br", true, description = "Disables auto leap after blood was opened.")
    private val bossTarget: SelectorSetting
    private val bossTargetName = StringSetting("Boss Target Name", description = "Name of the player you want to target in Boss. This is only active when Custom Boss is selected for Boss target.")


    private var doorOpener: String? = null
    private var bloodOpened = false
    private var thread: Thread? = null

    init {
        val targets = arrayListOf(
            "Healer",
            "Archer",
            "Berserk",
            "Tank",
            "Mage",
            "Furtherst",
            "Custom Boss",
            "None"
        )
        val clearTargets = arrayListOf(
            "Healer",
            "Archer",
            "Berserk",
            "Tank",
            "Mage",
            "Door opener",
            "Custom",
            "None"
        )
        bossTarget = SelectorSetting("Boss Target", targets[5], targets, description = "This Player will be targeted while in f7 boss.")
        brTarget = SelectorSetting("Br Target", clearTargets[5], clearTargets, description = "This Player will be targeted during blood rush.")
        this.addSettings(
            brTarget,
            brTargetName,
            disableAfterBr,
            bossTarget,
            bossTargetName
        )
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons || event.type.toInt() == 2) return
        val message = stripControlCodes(event.message.unformattedText)
        if (message.endsWith(" opened a WITHER door!")) {
            val opener = message.substringBefore(" ")
            if (opener != mc.thePlayer.name) {
                doorOpener = opener
            }
        } else if (message == "The BLOOD DOOR has been opened!") {
            bloodOpened = true
        }
    }

    @SubscribeEvent
    fun onClick(event: ClickEvent.LeftClickEvent) {
        if (!inDungeons) return
        if (disableAfterBr.enabled && bloodOpened && (!inF7Boss() || bossTarget.isSelected("None"))) return
        if (mc.thePlayer.isHoldingOneOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP") && doorOpener != null) {
            val mode = if (inF7Boss()) bossTarget.selected
            else if(!bloodOpened || !disableAfterBr.enabled) brTarget.selected
            else "None"
            if (mode == "None") return
            val name: String = when(mode) {
                "Healer", "Archer", "Berserk", "Tank", "Mage" -> {
                    Utils.dungeonTeammateWithClass(mode)?.name
                }
                "Furtherst" -> {
                    getFurthestTeammates()?.firstOrNull()?.name
                }
                "Door opener" -> {
                    doorOpener
                }
                "Custom" -> {
                    if (brTargetName.text != "") brTargetName.text else null
                }
                "Custom Boss" -> {
                    if (bossTargetName.text != "") bossTargetName.text else null
                }
                else -> {
                    null
                }
            } ?: return
            event.isCanceled = true
            rightClick()
            if (thread == null || !thread!!.isAlive) {
                thread = Thread({
                    for (i in 0..100) {
                        if (mc.thePlayer.openContainer is ContainerChest) {
                            val invSlots = mc.thePlayer.openContainer.inventorySlots
                            invSlots.subList(10, 19).find {
                                it.stack?.displayName?.contains(name) == true
                            }?.let {
                                mc.playerController.windowClick(
                                    mc.thePlayer.openContainer.windowId,
                                    it.slotNumber,
                                    2,
                                    3,
                                    mc.thePlayer
                                )
                                return@Thread
                            }
                        }
                        Thread.sleep(20)
                    }
                    modMessage("§aFast Spirit Leap failed for player $name")
                }, "Auto Spirit Leap")
                thread!!.start()
            }
        }
    }

    private fun getFurthestTeammates(): List<EntityPlayer>?{
        val xPos = mc.thePlayer.posX
        val yPos = mc.thePlayer.posY
        val zPos = mc.thePlayer.posZ

        val possibleTargets = mc.theWorld.playerEntities.filter { player ->
            Dungeon.dungeonTeammates.any {
                it.player == player && !it.dead && player != mc.thePlayer
            }
        }

        val target: List<EntityPlayer>? = if(yPos > 212) { //P1 --> leap to green pillar
            possibleTargets.filter {
                it.posY > 160 && it.posY < 212
            }.sortedBy {
                it.getDistance(46.0, 169.0, 47.0)
            }
        }else if(yPos > 160) { //P2
            possibleTargets.filter {
                it.posY > 104 && it.posY < 160
            }.sortedBy {
                it.getDistance(107.0, 120.0, 93.0)
            }
        }else if(yPos > 104) { //P3 only devices for now
            // R1: 89<X<111, 52<Z<122 +Z direction
            // SS: 107,120,93; T1: 109,113,73; T2: 109,119,79; T3: 92,112,92; T4 92,122,101, Lev(R): 95,123,113; Lev(L): 105?,123,113;
            // R2: 18<X<90, 121<Z<143 -X direction
            // R3: -3<X<18, 51<Z<120 -Z direction
            // Dev3: 2,120,77
            // R4: 20<X<89, 29<Z<50 +X direction
            if(xPos >= 90 && zPos in 53.0..121.0) { //part 1
                possibleTargets.filter {
                    it.posY > 104 && it.posY < 160 && it.posZ > 115
                }.sortedBy {
                    it.getDistance(90.0, 116.0, 132.0)
                }
            }else if(zPos > 121 && xPos >= 19 && xPos <= 89) { // part 2
                possibleTargets.filter {
                    it.posY > 104 && it.posY < 160 && it.posX < 25
                }.sortedBy {
                    it.getDistance(6.0, 116.0, 122.0)
                }
            }else if(xPos < 18 && zPos < 120 && zPos > 51) { // part 3
                possibleTargets.filter {
                    it.posY > 104 && it.posY < 160 && it.posZ < 56
                }.sortedBy {
                    it.getDistance(30.0, 116.0, 39.0)
                }
            }else if(zPos < 50 && xPos < 89 && xPos > 20) { // part 4
                possibleTargets.filter {
                    it.posY > 104 && it.posY < 160
                }.sortedBy {
                    it.getDistance(54.0, 116.0, 59.0)
                }
            }else { // default for P3
                null
            }
        }else if(yPos > 53) { //P4 --> mid clip / side clips
//            //ring platform y = 64; good pos: 70,64,105 (right); x= 36 for left?
//            //middle platform y = 64; good pos: 53,65,83 (should be on first block of first ledge)
            if (zPos > 85){
                possibleTargets.filter {
                    it.posY >= 63 && it.posY < 100
                }.sortedBy {
                    it.getDistance(53.5, 64.0, 83.5)
                }
            }else {
                possibleTargets.filter {
                    it.posY < 63 && it.posY > 2
                }.sortedBy {
                    it.getDistance(54.0, 5.0, 78.0)
                }
            }
        }else {
            possibleTargets.filter {
                it.posY < 63 && it.posY > 2
            }.sortedBy {
                it.getDistance(55.0, 5.0, 45.0)
            }
        }
        return target
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load?) {
        doorOpener = null
        bloodOpened = false
    }
}