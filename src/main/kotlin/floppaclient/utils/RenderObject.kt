package floppaclient.utils

import floppaclient.FloppaClient.Companion.mc
import org.lwjgl.opengl.GL11
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.awt.Color
// TODO Doc comments for this and the methods. Also maybe rename to WorldRenderUtils.
// TODO maybe merge the code for drawing boxes into one method to reduce redundancy.
object RenderObject {

    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: WorldRenderer = tessellator.worldRenderer
    private val renderManager = mc.renderManager

    fun drawLine(start: Vec3, finish: Vec3, color: Color, thickness: Float = 3f, phase: Boolean = true) {
        drawLine(start.xCoord, start.yCoord, start.zCoord, finish.xCoord, finish.yCoord, finish.zCoord, color, thickness, phase)
    }

    fun drawLine (x: Double, y: Double, z: Double, x2: Double, y2: Double, z2:Double, color: Color, thickness: Float = 3f, phase: Boolean = true) {
        GlStateManager.disableLighting()
        GL11.glBlendFunc(770, 771)
        GlStateManager.enableBlend()
        GL11.glLineWidth(thickness)
        if (phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.pushMatrix()

        GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, 1f)

        worldRenderer.pos(x, y, z).endVertex()
        worldRenderer.pos(x2, y2, z2).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawBoxAtBlock (blockPos: BlockPos, color: Color, thickness: Float = 3f, relocate: Boolean = true) {
        drawBoxAtBlock(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), color, thickness, relocate)
    }

    fun drawBoxAtBlock (x: Double, y: Double, z: Double, color: Color, thickness: Float = 3f, relocate: Boolean = true) {
        GlStateManager.disableLighting()
        GL11.glBlendFunc(770, 771)
        GlStateManager.enableBlend()
        GL11.glLineWidth(thickness)
        GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.pushMatrix()

        if (relocate) GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, 1f)


        worldRenderer.pos(x+1,y+1,z+1).endVertex()
        worldRenderer.pos(x+1,y+1,z).endVertex()
        worldRenderer.pos(x,y+1,z).endVertex()
        worldRenderer.pos(x,y+1,z+1).endVertex()
        worldRenderer.pos(x+1,y+1,z+1).endVertex()
        worldRenderer.pos(x+1,y,z+1).endVertex()
        worldRenderer.pos(x+1,y,z).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x,y,z+1).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x,y+1,z).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x+1,y,z).endVertex()
        worldRenderer.pos(x+1,y+1,z).endVertex()
        worldRenderer.pos(x+1,y,z).endVertex()
        worldRenderer.pos(x+1,y,z+1).endVertex()
        worldRenderer.pos(x,y,z+1).endVertex()
        worldRenderer.pos(x,y+1,z+1).endVertex()
        worldRenderer.pos(x+1,y+1,z+1).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawBoxByEntity (entity: Entity, color: Color, width: Double, height: Double, partialTicks: Float,
                         lineWidth: Double = 2.0, phase: Boolean = false,
                         xOffs: Double = 0.0, yOffs: Double = 0.0, zOffs: Double = 0.0
    ) {
        drawBoxByEntity(entity, color, width.toFloat(), height.toFloat(), partialTicks, lineWidth.toFloat(),phase,xOffs, yOffs, zOffs)
    }

    fun drawBoxByEntity (entity: Entity, color: Color, width: Float, height: Float, partialTicks: Float,
                         lineWidth: Float = 2f, phase: Boolean = false,
                         xOffs: Double = 0.0, yOffs: Double = 0.0, zOffs: Double = 0.0
    ){
        val x = entity.posX + ((entity.posX-entity.lastTickPosX)*partialTicks) + xOffs
        val y = entity.posY + ((entity.posY-entity.lastTickPosY)*partialTicks) + yOffs
        val z = entity.posZ + ((entity.posZ-entity.lastTickPosZ)*partialTicks) + zOffs

        val radius = width/2
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GL11.glBlendFunc(770, 771)
        GL11.glLineWidth(lineWidth)
        if(phase) GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()
        GlStateManager.pushMatrix()

        GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, 1f)


        worldRenderer.pos(x+radius,y+height,z+radius).endVertex()
        worldRenderer.pos(x+radius,y+height,z-radius).endVertex()
        worldRenderer.pos(x-radius,y+height,z-radius).endVertex()
        worldRenderer.pos(x-radius,y+height,z+radius).endVertex()
        worldRenderer.pos(x+radius,y+height,z+radius).endVertex()
        worldRenderer.pos(x+radius,y,z+radius).endVertex()
        worldRenderer.pos(x+radius,y,z-radius).endVertex()
        worldRenderer.pos(x-radius,y,z-radius).endVertex()
        worldRenderer.pos(x-radius,y,z+radius).endVertex()
        worldRenderer.pos(x-radius,y,z-radius).endVertex()
        worldRenderer.pos(x-radius,y+height,z-radius).endVertex()
        worldRenderer.pos(x-radius,y,z-radius).endVertex()
        worldRenderer.pos(x+radius,y,z-radius).endVertex()
        worldRenderer.pos(x+radius,y+height,z-radius).endVertex()
        worldRenderer.pos(x+radius,y,z-radius).endVertex()
        worldRenderer.pos(x+radius,y,z+radius).endVertex()
        worldRenderer.pos(x-radius,y,z+radius).endVertex()
        worldRenderer.pos(x-radius,y+height,z+radius).endVertex()
        worldRenderer.pos(x+radius,y+height,z+radius).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        if(phase) GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }

    fun drawCustomSizedBoxAt(x: Double, y: Double, z: Double, size: Double, color: Color, thickness: Float = 3f, relocate: Boolean = true) {
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(thickness)
        GlStateManager.disableDepth()
        GlStateManager.disableTexture2D()

        GlStateManager.pushMatrix()

        if (relocate) GlStateManager.translate(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        GlStateManager.color(color.red.toFloat() / 255f, color.green.toFloat() / 255f,
            color.blue.toFloat() / 255f, 1f)


        worldRenderer.pos(x+size,y+size,z+size).endVertex()
        worldRenderer.pos(x+size,y+size,z).endVertex()
        worldRenderer.pos(x,y+size,z).endVertex()
        worldRenderer.pos(x,y+size,z+size).endVertex()
        worldRenderer.pos(x+size,y+size,z+size).endVertex()
        worldRenderer.pos(x+size,y,z+size).endVertex()
        worldRenderer.pos(x+size,y,z).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x,y,z+size).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x,y+size,z).endVertex()
        worldRenderer.pos(x,y,z).endVertex()
        worldRenderer.pos(x+size,y,z).endVertex()
        worldRenderer.pos(x+size,y+size,z).endVertex()
        worldRenderer.pos(x+size,y,z).endVertex()
        worldRenderer.pos(x+size,y,z+size).endVertex()
        worldRenderer.pos(x,y,z+size).endVertex()
        worldRenderer.pos(x,y+size,z+size).endVertex()
        worldRenderer.pos(x+size,y+size,z+size).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
    }
}