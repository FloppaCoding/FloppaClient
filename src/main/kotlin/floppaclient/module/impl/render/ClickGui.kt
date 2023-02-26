package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.display
import floppaclient.FloppaClient.Companion.onHypixel
import floppaclient.module.AlwaysActive
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.*
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.ChatUtils.stripControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.awt.Color

/**
 * Settings for the CLick Gui
 * @author Aton
 */
@AlwaysActive
object ClickGui: Module(
    "ClickGUI",
    Keyboard.KEY_RSHIFT,
    category = Category.RENDER,
    description = "Appearance settings for the click gui. \n" +
            "You can set a custom chat prefix with formatting here. For formatting use & or the paragrph symbol followed by a modifier. " +
            "A benefit of using the paragraph symbol is, that you can directly see how it will look in the text field, but you wont be able to see the formatting. \n" +
            "§00...§ff§r are colors, l is §lBold§r, n is §nUnderlined§r, o is §oItalic§r, m is §mStrikethrough§r, k is §kObfuscated§r, r is Reset."
) {

    val design: StringSelectorSetting
    val blur: BooleanSetting = BooleanSetting("Blur", false, description = "Toggles the background blur for the gui.")
    val color = ColorSetting("Color", Color(255,200,0), false, description = "Color theme in the gui.")
    val colorSettingMode = StringSelectorSetting("Color Mode", "HSB", arrayListOf("HSB", "RGB"), description = "Mode for all color settings in the gui. Changes the way colors are put in.")
    val clientName: StringSetting = StringSetting("Name", "Floppa Client", description = "Name that will be rendered in the gui.")
    val prefixStyle: StringSelectorSetting = StringSelectorSetting("Prefix Style", "Long", arrayListOf("Long", "Short", "Custom"), description = "Chat prefix selection for mod messages.")
    val customPrefix = StringSetting("Custom Prefix", "§0§l[§4§lFloppa Client§0§l]§r", 40, description = "You can set a custom chat prefix that will be used when Custom is selected in the Prefix Style dropdown.")
    val chromaSize = NumberSetting("Chroma Size", 0.5, 0.0, 1.0, 0.01, description = "Determines how rapidly the chroma pattern changes spatially.")
    val chromaSpeed = NumberSetting("Chroma Speed", 0.5, 0.0, 1.0, 0.01, description = "Determines how fast the chroma changes with time.")
    val chromaAngle = NumberSetting("Chroma Angle", 45.0, 0.0, 360.0,1.0, description = "Determines the direction in which the chroma changes on your screen.")
    val showUsageInfo = BooleanSetting("Usage Info", true, visibility = Visibility.ADVANCED_ONLY, description = "Show info on how to use the GUI.")
    val apiKey = StringSetting("API Key", "", length = 100, visibility = Visibility.HIDDEN)

    val panelX: MutableMap<Category, NumberSetting> = mutableMapOf()
    val panelY: MutableMap<Category, NumberSetting> = mutableMapOf()
    val panelExtended: MutableMap<Category, BooleanSetting> = mutableMapOf()

    private const val pwidth = 120.0
    private const val pheight = 15.0

    val panelWidth: NumberSetting  = NumberSetting("Panel width", default = pwidth, visibility = Visibility.HIDDEN)
    val panelHeight: NumberSetting = NumberSetting("Panel height", default = pheight, visibility = Visibility.HIDDEN)

    const val advancedRelWidth = 0.5
    const val advancedRelHeight = 0.5

    val advancedRelX = NumberSetting("Advanced_RelX",(1 - advancedRelWidth)/2.0,0.0, (1- advancedRelWidth), 0.0001, visibility = Visibility.HIDDEN)
    val advancedRelY = NumberSetting("Advanced_RelY",(1 - advancedRelHeight)/2.0,0.0, (1- advancedRelHeight), 0.0001, visibility = Visibility.HIDDEN)

    init {
        val options = java.util.ArrayList<String>()
        options.add("JellyLike")
        options.add("New")
        design = StringSelectorSetting("Design","JellyLike", options, description = "Design theme of the gui.")

        addSettings(
            design,
            blur,
            color,
            colorSettingMode,
            clientName,
            prefixStyle,
            customPrefix,
            chromaSize,
            chromaSpeed,
            chromaAngle,
            showUsageInfo,
            apiKey,
            advancedRelX,
            advancedRelY
        )

        // The Panels

        // this will set the default click gui panel settings. These will be overwritten by the config once it is loaded
        resetPositions()

        addSettings(
            panelWidth,
            panelHeight
        )

        for(category in Category.values()) {
            addSettings(
                panelX[category]!!,
                panelY[category]!!,
                panelExtended[category]!!
            )
        }
    }

    /**
     * Adds if missing and sets the default click gui positions for the category panels.
     */
    fun resetPositions() {
        panelWidth.value = pwidth
        panelHeight.value = pheight

        var px = 10.0
        val py = 10.0
        val pxplus = panelWidth.value + 10
        for(category in Category.values()) {
            panelX.getOrPut(category) { NumberSetting(category.name + ",x", default = px, visibility = Visibility.HIDDEN) }.value = px
            panelY.getOrPut(category) { NumberSetting(category.name + ",y", default = py, visibility = Visibility.HIDDEN) }.value = py
            panelExtended.getOrPut(category) { BooleanSetting(category.name + ",extended", default = true, visibility = Visibility.HIDDEN) }.enabled = true
            px += pxplus
        }

        advancedRelX.reset()
        advancedRelY.reset()
    }

    /**
     * Overridden to prevent the chat message from being sent.
     */
    override fun onKeyBind() {
        this.toggle()
    }

    /**
     * Automatically disable it again and open the gui
     */
    override fun onEnable() {
        display = FloppaClient.clickGUI
        super.onEnable()
        toggle()
    }

    /**
     * Detect the API key.
     */
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (!onHypixel || event.type == 2.toByte()) return
        val text = event.message.unformattedText.stripControlCodes()
        if (text.startsWith("Your new API key is ") && event.message.siblings.size >= 1) {
            apiKey.text = event.message.siblings[0].chatStyle.chatClickEvent.value
            modMessage("§bupdated your Hypixel API key to §2${apiKey.text}")
            return
        }
    }
}