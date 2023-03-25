package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.ChatUtils
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.Utils.leftClickWindow
import floppaclient.utils.Utils.middleClickWindow
import floppaclient.utils.Utils.shiftClickWindow
import floppaclient.utils.inventory.ItemUtils.isDungeonMobDrop
import floppaclient.utils.inventory.ItemUtils.isRarityUpgraded
import floppaclient.utils.inventory.ItemUtils.isStarred
import floppaclient.utils.inventory.ItemUtils.itemID
import floppaclient.utils.inventory.ItemUtils.lore
import floppaclient.utils.inventory.ItemUtils.rarityBoost
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * This module is aimed at automatically selling all the items which are generally deemed useless when you enter any trades
 * menu. It provides a lot of customization as to which items should be sold.
 *
 * @author Aton, Stivais
 */
object SellGarbo : Module(
    "Auto Sell",
    category = Category.MISC,
    description = "Automatically sells the specified items in your inventory when you enter open the trades menu."
) {
    private val sleep: Double by NumberSetting("Sleep", 200.0, 10.0, 1000.0, 10.0, description = "Delay in between clicks.")
    private val onlyTrades: Boolean by BooleanSetting("Only Trades", true, description = "Only activates in the trades menu. If disabled auto sell will also work with all npc shops.")

    private val dungeonItems: Boolean by BooleanSetting("Dugeon Mob Drops", true, description = "Will auto sell Dungeon mob drops.")
    private val sellSuperBoom: Boolean by BooleanSetting("Sell SuperBoom", false, description = "Will also sell SuperBoom tnt.")
    private val sellRevStone: Boolean by BooleanSetting("Sell Revive Stone", false, description = "Will also sell Revive stones.")
    private val sellMaxStat: Boolean by BooleanSetting("Sell max stat", false, description = "Will also sell items with +50% stat boost.")
    private val sellStarred: Boolean by BooleanSetting("Sell Starred", false, description = "When enabled starred items will also be sold.")
    private val sellRecombed: Boolean by BooleanSetting("Sell Recombed", false, description = "When enabled rarity upgraded items will also be sold.")
    private val treasureTalis: Boolean by BooleanSetting("Treasure Talis", false, description = "Also sells treasure talisman.")
    private val other: String by StringSetting("Other", "", 100, description = "Name or item id of other items to be sold. Separate multiple with a semicolon ;.")

    private val message: Boolean by BooleanSetting("Message on Finish", true, description = "Puts a message in chat when it is done.")
    private val clickMethod = +StringSelectorSetting("Click Type", "Shift Click", arrayListOf("Left Click", "Shift Click", "Middle Click"), Visibility.ADVANCED_ONLY)

    private var nextClick = 0L
    private var inSellMenu = false
    private var hasSold = false

    private var garbo = listOf(
        "Healing VIII Splash Potion",
        "Healing 8 Splash Potion",
        "Training Weights",
        "Defuse Kit",
        "Beating Heart",
        "Premium Flesh",
        "Mimic Fragment",
        "Optic Lense",
        "Tripwire Hook",
        "Button",
        "Carpet",
        "Lever",
        "Sign"
    )

    private const val treasure = "Treasure Talisman"

    @SubscribeEvent
    fun onRender(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (mc.thePlayer == null) return
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return

        val inventoryName = container.lowerChestInventory.displayName.unformattedText
        if (onlyTrades && (!inventoryName.startsWith("Trades") || !inventoryName.startsWith("Booster Cookie"))) {
            inSellMenu = false
            return
        }

        val stack = container.inventorySlots[49].stack ?: return
        if (stack.displayName == "§aSell Item" || (stack.lore.isNotEmpty() && stack.lore.last() == "§eClick to buyback!")) inSellMenu = true
        else {
            inSellMenu = false
            return
        }

        if (System.currentTimeMillis() < nextClick || !inSellMenu) return
        val slotIndex = container.inventorySlots.subList(54, 90).firstOrNull { shouldSell(it) }?.slotNumber
            ?: return run {
                if (message && hasSold) ChatUtils.modMessage("Finished auto sell.")
                inSellMenu = false
                hasSold = false
            }

        when (clickMethod.selected) {
            "Shift Click" -> shiftClickWindow(container.windowId, slotIndex)
            "Left Click" -> leftClickWindow(container.windowId, slotIndex)
            "Middle Click" -> middleClickWindow(container.windowId, slotIndex)
        }

        hasSold = true
        nextClick = System.currentTimeMillis() + sleep.toLong()
        return
    }

    private fun shouldSell(slot: Slot): Boolean {
        val stack = slot.stack ?: return false
        if (stack.itemID == "ICE_SPRAY_WAND") return false
        if (!sellRecombed && stack.isRarityUpgraded) return false
        if (!sellStarred && stack.isStarred) return false
        if (dungeonItems && stack.isDungeonMobDrop) {
            return if (sellMaxStat) true
            else (stack.rarityBoost ?: 0) < 50
        }
        if (sellSuperBoom && stack.itemID == "SUPERBOOM_TNT") return true
        if (sellRevStone && stack.itemID == "REVIVE_STONE") return true
        if (treasureTalis && stack.displayName.contains(treasure)) return true
        if (stack.displayName.containsOneOf(garbo)) return true
        if (stack.itemID == "DUNGEON_LORE_PAPER") return true

        if (other != "") {
            val options = other.split(";")
            options.union(other.split("; "))

            if (stack.run { displayName.containsOneOf(options, ignoreCase = true) || itemID.equalsOneOf(options) })
                return true
        }
        return false
    }
}