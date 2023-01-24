package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.ui.hud.EditHudGUI

/**
 * Open the edit hid gui.
 * @author Aton
 */
object EditHud : Module(
    "Edit Hud",
    category = Category.RENDER,
    description = "Opens the edit hud gui."
){

    /**
     * Overridden to prevent the chat message from being sent.
     */
    override fun keyBind() {
        this.toggle()
    }

    /**
     * Automatically disable it again and open the gui
     */
    override fun onEnable() {
        FloppaClient.display = EditHudGUI
        toggle()
        super.onEnable()
    }
}