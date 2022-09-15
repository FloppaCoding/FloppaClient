package floppaclient.utils

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object ItemUtils {

    /**
     * Returns the `ExtraAttributes` compound tag from the item's NBT data.
     *
     * @author BiscuitDevelopment
     * @param item the item to get the tag from
     * @return the item's `ExtraAttributes` compound tag or `null` if the item doesn't have one
     */
    @JvmStatic
    fun getExtraAttributes(item: ItemStack?): NBTTagCompound? {
        return if (item == null || !item.hasTagCompound()) {
            null
        } else item.getSubCompound("ExtraAttributes", false)
    }

    val ItemStack.itemID: String
        get() {
            if (this.hasTagCompound() && this.tagCompound.hasKey("ExtraAttributes")) {
                val attributes = this.getSubCompound("ExtraAttributes", false)
                if (attributes.hasKey("id", 8)) {
                    return attributes.getString("id")
                }
            }
            return ""
        }

    val ItemStack.lore: List<String>
        get() {
            if (this.hasTagCompound() && this.tagCompound.hasKey("display", 10)) {
                val display = this.tagCompound.getCompoundTag("display")
                if (display.hasKey("Lore", 9)) {
                    val nbt = display.getTagList("Lore", 8)
                    val lore = ArrayList<String>()
                    (0..nbt.tagCount()).forEach {
                        lore.add(nbt.getStringTagAt(it))
                    }
                    return lore
                }
            }
            return emptyList()
        }

    /**
     * Checks whether the item has a right click ability.
     */
    val ItemStack?.hasAbility: Boolean
        get() {
            val lore = this?.lore
            lore?.forEach{
                if(it.contains("Ability:") && it.endsWith("RIGHT CLICK")) return true
            }
            return false
        }
}