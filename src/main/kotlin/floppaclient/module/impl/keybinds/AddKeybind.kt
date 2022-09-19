package floppaclient.module.impl.keybinds

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.ModuleManager

object AddKeybind : Module(
    "Add New Key Bind",
    category = Category.KEY_BIND,
    description = "Adds a new key bind you can customize.",
    toggled = true
){
    override fun onEnable() {}

    override fun onDisable() {
        ModuleManager.addNewKeybind()
        toggle()
        FloppaClient.clickGUI.setUpPanels()
    }

    override fun keyBind() {}
}