package floppaclient.mixins.entity;

import floppaclient.events.PositionUpdateEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {EntityPlayerSP.class}, priority = 799)
public abstract class MixinPlayerSP2 extends MixinAbstractClientPlayer {

    @Shadow
    private boolean serverSneakState;

    @Shadow private double lastReportedPosX;

    @Shadow private double lastReportedPosY;

    @Shadow private double lastReportedPosZ;

    @Shadow private float lastReportedYaw;

    @Shadow private float lastReportedPitch;

    @Inject(method = {"onUpdateWalkingPlayer"}, at = {@At("RETURN")})
    private void postPlayerWalkUpdate(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PositionUpdateEvent.Post(this.lastReportedPosX, this.lastReportedPosY, this.lastReportedPosZ, this.lastReportedYaw, this.lastReportedPitch, this.onGround, this.serverSneakState, this.serverSneakState));
    }
}
