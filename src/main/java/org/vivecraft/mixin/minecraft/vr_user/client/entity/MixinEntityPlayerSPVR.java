package org.vivecraft.mixin.minecraft.vr_user.client.entity;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSoundMinecartRiding;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.main.Main;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.api.NetworkHelper;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.entity.IEntityPlayerSPVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.jkatvr;

import java.util.List;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSPVR extends AbstractClientPlayer implements IEntityPlayerSPVR {

    // VIVE START - teleport movement
    public int movementTeleportTimer;
    public boolean initFromServer;
    // VIVE END - teleport movement
    /** MINECRIFT **/
    public float headPitch = 0;
    public double additionX = 0;
    public double additionZ = 0;
    public double additionY = 0;
    public final float PIOVER180 = (float)Math.PI / 180.0F;

    @Shadow
    protected Minecraft mc;
    @Shadow
    private boolean rowingBoat;
    @Shadow
    private boolean serverSprintState;
    @Shadow
    public NetHandlerPlayClient connection;
    @Shadow
    private boolean serverSneakState;
    @Shadow
    private double lastReportedPosX;
    @Shadow
    private double lastReportedPosY;
    @Shadow
    private double lastReportedPosZ;
    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
    @Shadow
    private boolean prevOnGround;
    @Shadow
    private int positionUpdateTicks;
    @Shadow
    private boolean autoJumpEnabled;
    @Shadow
    public MovementInput movementInput;
    @Shadow
    private int autoJumpTime;

    public MixinEntityPlayerSPVR(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Override
    public void setMovementTeleportTimer(int i) {
        movementTeleportTimer = i;
    }

    @Override
    public int getMovementTeleportTimer() {
        return movementTeleportTimer;
    }

    @Override
    public void setTeleported(boolean bool) {
        teleported = bool;
    }

    @Override
    public boolean getTeleported() {
        return teleported;
    }


    /** END MINECRIFT **/

    //VIVE
    public void setItemInUseClient(ItemStack item){
        this.activeItemStack = item;
    }


    public void setItemInUseCountClient(int count){
        this.activeItemStackUseCount = count;
    }

    @Override
    public boolean getOverrideEyeHeight() {
        return overrideEyeHeight;
    }

    @Override
    public void setOverrideEyeHeight(boolean bool) {
        overrideEyeHeight = bool;
    }

    @Override
    public boolean getInitFromServer() {
        return initFromServer;
    }

    @Override
    public void setInitFromServer(boolean bool) {
        initFromServer = bool;
    }

    //END VIVECRAFT

    @Overwrite
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        //VIVECRAFT
        if(amount > 0){
            int dur = 1000;
            if(source.isExplosion())dur = 2000;
            if(source == DamageSource.CACTUS) dur = 200;
            //Vivecraft trigger haptics
            MCOpenVR.triggerHapticPulse(0, dur);
            MCOpenVR.triggerHapticPulse(1, dur);
        }
        //END VIVECRAFT

        //Forge
        ForgeHooks.onLivingAttack((EntityPlayerSP)(IEntityPlayerSPVR)this, source, amount);
        return false;
    }

    private boolean snapReq = false;

    @Overwrite
    public boolean startRiding(Entity entityIn, boolean force)
    {
        if (!super.startRiding(entityIn, force))
        {
            return false;
        }
        else
        {
            if (entityIn instanceof EntityMinecart)
            {
                this.mc.getSoundHandler().playSound(new MovingSoundMinecartRiding((EntityPlayerSP)(IEntityPlayerSPVR)this, (EntityMinecart)entityIn));
            }

            if (entityIn instanceof EntityBoat)
            {
                this.prevRotationYaw = entityIn.rotationYaw;
                this.rotationYaw = entityIn.rotationYaw;
                this.setRotationYawHead(entityIn.rotationYaw);
            }

            //Vivecraft
            ((IMinecraftVR)this.mc).getVehicleTracker().onStartRiding(entityIn, (EntityPlayerSP)(IEntityPlayerSPVR)this);
            snapReq = true;
            //
            return true;
        }
    }

    private float startmountrotate;
    public boolean teleported;

    @Shadow
    public void dismountRidingEntity()
    {
        super.dismountRidingEntity();
        this.rowingBoat = false;

        //Vivecraft
        ((IMinecraftVR)this.mc).getVehicleTracker().onStopRiding((EntityPlayerSP)(IEntityPlayerSPVR)this);
        //
    }

    @Overwrite
    private void onUpdateWalkingPlayer()
    {
        boolean flag = this.isSprinting();

        if (flag != this.serverSprintState)
        {
            if (flag)
            {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SPRINTING));
            }
            else
            {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = flag;
        }

        boolean flag1 = this.isSneaking();

        if (flag1 != this.serverSneakState)
        {
            if (flag1)
            {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING));
            }
            else
            {
                this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING));
            }

            this.serverSneakState = flag1;
        }


        if (this.isCurrentViewEntity())
        {
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            double d0 = this.posX - this.lastReportedPosX;
            double d1 = axisalignedbb.minY - this.lastReportedPosY;
            double d2 = this.posZ - this.lastReportedPosZ;
            double d3 = (double)(this.rotationYaw - this.lastReportedYaw);
            double d4 = (double)(this.rotationPitch - this.lastReportedPitch);
            ++this.positionUpdateTicks;
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean flag3 = d3 != 0.0D || d4 != 0.0D;

            if(teleported){
                teleported = false;
                flag2 = true;
                ByteBuf payload = Unpooled.buffer();
                payload.writeFloat((float) this.posX);
                payload.writeFloat((float) this.posY);
                payload.writeFloat((float) this.posZ);
                byte[] out = new byte[payload.readableBytes()];
                payload.readBytes(out);
                CPacketCustomPayload pack = NetworkHelper.getVivecraftClientPacket(NetworkHelper.PacketDiscriminators.TELEPORT,out);
                this.connection.sendPacket(pack);
            } else{
                if (this.isRiding())
                {
                    this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.motionX, -999.0D, this.motionZ, this.rotationYaw, this.rotationPitch, this.onGround));
                    flag2 = false;
                }
                else if (flag2 && flag3)
                {
                    this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.posX, axisalignedbb.minY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround));
                }
                else if (flag2)
                {
                    this.connection.sendPacket(new CPacketPlayer.Position(this.posX, axisalignedbb.minY, this.posZ, this.onGround));
                }
                else if (flag3)
                {
                    this.connection.sendPacket(new CPacketPlayer.Rotation(this.rotationYaw, this.rotationPitch, this.onGround));
                }
                else if (this.prevOnGround != this.onGround)
                {
                    this.connection.sendPacket(new CPacketPlayer(this.onGround));
                }
            }

            if (flag2)
            {
                this.lastReportedPosX = this.posX;
                this.lastReportedPosY = axisalignedbb.minY;
                this.lastReportedPosZ = this.posZ;
                this.positionUpdateTicks = 0;
            }

            if (flag3)
            {
                this.lastReportedYaw = this.rotationYaw;
                this.lastReportedPitch = this.rotationPitch;
            }

            this.prevOnGround = this.onGround;

            //VIVECRAFT
            ((IMinecraftVR)mc).getSwingTracker().IAmLookingAtMyHand[0] = ((IMinecraftVR)mc).getSwingTracker().shouldIlookatMyHand[0];
            ((IMinecraftVR)mc).getSwingTracker().IAmLookingAtMyHand[1] = ((IMinecraftVR)mc).getSwingTracker().shouldIlookatMyHand[1];
            if(((IMinecraftVR)mc).getVRSettings().walkUpBlocks) mc.gameSettings.autoJump = false;
            //END VIVECRAFT

            this.autoJumpEnabled = this.mc.gameSettings.autoJump;

        }
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V", shift = At.Shift.AFTER))
    private void addOnLivingUpdateVR(CallbackInfo ci) {
        ((IMinecraftVR)mc).getVRPlayer().onLivingUpdate((EntityPlayerSP) (IEntityPlayerSPVR)this, this.mc, this.rand);
    }

    @Overwrite
    public void updateRidden()
    {
        super.updateRidden();
        this.rowingBoat = false;

        if (this.getRidingEntity() instanceof EntityBoat)
        {
            EntityBoat entityboat = (EntityBoat)this.getRidingEntity();
            entityboat.updateInputs(this.movementInput.leftKeyDown, this.movementInput.rightKeyDown, this.movementInput.forwardKeyDown, this.movementInput.backKeyDown);
            this.rowingBoat |=  ((IMinecraftVR)mc).getRowTracker().isRowing()|| this.movementInput.leftKeyDown || this.movementInput.rightKeyDown || this.movementInput.forwardKeyDown || this.movementInput.backKeyDown;
        }
    }

    @Overwrite
    public void move(MoverType type, double x, double y, double z)
    {
        if(x==0 && y==0 && z==0) return;
        if(this.isRiding())return;
        boolean freemove = OpenVRPlayer.get().getFreeMove();
        boolean doY = freemove || (((IMinecraftVR)mc).getVRSettings().simulateFalling && !this.isOnLadder()) && !this.isSneaking();
        if(((IMinecraftVR)mc).getClimbTracker().isActive((EntityPlayerSP) (IEntityPlayerSPVR)this) && (freemove || ((IMinecraftVR)mc).getClimbTracker().isGrabbingLadder())) doY = true;
        Vec3d roomOrigin = OpenVRPlayer.get().roomOrigin;
        //   	Vec3 camloc = Minecraft.getMinecraft().vrPlayer.getHMDPos_World();

        if ( (((IMinecraftVR)mc).getClimbTracker().isGrabbingLadder() || freemove || ((IMinecraftVR)mc).getSwimTracker().isActive((EntityPlayerSP) (IEntityPlayerSPVR)this)) && (this.moveForward != 0 || this.isElytraFlying() || Math.abs(this.motionX) > 0.01 || Math.abs(this.motionZ) > 0.01))
        {
            double ox = roomOrigin.x - posX;
            double oz = roomOrigin.z - posZ;
            double d0 = this.posX;
            double d1 = this.posZ;
            super.move(type,x,y,z);

            if(((IMinecraftVR)mc).getVRSettings().walkUpBlocks)
                this.stepHeight = 1.0f;
            else {
                this.stepHeight = 0.6f;
                this.updateAutoJump((float)(this.posX - d0), (float)(this.posZ - d1));
            }

            double oy = this.posY;
            OpenVRPlayer.get().setRoomOrigin(
                    posX + ox,
                    oy,
                    posZ  + oz, false);
        } else {
            if(doY) {
                super.move(type,0,y,0);
                OpenVRPlayer.get().setRoomOrigin(
                        OpenVRPlayer.get().roomOrigin.x,
                        this.posY,
                        OpenVRPlayer.get().roomOrigin.z, false);

            } else {
                this.onGround = true; //
                //do not move player, VRPlayer.moveplayerinroom will move him around.
            }
        }
    }

    @Overwrite
    protected void updateAutoJump(float p_189810_1_, float p_189810_2_)
    {
        if (this.isAutoJumpEnabled())
        {
            if (this.autoJumpTime <= 0 && this.onGround && !this.isSneaking() && !this.isRiding())
            {
                Vec2f vec2f = this.movementInput.getMoveVector();

                if (vec2f.x != 0.0F || vec2f.y != 0.0F)
                {
                    Vec3d vec3d = new Vec3d(this.posX, this.getEntityBoundingBox().minY, this.posZ);
                    double d0 = this.posX + (double)p_189810_1_;
                    double d1 = this.posZ + (double)p_189810_2_;
                    Vec3d vec3d1 = new Vec3d(d0, this.getEntityBoundingBox().minY, d1);
                    Vec3d vec3d2 = new Vec3d((double)p_189810_1_, 0.0D, (double)p_189810_2_);
                    float f = this.getAIMoveSpeed();
                    float f1 = (float)vec3d2.lengthSquared();

                    //VIVE
                    float yaw = ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().vrdata_world_pre.getBodyYaw();
                    //END VIVE

                    if (f1 <= 0.001F)
                    {
                        float f2 = f * vec2f.x;
                        float f3 = f * vec2f.y;
                        float f4 = MathHelper.sin(yaw * 0.017453292F);
                        float f5 = MathHelper.cos(yaw * 0.017453292F);
                        vec3d2 = new Vec3d((double)(f2 * f5 - f3 * f4), vec3d2.y, (double)(f3 * f5 + f2 * f4));
                        f1 = (float)vec3d2.lengthSquared();

                        if (f1 <= 0.001F)
                        {
                            return;
                        }
                    }

                    float f12 = (float)MathHelper.fastInvSqrt((double)f1);
                    Vec3d vec3d12 = vec3d2.scale((double)f12);
                    Vec3d vec3d13 = this.getForward();
                    float f13 = (float)(vec3d13.x * vec3d12.x + vec3d13.z * vec3d12.z);

                    if (f13 >= -0.15F)
                    {
                        BlockPos blockpos = new BlockPos(this.posX, this.getEntityBoundingBox().maxY, this.posZ);
                        IBlockState iblockstate = this.world.getBlockState(blockpos);

                        if (iblockstate.getCollisionBoundingBox(this.world, blockpos) == null)
                        {
                            blockpos = blockpos.up();
                            IBlockState iblockstate1 = this.world.getBlockState(blockpos);

                            if (iblockstate1.getCollisionBoundingBox(this.world, blockpos) == null)
                            {
                                float f6 = 7.0F;
                                float f7 = 1.2F;

                                if (this.isPotionActive(MobEffects.JUMP_BOOST))
                                {
                                    f7 += (float)(this.getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
                                }

                                float f8 = Math.max(f * 7.0F, 1.0F / f12);
                                Vec3d vec3d4 = vec3d1.add(vec3d12.scale((double)f8));
                                float f9 = this.width;
                                float f10 = this.height;
                                AxisAlignedBB axisalignedbb = (new AxisAlignedBB(vec3d, vec3d4.add(0.0D, (double)f10, 0.0D))).grow((double)f9, 0.0D, (double)f9);
                                Vec3d lvt_19_1_ = vec3d.add(0.0D, 0.5099999904632568D, 0.0D);
                                vec3d4 = vec3d4.add(0.0D, 0.5099999904632568D, 0.0D);
                                Vec3d vec3d5 = vec3d12.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
                                Vec3d vec3d6 = vec3d5.scale((double)(f9 * 0.5F));
                                Vec3d vec3d7 = lvt_19_1_.subtract(vec3d6);
                                Vec3d vec3d8 = vec3d4.subtract(vec3d6);
                                Vec3d vec3d9 = lvt_19_1_.add(vec3d6);
                                Vec3d vec3d10 = vec3d4.add(vec3d6);
                                List<AxisAlignedBB> list = this.world.getCollisionBoxes(this, axisalignedbb);

                                if (!list.isEmpty())
                                {
                                    ;
                                }

                                float f11 = Float.MIN_VALUE;
                                label86:

                                for (AxisAlignedBB axisalignedbb2 : list)
                                {
                                    if (axisalignedbb2.intersects(vec3d7, vec3d8) || axisalignedbb2.intersects(vec3d9, vec3d10))
                                    {
                                        f11 = (float)axisalignedbb2.maxY;
                                        Vec3d vec3d11 = axisalignedbb2.getCenter();
                                        BlockPos blockpos1 = new BlockPos(vec3d11);
                                        int i = 1;

                                        while (true)
                                        {
                                            if ((float)i >= f7)
                                            {
                                                break label86;
                                            }

                                            BlockPos blockpos2 = blockpos1.up(i);
                                            IBlockState iblockstate2 = this.world.getBlockState(blockpos2);
                                            AxisAlignedBB axisalignedbb1;

                                            if ((axisalignedbb1 = iblockstate2.getCollisionBoundingBox(this.world, blockpos2)) != null)
                                            {
                                                f11 = (float)axisalignedbb1.maxY + (float)blockpos2.getY();

                                                if ((double)f11 - this.getEntityBoundingBox().minY > (double)f7)
                                                {
                                                    return;
                                                }
                                            }

                                            if (i > 1)
                                            {
                                                blockpos = blockpos.up();
                                                IBlockState iblockstate3 = this.world.getBlockState(blockpos);

                                                if (iblockstate3.getCollisionBoundingBox(this.world, blockpos) != null)
                                                {
                                                    return;
                                                }
                                            }

                                            ++i;
                                        }
                                    }
                                }

                                if (f11 != Float.MIN_VALUE)
                                {
                                    float f14 = (float)((double)f11 - this.getEntityBoundingBox().minY);

                                    if (f14 > 0.5F && f14 <= f7)
                                    {
                                        this.autoJumpTime = 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Shadow
    public boolean isAutoJumpEnabled()
    {
        return this.autoJumpEnabled;
    }

    @Shadow
    protected boolean isCurrentViewEntity()
    {
        return this.mc.getRenderViewEntity() == this;
    }

    // VIVECREAFT ADDITIONS


    /**
     * Sets the location and Yaw/Pitch of an entity in the world
     */


    /** MINECRIFT **/
    public void doDrag()
    {

        float var3 = 0.91F;

        if (this.onGround)
        {
            var3 = this.world.getBlockState(new BlockPos(this.posX, this.getEntityBoundingBox().minY - 1.0D, this.posZ)).getBlock().slipperiness * 0.91F;
        }

        double xFactor = var3;
        double zFactor = var3;

        //VIVECRAFT account for stock drag code we can't change in EntityLivingBase
        this.motionX /= xFactor;
        this.motionZ /= zFactor;
        //

//    	if(!mc.vrSettings.seated && !this.onGround && !this.isElytraFlying() && !this.hasNoGravity() && mc.jumpTracker.isClimbeyJumpEquipped() && mc.vrSettings.realisticJumpEnabled) return; //no drag for jump boots.
//    	if(mc.climbTracker.isGrabbingLadder()) return; //no drag for climb.

        int inertiaFactor = ((IMinecraftVR)mc).getVRSettings().inertiaFactor;
        double addFactor = VRSettings.getInertiaAddFactor(inertiaFactor);

        double boundedAdditionX = getBoundedAddition(additionX);
        double targetLimitX = (var3 * boundedAdditionX) / (1f - var3);
        double multiFactorX = targetLimitX / (var3 * (targetLimitX + (boundedAdditionX * addFactor)));
        xFactor *= multiFactorX;

        double boundedAdditionZ = getBoundedAddition(additionZ);
        double targetLimitZ = (var3 * boundedAdditionZ) / (1f - var3);
        double multiFactorZ = targetLimitZ / (var3 * (targetLimitZ + (boundedAdditionZ * addFactor)));
        zFactor *= multiFactorZ;


        this.motionX *= xFactor;
        this.motionZ *= zFactor;
    }

    public double getBoundedAddition(double orig)
    {
        if (orig >= -1.0E-6D && orig <= 1.0E-6D) {
            return 1.0E-6D;
        }
        else {
            return orig;
        }
    }

    /**
     * Used in both water and by flying objects
     */
    @Override
    public void moveRelative(float strafe, float up, float forward, float friction)
    {
        //   	super.moveFlying(strafe, forward, friction);;

        OpenVRPlayer vr = ((IMinecraftVR)mc).getVRPlayer();
        if (!vr.getFreeMove()) {return;}

        int inertiaFactor = ((IMinecraftVR)mc).getVRSettings().inertiaFactor;
        float speed = strafe * strafe + forward * forward;

        double mX = 0d;
        double mZ = 0d;
        double mY = 0d;
        double addFactor = 1f;

        //|| Main.katvr
        if (speed >= 1.0E-4F)
        {
            speed = MathHelper.sqrt(speed);

            // || Main.katvr
            if (speed < 1.0F)
            {
                speed = 1.0F;
            }

            speed = friction / speed;
            strafe *= speed;
            forward *= speed;
            Vec3d directionVec = new Vec3d(strafe, 0,forward);
            OpenVRPlayer con = ((IMinecraftVR)mc).getVRPlayer();

            // TODO: figure out what to do about katvr
//            if(Main.katvr){
//                jkatvr.query();
//
//                speed =  jkatvr.getSpeed() * jkatvr.walkDirection() * mc.vrSettings.movementSpeedMultiplier;
//                directionVec = new Vec3d(0, 0,speed);
//                directionVec=directionVec.rotateYaw(-jkatvr.getYaw()* PIOVER180 + mc.vrPlayer.vrdata_world_pre.rotation_radians);
//
//                if(this.capabilities.isFlying || this.inWater){
//                    directionVec=directionVec.rotatePitch(con.vrdata_world_pre.hmd.getPitch()* PIOVER180);
//                }else{
//
//                }
//            } else
            if(((IMinecraftVR)mc).getVRSettings().seated){
                int c = 0;
                if(((IMinecraftVR)mc).getVRSettings().seatedUseHMD) c = 1;
                directionVec=directionVec.rotateYaw(-con.vrdata_world_pre.getController(c).getYaw() * PIOVER180);
            }else{
                if(this.capabilities.isFlying || this.inWater){
                    switch (((IMinecraftVR)mc).getVRSettings().vrFreeMoveMode){
                        case VRSettings.FREEMOVE_CONTROLLER:
                            directionVec = directionVec.rotatePitch(con.vrdata_world_pre.getController(1).getPitch()  * PIOVER180);
                            break;
                        case VRSettings.FREEMOVE_HMD:
                        case VRSettings.FREEMOVE_RUNINPLACE:
                        case VRSettings.FREEMOVE_ROOM:
                            //hmd pitch
                            directionVec = directionVec.rotatePitch(con.vrdata_world_pre.hmd.getPitch()* PIOVER180);
                            break;
                    }
                }

                if(((IMinecraftVR)mc).getJumpTracker().isjumping()){
                    directionVec=directionVec.rotateYaw(-con.vrdata_world_pre.hmd.getYaw() * PIOVER180);
                }else{
                    switch (((IMinecraftVR)mc).getVRSettings().vrFreeMoveMode){
                        case VRSettings.FREEMOVE_CONTROLLER:
                            directionVec = directionVec.rotateYaw(-con.vrdata_world_pre.getController(1).getYaw() * PIOVER180);
                            break;
                        case VRSettings.FREEMOVE_HMD:
                            directionVec = directionVec.rotateYaw(-con.vrdata_world_pre.hmd.getYaw() * PIOVER180);
                            break;
                        case VRSettings.FREEMOVE_RUNINPLACE:
                            directionVec = directionVec.rotateYaw((float) (-((IMinecraftVR)mc).getRunTracker().getYaw() * PIOVER180));
                            directionVec = directionVec.scale(((IMinecraftVR)mc).getRunTracker().getSpeed());
                            break;
                        case VRSettings.FREEMOVE_ROOM:
                            directionVec = directionVec.rotateYaw((180+((IMinecraftVR)mc).getVRSettings().vrWorldRotation) * PIOVER180);
                            break;

                    }
                }
            }


            mX = directionVec.x;
            mY = directionVec.y;
            mZ = directionVec.z;


            // Modify acceleration sequence (each tick)
            if(!this.capabilities.isFlying && !this.inWater) addFactor = VRSettings.getInertiaAddFactor(inertiaFactor);

            float yAdd = 1f;
            if(this.capabilities.isFlying) yAdd = 5f; //HACK

            this.motionX = this.motionX + (mX * addFactor);
            this.motionZ = this.motionZ + (mZ * addFactor);
            this.motionY = this.motionY + (mY * yAdd);

            this.additionX = mX;
            this.additionZ = mZ;
        }

        //if (this instanceof EntityPlayerSP) {
        //    System.out.println(String.format("FLYING: %B, forward: %.4f, strafe: %.4f, pitch: %.4f, yaw: %.4f, mx: %.4f, mz: %.4f, my: %.4f", allowYAdjust, forward, strafe, this.headPitch, this.rotationYaw, mX, mZ, mY));
        //}


        if(!this.capabilities.isFlying && !this.inWater) doDrag();       //TODO put back intertia while flying.. doesnt work for some reason.

        /** END MINECRIFT **/

    }

    public float eyeHeightOverride = 0;
    public boolean overrideEyeHeight;

    @Override
    public float getEyeHeight(){
        if(overrideEyeHeight) return eyeHeightOverride;
        return super.getEyeHeight();

    }

    private boolean isThePlayer(){
        return (EntityPlayerSP) (IEntityPlayerSPVR) this == Minecraft.getMinecraft().player;
    }

    @Override
    protected void updateItemUse(ItemStack stack, int eatingParticleCount)
    {
        if(!isThePlayer()){
            super.updateItemUse(stack, eatingParticleCount);;
        } else {
            if (!stack.isEmpty() && this.isHandActive())
            {
                if (stack.getItemUseAction() == EnumAction.DRINK)
                {
                    this.playSound(SoundEvents.ENTITY_GENERIC_DRINK, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
                }

                if (stack.getItemUseAction() == EnumAction.EAT)
                {
                    for (int i = 0; i < eatingParticleCount; ++i)
                    {
                        Vec3d vec3d = new Vec3d(((double)this.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
                        vec3d = vec3d.rotatePitch(-this.rotationPitch * 0.017453292F);
                        vec3d = vec3d.rotateYaw(-this.rotationYaw * 0.017453292F);
                        double d0 = (double)(-this.rand.nextFloat()) * 0.6D - 0.3D;
                        Vec3d vec3d1 = new Vec3d(((double)this.rand.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
                        vec3d1 = vec3d1.rotatePitch(-this.rotationPitch * 0.017453292F);
                        vec3d1 = vec3d1.rotateYaw(-this.rotationYaw * 0.017453292F);

                        vec3d1 = vec3d1.add(this.posX, this.posY + (double)this.getEyeHeight(), this.posZ);

                        //VIVE
                        EnumHand hand = getActiveHand();
                        if(hand == EnumHand.MAIN_HAND){
                            vec3d1 = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_pre.getController(0).getPosition();
                        } else {
                            vec3d1 = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_pre.getController(1).getPosition();
                        }
                        //

                        if (stack.getHasSubtypes())
                        {
                            this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, vec3d1.x, vec3d1.y, vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z, new int[] {Item.getIdFromItem(stack.getItem()), stack.getMetadata()});
                        }
                        else
                        {
                            this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, vec3d1.x, vec3d1.y, vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z, new int[] {Item.getIdFromItem(stack.getItem())});
                        }
                    }

                    this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.5F + 0.5F * (float)this.rand.nextInt(2), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
                }
            }
        }
    }

    public boolean isClimbeyJumpEquipped(){

        if(this.getItemStackFromSlot(EntityEquipmentSlot.FEET) != null){
            if(((IMinecraftVR)mc).getJumpTracker().isBoots(this.getItemStackFromSlot(EntityEquipmentSlot.FEET)))
                return true;
        }

        return false;

    }

    public boolean isClimbeyClimbEquipped(){

        if(this.getHeldItemMainhand() != null){
            if(((IMinecraftVR)mc).getClimbTracker().isClaws(this.getHeldItemMainhand()))
                return true;
        }

        if(this.getHeldItemOffhand() != null){
            if(((IMinecraftVR)mc).getClimbTracker().isClaws(this.getHeldItemOffhand()))
                return true;
        }

        return false;
    }

    /**
     * Called when the mob's health reaches 0.
     */
    @Override
    public void onDeath(DamageSource p_70645_1_){

        super.onDeath(p_70645_1_);
        MCOpenVR.triggerHapticPulse(0, 2000);
        MCOpenVR.triggerHapticPulse(1, 2000);

    }

    public void stepSound(BlockPos blockforNoise, Vec3d soundPos){
        Block b = this.world.getBlockState(blockforNoise).getBlock();
        SoundType soundtype = b.getSoundType();

        if (this.world.getBlockState(blockforNoise.up()).getBlock() == Blocks.SNOW_LAYER)
        {
            soundtype = Blocks.SNOW_LAYER.getSoundType();
        }

        float volume = soundtype.getVolume();
        float pitch = soundtype.getPitch();
        SoundEvent soundIn = soundtype.getStepSound();

        if (!this.isSilent() && !b.getDefaultState().getMaterial().isLiquid())
        {
            this.world.playSound((EntityPlayer)null, soundPos.x, soundPos.y, soundPos.z, soundIn, this.getSoundCategory(), volume, pitch);
        }
    }
}
