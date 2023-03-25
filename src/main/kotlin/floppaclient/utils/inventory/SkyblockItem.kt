package floppaclient.utils.inventory

/**
 * ## A collection of Skyblock items with data for those items.
 *
 * This class was made to make the use of skyblock [itemIDs][ItemUtils.itemID] easier for identifying items.
 * The [attributes] are also meant to make it easy to group similar items such as shortbows or witherblades,
 * so that those do not always have to be listed individually when it does not matter which one is used.
 *
 * Feel free to expand this list if you need any other items or attributes, or as new items are added to the game.
 *
 * @author Aton
 * @param itemID the Skyblock [itemID][ItemUtils.itemID] of an item
 * @param displayName the default [displayName][net.minecraft.item.ItemStack.getDisplayName] of the item without
 * formatting codes and possible extra elements such as reforge.
 * @see InventoryUtils
 * @see ItemUtils
 */
@Suppress("unused")
enum class SkyblockItem(
    val displayName: String,
    val itemID: String,
    private val attributes: List<Attribute> = listOf()
) {
    // WEAPONS / TOOLS
    AOTV("Aspect of the Void", "ASPECT_OF_THE_VOID"),
    TERMINATOR("Terminator", "TERMINATOR", Attribute.SHORTBOW),
    JUJU("Juju Shortbow", "JUJU_SHORTBOW", Attribute.SHORTBOW),
    ARTISANAL_SHORTBOW("Artisanal Shortbow", "ARTISANAL_SHORTBOW", Attribute.SHORTBOW),
    SPIRIT_BOW("Spirit Bow", "ITEM_SPIRIT_BOW", Attribute.SHORTBOW),
    ICE_SPRAY("Ice Spray Wand", "ICE_SPRAY_WAND"),
    AOTE("Aspect of the End", "ASPECT_OF_THE_END"),
    NECRONS_BLADE("Necron's Blade (Unrefined)", "NECRON_BLADE", Attribute.WITHERBLADE),
    ASTRAEA("Astraea", "ASTRAEA", Attribute.WITHERBLADE),
    SCYLLA("Scylla", "SCYLLA", Attribute.WITHERBLADE),
    VALKYRIE("Valkyrie", "VALKYRIE", Attribute.WITHERBLADE),
    HYPERION("Hyperion", "HYPERION", Attribute.WITHERBLADE),
    AOTD("Aspect of the Dragons", "ASPECT_OF_THE_DRAGON"),
    SOUL_WHIP("Soul Whip", "SOUL_WHIP"),
    WITHER_CLOAK("Wither Cloak Sword", "WITHER_CLOAK"),
    CLAYMORE("Dark Claymore", "DARK_CLAYMORE"),
    GIANTS_SWORD("Giant's Sword", "GIANTS_SWORD"),
    TRIBAL_SPEAR("Tribal Spear", "TRIBAL_SPEAR"),
    BONEMERANG("Bonemerang", "BONE_BOOMERANG"),
    JERRY_GUN("Jerry-chine Gun", "JERRY_STAFF"),
    /** @see BONZO_STAFF_FRAGGED */
    BONZO_STAFF("Bonzo's Staff", "BONZO_STAFF"),
    /**
     * The actual Item name will be "⚚ Reforge Bonzo's Staff". The ⚚ is omitted here so that the name can still match an item name even when a reforge is present.
     * @see BONZO_STAFF*/
    BONZO_STAFF_FRAGGED("Bonzo's Staff", "STARRED_BONZO_STAFF"),
    LEAPING_SWORD("Leaping Sword", "LEAPING_SWORD"),
    SILK_EDGE_SWORD("Silk-Edge Sword", "SILK_EDGE_SWORD"),
    AOTS("Axe of the Shredded", "AXE_OF_THE_SHREDDED"),
    ROGUE_SWORD("Rogue Sword", "ROGUE_SWORD"),

    //ARMOR
    SPRING_BOOTS("Spring Boots", "SPRING_BOOTS", Attribute.ARMOR),

    //MISC
    SPIRIT_LEAP("Spirit Leap", "SPIRIT_LEAP"),
    INFINILEAP("Infinileap", "INFINITE_SPIRIT_LEAP"),
    INFLATABLE_JERRY("Inflatable Jerry", "INFLATABLE_JERRY"),
    ;

    constructor(displayName: String, itemID: String, vararg attributes: Attribute) : this(
        displayName,
        itemID,
        attributes.asList()
    )

    fun hasAttribute(attribute: Attribute): Boolean = this.attributes.contains(attribute)

    enum class Attribute {
        SHORTBOW, ARMOR, WITHERBLADE;
    }

    companion object {
        val ASPECT_OF_THE_VOID = AOTV
        val ASPECT_OF_THE_END = AOTE
        val ASPECT_OF_THE_DRAGON = AOTD
        val AXE_OF_THE_SHREDDED = AOTS
    }
}