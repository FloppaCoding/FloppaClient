package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.inventory.ItemUtils.reforge
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.fakeactions.FakeActionUtils
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module aimed to provide everything needed to automatically swap weapons to use the abilities of other weapons while
 * attacking with a melee weapon.
 *
 * @author Derek, Aton
 */
object AutoWeaponSwap : Module(
    "Auto Weapon Swap",
    category = Category.MISC,
    description = "This module will automatically use the abilities of the specified items while you are attacking with a melee weapon. " +
            "It can be used for Axe of the Shredded, Soul Whip, Ice Spray and Terminator in any combination. " +
            "The abilities will only be used on cooldown.\n" +
            "Whether the currently held item qualifies as a melee weapon is determined by it's reforge and an item blacklist. " +
            "Items in the blacklist will never trigger a weapon swap by this module, regardless of the reforge. "+
            "Whitelisted reforges: §a§oSuspicious, Fabled, Heroic, Spicy, Withered§r\n" +
            "§fBlacklisted Items: §c§oAspect of the Void, Jerry-chine, Bonzo Staff"

) {

    private val axeOfTheShredded = BooleanSetting("AOTS Swap", false, description = "Include Axe of the Shredded in the weapon swap cycle.")
    private val soulWhip = BooleanSetting("Soul Whip Swap", false, description = "Include Soul Whip in the weapon swap cycle.")
    private val terminator = BooleanSetting("Terminator Swap", false, description = "Include Terminator in the weapon swap cycle.")
    private val iceSpray = BooleanSetting("Ice Spray Swap", false, description = "Include Ice Spray in the weapon swap cycle.")
    private val termSleep = NumberSetting("Sleep ms",50.0,10.0,100.0,5.0, description = "Delay between Terminator clicks. This will determine the CPS on the Terminator and lets it exceed your left click CPS.")
        .withDependency { this.terminator.enabled }
    private val fromInv = BooleanSetting("From Inv", false, description = "Lets you use Soul Whip, AOTS and Ice Spray from inventory. §cNot recommended.")

    private val leftClickItems = setOf("suspicious", "fabled", "heroic", "spicy", "withered")
    private val leftClickBlacklist = arrayOf(SkyblockItem.AOTV, SkyblockItem.JERRY_GUN, SkyblockItem.BONZO_STAFF, SkyblockItem.BONZO_STAFF_FRAGGED)

    private const val axeCooldown = 450
    private const val whipCooldown = 500
    private const val sprayCooldown = 5000
    /**
     * Determins for how long after a click the terminator will still be shot.
     */
    private const val activationTime = 150

    private var activeUntil = System.currentTimeMillis()
    private var nextAxe = System.currentTimeMillis()
    private var nextWhip = System.currentTimeMillis()
    private var nextTerm = System.currentTimeMillis()
    private var nextSpray = System.currentTimeMillis()


    init {
        this.addSettings(
            axeOfTheShredded,
            soulWhip,
            terminator,
            termSleep,
            iceSpray,
            fromInv,
        )
    }

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (mc.thePlayer.isHolding(*leftClickBlacklist)) return
        if (mc.thePlayer.heldItem?.reforge?.containsOneOf(leftClickItems) == true) {
            val nowMillis = System.currentTimeMillis()
            if (terminator.enabled && nowMillis in nextTerm until activeUntil) {
                FakeActionUtils.useItem(SkyblockItem.TERMINATOR)
                val overshoot =  (nowMillis - nextTerm).takeIf { it < 200 } ?: 0L
                nextTerm = nowMillis + (termSleep.value.toLong() - overshoot).coerceAtLeast(0L)
            }
            if (axeOfTheShredded.enabled && nowMillis >= nextAxe) {
                FakeActionUtils.useItem(SkyblockItem.AXE_OF_THE_SHREDDED, fromInv = fromInv.enabled)
                nextAxe = nowMillis + axeCooldown
            }
            if (soulWhip.enabled && nowMillis >= nextWhip) {
                FakeActionUtils.useItem(SkyblockItem.SOUL_WHIP, fromInv = fromInv.enabled)
                nextWhip = nowMillis + whipCooldown
            }
            if (iceSpray.enabled && nowMillis >= nextSpray) {
                FakeActionUtils.useItem(SkyblockItem.ICE_SPRAY, fromInv = fromInv.enabled)
                nextSpray = nowMillis + sprayCooldown
            }
            activeUntil = nowMillis + activationTime
        }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent?) {
        val nowMillis = System.currentTimeMillis()
        if (terminator.enabled && nowMillis < activeUntil) {
            if (nowMillis >= nextTerm) {
                FakeActionUtils.useItem(SkyblockItem.TERMINATOR)
                val overshoot =  (nowMillis - nextTerm).takeIf { it < 200 } ?: 0L
                nextTerm = nowMillis + (termSleep.value.toLong() - overshoot).coerceAtLeast(0L)
            }
        }
    }
}