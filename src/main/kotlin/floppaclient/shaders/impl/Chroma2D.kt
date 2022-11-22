package floppaclient.shaders.impl

import floppaclient.FloppaClient.Companion.mc
import floppaclient.FloppaClient.Companion.totalTicks
import floppaclient.module.impl.render.ClickGui
import floppaclient.shaders.Shader
import floppaclient.shaders.uniforms.impl.Uniform1F
import floppaclient.utils.Utils.timer
import kotlin.math.exp

/**
 * This shader will put a 2d chroma effect over anything that is rendered.
 * The color of the rendered vertices is overwritten.
 */
object Chroma2D: Shader("chroma2d/chroma2d") {

    /*
    The scaling for the values is not super straightforward, but makes configuring the chroma effect very smooth.
     */

    private val chromaSize = Uniform1F(this, "chromaSize") {
        (exp( ClickGui.chromaSize.value.toFloat() * 0.5f ) -1f)  / 30f
    }
    private val chromaSpeed = Uniform1F(this, "chromaTime") {
        if (ClickGui.chromaSpeed.value == 0.0)
            0f
        else
            ((totalTicks + mc.timer.renderPartialTicks) / 20.0 * (1- exp(ClickGui.chromaSpeed.value)) ).toFloat()
    }
    private val chromaAngle = Uniform1F(this, "chromaAngle") {
        (ClickGui.chromaAngle.value.toFloat() + 90f) * 0.017453292f
    }

    init {
        this.registerUniforms(
            chromaSize,
            chromaSpeed,
            chromaAngle
        )
    }
}