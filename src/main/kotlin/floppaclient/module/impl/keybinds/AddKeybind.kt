package floppaclient.module.impl.keybinds

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.ModuleManager
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.ui.clickgui.elements.ModuleButton

object AddKeybind : Module(
    "Add New Key Bind",
    category = Category.KEY_BIND,
    description = "Adds a new key bind you can customize.",
    toggled = true
){
    override fun onEnable() {}

    override fun onDisable() {
        val bind = ModuleManager.addNewKeybind()
        toggle()
        ClickGUI.panels.find { it.category === Category.KEY_BIND }?.let {
            it.moduleButtons.add(ModuleButton(bind, it))
        }
    }

    override fun keyBind() {}
}