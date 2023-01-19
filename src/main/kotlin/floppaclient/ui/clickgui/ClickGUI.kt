package floppaclient.ui.clickgui

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.moduleConfig
import floppaclient.module.Category
import floppaclient.module.impl.render.ClickGui
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.elements.menu.ElementColor
import floppaclient.ui.clickgui.elements.menu.ElementSlider
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException

/**
 * Provides and constructs the click gui menu
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
class ClickGUI : GuiScreen() {

    var scale = 2.0

    private val logo = ResourceLocation(FloppaClient.RESOURCE_DOMAIN, "gui/Icon.png")

    /**
     * Used to add a delay for closing the gui, so that it does not instantly get closed
     */
    private var openedTime = System.currentTimeMillis()

    /**
     * Used to create the advanced menu for modules
     */
    var advancedMenu: AdvancedMenu? = null

    init {
        FontUtil.setupFontUtils()
        setUpPanels()
    }

    fun setUpPanels() {
        /** Create a panel for each module category */
        panels = ArrayList()
        for (category in Category.values()) {
            panels.add(Panel(category, this))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {

        // Scale the gui and the mouse coordinates
        // the handling of the mouse coordinates is not nice, since it has to be done in multiple places
        val scaledresolution = ScaledResolution(mc)
        val prevScale = mc.gameSettings.guiScale
        scale = clickGuiScale / scaledresolution.scaleFactor
        mc.gameSettings.guiScale = 2
        GL11.glScaled(scale, scale, scale)

        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        /** Draw the Logo and the title */
        logo.let {
            val scaledResolution = ScaledResolution(mc)

            val temp = ColorUtil.clickGUIColor.darker()
            val titleColor = Color(temp.red, temp.green, temp.blue, 255).rgb
            val logoSize = 25

            GL11.glPushMatrix()
            GL11.glTranslated(
                scaledResolution.scaledWidth.toDouble(),
                scaledResolution.scaledHeight.toDouble(),
                0.0
            )

            GL11.glScaled(2.0, 2.0, 2.0)
            val titleWidth = FontUtil.getStringWidth(ClickGui.clientName.text)

            GlStateManager.color(255f, 255f, 255f, 255f)
            FloppaClient.mc.textureManager.bindTexture(it)
            Gui.drawModalRectWithCustomSizedTexture(
                - 5 - logoSize,
                -5 - logoSize,
                0f, 0f, logoSize, logoSize, logoSize.toFloat(), logoSize.toFloat()
            )

            FontUtil.drawString(
                ClickGui.clientName.text,
                -titleWidth.toDouble() - 10.0 - logoSize,
                -FontUtil.fontHeight.toDouble() / 2.0 - 5.0 - logoSize / 2.0,
                titleColor
            )
            GL11.glPopMatrix()
        }

        /* Calls all panels to render themselves and their module buttons and elements.
		  * Important to keep in mind: the panel rendered last will be on top.
          * For intuitive behaviour the panels have to be checked in reversed order for clicks.
          * This ensures that interactions will happen with the top panel. */
        for (p in panels) {
            p.drawScreen(scaledMouseX, scaledMouseY, partialTicks)
        }

        if(ClickGui.showUsageInfo.enabled) {
            val scaledResolution = ScaledResolution(mc)

            val lines = listOf<String>("GUI Usage:",
            "Left click Module Buttons to toggle the Module.",
            "Right click Module Buttons to extend the Settings dropdown.",
            "Middle click Module Buttons to open the Advanced Gui.",
            "Disable this Overlay in the Advanced Settings of the Click Gui Module in the Render Category."
            )

            val temp = ColorUtil.clickGUIColor.darker()
            val titleColor = Color(temp.red, temp.green, temp.blue, 255).rgb

            GL11.glPushMatrix()
            GL11.glTranslated(
                scaledResolution.scaledWidth.toDouble()*0.1,
                scaledResolution.scaledHeight.toDouble()*0.7,
                0.0
            )

            GL11.glScaled(1.5, 1.5, 1.5)
            for ((ii, line) in lines.withIndex()) {
                FontUtil.drawString(
                    line,
                    0.0,
                    FontUtil.fontHeight.toDouble() * ii,
                    titleColor
                )
            }
            GL11.glPopMatrix()
        }

        if (advancedMenu != null) {
            advancedMenu?.drawScreen(scaledMouseX, scaledMouseY, partialTicks)
        }

        /** Might be needed to use gui buttons */
        super.drawScreen(scaledMouseX, scaledMouseY, partialTicks)

        mc.gameSettings.guiScale = prevScale
    }

    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        var i = Mouse.getEventDWheel()
        if (i != 0) {
            if (i > 1) {
                i = 1
            }
            if (i < -1) {
                i = -1
            }
            if (isShiftKeyDown()) {
                i *= 7
            }
            // Scroll the advanced gui
            advancedMenu?.scroll(i, scaledMouseX, scaledMouseY)

            /** Checking all panels for scroll action.
             * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
            for (panel in panels.reversed()) {
                if (panel.scroll(i, scaledMouseX, scaledMouseY)) return
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        // handle the advanced gui first
        if (advancedMenu?.mouseClicked(scaledMouseX, scaledMouseY, mouseButton) == true) return

        /** Checking all panels for click action.
          * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
        for (panel in panels.reversed()) {
            // Handle panel button first.
            if (panel.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)) return

            // If no panel button was clicked check the module buttons and elements
            if (panel.extended && panel.visible) {
                for (moduleButton in panel.moduleButtons) {
                    if (moduleButton.extended) {
                        for (menuElement in moduleButton.menuElements) {
                            if (panel.shouldRender(menuElement.y + menuElement.height)) {
                                if (menuElement.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)) {
                                    moduleButton.updateElements()
                                    return
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            super.mouseClicked(scaledMouseX, scaledMouseY, mouseButton)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        val scaledMouseX = getScaledMouseX()
        val scaledMouseY = getScaledMouseY()

        // handle mouse release for advanced menu first
        advancedMenu?.mouseReleased(scaledMouseX, scaledMouseY, state)

        /** Checking all panels for mouse release action.
         * Reversed order is used to guarantee that the panel rendered on top will be handled first. */
        for (panel in panels.reversed()) {
            // Handle panel button first.
            panel.mouseReleased(scaledMouseX, scaledMouseY, state)

            // If no panel button was clicked check the module buttons and elements
            if (panel.extended && panel.visible) {
                for (moduleButton in panel.moduleButtons) {
                    if (moduleButton.extended) {
                        for (menuElement in moduleButton.menuElements) {
                            if (panel.shouldRender(menuElement.y + menuElement.height)) {
                                menuElement.mouseReleased(scaledMouseX, scaledMouseY, state)
                            }
                        }
                    }
                }
            }
        }

        super.mouseReleased(scaledMouseX, scaledMouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {

        // First handle the advanced gui
        /** If in an advanced menu only hande that */
        if (advancedMenu != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                advancedMenu = null
            }
            advancedMenu?.keyTyped(typedChar,keyCode)
            return
        }

        /** For key registration in the menu elements. Required for text fields.
         * Reversed order to check the panel on top first! */
        for (panel in panels.reversed()) {
            if (panel.extended && panel.visible && panel.moduleButtons.size > 0) {
                for (moduleButton in panel.moduleButtons) {
                    if (moduleButton.extended) {
                        for (menuElement in moduleButton.menuElements) {
                            if (menuElement.keyTyped(typedChar, keyCode)) return
                        }
                    }
                }
            }
        }

        /** Exits the menu when the toggle key is pressed */
        if (keyCode == ClickGui.keyCode && System.currentTimeMillis() - openedTime > 200) {
            mc.displayGuiScreen(null as GuiScreen?)
            if (mc.currentScreen == null) {
                mc.setIngameFocus()
            }
            return
        }

        /** keyTyped in GuiScreen gets used to exit the gui on escape */
        try {
            super.keyTyped(typedChar, keyCode)
        } catch (e2: IOException) {
            e2.printStackTrace()
        }
    }

    override fun initGui() {
        openedTime = System.currentTimeMillis()
        /** Start blur */
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGui.blur.enabled) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }

        /** update panel positions to make it possible to update the positions
         * this is required for loading the panel positions from the config and for resetting the gui */
        for (panel in panels) {
            panel.x = ClickGui.panelX[panel.category]!!.value
            panel.y = ClickGui.panelY[panel.category]!!.value
            panel.extended = ClickGui.panelExtended[panel.category]!!.enabled
        }
    }

    override fun onGuiClosed() {
        /** End blur */
        mc.entityRenderer.stopUseShader()

        /** stop sliders from being active */
        for (panel in panels.reversed()) {
            if (panel.extended && panel.visible) {
                for (moduleButton in panel.moduleButtons) {
                    if (moduleButton.extended) {
                        for (menuElement in moduleButton.menuElements) {
                            if (menuElement is ElementSlider) {
                                menuElement.dragging = false
                            }
                            if (menuElement is ElementColor) {
                                menuElement.dragging = null
                            }
                        }
                    }
                }
            }
        }

        /** Save the changes to the config file */
        moduleConfig.saveConfig()
    }

    fun closeAllSettings() {
        for (panel in panels) {
            if (panel.visible && panel.extended && panel.moduleButtons.size > 0) {
                for (moduleButton in panel.moduleButtons) {
                    moduleButton.extended = false
                }
            }
        }
    }

    private fun getScaledMouseX(): Int {
        return MathHelper.ceiling_double_int(Mouse.getX() / clickGuiScale)
    }
    private fun getScaledMouseY(): Int {
        // maybe -1 or floor required here because of the inversion.
        return MathHelper.ceiling_double_int( (mc.displayHeight - Mouse.getY()) / clickGuiScale)
    }

    companion object {
        const val clickGuiScale = 2.0
        var panels: ArrayList<Panel> = arrayListOf()
    }
}