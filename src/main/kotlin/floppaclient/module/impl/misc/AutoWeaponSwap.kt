package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.events.ClickEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 *Module aimed to provide everything needed to automatically swap weapons to attack with all of them.
 *
 * @author Derek, Aton
 */
object AutoWeaponSwap : Module(
    "Auto Weapon Swap",
    category = Category.MISC,
    description = "Automatically switches between Axe of the Shredded, Soul Whip, or Terminator" +
            "with the user's choice of weapon. They can also be used together in any combination." +
            "The items will only be used on cooldown."+
            "Additionally it determines whether or not you can swap on an item based on reforges and" +
            "checks if they are in a blacklist of items that you would not want to swap with.\n" +
            "Whitelisted reforges: §a\"Suspicious\", \"Fabled\", \"Heroic\", \"Withered\"§r\n" +
            "§fBlacklisted Items: §c\"Aspect of the Void\", \"Jerry\", \"Bonzo\""

) {

    private val axeOfTheShredded = BooleanSetting("AOTS Swap", false, description = "Include Axe of the Shredded in the weapon swap cycle.")
    private val soulWhip = BooleanSetting("Soul Whip Swap", false, description = "Include Soul WHip in the weapon swap cycle.")
    private val terminator = BooleanSetting("Terminator Swap", false, description = "Include Terminator in the weapon swap cycle.")
    private val iceSpray = BooleanSetting("Ice Spray Swap", false, description = "Include Ice Spray in the weapon swap cycle.")
    private val termSleep = NumberSetting("Sleep ms",50.0,10.0,100.0,5.0, description = "Delay between Terminator clicks. This will determine the CPS on the Terminator and lets it exceed your left click CPS.")

    private val leftClickItems = setOf("Suspicious", "Fabled", "Heroic", "Withered")
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
            termSleep
        )
    }

    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (FloppaClient.mc.thePlayer.heldItem?.displayName?.containsOneOf(leftClickBlacklist) == true) return
        if (FloppaClient.mc.thePlayer.heldItem?.displayName?.containsOneOf(leftClickItems) == true) {
            if (terminator.enabled && System.currentTimeMillis() < activeUntil) {
                if (System.currentTimeMillis() - lastTerm >= termSleep.value) {
                    FakeActionUtils.useItem("Terminator")
                    lastTerm = System.currentTimeMillis() - ((System.currentTimeMillis() - lastTerm) % termSleep.value.toInt())
                }
            }
            if (axeOfTheShredded.enabled) {
                if (System.currentTimeMillis() - lastAxe >= axeCooldown) {
                    FakeActionUtils.useItem("Axe of the Shredded")
                    lastAxe = System.currentTimeMillis()
                }
            }
            if (soulWhip.enabled) {
                if (System.currentTimeMillis() - lastWhip >= whipCooldown) {
                    FakeActionUtils.useItem("Soul Whip")
                    lastWhip = System.currentTimeMillis()
                }
            }
            if (iceSpray.enabled) {
                if (System.currentTimeMillis() - lastSpray >= sprayCooldown) {
                    FakeActionUtils.useItem("Ice Spray")
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