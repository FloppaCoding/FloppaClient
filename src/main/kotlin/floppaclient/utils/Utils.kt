package floppaclient.utils

import floppaclient.FloppaClient.Companion.CHAT_PREFIX
import floppaclient.FloppaClient.Companion.SHORT_PREFIX
import floppaclient.FloppaClient.Companion.mc
import floppaclient.mixins.MinecraftAccessor
import floppaclient.module.impl.render.ClickGui
import floppaclient.utils.ItemUtils.itemID
import floppaclient.utils.ScoreboardUtils.sidebarLines
import net.minecraft.block.BlockDoor
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockSign
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.StringUtils
import net.minecraftforge.client.ClientCommandHandler
import kotlin.math.round


object Utils {

    var shouldBypassVolume: Boolean = false

    fun Any?.equalsOneOf(vararg other: Any): Boolean {
        return other.any {
            this == it
        }
    }

    /**
     * Test whether the String contains one of the stings in the list.
     */
    fun String.containsOneOf(options: List<String>, ignoreCase: Boolean = false): Boolean {
        return this.containsOneOf(options.toSet(),ignoreCase)

    }

    /**
     * Test whether the String contains one of the stings in the list.
     */
    fun String.containsOneOf(options: Set<String>, ignoreCase: Boolean = false): Boolean {
        options.forEach{
            if (this.contains(it, ignoreCase)) return true
        }
        return false
    }

    /**
     * The current dungeon floor (1..7) or null if not in dungeon
     */
    val currentFloor: Int?
        get() {
            sidebarLines.forEach {
                val line = ScoreboardUtils.cleanSB(it)
                if (line.contains("The Catacombs (")) {
                    return line.substringAfter("(").substringBefore(")").last().digitToInt()
                }
            }
            return null
        }

    fun inF7Boss(): Boolean {
        if(currentFloor == 7) { // check whether floor is 7
            if(mc.thePlayer.posZ > 0 ) { //check whether in boss room
                return true
            }}
        return false
    }

    fun isFloor(floor: Int): Boolean {
        sidebarLines.forEach {
            val line = ScoreboardUtils.cleanSB(it)
            if (line.contains("The Catacombs (")) {
                if (line.substringAfter("(").substringBefore(")").equalsOneOf("F$floor", "M$floor")) {
                    return true
                }
            }
        }
        return false
    }

