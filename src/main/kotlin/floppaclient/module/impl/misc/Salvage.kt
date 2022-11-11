package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.ItemUtils.isDungeonMobDrop
import floppaclient.utils.ItemUtils.isRarityUpgraded
import floppaclient.utils.ItemUtils.isStarred
import floppaclient.utils.ItemUtils.itemID
import floppaclient.utils.ItemUtils.rarityBoost
import floppaclient.utils.Utils
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.Utils.leftClickWindow
import floppaclient.utils.Utils.shiftClickWindow
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Blocks
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * A module aimed to automatically click the correct items in the Salvage gui.
 * It offers a lot of customization for which items are to be salvaged.
 *
 * @author Aton
 */
object Salvage : Module(
    "Auto Salvage",
    category = Category.MISC,
    description = "Automatically salvages all salvageable items in your inventory when you enter the salvage gui."
){
    private val sleep = NumberSetting("Sleep", 200.0, 50.0, 1000.0, 10.0, description = "Delay in between clicks.")
    private val dungeonItems = BooleanSetting("Dugeon Drops", true, description = "Will auto salvage Dungeon mob drops.")
    private val salgave50 = BooleanSetting("Salvage max stat", false, description = "Will auto salvage items with +50% stat boost.")
    private val netherItems = BooleanSetting("Nether Items", true, description = "Will also salvage nether fishing items.")
    private val salvageStarred = BooleanSetting("Salvage Starred", false, description = "When enabled starred items will also be salvaged.")
    private val salvageRecombed = BooleanSetting("Salvage Recombed", false, description = "When enabled rarity upgraded items will also be salvaged.")
    private val other = StringSetting("Other","", 100, description = "Name or item id of other items to be salvaged. Separate multiple with a semicolon ;.")
    private val message = BooleanSetting("Message on Finish", true, description = "Puts a message in chat when it is done.")

    init {
        this.addSettings(
            sleep,
            dungeonItems,
            salgave50,
            netherItems,
            salvageStarred,
            salvageRecombed,
            other,
            message
        )
    }
    private var nextClick = 0L
    private var inSalvage = false
    private val netherIds = setOf(
        "STAFF_OF_THE_VOLCANO",
        "BLADE_OF_THE_VOLCANO",
        "FLAMING_CHESTPLATE",
        "TAURUS_HELMET",
        "MOOGMA_LEGGINGS",
        "SLUG_BOOTS",
    )

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest || !FloppaClient.inSkyblock) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Salvage Item")) {
                inSalvage = true
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!inSalvage || mc.thePlayer == null) return
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return
        val inventoryName = container.inventorySlots?.get(0)?.inventory?.name
        if (inventoryName == null || !inventoryName.startsWith("Salvage Item")) {
            inSalvage = false
            return
        }
        if (System.currentTimeMillis() < nextClick) return
        val locked = container.inventorySlots[31].stack?.item == Item.getItemFromBlock(Blocks.barrier)
        val itemReady = container.inventorySlots[22].hasStack
        val slotIndex = if (locked) 22 else if (itemReady) 31
        else container.inventorySlots.subList(54,90).firstOrNull { shouldSalvage(it) }?.slotNumber
            ?: return (if (message.enabled) Utils.modMessage("Finished auto salvage.") else Unit).also { inSalvage = false }
        if (slotIndex == 31) {
            leftClickWindow(container.windowId, slotIndex)
        }else {
            shiftClickWindow(container.windowId, slotIndex)
        }
        nextClick = System.currentTimeMillis() + sleep.value.toLong()
        return
    }

    private fun shouldSalvage(slot: Slot): Boolean {
        val stack = slot.stack ?: return false
        if (stack.itemID == "ICE_SPRAY_WAND") return false
        if (!salvageRecombed.enabled && stack.isRarityUpgraded) return false
        if (!salvageStarred.enabled && stack.isStarred) return false
        if (netherItems.enabled && stack.itemID.equalsOneOf(netherIds)) return true
        if (dungeonItems.enabled && stack.isDungeonMobDrop){
            return if (salgave50.enabled) true
            else (stack.rarityBoost ?: 0) < 50
        }
        if (other.text != "") {
            val options = other.text.split(";")
            if( stack.run { displayName.containsOneOf(options, ignoreCase = true) || itemID.equalsOneOf(options) })
                return true
        }
        return false
    }
}