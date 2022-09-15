package floppaclient.hooks

import floppaclient.utils.Utils
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.client.audio.SoundPoolEntry
import net.minecraft.util.MathHelper
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Taken from Skytils
 */
object SoundManagerHook {

    @JvmStatic
    fun bypassCategoryVolume(
        sound: ISound,
        entry: SoundPoolEntry,
        category: SoundCategory,
        cir: CallbackInfoReturnable<Float>
    ) {
        if (Utils.shouldBypassVolume) cir.returnValue = MathHelper.clamp_float(sound.volume, 0f, 1f)
    }
}