    fun modMessage(message: String) = chatMessage("${when(ClickGui.prefixStyle.index) { 0 -> CHAT_PREFIX; 1 -> SHORT_PREFIX 
        else -> ClickGui.customPrefix.text }} $message")

    fun chatMessage(message: String) {
        mc.thePlayer.addChatMessage(ChatComponentText(message))
    }

    fun sendChat(message: String) {
        mc.thePlayer.sendChatMessage(message)
    }

    /**
     * Runs the specified command. Per default sends it to the server  but has client side option.
     */
    fun command(text: String, clientSide: Boolean = false) {
        if (clientSide) ClientCommandHandler.instance.executeCommand(mc.thePlayer, "/$text")
        else mc.thePlayer?.sendChatMessage("/$text")
    }

    fun getDungeonClass(tabEntries: List<Pair<NetworkPlayerInfo, String>>): String? {
        for (i in listOf(5, 9, 13, 17, 1)) {
            val tabText = StringUtils.stripControlCodes(tabEntries[i].second).trim()
            val name = tabText.split(" ")[0]

            // Here the stuff to get the class
            // first check whether it is the correct player
            if (name != mc.thePlayer.name) continue
            // this will still contain some formatting. iirc it should look like (Mage but maybe (MageVL)
            val classWithFormatting = tabText.split(" ")[1]
            if (classWithFormatting.contains("(DEAD)")) return null
            return classWithFormatting.drop(1)
        }
        return null
    }

    /**
     * Returns the players attack speed from the tab list info.
     * If no attack speed info can be found return null.
     */
    fun getAttackspeed(): Int? {
        val nethandlerplayclient: NetHandlerPlayClient = mc.thePlayer.sendQueue
        val list = nethandlerplayclient.playerInfoMap
        list.forEach {
            //  "Attack Speed: ⚔50"
            val attackSpeedText = it?.displayName?.unformattedText ?: return@forEach
            if (!attackSpeedText.contains("Attack Speed")) return@forEach

            return attackSpeedText.substringAfter("⚔").toInt()
        }
        return null
    }

    fun renderText(
        text: String,
        x: Int,
        y: Int,
        scale: Double = 1.0,
        color: Int = 0xFFFFFF
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()
        GlStateManager.scale(scale, scale, scale)
        var yOffset = y - mc.fontRendererObj.FONT_HEIGHT
        text.split("\n").forEach {
            yOffset += (mc.fontRendererObj.FONT_HEIGHT * scale).toInt()
            mc.fontRendererObj.drawString(
                it,
                round(x / scale).toFloat(),
                round(yOffset / scale).toFloat(),
                color,
                true
            )
        }
        GlStateManager.popMatrix()
    }

    /**
     * Returns the first slot where the item name contains the gives string.
     * Returns null if no matches were found.
     * @param name The name or item ID to find.
     * @param ignoreCase Applies for the item name check.
     */
    fun findItem(name: String, ignoreCase: Boolean = false): Int? {
        for (i in 0..8) {
            if (mc.thePlayer.inventory.getStackInSlot(i)
                    ?.run { displayName.contains(name, ignoreCase) || itemID == name } == true
            ) {
                return i
            }
        }
        return null
    }

    fun isInTerminal(): Boolean {
        if (mc.thePlayer == null) return false
        val container: Container = mc.thePlayer.openContainer
        if (container !is ContainerChest) return  false
        val name = container.lowerChestInventory.name
        return name.contains("Correct all the panes!") || name.contains("Navigate the maze!") || name.contains(
            "Click in order!"
        ) || name.contains("What starts with:") || name.contains("Select all the") || name.contains("Change all to same color!") || name.contains(
            "Click the button on time!"
        )
    }

    /**
     * Check whether the player is holding the given item.
     * Checks both the name and item ID.
     * @param name The name or item ID.
     * @param ignoreCase Applies for the item name check.
     */
    fun EntityPlayerSP?.isHolding(name: String, ignoreCase: Boolean = false): Boolean {
        return this?.heldItem?.run { displayName.contains(name, ignoreCase) || itemID == name } == true
    }

    /**
     * Taken from Skytils:
     * Taken from SkyblockAddons under MIT License
     * https://github.com/BiscuitDevelopment/SkyblockAddons/blob/master/LICENSE
     * @author BiscuitDevelopment
     */
    fun playLoudSound(sound: String?, volume: Float, pitch: Float) {
        shouldBypassVolume = true
        mc.thePlayer?.playSound(sound, volume, pitch)
        shouldBypassVolume = false
    }

    fun isValidEtherwarpPos(obj: MovingObjectPosition): Boolean {
        val pos = obj.blockPos
        val sideHit = obj.sideHit

        return mc.theWorld.getBlockState(pos).block.material.isSolid && (1..2).all {
            val newPos = pos.up(it)
            val newBlock = mc.theWorld.getBlockState(newPos)
            if (sideHit === EnumFacing.UP && (newBlock.block.equalsOneOf(
                    Blocks.fire,
                    Blocks.skull
                ) || newBlock.block is BlockLiquid)
            ) return@all false
            if (sideHit !== EnumFacing.UP && newBlock.block is BlockSign) return@all false
            if (newBlock.block is BlockLadder || newBlock.block is BlockDoor) return@all false
            return@all newBlock.block.isPassable(mc.theWorld, newPos)
        }
    }

    fun rightClick() {
        (mc as MinecraftAccessor).rightClickMouse()
    }

    fun leftClick() {
        (mc as MinecraftAccessor).clickMouse()
    }
}
