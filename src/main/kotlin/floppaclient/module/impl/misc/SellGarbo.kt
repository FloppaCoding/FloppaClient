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
 * This module is aimed at automatically selling all the items which are generally deemed useless when you enter any trades
 * menu. It provides a lot of customization as to which items should be sold.
 *
 * @author Aton
 */
object SellGarbo : Module(
    "Auto Sell",
    category = Category.MISC,
    description = "Automatically sells teh specified items in your inventory when you enter open the trades menu."
){
    private val sleep = NumberSetting("Sleep", 200.0, 10.0, 1000.0, 10.0, description = "Delay in between clicks.")
    private val onlyTrades = BooleanSetting("Only Trades", true, description = "Only activates in the trades menu. If disabled auto sell will also work with all npc shops.")
    private val dungeonItems = BooleanSetting("Dugeon Mob Drops", true, description = "Will auto sell Dungeon mob drops.")
    private val sellSuperBoom = BooleanSetting("Sell SuperBoom", false, description = "Will also sell SuperBoom tnt.")
    private val sellMaxStat = BooleanSetting("Sell max stat", false, description = "Will also sell items with +50% stat boost.")
    private val sellStarred = BooleanSetting("Sell Starred", false, description = "When enabled starred items will also be sold.")
    private val sellRecombed = BooleanSetting("Sell Recombed", false, description = "When enabled rarity upgraded items will also be sold.")
    private val treasureTalis = BooleanSetting("Treasure Talis", false, description = "Also sells treasure talisman.")
    private val other = StringSetting("Other","", 100, description = "Name or item id of other items to be sold. Separate multiple with a semicolon ;.")
    private val message = BooleanSetting("Message on Finish", true, description = "Puts a message in chat when it is done.")

    init {
        this.addSettings(
            sleep,
            onlyTrades,
            dungeonItems,
            sellSuperBoom,
            sellMaxStat,
            sellStarred,
            sellRecombed,
            treasureTalis,
            other,
            message
        )
    }
    private var nextClick = 0L
    private var inSellMenu = false
    private var sellMenuName = "akljsdlkmnfldskhfsdhf"
    private var confirmed = false

    private var garbo = listOf(
        "Health Potion VIII Splash Potion", //"§5Health Potion VIII Splash Potion"
        "Training Weights", // "§aTraining Weights"
        "Defuse Kit", // "§aDefuse Kit"
        "Revive Stone",
        "Healing Potion 8 Slash Potion",
        "Healing Potion VIII Splash Potion",
        "Beating Heart",
        "Premium Flesh",
        "Mimic Fragment",
        "Optic Lense",
        "Tripwire Hook",
        "Button",
        "Carpet",
        "Lever",
        "Journal Entry",
        "Sign"
    )

    private const val treasure = "Treasure Talisman"

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest || !FloppaClient.inSkyblock) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            confirmed = chestName.startsWith("Trades")
            if (!onlyTrades.enabled || confirmed) {
                sellMenuName = chestName
                inSellMenu = true
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!inSellMenu || mc.thePlayer == null) return
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return
        val inventoryName = container.lowerChestInventory.displayName.unformattedText
        if (inventoryName != sellMenuName) {
            inSellMenu = false
            return
        }
        if (!confirmed) {
            val stack = container.inventorySlots[49].stack
            if (stack?.item == Item.getItemFromBlock(Blocks.hopper) && stack?.displayName == "§aSell Item")
                confirmed = true
            else if(stack == null) {
                return
            }else {
                inSellMenu = false
                return
            }
        }
        if (System.currentTimeMillis() < nextClick) return
        val slotIndex = container.inventorySlots.subList(54,90).firstOrNull { shouldSell(it) }?.slotNumber
            ?: return (if (message.enabled) Utils.modMessage("Finished auto sell.") else Unit).also { inSellMenu = false }

        shiftClickWindow(container.windowId, slotIndex)
        nextClick = System.currentTimeMillis() + sleep.value.toLong()
        return
    }

    private fun shouldSell(slot: Slot): Boolean {
        val stack = slot.stack ?: return false
        if (stack.itemID == "ICE_SPRAY_WAND") return false
        if (!sellRecombed.enabled && stack.isRarityUpgraded) return false
        if (!sellStarred.enabled && stack.isStarred) return false
        if (dungeonItems.enabled && stack.isDungeonMobDrop){
            return if (sellMaxStat.enabled) true
            else (stack.rarityBoost ?: 0) < 50
        }
        if (sellSuperBoom.enabled && stack.itemID == "SUPERBOOM_TNT") return true
        if (treasureTalis.enabled &&  stack.displayName.contains(treasure)) return true
        if (stack.displayName.containsOneOf(garbo)) return true
        if (other.text != "") {
            val options = other.text.split(";")
            if( stack.run { displayName.containsOneOf(options, ignoreCase = true) || itemID.equalsOneOf(options) })
                return true
        }
        return false
    }
}