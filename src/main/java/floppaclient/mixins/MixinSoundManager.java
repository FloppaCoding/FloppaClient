package floppaclient.mixins;

import floppaclient.events.PlaySoundEventPre;
import floppaclient.hooks.SoundManagerHook;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Taken from Skytils
 */
@Mixin(value = {SoundManager.class}, priority = 900)
public class MixinSoundManager {

    @Inject(method = "getNormalizedVolume", at = @At("HEAD"), cancellable = true)
    private void bypassPlayerVolume(ISound sound, SoundPoolEntry entry, SoundCategory category, CallbackInfoReturnable<Float> cir) {
        SoundManagerHook.bypassCategoryVolume(sound, entry, category, cir);
    }

    @Inject(method = "playSound", at = @At("HEAD"))
    public void onPlaySound(ISound p_sound, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PlaySoundEventPre(p_sound));
    }
}
