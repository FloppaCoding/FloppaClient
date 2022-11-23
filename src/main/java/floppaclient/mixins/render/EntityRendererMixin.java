package floppaclient.mixins.render;

import floppaclient.module.impl.misc.InvActions;
import floppaclient.module.impl.misc.QOL;
import floppaclient.module.impl.render.Camera;
import floppaclient.module.impl.render.ChestEsp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( value = {EntityRenderer.class})
public class EntityRendererMixin{

    @Shadow private float thirdPersonDistance;

    @Shadow private float thirdPersonDistanceTemp;

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistance:F"))
    public float thirdPersonDistance(EntityRenderer instance) {
        Float dist = Camera.INSTANCE.thirdPersonDistanceHook();
        return (dist != null) ? dist : this.thirdPersonDistance;
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistanceTemp:F"))
    public float thirdPersonDistanceTemp(EntityRenderer instance) {
        Float dist = Camera.INSTANCE.thirdPersonDistanceHook();
        return (dist != null) ? dist : this.thirdPersonDistanceTemp;
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    public double cameraDistance(Vec3 instance, Vec3 vec) {
        return Camera.INSTANCE.cameraClipHook(instance,vec);
    }

    @Redirect(method = {"setupFog"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    public boolean shouldBeBlind(EntityLivingBase instance, Potion potionIn) {
        return QOL.INSTANCE.blindnessHook();
    }

    @Redirect(method = {"updateCameraAndRender"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHasFocus:Z"))
    public boolean shouldMoveMouse(Minecraft instance) {
        return InvActions.INSTANCE.shouldRotateHook();
    }

    @Inject(method = {"updateCameraAndRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V", shift = At.Shift.BEFORE))
    public void startWorldRender(float partialTicks, long nanoTime, CallbackInfo ci){
        ChestEsp.INSTANCE.setDrawingWorld(true);
    }

    @Inject(method = {"updateCameraAndRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", shift = At.Shift.BEFORE))
    public void endWorldRender(float partialTicks, long nanoTime, CallbackInfo ci){
        ChestEsp.INSTANCE.setDrawingWorld(false);
    }
}
