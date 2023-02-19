package floppaclient.floppamap.utils

import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.core.DungeonPlayer
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.module.impl.render.DungeonMap
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EnumPlayerModelParts
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * A Collection of methods for rendering 2D Objects in orthographic projection for the HUD or for a gui.
 *
 * Heavily based on the rendering for [Funny Map by Harry282](https://github.com/Harry282/FunnyMap/blob/master/src/main/kotlin/funnymap/utils/RenderUtils.kt).
 */
object HUDRenderUtils {

    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer

    fun renderRect(x: Double, y: Double, w: Double, h: Double, color: Color) {
        if (color.alpha == 0) return
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        addQuadVertices(x, y, w, h)
        tessellator.draw()

        GlStateManager.disableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    fun renderRectBorder(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        GlStateManager.shadeModel(GL11.GL_FLAT)

        addQuadVertices(x - thickness, y, thickness, h)
        addQuadVertices(x - thickness, y - thickness, w + thickness * 2, thickness)
        addQuadVertices(x + w, y, thickness, h)
        addQuadVertices(x - thickness, y + h, w + thickness * 2, thickness)

        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
    }

    private fun addQuadVertices(x: Double, y: Double, w: Double, h: Double) {
        worldRenderer.pos(x, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y + h, 0.0).endVertex()
        worldRenderer.pos(x + w, y, 0.0).endVertex()
        worldRenderer.pos(x, y, 0.0).endVertex()
    }

    /**
     * Used for funny map text rendering. Has the text scaling directly integrated.
     */
    fun renderCenteredText(text: List<String>, x: Int, y: Int, color: Int) {
        GlStateManager.pushMatrix()

        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(DungeonMap.textScale.value, DungeonMap.textScale.value, 1.0)

        if (text.isNotEmpty()) {
            val yTextOffset = text.size * 5f
            for (i in text.indices) {
                mc.fontRendererObj.drawString(
                    text[i],
                    (-mc.fontRendererObj.getStringWidth(text[i]) shr 1).toFloat(),
                    i * 10 - yTextOffset,
                    color,
                    true
                )
            }
        }
        GlStateManager.popMatrix()
    }

    fun drawTexturedModalRect(x: Int, y: Int, width: Int, height: Int) {
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(0.0, 1.0).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(1.0, 1.0).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(1.0, 0.0).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(0.0, 0.0).endVertex()
        tessellator.draw()
    }

    /**
     * Renders the player heads for funny map. Has the scaling directly integrated.
     */
    fun drawPlayerHead(player: DungeonPlayer) {
        if (player.dead || player.player == null) return
        GlStateManager.pushMatrix()
        try {
            val skin = mc.netHandler.getPlayerInfo(player.player.uniqueID).locationSkin ?: return
            if (player.player == mc.thePlayer) {
                GlStateManager.translate(
                    (mc.thePlayer.posX - Dungeon.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first - 2,
                    (mc.thePlayer.posZ - Dungeon.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second - 2,
                    0.0
                )
            } else {
                GlStateManager.translate(player.mapX, player.mapZ, 0.0)
            }

            if (DungeonMap.playerNameMode.index == 2 || DungeonMap.playerNameMode.index == 1
                && mc.thePlayer.isHolding(SkyblockItem.SPIRIT_LEAP, SkyblockItem.INFINILEAP)
            ) {
                GlStateManager.pushMatrix()
                GlStateManager.scale(0.8, 0.8, 1.0)
                if (DungeonMap.spinnyMap.enabled) GlStateManager.rotate(mc.thePlayer.rotationYawHead + 180f, 0f, 0f, 1f)
                mc.fontRendererObj.drawString(
                    player.name,
                    -mc.fontRendererObj.getStringWidth(player.name) shr 1,
                    10,
                    0xffffff
                )
                GlStateManager.popMatrix()
            }
            if (player.player == mc.thePlayer) {
                GlStateManager.rotate(mc.thePlayer.rotationYawHead + 180f, 0f, 0f, 1f)
            } else {
                GlStateManager.rotate(player.yaw + 180f, 0f, 0f, 1f)
            }
            GlStateManager.scale(DungeonMap.playerHeadScale.value, DungeonMap.playerHeadScale.value, 1.0)
            renderRectBorder(-6.0, -6.0, 12.0, 12.0, 1.0, Color(0, 0, 0, 255))
            GlStateManager.color(1f, 1f, 1f, 1f)
            mc.textureManager.bindTexture(skin)
            Gui.drawScaledCustomSizeModalRect(-6, -6, 8f, 8f, 8, 8, 12, 12, 64f, 64f)
            if (player.player.isWearing(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(-6, -6, 40f, 8f, 8, 8, 12, 12, 64f, 64f)
            }
        } catch (_: Exception) {
        }
        GlStateManager.popMatrix()
    }
}
