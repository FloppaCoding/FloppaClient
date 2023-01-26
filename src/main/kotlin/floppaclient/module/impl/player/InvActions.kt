package floppaclient.module.impl.player

import floppaclient.FloppaClient.Companion.RESOURCE_DOMAIN
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.*
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.dungeon.AutoTerms
import floppaclient.module.impl.player.Blink.sendPackets
import floppaclient.module.impl.player.Blink.noLag
import floppaclient.module.impl.player.Blink.packets
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.utils.ChatUtils.modMessage
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
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse
import java.awt.Color


/**
 * Module aimed to allow for as many actions from the inventory as possible without flagging.
 * Should reword alot of the module names and descriptions AND FIX A BUG THAT WHEN YOU ENABLE GUI MOVE WHILE MOVING U FLAG SOMETHING
 * @author Aton, Stivais
 */
object InvActions : Module(
    "Gui Move",
    category = Category.PLAYER,
    description = "Allows you to do stuff Inside GUIs"
) {
    private val invWalk = BooleanSetting(
        "Gui Move",
        true,
        description = "Allows you to move whilst in GUIs",
        visibility = Visibility.ADVANCED_ONLY
    )
    private val invWalkMode =
        SelectorSetting("Mode", "Vanilla", arrayListOf("Vanilla", "Lag"), description = "Mode for GUI Move")
            .withDependency { invWalk.enabled }
    private val rotate = BooleanSetting("Rotate", true, description = "Allows you to rotate whilst in GUIs")
    private val toggleRot = BooleanSetting("Toggle Rotation", false, description = "When pressing Tab it will toggle rotation")
        .withDependency { rotate.enabled}
    private val grabCursor = BooleanSetting("Grab Cursor", true, description = "If enabled the cursor does not get un grabbed. This allows you to rotate further even when the edge of the screen is reached. A custom cursor wil be rendered instead.")
        .withDependency { rotate.enabled }
    private val hotbarSelection = BooleanSetting("Hotbar Select", true, description = "When enabled, you can switch between Hotbar slots with Hotkeys")
    private val rightClick = BooleanSetting("Right Click", true, description = "Right-clicking in a GUI will allow you to use your Item. This is disabled for swords because blocking in inventory might flag.")
    private val onlyInTerminal = BooleanSetting("Only In Terminal", false, description = "If enabled, this module will only work in terminals")
    private val hideTerminal = BooleanSetting("Hide Terminals", true, description = "Will hide your inventory and render the terminals in a corner.\n§cClicks and key presses the Inventory will not be suppressed so be careful not to drop anything.")
        .withDependency { rotate.enabled }
    private val blockClicks = BooleanSetting("Block Clicks", true, description = "Suppresses Clicks and key presses in the Inventory when it is hidden.")
        .withDependency { hideTerminal.enabled }
    private val stopInMelody = BooleanSetting("Stop in Melody", false, description = "Will prevent you from walking while in the melody terminal.")
    private val cursor = ResourceLocation(RESOURCE_DOMAIN, "gui/cursor.png")
    private val debug = BooleanSetting("Debug Messages", false, visibility = Visibility.ADVANCED_ONLY)

    private var nextPulse = System.currentTimeMillis()
    private var didClick = System.currentTimeMillis()
    private var unLag = false

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
            invWalk,
            invWalkMode,
            rotate,
            grabCursor,
            hotbarSelection,
            rightClick,
            toggleRot,
            onlyInTerminal,
            hideTerminal,
            blockClicks,
            stopInMelody,
            debug,
        )
    }

    /**
     * Called by the entity renderer mixin to determine whether the mouse input should rotate the screen
     */
    fun shouldRotateHook(): Boolean {
        if (this.enabled && rotate.enabled && (mc.currentScreen is GuiContainer) && (!onlyInTerminal.enabled || isInTerminal())) {
            return true
        }
        return mc.inGameHasFocus
    }

    /**
     * Called by the Minecraft Mixin to determine whether the cursor should be ungrabbed for the gui.
     */
    fun shouldSkipUngrabMouse(): Boolean {
        if (this.enabled && grabCursor.enabled && rotate.enabled && (mc.currentScreen is GuiContainer) && (!onlyInTerminal.enabled || isInTerminal())) return true
        return false
    }

    @SubscribeEvent
    fun onMouseClicked(event: ContainerMouseClickedEvent) {
        if (!rightClick.enabled || (onlyInTerminal.enabled && !isInTerminal())) return
        if (event.mouseButton == mc.gameSettings.keyBindUseItem.keyCode + 100) {
            if (mc.thePlayer?.heldItem?.itemUseAction == EnumAction.BLOCK) return
            FakeActionUtils.useItem(mc.thePlayer.inventory.currentItem)
            event.isCanceled = true
            return
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
            rotate.toggle()
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
            // rendered here as well as in onRenderLast. Only one of those can be active at a time.
        }
    }

    /**
     * Handles the Lag Mode for GUI Move
     */

    @SubscribeEvent(receiveCanceled = true)
    fun onPacket(event: PacketSentEvent) {
        if (invWalkMode.selected != "Lag" || mc.thePlayer == null) return
        val packet = event.packet

        if (packet is C0EPacketClickWindow) {
            if (debug.enabled) return modMessage("C0EPacketClickWindow")

            unLag = true
            didClick = System.currentTimeMillis() + 25
        }

        if (packet is C03PacketPlayer) {
            if (noLag) return
            event.isCanceled = true
            packets.add(packet as Packet<INetHandlerPlayServer>)
        }
    }

    @SubscribeEvent
    fun onLivingUpdate(event: LivingEvent.LivingUpdateEvent) {
        if (invWalkMode.selected != "Lag") return

        if (mc.currentScreen !is GuiContainer) noLag = true

        if (didClick < System.currentTimeMillis() && unLag) {
            sendPackets()
            unLag = false
            nextPulse = System.currentTimeMillis() + 200

        } else if (nextPulse < System.currentTimeMillis()) {
            sendPackets()
            nextPulse = System.currentTimeMillis() + 200
        }
    }

    /**
     * Render the Auto Kb indicator and the cursor.
     * This will only be called when the gui is not hidden.
     */
    @SubscribeEvent
    fun onRenderLast(event: DrawContainerLastEvent) {
        if (!this.enabled || (mc.currentScreen !is GuiContainer) || (onlyInTerminal.enabled && !isInTerminal())) return

        //Render the cursor
        if (!grabCursor.enabled || !rotate.enabled) return
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
                        Color(0, 255, 0, 100)
                    else
                        Color(255, 255, 0, 100)
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
        hideTerminal.enabled && rotate.enabled && mc.thePlayer.openContainer is ContainerChest && isInTerminal()

    @SubscribeEvent
    fun onWarp(event: WorldEvent.Load) {
        packets.clear()
    }
}