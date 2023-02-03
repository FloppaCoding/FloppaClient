package floppaclient.utils

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.DungeonPlayer
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.mixins.MinecraftAccessor
import floppaclient.utils.ItemUtils.itemID
import floppaclient.utils.ScoreboardUtils.sidebarLines
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
import kotlin.math.round

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

    /**
     * The current dungeon floor (1..7) or null if not in dungeon
     * @see [floppaclient.floppamap.dungeon.RunInformation]
     */
    val currentFloor: Int?
        get() {
            // TODO merge this with Run INformation?
            sidebarLines.forEach {
                val line = ScoreboardUtils.cleanSB(it)
                if (line.contains("The Catacombs (")) {
                    return line.substringAfter("(").substringBefore(")").last().digitToIntOrNull()
                }
            }
            return null
        }

    fun inF7Boss(): Boolean {
        if (!inDungeons) return false
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

    fun isInM7(): Boolean {
        sidebarLines.forEach {
            val line = ScoreboardUtils.cleanSB(it)
            if (line.contains("The Catacombs (M7)")) {
                    return true
                }
            }
        return false
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

    /**
     * Returns the current area from the tab list info.
     * If no info can be found return null.
     */
    fun getArea(): String? {
        if (!FloppaClient.inSkyblock) return null
        val nethandlerplayclient: NetHandlerPlayClient = mc.thePlayer?.sendQueue ?: return null
        val list = nethandlerplayclient.playerInfoMap ?: return null
        var area: String? = null
        var extraInfo: String? = null
        for (entry in list) {
            //  "Area: Hub"
            val areaText = entry?.displayName?.unformattedText ?: continue
            if (areaText.startsWith("Area: ")) {
                area = areaText.substringAfter("Area: ")
                if (!area.contains("Private Island")) break
            }
            if (areaText.contains("Owner:")){
                extraInfo = areaText.substringAfter("Owner:")
            }

        }
        return if (area == null)
            null
        else
            area + (extraInfo ?: "")
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

    /**
     * Returns the first slot where the item name or id passes a check for the name.
     * The item name is checked to contain the name.
     * The item id is checked for a full match.
     * Returns null if no matches were found.
     * @param name The name or item ID to find.
     * @param ignoreCase Applies for the item name check.
     * @param inInv Will also search in the inventory and not only in the hotbar
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun findItem(name: String, ignoreCase: Boolean = false, inInv: Boolean = false, mode: Int = 0): Int? {
        val regex = Regex("${if (ignoreCase) "(?i)" else ""}$name")
        return findItem(regex, inInv, mode)
    }

    /**
     * Returns the first slot where the item name or id passes a check for the regex.
     * The item name is checked to contain the regex.
     * The item id is checked for a full match.
     * Returns null if no matches were found.
     *
     * For case insensitivity use the flag "(?i)":
     *
     *     val regex = Regex("(?i)item name")
     * @param regex regex that has to be matched.
     * @param inInv Will also search in the inventory and not only in the hotbar
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun findItem(regex: Regex, inInv: Boolean = false, mode: Int = 0): Int? {
        for (i in 0..if (inInv) 35 else 8) {
            if (mc.thePlayer.inventory.getStackInSlot(i)?.run {
                    when (mode) {
                        0 -> displayName.contains(regex) || itemID.matches(regex)
                        1 -> displayName.contains(regex)
                        2 -> itemID.matches(regex)
                        else -> false
                    }
                } == true
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
     * Check whether the player is holding one of the given items.
     * Checks both the name and item ID.
     * @param names The names or item IDs.
     * @param ignoreCase Applies for the item name check.
     */
    fun EntityPlayerSP?.isHoldingOneOf(vararg names: String, ignoreCase: Boolean = false): Boolean {
        names.forEach {
            if (this.isHolding(it, ignoreCase)) return true
        }
        return false
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
}
