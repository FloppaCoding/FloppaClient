package floppaclient.mixins.render;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import floppaclient.module.impl.misc.InvActions;
import floppaclient.module.impl.misc.QOL;
import floppaclient.module.impl.player.FreeCam2;
import floppaclient.module.impl.render.Camera;
import floppaclient.module.impl.render.ChestEsp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <h2>{@link EntityRenderer} is the class which dispatches all rendering actions.</h2>
 *
 * <p>It also handles things like setting up the camera and effects.
 * Entities are rendered in {@link net.minecraft.client.renderer.RenderGlobal RenderGlobal}.</p>
 *
 * The mixins in here are used for
 * {@link FreeCam2},
 * {@link Camera},
 * {@link ChestEsp},
 * {@link QOL},
 * {@link InvActions}.
 *
 * @author Aton
 */
@Mixin( value = {EntityRenderer.class})
abstract public class EntityRendererMixin implements IResourceManagerReloadListener {

    @Shadow private float thirdPersonDistance;

    @Shadow private float thirdPersonDistanceTemp;

    @Shadow private Minecraft mc;

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
    public void onStartWorldRender(float partialTicks, long nanoTime, CallbackInfo ci){
        ChestEsp.INSTANCE.setDrawingWorld(true);
    }

    @Inject(method = {"updateCameraAndRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", shift = At.Shift.BEFORE))
    public void onEndWorldRender(float partialTicks, long nanoTime, CallbackInfo ci){
        ChestEsp.INSTANCE.setDrawingWorld(false);
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    public void onSetCameraAngle(EntityPlayerSP instance, float yaw, float pitch) {
        if (FreeCam2.INSTANCE.shouldTweakMovement()) {
            FreeCam2.INSTANCE.setViewAngles(yaw, pitch);
        }else {
            instance.setAngles(yaw, pitch);
        }
    }

    /**
     * Tweak the renderViewEntity for {@link FreeCam2}.
     *
     * Unfortunately using a regex to target multiple methods with a single Redirect did not work so there are three now.
     * <pre>
     * {@code
     * @Redirect(method = "name=/renderWorldPass|orientCamera|getMouseOver/", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
     * }
     * </pre>
     */
    @Redirect(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    public Entity tweakRenderViewEntity(Minecraft instance) {
        if (FreeCam2.INSTANCE.isFreecamActive()) {
            return FreeCam2.INSTANCE.tweakRenderViewEntityHook();
        }else {
            return instance.getRenderViewEntity();
        }
    }

    /**
     * Tweak the renderViewEntity for {@link FreeCam2}.
     */
    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    public Entity tweakRenderViewEntity2(Minecraft instance) {
        if (FreeCam2.INSTANCE.isFreecamActive()) {
            return FreeCam2.INSTANCE.tweakRenderViewEntityHook();
        }else {
            return instance.getRenderViewEntity();
        }
    }

    /**
     * Adjust what the player is looking at when in Freecam.
     */
    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"))
    public Entity tweakRenderViewEntity3(Minecraft instance) {
        if (FreeCam2.INSTANCE.shouldTweakLookingAt()) {
            return FreeCam2.INSTANCE.tweakRenderViewEntityHook();
        }else {
            return instance.getRenderViewEntity();
        }
    }

    /**
     * Ensures that the player cannot interact with their own character when in Freecam.
     */
    @SuppressWarnings("Guava")
    @Redirect(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getEntitiesInAABBexcluding(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;"))
    public List<Entity> tweakMouseOver(WorldClient instance, Entity entity, AxisAlignedBB axisAlignedBB, Predicate<Entity> predicate) {
        return instance.getEntitiesInAABBexcluding(entity, axisAlignedBB, Predicates.and(predicate, input -> input != this.mc.thePlayer));
    }
}
