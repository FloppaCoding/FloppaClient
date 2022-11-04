package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ContainerKeyTypedEvent
import floppaclient.events.ContainerMouseClickedEvent
import floppaclient.events.DrawContainerEvent
import floppaclient.events.DrawContainerLastEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.dungeon.AutoTerms
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.Utils.isInTerminal
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.EnumAction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse
import java.awt.Color


/**
 * Module aimed to allow for as much actions from the inventory as possible without flagging.
 *
 * @author Aton
 */
object InvActions : Module(
    "Inv Action",
    category = Category.MISC,
    description = "Lets you perform certain actions while in inventory. §cAs of right now invwalk does not watchdog ban. But if you click you get sent to limbo."
) {
    private val rotate = BooleanSetting("Rotate", true, description = "Makes mouse movements in the inventory rotate your view angles. Can be toggled while in gui with Tab, if \"Tab toggle rot\" is enabled.")
    private val toggleRot = BooleanSetting("Tab toggle rot", true, description = "When enabled tab will toggle Rotate. This will also toggle hide terms if that is enabled.")
    private val grabCursor = BooleanSetting("Grab Cursor", true, description = "If enabled the cursor does not get un grabbed. This allows you to rotate further even when the edge of the screen is reached. A custom cursor wil be rendered instead.")
    private val hotbarSelection = BooleanSetting("Hotbar Select", true, description = "If enabled the Hotkeys will select your current hotbar slot but can no longer be used to move items in the inventory.")
    private val rightClick = BooleanSetting("Right Click", true, description = "If enabled right clicks anywhere in the gui will attempt using the right click ability of the currently held item. This is disabled for swords because blocking in inventory might flag.")
    private val invWalk = BooleanSetting("Inv Walk", true, description= "Lets you walk while in inventory.")
    private val onlyInTerminal = BooleanSetting("Only In Terminal", false, description = "If enabled this module will only enabled in terminals.")
    private val hideTerminal = BooleanSetting("Hide Terminals", true, description = "Hides the inventory gui from rendering when in a terminal. A preview will be rendered instead in the corner of your screen. Only activates when rotate is enabled. \n§cClicks and key presses the Inventory will not be suppressed so be careful not to drop anything.")
    private val blockClicks = BooleanSetting("Block Clicks", true, description = "Suppresses Clicks and key presses in the Inventory when it is hidden.")
    private val stopInMelody = BooleanSetting("Stop in Melody", false, description = "Will prevent you from walking while in the melody terminal.")

    private val cursor = ResourceLocation("floppaclient", "gui/cursor.png")

    private val moveKeys = listOf(
        mc.gameSettings.keyBindSneak,
        mc.gameSettings.keyBindJump,
        mc.gameSettings.keyBindSprint,
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight
    )

    init {
        this.addSettings(
            rotate,
            grabCursor,
            hotbarSelection,
            rightClick,
            invWalk,
            toggleRot,
            onlyInTerminal,
            hideTerminal,
            blockClicks,
            stopInMelody
        )
    }

    /**
     * Called by the entity renderer mixin to determine whether the mouse input should rotate the screen
     */
    fun shouldRotateHook(): Boolean {
        if (this.enabled && this.rotate.enabled && (mc.currentScreen is GuiContainer) && (!onlyInTerminal.enabled || isInTerminal())) {
            return true
        }
        return mc.inGameHasFocus
    }

    /**
     * Called by the Minecraft Mixin to determine whether the cursor should be ungrabbed for the gui.
     */
    fun shouldSkipUngrabMouse(): Boolean {
        if (this.enabled && this.grabCursor.enabled && rotate.enabled && (mc.currentScreen is GuiContainer) && (!onlyInTerminal.enabled || isInTerminal())) return true
        return false
    }

    @SubscribeEvent
    fun onMouseClicked(event: ContainerMouseClickedEvent) {
        if (!rightClick.enabled || (onlyInTerminal.enabled && !isInTerminal())) return
        if (event.mouseButton == mc.gameSettings.keyBindUseItem.keyCode + 100) {
            if (mc.thePlayer?.heldItem?.itemUseAction == EnumAction.BLOCK) return
            FakeActionUtils.useItem(mc.thePlayer.inventory.currentItem)
            event.isCanceled = true
        }
        if (blockClicks.enabled && shouldHideContainer()) event.isCanceled = true
    }

    @SubscribeEvent
    fun onKeyTyped(event: ContainerKeyTypedEvent) {
        if ((onlyInTerminal.enabled && !isInTerminal())) return
        if (hotbarSelection.enabled) {
            for (i in 0..8) {
                if (event.keyCode == mc.gameSettings.keyBindsHotbar[i].keyCode) {
                    mc.thePlayer.inventory.currentItem = i
                    event.isCanceled = true
                    return
                }
            }
        }
        if (toggleRot.enabled && event.keyCode == mc.gameSettings.keyBindPlayerList.keyCode) {
            this.rotate.toggle()
            if (rotate.enabled)
                Mouse.setGrabbed(grabCursor.enabled)
            else
                Mouse.setGrabbed(false)
        }
        if (blockClicks.enabled && shouldHideContainer()) event.isCanceled = true
    }

    /**
     * Render then mouse cursor if it is grabbed.
     * and Handle Movement.
     */
    @SubscribeEvent
    fun onRender(event: DrawContainerEvent) {
        if (!this.enabled || (mc.currentScreen !is GuiContainer) || (onlyInTerminal.enabled && !isInTerminal())) return
        if (stopInMelody.enabled && AutoTerms.currentTerminal == AutoTerms.TerminalType.TIMING) return
        if (invWalk.enabled) {
            for (bind in moveKeys) {
                KeyBinding.setKeyBindState(bind.keyCode, GameSettings.isKeyDown(bind))
            }
        } else {
            for (bind in moveKeys) {
                KeyBinding.setKeyBindState(bind.keyCode, false)
            }
        }

        // Render terminal preview
        if (shouldHideContainer()) {
            event.isCanceled = true
            renderTermPreview()
        }
    }

    /**
     * Render the cursor.
     * This will only be called when the gui is not hidden.
     */
    @SubscribeEvent
    fun onRenderLast(event: DrawContainerLastEvent) {
        if (!this.enabled || (mc.currentScreen !is GuiContainer) || (onlyInTerminal.enabled && !isInTerminal())) return
        //Render the cursor
        if (!this.grabCursor.enabled || !this.rotate.enabled) return
        cursor.let {
            val cursorSize = 10

            GlStateManager.pushMatrix()
            GlStateManager.doPolygonOffset(1.0f, -2000000.0f)
            GlStateManager.enablePolygonOffset()
            GlStateManager.disableLighting()
            GlStateManager.color(255f, 255f, 255f, 255f)
            mc.textureManager.bindTexture(it)
            Gui.drawModalRectWithCustomSizedTexture(
                event.mouseX,
                event.mouseY,
                0f, 0f, cursorSize, cursorSize, cursorSize.toFloat(), cursorSize.toFloat()
            )
            GlStateManager.doPolygonOffset(0.0f, 0.0f)
            GlStateManager.disablePolygonOffset()
            GlStateManager.popMatrix()
        }
    }

    private fun renderTermPreview() {
        val container = mc.thePlayer.openContainer ?: return
        if (container !is ContainerChest) return

        val i = 10
        val j = 10
        Gui.drawRect(i, j, i + 9 * 20 + 10, j + 6 * 20 + 10, Color(30, 30, 30, 150).rgb)
        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        RenderHelper.enableGUIStandardItemLighting()
        GlStateManager.pushMatrix()
        GlStateManager.translate(i.toFloat(), j.toFloat(), 0.0f)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.enableRescaleNormal()
        val k = 240
        val l = 240
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, k.toFloat() / 1.0f, l.toFloat() / 1.0f)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        var y = j - 20 - 3

        for (i1 in 0..53) {
            if (i1 % 9 == 0)
                y += 20
            val x = i - 3 + 20 * (i1 % 9)
            val stack = container.lowerChestInventory.getStackInSlot(i1)
            val slot = container.inventorySlots.getOrNull(i1)
            if (stack != null && slot != null) {

                if (AutoTerms.showClicks.enabled && AutoTerms.clickQueue.contains(slot)) {
                    val color = if (AutoTerms.clickQueue.indexOf(slot) == 0)
                        Color(0,255,0,100)
                    else
                        Color(255,255,0,100)
                    Gui.drawRect(x, y, x + 16, y + 16, color.rgb)
                }

                mc.renderItem.renderItemAndEffectIntoGUI(stack, x, y)
                mc.renderItem.renderItemOverlayIntoGUI(
                    mc.fontRendererObj,
                    stack,
                    x,
                    y,
                    null
                )
            }
        }
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.popMatrix()
    }

    private fun shouldHideContainer(): Boolean =
        this.hideTerminal.enabled && rotate.enabled && mc.thePlayer.openContainer is ContainerChest && isInTerminal()
}