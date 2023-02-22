package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.fakeactions.FakeActionUtils

/**
 * Module to swap the equipped armor with the press of a button.
 * @author Aton
 */
object ArmorSwapper : Module(
    "Armor Swapper",
    category = Category.PLAYER,
    description = "Swaps out the the  specified items with the armor you are wearing."
) {
    private val item0 =
        StringSetting("Item 0", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item1 =
        StringSetting("Item 1", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item2 =
        StringSetting("Item 2", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item3 =
        StringSetting("Item 3", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item4 =
        StringSetting("Item 4", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item5 =
        StringSetting("Item 5", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item6 =
        StringSetting("Item 6", "", description = "Name of an item to be swapped, leave empty to do nothing.")
    private val item7 =
        StringSetting("Item 7", "", description = "Name of an item to be swapped, leave empty to do nothing.")

    private val items = arrayListOf<Setting<*>>(
        item0,
        item1,
        item2,
        item3,
        item4,
        item5,
        item6,
        item7,
    )

    init {
        this.addSettings(items)
    }

    override fun onKeyBind() {
        if (this.enabled) {

            // Swap slots. No direct reference to the FakeInventoryActionManager needed here. swapArmorItem takes care of that.
            /** Used to keep track of swapped items, the 4 right most bits correspond to the swapped slots. */
            var swappedSlots = 0b0000
            if (mc.thePlayer.inventory.itemStack == null) {
                for (i in 0 until items.size) {
                    if (swappedSlots == 0b1111) break
                    val itemName = (items[i] as StringSetting).text
                    if (itemName == "") continue

                    val swappedIndex = FakeActionUtils.swapArmorItem(itemName, swappedSlots) ?: continue

                    // Update alr swapped pieces
                    val swapNum = 0b1 shl swappedIndex
                    swappedSlots = swappedSlots or swapNum
                }
            }
        }
    }
}

