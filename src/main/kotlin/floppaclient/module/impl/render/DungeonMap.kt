package floppaclient.module.impl.render

import floppaclient.funnymap.features.dungeon.MapRender
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import net.minecraftforge.common.MinecraftForge
import java.awt.Color

/**
 * This Module functions as a setting storage for the funnymap dungeon map.
 * The actual implementation is in floppaclient.funnymap
 */
object DungeonMap : Module(
    "Dungeon Map",
    category = Category.RENDER,
    description = "Shows the full dungeon map."
){
    val hideInBoss = BooleanSetting("Hide in Boss", true, description = "Hides the map in boss.")
    val showRunInformation = BooleanSetting("Show Run Info", true, description = "Shows run information under map.")
    val playerNameMode = SelectorSetting("Player Names", "Holding Leap", arrayListOf("Off", "Holding Leap", "Always"), description = "Show player name under player head.")
    val autoScan = BooleanSetting("Auto Scan", true, description = "Automatically scans when entering dungeon. Manual scan can be done with \"/fcl scan\".")
    val scanChatInfo = BooleanSetting("Scan Chat Info", true, description = "Show dungeon overview information after scanning.")
    val mimicInfo = BooleanSetting("Mimic Info", true, description = "Show message when the mimic is found.")
    val mapScale = NumberSetting("Map Scale",1.25,0.1,4.0,0.02, description = "Scale of entire map.")
    val roomScale = NumberSetting("Dungeon Scale", 1.0,0.5,1.5, 0.01, description = "Scales the size of the displayed dungeon inside of the map HUD element.")
    val textScale = NumberSetting("Text Scale",0.75,0.0,2.0,0.02, description = "Scale of room names and secret counts relative to map size.")
    val playerHeadScale = NumberSetting("Head Scale",1.0,0.0,2.0,0.02, description = "Scale of player heads relative to map size.")
    val spinnyMap = BooleanSetting("Spinny Map", false, description = "Centers the map on you and rotates it.")
    val centerOnPlayer = BooleanSetting("Center on Player", false, description = "Centers the map on your own Player Head.")
    val mapBackground = ColorSetting("Background", Color(0, 0, 0, 100),true, description = "Background Color for the map.")
    val mapBorder = ColorSetting("Border", Color(0, 0, 0, 255),true, description = "Border Color for the map.")
    val mapBorderWidth = NumberSetting("Border Width",3.0,0.0,10.0,0.1, description = "Map border width.")

    val xHud = NumberSetting("x", default = 10.0, hidden = true)
    val yHud = NumberSetting("y", default = 10.0, hidden = true)

    init {
        this.addSettings(
            hideInBoss,
            showRunInformation,
            playerNameMode,
            autoScan,
            scanChatInfo,
            mimicInfo,
            mapScale,
            roomScale,
            textScale,
            playerHeadScale,
            spinnyMap,
            centerOnPlayer,
            mapBackground,
            mapBorder,
            mapBorderWidth,
            xHud,
            yHud
        )
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(MapRender)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(MapRender)
        super.onDisable()
    }
}