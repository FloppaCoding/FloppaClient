package floppaclient.module.impl.render

import floppaclient.floppamap.dungeon.MapRender
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import net.minecraftforge.common.MinecraftForge
import java.awt.Color

/**
 * This Module functions as a setting storage for the floppamap dungeon map.
 * The actual implementation is in [floppaclient.floppamap]
 */
object DungeonMap : Module(
    "Dungeon Map",
    category = Category.RENDER,
    description = "Shows the full dungeon map."
){
    // General
    val autoScan = BooleanSetting("Auto Scan", true, description = "Automatically scans when entering dungeon. Manual scan can be done with \"/fcl scan\".")
    val mapItemScan = BooleanSetting("Map Item Scan", true, description = "Will use the held map item for scanning the dungeon.")
    val legitMode = BooleanSetting("Legit Mode", false, description = "Will only show you the information that is available legitimately.")
    val trackSecrets = BooleanSetting("Track Secrets", true, visibility = Visibility.ADVANCED_ONLY, description = "Uses the Hypixel API to track how many secrets are collected in which room.")
    val hideInBoss = BooleanSetting("Hide in Boss", true, visibility = Visibility.ADVANCED_ONLY, description = "Hides the map in boss.")
    val showRunInformation = BooleanSetting("Show Run Info", true, description = "Shows run information under map.")
    val playerNameMode = SelectorSetting("Player Names", "Holding Leap", arrayListOf("Off", "Holding Leap", "Always"), visibility = Visibility.ADVANCED_ONLY, description = "Show player name under player head.")
    // Chat info
    val scanChatInfo = BooleanSetting("Scan Chat Info", true, description = "Show dungeon overview information after scanning.")
    val mimicInfo = BooleanSetting("Mimic Info", false, visibility = Visibility.ADVANCED_ONLY, description = "Show message when the mimic is found.")
    // Scaling
    val mapScale = NumberSetting("Map Scale",1.25,0.1,4.0,0.02, visibility = Visibility.HIDDEN, description = "Scale of entire map.")
    val roomScale = NumberSetting("Dungeon Scale", 1.0,0.5,1.5, 0.01, description = "Scales the size of the displayed dungeon inside of the map HUD element.")
    val textScale = NumberSetting("Text Scale",0.75,0.0,2.0,0.02, description = "Scale of room names and secret counts relative to map size.")
    val playerHeadScale = NumberSetting("Head Scale",1.0,0.0,2.0,0.02, description = "Scale of player heads relative to map size.")
    // Spinny Map
    val spinnyMap = BooleanSetting("Spinny Map", false, description = "Centers the map on you and rotates it.")
    val centerOnPlayer = BooleanSetting("Center on Player", false, description = "Centers the map on your own Player Head.")
    // Border
    val mapBackground = ColorSetting("Background", Color(0, 0, 0, 100),true, visibility = Visibility.ADVANCED_ONLY, description = "Background Color for the map.")
    val mapBorder = ColorSetting("Border", Color(0, 0, 0, 255),true, visibility = Visibility.ADVANCED_ONLY, description = "Border Color for the map.")
    val chromaBorder = BooleanSetting("Chroma Border", false, visibility = Visibility.ADVANCED_ONLY, description = "Will add a chroma effect to your map border. The chroma can be configured in the ClickGui Module.")
    val mapBorderWidth = NumberSetting("Border Width",3.0,0.0,10.0,0.1, visibility = Visibility.ADVANCED_ONLY, description = "Map border width.")




    val xHud = NumberSetting("x", default = 10.0, visibility = Visibility.HIDDEN)
    val yHud = NumberSetting("y", default = 10.0, visibility = Visibility.HIDDEN)

    init {
        this.addSettings(
            autoScan,
            mapItemScan,
            legitMode,
            trackSecrets,
            hideInBoss,
            showRunInformation,
            playerNameMode,
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
            chromaBorder,
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