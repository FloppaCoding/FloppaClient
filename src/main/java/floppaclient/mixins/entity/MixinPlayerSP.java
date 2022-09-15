package floppaclient.mixins.entity;

import floppaclient.FloppaClient;
import floppaclient.events.PositionUpdateEvent;
import floppaclient.module.impl.misc.QOL;
import floppaclient.utils.fakeactions.FakeActionManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovementInput;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {EntityPlayerSP.class}, priority = 800)
public abstract class MixinPlayerSP extends MixinAbstractClientPlayer {
    @Shadow
    private boolean serverSprintState;

    @Shadow
    private boolean serverSneakState;

    @Shadow
    private float lastReportedYaw;

    @Shadow
    private double lastReportedPosY;

    @Shadow
    private double lastReportedPosX;

    @Shadow
    private double lastReportedPosZ;

    @Shadow
    private int positionUpdateTicks;

    @Shadow
    private float lastReportedPitch;

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    public MovementInput movementInput;

    @Inject(method = {"onUpdateWalkingPlayer"}, at = {@At("HEAD")}, cancellable = true)
    private void onPlayerWalkUpdate(CallbackInfo ci) {

        PositionUpdateEvent.Pre pre = new PositionUpdateEvent.Pre(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround, isSprinting(), isSneaking());
        MinecraftForge.EVENT_BUS.post(pre);

        if (movementInput != null) {
            movementInput.sneak = pre.getSneaking();
        }

        boolean doAction = FakeActionManager.INSTANCE.getDoAction();

        if (doAction) {
            FakeActionManager.INSTANCE.fakeRotate();


            boolean flag = FloppaClient.mc.thePlayer.isSprinting();

            if (flag != this.serverSprintState) {
                if (flag) {
                    FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(FloppaClient.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                } else {
                    FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(FloppaClient.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }

                this.serverSprintState = flag;
            }

            boolean flag1 = FloppaClient.mc.thePlayer.isSneaking();

            if (flag1 != this.serverSneakState) {
                if (flag1) {
                    FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(FloppaClient.mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                } else {
                    FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(FloppaClient.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }

                this.serverSneakState = flag1;
            }

            if (FloppaClient.mc.getRenderViewEntity() == FloppaClient.mc.thePlayer) {
                double d0 = FloppaClient.mc.thePlayer.posX - this.lastReportedPosX;
                double d1 = FloppaClient.mc.thePlayer.getEntityBoundingBox().minY - this.lastReportedPosY;
                double d2 = FloppaClient.mc.thePlayer.posZ - this.lastReportedPosZ;
                double d3 = (FloppaClient.mc.thePlayer.rotationYaw - this.lastReportedYaw);
                double d4 = (FloppaClient.mc.thePlayer.rotationPitch - this.lastReportedPitch);
                boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
                boolean flag3 = d3 != 0.0D || d4 != 0.0D;

                if (FloppaClient.mc.thePlayer.ridingEntity == null) {
                    if (flag2 && flag3) {
                        FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(FloppaClient.mc.thePlayer.posX, FloppaClient.mc.thePlayer.getEntityBoundingBox().minY, FloppaClient.mc.thePlayer.posZ, FloppaClient.mc.thePlayer.rotationYaw, FloppaClient.mc.thePlayer.rotationPitch, FloppaClient.mc.thePlayer.onGround));
                    } else if (flag2) {
                        FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(FloppaClient.mc.thePlayer.posX, FloppaClient.mc.thePlayer.getEntityBoundingBox().minY, FloppaClient.mc.thePlayer.posZ, FloppaClient.mc.thePlayer.onGround));
                    } else if (flag3) {
                        FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(FloppaClient.mc.thePlayer.rotationYaw, FloppaClient.mc.thePlayer.rotationPitch, FloppaClient.mc.thePlayer.onGround));
                    } else {
                        FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(FloppaClient.mc.thePlayer.onGround));
                    }
                } else {
                    FloppaClient.mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(FloppaClient.mc.thePlayer.motionX, -999.0D, FloppaClient.mc.thePlayer.motionZ, FloppaClient.mc.thePlayer.rotationYaw, FloppaClient.mc.thePlayer.rotationPitch, FloppaClient.mc.thePlayer.onGround));
                    flag2 = false;
                }

                ++this.positionUpdateTicks;

                if (flag2) {
                    this.lastReportedPosX = FloppaClient.mc.thePlayer.posX;
                    this.lastReportedPosY = FloppaClient.mc.thePlayer.getEntityBoundingBox().minY;
                    this.lastReportedPosZ = FloppaClient.mc.thePlayer.posZ;
                    this.positionUpdateTicks = 0;
                }

                if (flag3) {
                    this.lastReportedYaw = FloppaClient.mc.thePlayer.rotationYaw;
                    this.lastReportedPitch = FloppaClient.mc.thePlayer.rotationPitch;
                }
            }

            FakeActionManager.INSTANCE.interact();
            FakeActionManager.INSTANCE.rotateBack();
            FakeActionManager.INSTANCE.reset();
            ci.cancel();
        }
    }

    @Redirect(method = {"pushOutOfBlocks"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;noClip:Z"))
    public boolean shouldCancelPushOut(EntityPlayerSP instance){
        return QOL.INSTANCE.preventPushOut(this.noClip);
    }
}
