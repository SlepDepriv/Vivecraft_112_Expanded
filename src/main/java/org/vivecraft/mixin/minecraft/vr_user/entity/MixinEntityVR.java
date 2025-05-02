package org.vivecraft.mixin.minecraft.vr_user.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.entity.IEntityPlayerSPVR;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public class MixinEntityVR {

    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public World world;
    @Shadow
    public float width;
    @Shadow
    public float height;


    // This is gross, I hate this, I hate this.
    @Inject(method = "setLocationAndAngles", at = @At("TAIL"))
    public void onSetLocationAndAngles(double x, double y, double z, float yaw, float pitch, CallbackInfo ci)
    {
        if (this instanceof IEntityPlayerSPVR) {
            //Vivecraft - this is for when the server moves the player entity, such as spawning, dimension change
            //or dismount.
            if(!((IEntityPlayerSPVR)this).getInitFromServer())
                return;

            ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().snapRoomOriginToPlayerEntity((EntityPlayerSP) (IEntityPlayerSPVR)this, false, false);
            //mc.vrSettings.vrWorldRotation = yaw; this was a terrible idea
        }
    }

    @Inject(method = "setPositionAndRotation", at = @At("TAIL"))
    public void onSetPositionAndRotation(double x, double y, double z, float yaw, float pitch, CallbackInfo ci)
    {
        if (this instanceof IEntityPlayerSPVR) {

            ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().snapRoomOriginToPlayerEntity((EntityPlayerSP) (IEntityPlayerSPVR) this, false, false);
            //mc.vrSettings.vrWorldRotation = yaw;

            if (!((IEntityPlayerSPVR) this).getInitFromServer()) {
                this.setLocationAndAngles(x, y, z, yaw, pitch);
                ((IEntityPlayerSPVR) this).setInitFromServer(true);
            }
        }
    }

    // VIVE START - update room origin when player entity is moved
    @Inject(method = "setPosition", at = @At("HEAD"), cancellable = true)
    public void onSetPosition(double x, double y, double z, CallbackInfo ci)
    { //this is also called when riding to move this around.
        if (this instanceof IEntityPlayerSPVR) {
            double bx = this.posX;
            double by = this.posY;
            double bz = this.posZ;

            // Just copy the original setPosition code here because I'm lazy
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            if (this.isAddedToWorld() && !this.world.isRemote) this.world.updateEntityWithOptionalForce((EntityPlayerSP)(IEntityPlayerSPVR)this, false); // Forge - Process chunk registration after moving.
            float f = this.width / 2.0F;
            float f1 = this.height;
            this.setEntityBoundingBox(new AxisAlignedBB(x - (double)f, y, z - (double)f, x + (double)f, y + (double)f1, z + (double)f));
            //

            double ax = this.posX;
            double ay = this.posY;
            double az = this.posZ;

            Entity mount = this.getRidingEntity();
            if (isRiding()){
                Vec3d offset = ((IMinecraftVR)Minecraft.getMinecraft()).getVehicleTracker().Premount_Pos_Room;
                offset = offset.rotateYaw(((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().vrdata_world_pre.rotation_radians);
                Entity e= mount;
                x = x - offset.x;
                y = ((IMinecraftVR)Minecraft.getMinecraft()).getVehicleTracker().getVehicleFloor(mount, y);
                z = z - offset.z;
                ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().setRoomOrigin(x, y, z, false);
            } else {
                System.out.println(((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer() == null);
                System.out.println(((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().roomOrigin == null);
                Vec3d roomOrigin = ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().roomOrigin;
                OpenVRPlayer.get().setRoomOrigin(
                        roomOrigin.x + (ax - bx),
                        roomOrigin.y + (ay - by),
                        roomOrigin.z + (az - bz),
                        false
                );
            }

            // 	}
            ci.cancel();
        }
    }

    @Shadow
    @Nullable
    public Entity getRidingEntity()
    {
        return null;
    }

    @Shadow
    public final boolean isAddedToWorld() {
        return false;
    }

    @Shadow
    public void setEntityBoundingBox(AxisAlignedBB bb)
    {

    }

    @Shadow
    public void setPosition(double x, double y, double z)
    {

    }

    @Shadow
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch)
    {

    }
    @Shadow
    public boolean isRiding()
    {
        return false;
    }
}
