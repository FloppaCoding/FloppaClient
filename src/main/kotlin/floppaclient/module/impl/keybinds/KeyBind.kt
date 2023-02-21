package floppaclient.module.impl.keybinds

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.ModuleManager
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.ActionSetting
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.ChatUtils
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.fakeactions.FakeActionUtils

class KeyBind(name: String) : Module(name, category = Category.KEY_BIND){
    private val modes = arrayListOf("Use Item","Command","Chat message")

    val bindName = StringSetting("Name", this.name, description = "The name of this Key Bind that will be shown on the toggle button in the GUI.")
    private val mode = SelectorSetting("Mode", modes[0], modes, description = "Action performed by the Bey Bind. Use Command for client side commands and Chat Message for server side commands.\nE.g. to open the Storage select Chat Message and type /storage in the action field.")
    private val command = StringSetting("Command","",50, description = "Command to be executed when command mode is selected. §eNo / needed!")
        .withDependency { this.mode.index == 1 }
    private val message = StringSetting("Message","",50, description = "Chat message to be sent when message mode is selected. §eFor server commands a / is needed.")
        .withDependency { this.mode.index == 2 }
    private val item = StringSetting("Item","",50, description = "Name of the Item to be used  when use item mode is selected.")
        .withDependency { this.mode.index == 0 }
    private val condition = StringSetting("Condition", description = "Only perform the action when holding the specified item. Only used for use item mode.")
        .withDependency { this.mode.index == 0 }
    private val fromInventory = BooleanSetting("From Inv", description = "Allows you to use items from your inventory.")
        .withDependency { this.mode.index == 0 }
    private val removeButton = ActionSetting("Remove Key Bind", visibility = Visibility.ADVANCED_ONLY, description = "Removes the Key Bind."){
        ModuleManager.removeKeyBind(this@KeyBind)
    }
    // Used by the config loader to determine whether a setting is a keybind
    private val flag = BooleanSetting("THIS_IS_A_KEY_BIND", visibility = Visibility.HIDDEN)

    init {
        this.addSettings(
            bindName,
            mode,
            command,
            message,
            item,
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
                ChatUtils.command(command.text, true)
            }
            "Chat message" -> {
                ChatUtils.sendChat(message.text)
            }
            "Use Item" -> {
                if (condition.text == "" || mc.thePlayer.isHolding(condition.text, ignoreCase = true)) {
                    FakeActionUtils.useItem(item.text,true, fromInventory.enabled, ignoreCase = true)
                }
            }
        }
    }
}