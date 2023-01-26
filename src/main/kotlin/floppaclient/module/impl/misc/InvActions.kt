package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.RESOURCE_DOMAIN
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.*
import floppaclient.floppamap.utils.HUDRenderUtils
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.dungeon.AutoTerms
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.ui.clickgui.util.FontUtil
import floppaclient.utils.Utils
import floppaclient.utils.Utils.isHolding
import floppaclient.utils.Utils.isInTerminal
import floppaclient.utils.fakeactions.FakeActionManager
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.EnumAction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
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
    description = "Lets you perform certain actions while in inventory. Automatically shoots the Bonzo staff at the floor " +
            "to apply knockback to the player. This bypasses watchdog. The knockback from the Bonzo staff will be cancelld."
) {
    private val rotate = BooleanSetting("Rotate", true, description = "Makes mouse movements in the inventory rotate your view angles. Can be toggled while in gui with Tab, if \"Tab toggle rot\" is enabled.")
    private val toggleRot = BooleanSetting("Tab toggle rot", true, description = "When enabled tab will toggle Rotate. This will also toggle hide terms if that is enabled.")
    private val grabCursor = BooleanSetting("Grab Cursor", true, description = "If enabled the cursor does not get un grabbed. This allows you to rotate further even when the edge of the screen is reached. A custom cursor wil be rendered instead.")
    private val hotbarSelection = BooleanSetting("Hotbar Select", true, description = "If enabled the Hotkeys will select your current hotbar slot but can no longer be used to move items in the inventory.")
    private val rightClick = BooleanSetting("Right Click", true, description = "If enabled right clicks anywhere in the gui will attempt using the right click ability of the currently held item. This is disabled for swords because blocking in inventory might flag.")
    private val invWalk = BooleanSetting("Inv Walk", true, description= "Lets you walk while in inventory.")
    private val kbmove = BooleanSetting("Kb Move", false, description= "Lets you walk while in inventory if enough knockback was recently taken.")
    private val kbWithBonzo = BooleanSetting("Kb holding Bonzo", true, description = "If enabled you can take knockback when holding the Bonzo Staff")
    private val autoKb = BooleanSetting("Auto Kb", true, visibility = Visibility.HIDDEN)
    private val onlyMoveOnKey = BooleanSetting("Only on Input", false, description = "Only starts shooting the Bonzo staff when a movement key is being held. Especially with high ping this will result in quite a bit of delay.")
    private val toggleAutoKb = BooleanSetting("f5 Toggle kb", true, description= "If enabled the perspective keybind will toggle whether the bonzo staff is being shot at the floor.")
    private val onlyInTerminal = BooleanSetting("Only In Terminal", false, description = "If enabled this module will only enabled in terminals.")
    private val hideTerminal = BooleanSetting("Hide Terminals", true, description = "Hides the inventory gui from rendering when in a terminal. A preview will be rendered instead in the corner of your screen. Only activates when rotate is enabled. \n§cClicks and key presses the Inventory will not be suppressed so be careful not to drop anything.")
    private val blockClicks = BooleanSetting("Block Clicks", true, description = "Suppresses Clicks and key presses in the Inventory when it is hidden.")
    private val stopInMelody = BooleanSetting("Stop in Melody", false, description = "Will prevent you from walking while in the melody terminal.")
    private val cursor = ResourceLocation(RESOURCE_DOMAIN, "gui/cursor.png")

    private var moveTime = 0L
    private const val moveBypass = 700L

    private var clickTime = 0L
    private var clickCooldown = 600L

    private val walkKeys = listOf(
        mc.gameSettings.keyBindSprint,
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight
    )

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
            kbmove,
            invWalk,
            kbWithBonzo,
            autoKb,
            onlyMoveOnKey,
            toggleRot,
            toggleAutoKb,
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

    /**
     * Takes care of automatically shooting the bonzo staff down so you can move.
     */
    @SubscribeEvent
    fun onTick(event: PositionUpdateEvent.Pre) {
        if (!this.enabled || (mc.currentScreen !is GuiContainer) || (onlyInTerminal.enabled && !isInTerminal())) return
        if (kbmove.enabled && autoKb.enabled) {
            //Check whether above blocks
            if (mc.thePlayer.isInLava || (!mc.thePlayer.onGround && !mc.theWorld.getBlockState(mc.thePlayer.position.down()).block.material.isSolid)) return

            if (kbWithBonzo.enabled && mc.thePlayer.isHolding("Bonzo's Staff")) return
            if (System.currentTimeMillis() > clickTime) {
                clickTime = System.currentTimeMillis() + clickCooldown
                if (onlyMoveOnKey.enabled) {
                    var keyDown = false
                    for (bind in walkKeys) {
                        if (GameSettings.isKeyDown(bind)) keyDown = true
                    }
                    if (!keyDown) return
                }
                val slot = Utils.findItem("Bonzo's Staff", true)
                if (slot != null) {
                    FakeActionManager.stageRightClickSlot(
                        mc.thePlayer.rotationYaw,
                        70f,
                        slot
                    )
                }
            }
        }
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
            this.rotate.toggle()
            if (rotate.enabled)
                Mouse.setGrabbed(grabCursor.enabled)
            else
                Mouse.setGrabbed(false)
        }
        if (toggleAutoKb.enabled && event.keyCode == mc.gameSettings.keyBindTogglePerspective.keyCode) {
            this.autoKb.toggle()
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
        if (invWalk.enabled || kbmove.enabled) {
            if (invWalk.enabled || System.currentTimeMillis() < moveTime) {
                for (bind in moveKeys) {
                    KeyBinding.setKeyBindState(bind.keyCode, GameSettings.isKeyDown(bind))
                }
            } else {
                for (bind in moveKeys) {
                    KeyBinding.setKeyBindState(bind.keyCode, false)
                }
            }

        }

        // Render terminal preview
        if (shouldHideContainer()) {
            event.isCanceled = true

            renderTermPreview()
            // rendered here as well as in onRenderLast. Only one of those can be active at a time.
            renderKbIndicator()
        }
    }

    /**
     * Render the Auto Kb indicator and the cursor.
     * This will only be called when the gui is not hidden.
     */
    @SubscribeEvent
    fun onRenderLast(event: DrawContainerLastEvent) {
        if (!this.enabled || (mc.currentScreen !is GuiContainer) || (onlyInTerminal.enabled && !isInTerminal())) return

        if (kbmove.enabled) {
            renderKbIndicator()
        }
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onKb(event: VelocityUpdateEvent) {
        val entity = mc.theWorld.getEntityByID(event.packet.entityID)
        if (entity == mc.thePlayer) {
            if (!FloppaClient.inSkyblock) return
            val totalMomentumSquare =
                (event.packet.motionX * event.packet.motionX + event.packet.motionY * event.packet.motionY + event.packet.motionZ * event.packet.motionZ)
//            if (packetInfo.enabled) {
//
//                modMessage("${event.packet.motionX}, ${event.packet.motionY}, ${event.packet.motionZ}, total: $totalMomentumSquare, normalized: ${totalMomentumSquare/8000.0/8000.0}")
//            }
            // This is true for bonzo staff, but false for chine
            if (totalMomentumSquare > 159_000_000) {
                if (kbmove.enabled) {
                    moveTime = System.currentTimeMillis() + moveBypass
                    if ((onlyInTerminal.enabled && !isInTerminal())) return
                    if (!mc.thePlayer.isInLava && (mc.currentScreen is GuiContainer) && (!kbWithBonzo.enabled || !mc.thePlayer.isHolding("Bonzo's Staff"))) event.isCanceled = true
                }
            }
        }
    }

    private fun renderKbIndicator() {
        val text = "Auto KB ${if (autoKb.enabled) "enabled" else "disabled"}"
        val scaledResolution = ScaledResolution(mc)

        val textColor = Color(0,0, 255, 255).rgb
        GlStateManager.pushMatrix()
        GlStateManager.translate(
            scaledResolution.scaledWidth.toDouble() / 2.0,
            scaledResolution.scaledHeight.toDouble() * 0.2,
            0.0
        )
        // Note: if you change these values they also have to be changed in isCursorOnReset
        val textWidth = FontUtil.getStringWidth(text)
        val textHeight = FontUtil.fontHeight.toDouble()
        val textX = -textWidth / 2.0
        val textY = -textHeight - 25
        val boxX = textX - 20
        val boxY = textY - 5
        val boxHeight = textHeight + 10
        val boxWidth = textWidth + 40.0

        val indicatorColor = if (!autoKb.enabled) {
            Color(200, 30, 30, 150)
        } else {
            Color(100, 200, 10, 150)
        }
        HUDRenderUtils.renderRect(
            boxX,
            boxY,
            boxWidth,
            boxHeight,
            indicatorColor
        )

        FontUtil.drawString(
            text,
            textX,
            textY,
            textColor
        )
        GlStateManager.popMatrix()
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

    @SubscribeEvent
    fun onWarp(event: WorldEvent.Load) {
        moveTime = 0L
        clickTime = 0L
    }
}