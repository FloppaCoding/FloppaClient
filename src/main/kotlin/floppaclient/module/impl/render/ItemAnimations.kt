package floppaclient.module.impl.render

import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.MathHelper
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.exp
import kotlin.math.pow

/**
 * Module to change the appearance of held items.
 *
 * This module uses the EntityLivingBase and ItemRenderer Mixins to function.
 * Because only this module and no others are supposed to modify their behavior direct references are used instead of
 * forge events.
 *
 * @author Aton
 */
object ItemAnimations : Module(
    "Animations",
    category = Category.RENDER,
    description = "Changes the appearance of held items."
) {


    private val size = NumberSetting("Size", 0.0, -1.5, 1.5, 0.05, description = "Scales the size of your currently held item. Default: 0")
    private val scaleSwing = BooleanSetting("Scale Swing", true, description = "Also scale the size of the swing animation.")
    private val x = NumberSetting("X", 0.0, -2.5, 1.5, 0.05, description = "Moves the held item. Default: 0")
    private val y = NumberSetting("Y", 0.0, -1.5, 1.5, 0.05, description = "Moves the held item. Default: 0")
    private val z = NumberSetting("Z", 0.0, -1.5, 3.0, 0.05, description = "Moves the held item. Default: 0")
    private val yaw = NumberSetting("Yaw", 0.0, -180.0, 180.0, 5.0, description = "Rotates your held item. Default: 0")
    private val pitch = NumberSetting("Pitch", 0.0, -180.0, 180.0, 5.0, description = "Rotates your held item. Default: 0")
    private val roll = NumberSetting("Roll", 0.0, -180.0, 180.0, 5.0, description = "Rotates your held item. Default: 0")
    private val blockAnimation = BooleanSetting("No Block Animation", false, description = "Doesn't show blocking animation.")

    /**
     * Used in the EntitiyLivingBaseMixin
     */
    val speed = NumberSetting("Speed", 0.0, -2.0, 1.0, 0.05, description = "Speed of the swing animation.")
    val ignoreHaste  = BooleanSetting("Ignore Haste", false, description = "Makes the chosen speed override haste modifiers.")

    init {
        this.addSettings(
            size,
            scaleSwing,
            x, y, z,
            pitch, yaw, roll,
            speed,
            ignoreHaste,
            blockAnimation
        )
    }

    private var isRightClickKeyDown = false

    /**
     * Directly referenced hook for the itemTransform Inject in the ItemRenderer Mixin.
     * Takes care of scaling and positioning the held item.
     */
    fun itemTransforHook(equipProgress: Float, swingProgress: Float): Boolean {
        if (!this.enabled) return false
        val newSize = (0.4 * exp(size.value)).toFloat()
        val newX = (0.56f * (1 + x.value)).toFloat()
        val newY = (-0.52f * (1 - y.value)).toFloat()
        val newZ = (-0.71999997f * (1 + z.value)).toFloat()
        GlStateManager.translate(newX, newY, newZ)
        GlStateManager.translate(0.0f, equipProgress * -0.6f, 0.0f)

        //Rotation
        GlStateManager.rotate(pitch.value.toFloat(), 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(yaw.value.toFloat(), 0.0f, 1f, 0f)
        GlStateManager.rotate(roll.value.toFloat(), 0f, 0f, 1f)

        GlStateManager.rotate(45f, 0.0f, 1f, 0f)

        val f = MathHelper.sin(swingProgress * swingProgress * Math.PI.toFloat())
        val f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * Math.PI.toFloat())
        GlStateManager.rotate(f * -20.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(f1 * -20.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate(f1 * -80.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(newSize, newSize, newSize)
        return true
    }

    /**
     * Directly referenced by the ItemRendereMixin. If enabled will scale the item swing animation.
     * Returns whether custom animation was performed.
     */
    fun scaledSwing(swingProgress: Float): Boolean {
        if (!this.enabled || !scaleSwing.enabled) return false
        val scale = exp(size.value).toFloat()
        val f = -0.4f * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * Math.PI.toFloat()) * scale
        val f1 = 0.2f * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * Math.PI.toFloat() * 2.0f) * scale
        val f2 = -0.2f * MathHelper.sin(swingProgress * Math.PI.toFloat()) * scale
        GlStateManager.translate(f, f1, f2)
        return true
    }

    /**
     * Directly referenced by the ItemRendereMixin. If enabled will scale the potion drink animation.
     * Returns whether custom animation was performed.
     */
    fun scaledDrinking(clientPlayer: AbstractClientPlayer, partialTicks: Float, itemToRender: ItemStack): Boolean {
        if (!this.enabled) return false
        val f: Float = clientPlayer.itemInUseCount.toFloat() - partialTicks + 1.0f
        val f1: Float = f / itemToRender.maxItemUseDuration.toFloat()
        var f2 = MathHelper.abs(MathHelper.cos(f / 4.0f * Math.PI.toFloat()) * 0.1f)

        if (f1 >= 0.8f) {
            f2 = 0.0f
        }

        // Transform to correct rotation center
        val newX = (0.56f * (1 + x.value)).toFloat()
        val newY = (-0.52f * (1 - y.value)).toFloat()
        val newZ = (-0.71999997f * (1 + z.value)).toFloat()
        GlStateManager.translate(-0.56f, 0.52f, 0.71999997f)
        GlStateManager.translate(newX, newY, newZ)

        GlStateManager.translate(0.0f, f2, 0.0f)
        val f3 = 1.0f - f1.toDouble().pow(27.0).toFloat()
        GlStateManager.translate(f3 * 0.6f, f3 * -0.5f, f3 * 0.0f)
        GlStateManager.rotate(f3 * 90.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(f3 * 10.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(f3 * 30.0f, 0.0f, 0.0f, 1.0f)

        // Transform back
        GlStateManager.translate(0.56f, -0.52f, -0.71999997f)
        GlStateManager.translate(-newX, -newY, -newZ)
        return true
    }

    // A lot of the code was from sbc
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !blockAnimation.enabled) return
        isRightClickKeyDown = mc.gameSettings.keyBindUseItem.isKeyDown
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (!blockAnimation.enabled) return
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            val item = mc.thePlayer.heldItem ?: return
            if (item.item !is ItemSword) return
            event.isCanceled = true
            if (!isRightClickKeyDown) {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            }
        }
    }
}