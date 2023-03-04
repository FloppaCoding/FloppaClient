package floppaclient.utils

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.DungeonPlayer
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.mixins.MinecraftAccessor
import net.minecraft.block.BlockDoor
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockSign
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.util.*
import net.minecraft.util.Timer
import java.util.*
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * ## A general collection of utility functions.
 *
 * This is the place for all the utility functions that do not (yet) have their own class files for a category.
 *
 * @author Aton
 */
object Utils {
    //TODO clean this up and put the functions in respective classes to better sort them.

    /**
     * Referenced in the sound manager hook.
     */
    var shouldBypassVolume: Boolean = false

    /**
     * Checks whether any of the inputs in [other] fulfils structural equality (==).
     *
     * Important: collections as input may lead to unexpected behaviour. Use the spread operator on arrays:
     *
     *      obj.equalsOneOf(*arrayOf(Obj2, Obj3))
     *
     * @see identicalToOneOf
     */
    fun Any?.equalsOneOf(vararg other: Any): Boolean {
        return other.any {
            this == it
        }
    }

    /**
     * Checks whether any of the inputs in [other] fulfils referential equality (===).
     *
     * Important: collections as input may lead to unexpected behaviour. Use the spread operator on arrays:
     *
     *      obj.identicalToOneOf(*arrayOf(Obj2, Obj3))
     *
     * @see equalsOneOf
     */
    fun Any?.identicalToOneOf(vararg other: Any): Boolean {
        return other.any {
            this === it
        }
    }

    /**
     * Returns the actual block pos of the player. The value obtained by .position is shifted by 0.5 before flooring.
     */
    val EntityPlayerSP.flooredPosition: BlockPos
        get() = BlockPos(this.posX, this.posY, this.posZ)

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

    fun <K, V> MutableMap<K, V>.removeIf(filter: (Map.Entry<K, V>) -> Boolean) : Boolean {
        Objects.requireNonNull(filter)
        var removed = false
        val each: MutableIterator<Map.Entry<K, V>> = this.iterator()
        while (each.hasNext()) {
            if (filter(each.next())) {
                each.remove()
                removed = true
            }
        }
        return removed
    }

    fun getDungeonClass(tabEntries: List<Pair<NetworkPlayerInfo, String>>, playerName: String = mc.thePlayer.name): String? {
        for (i in listOf(5, 9, 13, 17, 1)) {
            val tabText = StringUtils.stripControlCodes(tabEntries[i].second).trim()
            val name = tabText.split(" ").getOrNull(1) ?: ""

            // Here the stuff to get the class
            // first check whether it is the correct player
            if (name != playerName) continue
            // this will still contain some formatting. iirc it should look like (Mage but maybe (MageVL)
            val classWithFormatting = tabText.split(" ").getOrNull(2) ?: return null
            if (classWithFormatting.contains("(DEAD)")) return null
            return classWithFormatting.drop(1)
        }
        return null
    }

    /**
     * Returns the first dungeon Teammate with the chose class. Or null if not found / dead
     */
    fun dungeonTeammateWithClass(targetClass: String, allowSelf: Boolean = false): DungeonPlayer? {
        Dungeon.getDungeonTabList()?.let{ tabList ->
            Dungeon.dungeonTeammates.forEach {
                if (!allowSelf && it.name == mc.thePlayer.name) return@forEach
                if (getDungeonClass(tabList, it.name) == targetClass) return it
            }
        }
        return null
    }

    /**
     * Returns a string formatted for minutes and seconds from a value
     */
    fun timeFormat(long: Long): String {
        val seconds = (long.toDouble() / 10).roundToInt().toDouble() / 100
        return if (seconds >= 60) {
            "${floor(seconds / 60).toInt()}m ${((seconds % 60) * 100).roundToInt().toDouble() / 100}s"
        } else {
            "${seconds}s"
        }
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

    // TODO put this in HUDRenderUtils
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

    //TODO all of the following click functions are probably better off in a fakeactions class

    /**
     * Shift left clicks the specified slot.
     */
    fun shiftClickWindow(windowId: Int, index : Int) {
        windowClick(windowId, index, 0, 1)
    }

    /**
     * Middle clicks the specified slot.
     */
    fun middleClickWindow(windowId: Int, index : Int) {
        windowClick(windowId, index, 2, 3)
    }

    /**
     * Left clicks the specified slot.
     */
    fun leftClickWindow(windowId: Int, index : Int) {
        windowClick(windowId, index, 0, 0)
    }

    /**
     * Performs a click on the specified slot.
     */
    fun windowClick(windowId: Int, index: Int, button: Int, mode: Int) {
        mc.playerController.windowClick(
            windowId,
            index,
            button,
            mode,
            mc.thePlayer
        )
    }

    val Minecraft.timer: Timer
        get() = (this as MinecraftAccessor).timer

    fun rightClick() {
        (mc as MinecraftAccessor).rightClickMouse()
    }

    fun leftClick() {
        (mc as MinecraftAccessor).clickMouse()
    }

    fun isInt(string: String): Boolean {
        return string.toIntOrNull() != null
    }
}
