package floppaclient.module.impl.keybinds

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.ModuleManager
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.ActionSetting
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils
import floppaclient.utils.Utils.isHolding
import floppaclient.utils.fakeactions.FakeActionUtils

class KeyBind(name: String) : Module(name, category = Category.KEY_BIND){
    private val modes = arrayListOf("Use Item","Command","Chat message")

    val bindName = StringSetting("Name",this.name)
    private val mode = SelectorSetting("Mode", modes[0], modes, description = "Action performed by the keybind. Use Command for client side commands and Chat Message for server side commands.\nE.g. to open the Storage select Chat Message and type /storage in the action field.")
    private val action = StringSetting("Action","",50, description = "Name of the Item to be used / command to be executed or chat message to be sent.")
    private val condition = StringSetting("Condition", description = "Only perform the action when holding the specified item. Only used for items.")
    private val fromInventory = BooleanSetting("From Inv", description = "Allows you to use items from your inventory.")
    private val removeButton = ActionSetting("Remove Key Bind", description = "Removes the Key Bind.").apply {
        action = {
            ModuleManager.removeKeyBind(this@KeyBind)
        }
    }
    // Used by the config loader to determine whether a setting is a keybind
    private val flag = BooleanSetting("THIS_IS_A_KEY_BIND", visibility = Visibility.HIDDEN)

    init {
        this.addSettings(
            bindName,
            mode,
            action,
            condition,
            fromInventory,
            removeButton,
            flag
        )
    }

    override fun keyBind() {
        if (!this.enabled) return
        performAction()
    }

    private fun performAction(){
        when(mode.selected){
            "Command" -> {
                Utils.command(action.text, true)
            }
            "Chat message" -> {
                Utils.sendChat(action.text)
            }
            "Use Item" -> {
                if (condition.text == "" || mc.thePlayer.isHolding(condition.text, true)) {
                    FakeActionUtils.useItem(action.text,true, fromInventory.enabled, ignoreCase = true)
                }
            }
        }
    }
}