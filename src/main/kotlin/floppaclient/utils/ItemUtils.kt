package floppaclient.utils

import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraftforge.common.util.Constants
import java.io.ByteArrayInputStream
import java.io.IOException

object ItemUtils {


    private val ItemStack.extraAttributes: NBTTagCompound?
        get() {
            return this.getSubCompound("ExtraAttributes", false)
        }

    val ItemStack.isDungeonMobDrop: Boolean
        get() {
            val attributes = this.extraAttributes
            return attributes.hasKey("baseStatBoostPercentage") && !attributes.hasKey("dungeon_item_level")
        }

    val ItemStack.rarityBoost: Int?
        get() {
            return this.extraAttributes?.getInteger("baseStatBoostPercentage")
        }

    val ItemStack.isRarityUpgraded: Boolean
        get() {
            return (this.extraAttributes?.getInteger("rarity_upgrades") ?: 0) > 0
        }

    val ItemStack.isStarred: Boolean
        get() {
            return (this.extraAttributes?.getInteger("upgrade_level") ?: 0) > 0
        }

    val ItemStack.itemID: String
        get() {
            return this.extraAttributes?.getString("id") ?: ""
        }

    private val ItemStack.lore: List<String>
        get() {
            val display = this.getSubCompound("display", false) ?: return emptyList()
            if (display.hasKey("Lore", 9)) {
                val nbt = display.getTagList("Lore", 8)
                val lore = ArrayList<String>()
                (0..nbt.tagCount()).forEach {
                    lore.add(nbt.getStringTagAt(it))
                }
                return lore
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


    val ItemStack?.reforge : String
        get() {
            return this?.extraAttributes?.getString("modifier") ?: ""
        }


    private fun NBTTagCompound?.hasKey(key: String) : Boolean {
        return this?.hasKey(key) ?: false
    }


    /**
     *
     * Converts an NBT tag into a pretty-printed string.
     *
     * For constant definitions, see [Constants.NBT]
     * From SBA
     * @link https://github.com/BiscuitDevelopment/SkyblockAddons/blob/9a45f04f8c07e9127674c0d7fbfeb0dd45222d0e/src/main/java/codes/biscuit/skyblockaddons/utils/DevUtils.java#L467
     * @param nbt the NBT tag to pretty print
     * @return pretty-printed string of the NBT data
     */
    fun prettyPrintNBT(nbt: NBTBase): String {
        val INDENT = "    "
        val tagID = nbt.id.toInt()
        var stringBuilder = StringBuilder()

        // Determine which type of tag it is.
        if (tagID == Constants.NBT.TAG_END) {
            stringBuilder.append('}')
        } else if (tagID == Constants.NBT.TAG_BYTE_ARRAY || tagID == Constants.NBT.TAG_INT_ARRAY) {
            stringBuilder.append('[')
            if (tagID == Constants.NBT.TAG_BYTE_ARRAY) {
                val nbtByteArray = nbt as NBTTagByteArray
                val bytes = nbtByteArray.byteArray
                for (i in bytes.indices) {
                    stringBuilder.append(bytes[i].toInt())

                    // Don't add a comma after the last element.
                    if (i < bytes.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
            } else {
                val nbtIntArray = nbt as NBTTagIntArray
                val ints = nbtIntArray.intArray
                for (i in ints.indices) {
                    stringBuilder.append(ints[i])

                    // Don't add a comma after the last element.
                    if (i < ints.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
            }
            stringBuilder.append(']')
        } else if (tagID == Constants.NBT.TAG_LIST) {
            val nbtTagList = nbt as NBTTagList
            stringBuilder.append('[')
            for (i in 0 until nbtTagList.tagCount()) {
                val currentListElement = nbtTagList[i]
                stringBuilder.append(prettyPrintNBT(currentListElement))

                // Don't add a comma after the last element.
                if (i < nbtTagList.tagCount() - 1) {
                    stringBuilder.append(", ")
                }
            }
            stringBuilder.append(']')
        } else if (tagID == Constants.NBT.TAG_COMPOUND) {
            val nbtTagCompound = nbt as NBTTagCompound
            stringBuilder.append('{')
            if (!nbtTagCompound.hasNoTags()) {
                val iterator: Iterator<String> = nbtTagCompound.keySet.iterator()
                stringBuilder.append(System.lineSeparator())
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val currentCompoundTagElement = nbtTagCompound.getTag(key)
                    stringBuilder.append(key).append(": ").append(
                        prettyPrintNBT(currentCompoundTagElement)
                    )
                    if (key.contains("backpack_data") && currentCompoundTagElement is NBTTagByteArray) {
                        try {
                            val backpackData = CompressedStreamTools.readCompressed(
                                ByteArrayInputStream(
                                    currentCompoundTagElement.byteArray
                                )
                            )
                            stringBuilder.append(",").append(System.lineSeparator())
                            stringBuilder.append(key).append("(decoded): ").append(
                                prettyPrintNBT(backpackData)
                            )
                        } catch (_: IOException) {
                            // error message maybe
                        }
                    }

                    // Don't add a comma after the last element.
                    if (iterator.hasNext()) {
                        stringBuilder.append(",").append(System.lineSeparator())
                    }
                }

                // Indent all lines
                val indentedString =
                    stringBuilder.toString().replace(System.lineSeparator().toRegex(), System.lineSeparator() + INDENT)
                stringBuilder = StringBuilder(indentedString)
            }
            stringBuilder.append(System.lineSeparator()).append('}')
        } else {
            stringBuilder.append(nbt)
        }
        return stringBuilder.toString()
    }
}