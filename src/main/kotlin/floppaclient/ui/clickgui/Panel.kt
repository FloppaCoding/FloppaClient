package floppaclient.ui.clickgui

import floppaclient.module.Category
import floppaclient.module.impl.render.ClickGui
import floppaclient.ui.clickgui.elements.ModuleButton
import floppaclient.ui.clickgui.util.ColorUtil
import floppaclient.ui.clickgui.util.FontUtil
import floppaclient.ui.clickgui.util.FontUtil.capitalizeOnlyFirst
import net.minecraft.client.gui.Gui
import java.awt.Color

/**
 * Provides a category panel for the click gui.
 * Based on HeroCode's gui.
 *
 * @author HeroCode, Aton
 */
open class Panel(
    var category: Category,
    var clickgui: ClickGUI
) {
    private val title: String = category.name.capitalizeOnlyFirst()
    private var x2 = 0.0
    private var y2 = 0.0
    var dragging = false
    var visible = true
    var moduleButtons: ArrayList<ModuleButton> = ArrayList<ModuleButton>()

    var width: Double = ClickGui.panelWidth.value
    var height: Double = ClickGui.panelHeight.value
    var x: Double = ClickGui.panelX[category]!!.value
    var y: Double = ClickGui.panelY[category]!!.value
    var extended: Boolean = ClickGui.panelExtended[category]!!.enabled

    private var scrollOffset: Double = 0.0
    private val scrollAmmount: Double = 15.0

    init {
        setup()
    }

    /**
	 * Gets overridden in ClickGUI to add module buttons
	 */
    open fun setup() {}

    /**
	 * Rendering the element
	 */
    fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!visible) return
        if (dragging) {
            x = x2 + mouseX
            y = y2 + mouseY
        }

        /** Set color values */
        val temp = ColorUtil.clickGUIColor.darker()
        val outlineColor = Color(temp.red, temp.green, temp.blue, 170).rgb

        /** Render the module buttons and the Settings elements */
        if (extended && moduleButtons.isNotEmpty()) {
            var startY = y + height - scrollOffset
            val epanelcolor = ColorUtil.moduleButtonColor
            for (moduleButton in moduleButtons) {
                // Render the module Button
                if (shouldRender(startY + moduleButton.height)) {
                    if (ClickGui.design.isSelected("New")) {
                        Gui.drawRect(
                            (x - 2).toInt(),
                            startY.toInt(),
                            (x + width).toInt(),
                            (startY + moduleButton.height + 1).toInt(),
                            outlineColor
                        )
                    }
                    Gui.drawRect(
                        x.toInt(),
                        startY.toInt(),
                        (x + width).toInt(),
                        (startY + moduleButton.height + 1).toInt(),
                        epanelcolor
                    )
                }
                moduleButton.x = x + 2
                moduleButton.y = startY
                moduleButton.width = width - 4
                if (shouldRender(startY + moduleButton.height)) {
                    moduleButton.drawScreen(mouseX, mouseY, partialTicks)
                }

                /** Render the settings elements */
                var offs = moduleButton.height + 1
                if (moduleButton.extended && moduleButton.menuelements.isNotEmpty()) {
                    for (menuElement in moduleButton.menuelements) {
                        menuElement.offset = offs
                        menuElement.update()
                        if (shouldRender(menuElement.y + menuElement.height)) {
                            if (ClickGui.design.isSelected("New")) {
                                val validStartY = validStart(menuElement.y)
                                Gui.drawRect(
                                    menuElement.x.toInt(),
                                    validStartY.toInt(),
                                    (menuElement.x + menuElement.width + 2).toInt(),
                                    (menuElement.y + menuElement.height).toInt(),
                                    outlineColor
                                )
                            }
                            menuElement.drawScreen(mouseX, mouseY, partialTicks)
                        }
                        offs += menuElement.height
                    }
                }

                startY += offs
            }
            Gui.drawRect(x.toInt(), (startY + 1).toInt(), (x + width).toInt(), (startY + 1).toInt(), epanelcolor)
        }

        // Render the Panel
        Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), -0xededee)

        if (ClickGui.design.isSelected("New")) {
            Gui.drawRect((x - 2).toInt(), y.toInt(), x.toInt(), (y + height).toInt(), outlineColor)
            FontUtil.drawStringWithShadow(title, x + 2, y + height / 2 - FontUtil.fontHeight / 2, -0x101011)
        } else if (ClickGui.design.isSelected("JellyLike")) {
            Gui.drawRect((x + 4).toInt(), (y + 2).toInt(), (x + 4.3).toInt(), (y + height - 2).toInt(), -0x555556)
            Gui.drawRect((x - 4 + width).toInt(), (y + 2).toInt(), (x - 4.3 + width).toInt(), (y + height - 2).toInt(), -0x555556)
            FontUtil.drawTotalCenteredStringWithShadow(title, x + width / 2, y + height / 2, -0x101011)
        }
    }

    /**
     * Checks the given start position whether it is below the panel button.
     */
    fun shouldRender(endY: Double): Boolean{
        return endY > y + height || ClickGui.scrollPastTop.enabled
    }

    /**
     * Returns the y, from which on rendering is allowed to account for scrolling past the top.
     */
    fun validStart(startY: Double): Double{
        return if (ClickGui.scrollPastTop.enabled)
            startY
        else
            startY.coerceAtLeast(y)
    }

    /**
	 * Handles interactions with the panel
	 */
    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!visible) {
            return false
        }
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            x2 = x - mouseX
            y2 = y - mouseY
            dragging = true
            return true
        } else if (mouseButton == 1 && isHovered(mouseX, mouseY)) {
            extended = !extended
            return true
        } else if (extended) {
            for (moduleButton in moduleButtons) {
                if (shouldRender(moduleButton.y + moduleButton.height)) {
                    if (moduleButton.mouseClicked(mouseX, mouseY, mouseButton)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
	 * Handles interactions with the panel
	 */
    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (!visible) {
            return
        }
        if (state == 0) {
            dragging = false
        }

        // saving changes on mouse release instead of in the individual positions to have it all in one place
        ClickGui.panelX[category]!!.value = x
        ClickGui.panelY[category]!!.value = y
        ClickGui.panelExtended[category]!!.enabled = extended
    }

    /**
     * Scrolls the panel extension by the given number of elements.
     * If the panel is not extended does nothing.
     *
     * @param amount The amount to scroll
     */
    fun scroll(amount: Int, mouseX: Int, mouseY: Int): Boolean {
        val length = isHoveredExtended(mouseX, mouseY)
        if (length != null) {
            val diff = (-amount * scrollAmmount).coerceAtMost(length - 13)

            scrollOffset = (scrollOffset + diff).coerceAtLeast(0.0)
            return true
        }
        return false
    }

    /**
	 * HoverCheck
	 */
    private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    /**
     * HoverCheck for extended buttons
     */
    private fun isHoveredExtended(mouseX: Int, mouseY: Int): Double? {
        if (extended && moduleButtons.isNotEmpty()) {
            var length = - scrollOffset
            for (moduleButton in moduleButtons) {
                var offs = moduleButton.height + 1
                if (moduleButton.extended && moduleButton.menuelements.isNotEmpty()) {
                    for (menuElement in moduleButton.menuelements) {
                        menuElement.offset = offs
                        menuElement.update()
                        offs += menuElement.height
                    }
                }
                length += offs
            }
            return if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height + length.coerceAtLeast(0.0))
                length
            else
                null
        }
        return null
    }
}