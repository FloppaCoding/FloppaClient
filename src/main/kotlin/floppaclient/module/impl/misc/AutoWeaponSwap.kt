package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.fakeactions.FakeActionUtils
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
    description = "This module will automatically use the abilities of the specified items while you are attacking with a melee weapon." +
            "It can be used for Axe of the Shredded, Soul Whip, Ice Spray and Terminator in any combination. " +
            "The abilities will only be used on cooldown. " +
            "Whether the currently held item qualifies as a melee weapon is determined by the reforge and an item blacklist. " +
            "Items in the blacklist will never trigger a weapon swap by this module, regardless of the reforge. "+
            "You can effectively extend the whitelist through the use of the \"Custom Filter\" setting. "+
            "Whitelisted reforges: §a\"Suspicious\", \"Fabled\", \"Heroic\", \"Spicy\", \"Withered\"§r\n" +
            "§fBlacklisted Items: §c\"Aspect of the Void\", \"Jerry\", \"Bonzo\""

) {

    private val axeOfTheShredded = BooleanSetting("AOTS Swap", false, description = "Include Axe of the Shredded in the weapon swap cycle.")
    private val soulWhip = BooleanSetting("Soul Whip Swap", false, description = "Include Soul WHip in the weapon swap cycle.")
    private val terminator = BooleanSetting("Terminator Swap", false, description = "Include Terminator in the weapon swap cycle.")
    private val iceSpray = BooleanSetting("Ice Spray Swap", false, description = "Include Ice Spray in the weapon swap cycle.")
    private val termSleep = NumberSetting("Sleep ms",50.0,10.0,100.0,5.0, description = "Delay between Terminator clicks. This will determine the CPS on the Terminator and lets it exceed your left click CPS.")
    private val fromInv = BooleanSetting("From Inv", false, description = "Lets you use Soul Whip, AOTS and Ice Spray from inventory.")
    private val customFilter = StringSetting("Custom Filter", description = "Any item which contains this string in the name will also trigger a weapon swap by this module. Leave empty for this to be ignored. Case sensitive.")

    private val leftClickItems = setOf("Suspicious", "Fabled", "Heroic", "Spicy", "Withered")
        get() {
            return if (customFilter.text == "") {
                field
            }else {
                val temp = field.toMutableSet()
                temp.add(customFilter.text)
                return temp
            }
        }
    private val leftClickBlacklist = setOf("Aspect of the Void", "Jerry", "Bonzo")
    private const val axeCooldown = 450
    private const val whipCooldown = 500
    private const val sprayCooldown = 5000
    /**
     * Determins for how long after a click the terminator will still be shot.
     */
    private const val activationTime = 150

    private var activeUntil = System.currentTimeMillis()
    private var lastAxe = System.currentTimeMillis()
    private var lastWhip = System.currentTimeMillis()
    private var lastTerm = System.currentTimeMillis()
    private var lastSpray = System.currentTimeMillis()


    init {
        this.addSettings(
            axeOfTheShredded,
            soulWhip,
            terminator,
            iceSpray,
            termSleep,
            fromInv,
            customFilter
        )
    }

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (mc.thePlayer.heldItem?.displayName?.containsOneOf(leftClickBlacklist) == true) return
        if (mc.thePlayer.heldItem?.displayName?.containsOneOf(leftClickItems) == true) {
            if (terminator.enabled && System.currentTimeMillis() < activeUntil) {
                if (System.currentTimeMillis() - lastTerm >= termSleep.value) {
                    FakeActionUtils.useItem("Terminator")
                    lastTerm = System.currentTimeMillis() - ((System.currentTimeMillis() - lastTerm) % termSleep.value.toInt())
                }
            }
            if (axeOfTheShredded.enabled) {
                if (System.currentTimeMillis() - lastAxe >= axeCooldown) {
                    FakeActionUtils.useItem("Axe of the Shredded", fromInv = fromInv.enabled)
                    lastAxe = System.currentTimeMillis()
                }
            }
            if (soulWhip.enabled) {
                if (System.currentTimeMillis() - lastWhip >= whipCooldown) {
                    FakeActionUtils.useItem("Soul Whip", fromInv = fromInv.enabled)
                    lastWhip = System.currentTimeMillis()
                }
            }
            if (iceSpray.enabled) {
                if (System.currentTimeMillis() - lastSpray >= sprayCooldown) {
                    FakeActionUtils.useItem("Ice Spray", fromInv = fromInv.enabled)
                    lastSpray = System.currentTimeMillis()
                }
            }
            activeUntil = System.currentTimeMillis() + activationTime
        }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent?) {
        if (terminator.enabled && System.currentTimeMillis() < activeUntil) {
            if (System.currentTimeMillis() - lastTerm >= termSleep.value) {
                FakeActionUtils.useItem("Terminator")
                lastTerm = System.currentTimeMillis() - ((System.currentTimeMillis() - lastTerm) % termSleep.value.toInt())
            }
        }
    }
}