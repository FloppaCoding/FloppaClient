package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.Utils.playLoudSound
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.ceil

/**
 * Display a warp timer for dungoen warp cooldowns.
 * @author Aton
 */
object DungeonWarpTimer : Module(
    "Warp Timer",
    category = Category.RENDER,
    description = "Renders a timer for when you can join the next dungeon."
){

    /**
     * To toggle whether it should play a sound once the own warp cooldown is over.
     */
    private val alerts = BooleanSetting("Alerts", false, description = "Plays a sound when your cooldown is over.")
    val trackInBackground = BooleanSetting("Track when off", false, description = "If enabled will keep tracking cooldowns in the background so that you can disable / enable the HUD whenever.")

    private val volume = NumberSetting("Volume", 1.0, 0.0, 1.0, 0.01, description = "Volume of the alert.")

    /**
     * If enabled only the own cool down will be tracked.
     */
    private val onlyOwnCD = BooleanSetting("Only Own CD", false, description = "Only track your own cooldown.")

    private val xHud = NumberSetting("x", default = 0.0, visibility = Visibility.HIDDEN)
    private val yHud = NumberSetting("y", default = 128.0, visibility = Visibility.HIDDEN)
    private val scaleHud = NumberSetting("scale",1.0,0.1,4.0, 0.01, visibility = Visibility.HIDDEN)

    private var warps: MutableMap<String, Int> = mutableMapOf()

    init {
        this.addSettings(
            alerts,
            trackInBackground,
            volume,
            onlyOwnCD,
            xHud,
            yHud
        )
    }

    /**
     * Register this class to the event bus if [trackInBackground] is enabled.
     */
    override fun onInitialize() {
        if(!this.enabled && trackInBackground.enabled) {
            MinecraftForge.EVENT_BUS.register(DungeonWarpTimer)
        }
        super.onInitialize()
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(DungeonWarpTimerHUD)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(DungeonWarpTimerHUD)
        if (trackInBackground.enabled) return
        super.onDisable()
    }

    /**
     * Registers the warp through the chat message.
     * Also registers who warped the party.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2) return
        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        when {
            text.endsWith("warped the party to a SkyBlock dungeon!") -> {
                if (onlyOwnCD.enabled) return
                val regex = Regex("(.+) warped the party to a SkyBlock dungeon!")
                val player = regex.find(text)?.groupValues?.get(1)
                if (player != null) {
                    warps[player] = 70*20
                }
            }
            text.startsWith("SkyBlock Dungeon Warp") -> {
                val regex = Regex("SkyBlock Dungeon Warp \\(\\d players\\)")
                if (regex.matches(text)) {
                    warps["§6You"] = 70*20
                }
            }
        }
    }

    /**
     * Updates the cool downs and removes finished cool down from the list.
     * Also plays a sound when the own cool down is over.
     */
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val keyList = warps.keys.toList()
        for (name in keyList) {
            warps[name] = warps[name]?.minus(1) ?: 0
            if (warps[name]!! <= 0) {
                warps.remove(name)
                if(name == "§6You" && alerts.enabled) {
                    playLoudSound("random.orb", volume.value.toFloat(),0f)
                }
            }
        }
    }

    object DungeonWarpTimerHUD : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("Warp Cooldowns") + 2,
        scale = scaleHud
    ){
        override fun renderHud() {

            if (warps.isEmpty()) return
            val lines = mutableListOf("Warp Cooldowns")
            lines.addAll(warps.map {
                "${it.key}§r: ${ceil( it.value / 20.0).toInt()} s"
            })

            var yOffs = 0
            lines.forEach { text ->
                mc.fontRendererObj.drawString(text, 0, yOffs, 0xffffff)
                yOffs += mc.fontRendererObj.FONT_HEIGHT
            }

            this.height = yOffs

            super.renderHud()
        }
    }

}