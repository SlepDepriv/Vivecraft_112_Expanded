package org.vivecraft.mixin.minecraft.vr_user.client.renderer;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.main.Main;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.Project;
import org.lwjgl.util.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.control.ControllerType;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.gameplay.trackers.BowTracker;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.entity.IEntityPlayerSPVR;
import org.vivecraft.intermediaries.interfaces.client.gui.IGUIIngameVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IItemRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IRenderGlobalVR;
import org.vivecraft.intermediaries.replacements.VRDefaultVertexFormats;
import org.vivecraft.intermediaries.replacements.VRFrameBuffer;
import org.vivecraft.intermediaries.replacements.VRGLStateManagerColor;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.render.RenderPass;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.MCReflection;
import org.vivecraft.utils.Utils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Mixin(EntityRenderer.class)
public class MixinEntityRendererVR implements IEntityRendererVR {

    /** MINECRIFT */
    private static float MAX_CROSSHAIR_DISTANCE = 64f;
    public int renderpass = 0;

    private boolean eyeCollision = false;
    Block eyeCollisionBlock;
    public float clipDistance = 0f;
    public float minClipDistance = 0.05f;
    public Vec3d crossVec;
    private FloatBuffer matrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    public org.lwjgl.util.vector.Matrix4f thirdPassProjectionMatrix = new org.lwjgl.util.vector.Matrix4f();

    public boolean menuWorldFastTime;

    /** END MINECRIFT */

    // The whole will be cast into eternal night... Or atleast that's what going to happen with all these shadows about
    @Shadow
    private Minecraft mc;
    @Shadow
    private Entity pointedEntity;
    @Shadow
    private float thirdPersonDistancePrev;
    @Shadow
    private boolean cloudFog;
    @Shadow
    private int rendererUpdateCount;
    @Shadow
    private int debugViewDirection;
    @Shadow
    public ItemRenderer itemRenderer;
    @Shadow
    private MapItemRenderer mapItemRenderer;
    @Shadow
    private long renderEndNanoTime;
    @Shadow
    private long timeWorldIcon;
    @Shadow
    private boolean debugView;
    @Shadow
    public int frameCount;
    @Shadow
    public static boolean anaglyphEnable;
    @Shadow
    private ShaderGroup shaderGroup;
    @Shadow
    private boolean useShader;
    @Shadow
    private float farPlaneDistance;
    @Shadow
    private FloatBuffer fogColorBuffer;
    @Shadow
    public float fogColorRed;
    @Shadow
    public float fogColorGreen;
    @Shadow
    public float fogColorBlue;
    @Shadow
    private float fogColor2;
    @Shadow
    private float fogColor1;
    @Shadow
    private float bossColorModifier;
    @Shadow
    private float bossColorModifierPrev;

    // Method stuffs


    @Override
    public Vec3d getCrossVec() {
        return crossVec;
    }

    @Override
    public void setCrossVec(Vec3d vec) {
        this.crossVec = vec;
    }

    @Override
    public float getClipDistance() {
        return clipDistance;
    }

    @Override
    public float getMinClipDistance() {
        return minClipDistance;
    }

    @Override
    public boolean getInWater() {
        return inwater;
    }

    @Override
    public boolean getWasinwater() {
        return wasinwater;
    }

    @Override
    public void setWasInwater(boolean bool) {
        wasinwater = bool;
    }

    @Override
    public void setInPortal(boolean bool) {
        inportal = bool;
    }

    @Override
    public void setInBlock(boolean bool) {
        inblock = bool;
    }

    @Override
    public void setOnFire(boolean bool) {
        onfire = bool;
    }

    @Override
    public boolean getInPortal() {
        return inportal;
    }

    @Override
    public boolean getInBlock() {
        return inblock;
    }

    @Override
    public boolean getOnFire() {
        return onfire;
    }

    @Override
    public float getInBlockFloat() {
        return inBlock;
    }

    @Override
    public Matrix4f getThirdPassProjectionMatrix() {
        return thirdPassProjectionMatrix;
    }

    @Override
    public boolean getMenuWorldFastTime() {
        return menuWorldFastTime;
    }

    @Override
    public void setInWater(boolean bool) {
        inwater = bool;
    }

    @Override
    public void setInBlockFloat(float num) {
        inBlock = num;
    }

    @Override
    public double getRveY() {
        return rveY;
    }

    @Override
    public void setMenuWorldFastTime(boolean bool) {
        menuWorldFastTime = bool;
    }

    //

    public boolean inwater, wasinwater, inportal, inblock, onfire;
    public float inBlock = 0;

    Frustum currentFrustum = new Frustum();

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private static void addLoginToInit(Minecraft mcIn, IResourceManager resourceManagerIn, CallbackInfo cir) {
        if(Minecraft.getMinecraft().entityRenderer!=null){
            System.out.println("**********NEW ENTITY RENDERER ***********");
            java.lang.Thread.dumpStack();
        }
    }

    // This isn't used, but I think it's funny so it stays
    public boolean hahahaCompilerYoureNotThisSmart = true;

    /**
     * Gets the block or object that is being moused over.
     */
    public void getMouseOverVR(float partialTicks)
    { //In vanilla this method is called during render AND during tick. It won't work on tick atm.
        Entity entity = this.mc.getRenderViewEntity();

        IMinecraftVR mcVR = (IMinecraftVR) mc;

        //THIS IS DUMB AND WE SHOULD JUST GO BACK TO GETPOINTEDBLOCK
        if (entity != null && this.mc.world != null && mcVR.getVRPlayer().vrdata_world_render != null)
        // Vivecraft this method is only usable during render after the RVE has been overridden.
        {
            this.mc.profiler.startSection("pick");
            this.mc.pointedEntity = null;
            double d0 = (double)this.mc.playerController.getBlockReachDistance();

            //Vivecraft override raytrace
            this.mc.objectMouseOver = mcVR.getVRPlayer().rayTraceVR(mcVR.getVRPlayer().vrdata_world_render, 0 ,d0);
            this.crossVec = mcVR.getVRPlayer().AimedPointAtDistance(mcVR.getVRPlayer().vrdata_world_render, 0, d0);
            Vec3d vec3d = mcVR.getVRPlayer().vrdata_world_render.getController(0).getPosition(); // entity.getEyePosition(partialTicks);
            //TODO: Test how to handle vec3d for best server-side compatibility

            boolean flag = false;
            int i = 3;
            double d1 = d0;

            if (this.mc.playerController.extendedReach())
            {
                d1 = 6.0D;
                d0 = d1;
            }
            else if (d0 > 3.0D)
            {
                flag = true;
            }

            if (this.mc.objectMouseOver != null)
            {
                d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3d);
            }
            //Vivecraft
            Vec3d vec3d1 =  mcVR.getVRPlayer().vrdata_world_render.getController(0).getDirection(); // entity.getLook(1.0F);
            //
            Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
            this.pointedEntity = null;
            Vec3d vec3d3 = null;
            float f = 1.0F;
            List<Entity> list = this.mc.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>()
            {
                public boolean apply(@Nullable Entity p_apply_1_)
                {
                    return p_apply_1_ != null && p_apply_1_.canBeCollidedWith();
                }
            }));
            double d2 = d1;

            for (int j = 0; j < list.size(); ++j)
            {
                Entity entity1 = list.get(j);
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow((double)entity1.getCollisionBorderSize());
                RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                if (axisalignedbb.contains(vec3d))
                {
                    if (d2 >= 0.0D)
                    {
                        this.pointedEntity = entity1;
                        vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                        d2 = 0.0D;
                    }
                }
                else if (raytraceresult != null)
                {
                    double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                    if (d3 < d2 || d2 == 0.0D)
                    {
                        boolean flag1 = entity1.canRiderInteract();

                        if (!flag1 && entity1.getLowestRidingEntity() == entity.getLowestRidingEntity())
                        {
                            if (d2 == 0.0D)
                            {
                                this.pointedEntity = entity1;
                                vec3d3 = raytraceresult.hitVec;
                            }
                        }
                        else
                        {
                            this.pointedEntity = entity1;
                            vec3d3 = raytraceresult.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (this.pointedEntity != null && flag && vec3d.distanceTo(vec3d3) > 3.0D)
            {
                this.pointedEntity = null;
                this.mc.objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, (EnumFacing)null, new BlockPos(vec3d3));
            }

            if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null))
            {
                this.mc.objectMouseOver = new RayTraceResult(this.pointedEntity, vec3d3);

                if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame)
                {
                    this.mc.pointedEntity = this.pointedEntity;
                }
            }

            this.mc.profiler.endSection();
        }
    }

    @Inject(method = "getFOVModifier", at = @At("HEAD"), cancellable = true)
    private void getFOVModifierMixin(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        // Vivecraft: using this on the main menu
        if (mc.world == null)
            cir.setReturnValue(this.mc.gameSettings.fovSetting);
    }

    @Shadow
    private float getFOVModifier(float partialTicks, boolean useFOVSetting)
    {
        return 0;
    }

    /**
     * @reason Vivecraft completely replaces this method's code, so I this overwrite is completely reasonable unlike my last ones
     * @author SlepDepriv
     * @param partialTicks
     */
    @Overwrite
    private void orientCamera(float partialTicks) {
        Entity entity = this.mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks;// + (double)f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;

        //        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPlayerSleeping())
        //        {
        //            f = (float)((double)f + 1.0D);
        //            GlStateManager.translate(0.0F, 0.3F, 0.0F);
        //
        //            if (!this.mc.gameSettings.debugCamEnable)
        //            {
        //                BlockPos blockpos = new BlockPos(entity);
        //                IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
        //                Block block = iblockstate.getBlock();
        //
        //                if (Reflector.ForgeHooksClient_orientBedCamera.exists())
        //                {
        //                    Reflector.callVoid(Reflector.ForgeHooksClient_orientBedCamera, new Object[] {this.mc.world, blockpos, iblockstate, entity});
        //                }
        //                else if (block == Blocks.BED)
        //                {
        //                    int j = ((EnumFacing)iblockstate.getValue(BlockBed.FACING)).getHorizontalIndex();
        //                    GlStateManager.rotate((float)(j * 90), 0.0F, 1.0F, 0.0F);
        //                }
        //
        //                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
        //                GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
        //            }
        //       }

        if (this.mc.gameSettings.thirdPersonView > 0)
        {
            double d3 = (double)(this.thirdPersonDistancePrev + (4.0F - this.thirdPersonDistancePrev) * partialTicks);

            if (this.mc.gameSettings.debugCamEnable)
            {
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
            }
            else
            {
                float f1 = rveyaw;// entity.rotationYaw;
                float f2 = rvepitch;// entity.rotationPitch;

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    f2 += 180.0F;
                }

                double d4 = (double)(-MathHelper.sin(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d5 = (double)(MathHelper.cos(f1 * 0.017453292F) * MathHelper.cos(f2 * 0.017453292F)) * d3;
                double d6 = (double)(-MathHelper.sin(f2 * 0.017453292F)) * d3;

                for (int i = 0; i < 8; ++i)
                {
                    float f3 = (float)((i & 1) * 2 - 1);
                    float f4 = (float)((i >> 1 & 1) * 2 - 1);
                    float f5 = (float)((i >> 2 & 1) * 2 - 1);
                    f3 = f3 * 0.1F;
                    f4 = f4 * 0.1F;
                    f5 = f5 * 0.1F;

                    Vec3d pos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition();

                    RayTraceResult raytraceresult = this.mc.world.rayTraceBlocks(
                            new Vec3d(pos.x + (double)f3, pos.y + (double)f4, pos.z + (double)f5),
                            new Vec3d(pos.x - d4 + (double)f3 + (double)f5, pos.y - d6 + (double)f4, pos.z - d5 + (double)f5));

                    if (raytraceresult != null)
                    {
                        double d7 = raytraceresult.hitVec.distanceTo(new Vec3d(d0, d1, d2));

                        if (d7 < d3)
                        {
                            d3 = d7;
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2)
                {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(rvepitch - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(rveyaw - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
                GlStateManager.rotate(f1 - rveyaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - rvepitch, 1.0F, 0.0F, 0.0F);

                GlStateManager.translate(0.0F, -f, 0.0F);

            }
        }
        else if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD){
            //mc.printGLMatrix("er");
            applyMRCameraRotation(false);
        } else 	{
            /** MINECRIFT */

            // do proper 1st person camera orientation.

            GlStateManager.multMatrix(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getMatrix().toFloatBuffer());

            //dont do depth here. Do later. cause reasons.
        }

        this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);

        //Forge raise the event but then totally ignore what it returns.
        IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
        net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event = new net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup((EntityRenderer)(IEntityRendererVR) this, entity, state, partialTicks, ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getYaw(), ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getPitch(), 0);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }

    //do tis separte to hide it from shadersmod shadow pass, also do the sky.
    public void applyCameraDepth(boolean reverse){
        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD) return; //FUCK YOU stupid thing.
        // Position
        //backwards in 1.10
        Vec3d campos =  ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition().subtract(mc.getRenderViewEntity().getPositionVector());
        float x = (float) (campos.x );
        float y = (float) (campos.y);
        float z = (float) (campos.z );
        int i = 1;
        if(reverse) i = -1;
        //This is just for depth.
        GlStateManager.translate(-x*i, -y*i, -z*i);
    }

    @Overwrite
    public void setupCameraTransform(float partialTicks, int pass){
        this.setupCameraTransform(partialTicks, pass, false);
    }

    public void setupCameraTransform(float partialTicks, int pass, boolean isClouds) {
        /** MINECRIFT */

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();

        float var3 = 0.07F;

        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT || ((IMinecraftVR)mc).getCurrentPass() == RenderPass.RIGHT)
        {
            int i = ((IMinecraftVR)mc).getCurrentPass().ordinal();
            if(i>1) i = 0;
            if(isClouds){
                GlStateManager.multMatrix(((IMinecraftVR)mc).getStereoProvider().cloudeyeproj[i]);
            }else{
                GlStateManager.multMatrix(((IMinecraftVR)mc).getStereoProvider().eyeproj[i]);
            }
        }
        else
        {
            float clip = isClouds ? clipDistance * 4 : clipDistance;
            if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD) {
                Project.gluPerspective(((IMinecraftVR)mc).getVRSettings().mixedRealityFov, ((IMinecraftVR)mc).getVRSettings().mixedRealityAspectRatio, minClipDistance, clip);

                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
                matrixBuffer.rewind();
                this.thirdPassProjectionMatrix.load(matrixBuffer); //save it.
                matrixBuffer.rewind();

            } else {
                Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float) this.mc.displayHeight, minClipDistance, clip);
            }
        }

        float var4;

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        var4 = this.mc.player.prevTimeInPortal + (this.mc.player.timeInPortal - this.mc.player.prevTimeInPortal) * partialTicks;

        if (var4 > 0.0F )
        {
            byte var7 = 20;

            if (this.mc.player.isPotionActive(MobEffects.NAUSEA))
            {
                var7 = 7;
            }

            float var6 = 5.0F / (var4 * var4 + 5.0F) - var4 * 0.04F;
            var6 *= var6;

            //Vivecraft tone that shit down
            var7 = (byte) (var7 / 5);
            var6 = 1.1f;
            //

            GL11.glRotatef(((float)this.rendererUpdateCount + partialTicks) * (float)var7, 0.0F, 1.0F, 1.0F);
            GL11.glScalef(1.0F / var6, 1.0F, 1.0F);
            GL11.glRotatef(-((float)this.rendererUpdateCount + partialTicks) * (float)var7, 0.0F, 1.0F, 1.0F);
        }

        this.orientCamera(partialTicks);

        if (this.debugViewDirection > 0)
        {
            int var71 = this.debugViewDirection - 1;

            if (var71 == 1)
            {
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var71 == 2)
            {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var71 == 3)
            {
                GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (var71 == 4)
            {
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (var71 == 5)
            {
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            }
        }
        /** END MINECRIFT */
    }

    @Overwrite
    public void renderHand(float partialTicks, int pass) {
//        if(shadersMod){
//            Shaders.beginHand(false);
//        }else {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
//        }

        //from player face to HMD
        setupCameraTransform(partialTicks, pass);
        applyCameraDepth(false);
        if (this.mc.gameSettings.thirdPersonView == 0)
        {

            // VIVE START - from HMD to controller
            SetupRenderingAtController(0);

            ItemStack item = mc.player.getHeldItemMainhand();

            if(((IMinecraftVR)mc).getClimbTracker().isClimbeyClimb() && (item.getItem() != Items.SHEARS)){
                itemRenderer.renderItemInFirstPerson(mc.player, partialTicks, 0, EnumHand.MAIN_HAND, mc.player.getSwingProgress(partialTicks), mc.player.getHeldItemOffhand(), 0);
            }

            if(BowTracker.isHoldingBow(mc.player, EnumHand.MAIN_HAND)){
                //do ammo override
                int c = 0;
                if (((IMinecraftVR)mc).getVRSettings().vrReverseShootingEye) c = 1;
                ItemStack ammo = ((IMinecraftVR)mc).getBowTracker().findAmmoItemStack(mc.player);
                if (ammo !=null  && !((IMinecraftVR)mc).getBowTracker().isNotched()) { //render the arrow in right, left hand will check for and render bow.
                    itemRenderer.renderItemInFirstPerson(mc.player, partialTicks, 0, EnumHand.MAIN_HAND, mc.player.getSwingProgress(partialTicks), ammo, 0);
                } else {
                    itemRenderer.renderItemInFirstPerson(mc.player, partialTicks, 0, EnumHand.MAIN_HAND, mc.player.getSwingProgress(partialTicks), ItemStack.EMPTY, 0);
                }
            }
            else if(BowTracker.isHoldingBow(mc.player, EnumHand.OFF_HAND) && ((IMinecraftVR)mc).getBowTracker().isNotched()){
                int c = 0;
                if (((IMinecraftVR)mc).getVRSettings().vrReverseShootingEye) c = 1;
                itemRenderer.renderItemInFirstPerson(mc.player, partialTicks, 0, EnumHand.MAIN_HAND, mc.player.getSwingProgress(partialTicks), ItemStack.EMPTY, 0);
            }else {
                itemRenderer.renderItemInFirstPerson(mc.player, partialTicks, 0, EnumHand.MAIN_HAND, mc.player.getSwingProgress(partialTicks), item, 0);
            }
        }


//        if(shadersMod)
//            Shaders.endHand();
//        else {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
//        }

    }

    @Overwrite
    public void updateCameraAndRender(float partialTicks, long nanoTime) {// called each eye

        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT) //contains all the stuff that was in this method that only needs to be called once per frame.
            updateCameraAndRender_OnePass(partialTicks);

        if (!this.mc.skipRenderWorld)
        {//RENDER WORLD
            /** MINECRIFT */
            anaglyphEnable = false;
            this.mc.gameSettings.anaglyph = false; //just no.
            /** END MINECRIFT */

            this.timeWorldIcon = Minecraft.getSystemTime();

            if (!isInMenuRoom())
            {
                this.mc.profiler.startSection("renderWorld");
                this.renderWorld(partialTicks, Long.MAX_VALUE);
                this.mc.profiler.endSection();

                if (OpenGlHelper.shadersSupported)
                {
                    this.mc.renderGlobal.renderEntityOutlineFramebuffer();

                    if (this.shaderGroup != null && this.useShader)
                    {
                        GlStateManager.matrixMode(5890);
                        GlStateManager.pushMatrix();
                        GlStateManager.loadIdentity();
                        this.shaderGroup.render(partialTicks);
                        /** MINECRIFT */
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        /** END MINECRIFT */

                    }

                    this.mc.getFramebuffer().bindFramebuffer(true);
                }

                this.renderEndNanoTime = System.nanoTime();

                /** MINECRIFT */

                if (this.mc.currentScreen != null)
                {
                    // this.setupOverlayRendering();
                    //	this.renderItemActivation(640,480, partialTicks);

                    //						GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
                    //						boolean var12 = this.mc.gameSettings.fancyGraphics;
                    //
                    //						if (!Config.isVignetteEnabled())
                    //						{
                    //							this.mc.gameSettings.fancyGraphics = false;
                    //						}
                    //
                    //						//why was this called twice per loop??
                    //					//	this.mc.ingameGUI.renderGameOverlay(par1, this.mc.currentScreen != null, var161, var171);
                    //
                    //						this.mc.gameSettings.fancyGraphics = var12;

                }
            } else {
                // Forge: Fix MC-112292
                TileEntityRendererDispatcher.instance.renderEngine = this.mc.getTextureManager();
                // Forge: also fix rendering text before entering world (not part of MC-112292, but the same reason)
                MCReflection.TileEntityRendererDispatcher_fontRenderer.set(TileEntityRendererDispatcher.instance, this.mc.fontRenderer); //TileEntityRendererDispatcher.instance.fontRenderer = this.mc.fontRendererObj;
            }
            /** END MINECRIFT */
        }//END RENDER WORLD


        renderFadeEffects();

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();

        if (isInMenuRoom()) {
            this.mc.profiler.startSection("renderGui");
            GL11.glDisable(GL11.GL_STENCIL_TEST);

            renderGuiLayer(partialTicks);
            if(KeyboardHandler.Showing) {
                if (((IMinecraftVR)mc).getVRSettings().physicalKeyboard)
                    renderPhysicalKeyboard(partialTicks);
                else
                    render2D(partialTicks, KeyboardHandler.Framebuffer, KeyboardHandler.Pos_room, KeyboardHandler.Rotation_room);
            }

            if( ((IMinecraftVR)mc).getCurrentPass() != RenderPass.THIRD || ((IMinecraftVR)mc).getVRSettings().mixedRealityRenderHands){
                // VIVE START - render controllers in main menu
                this.mc.profiler.startSection("mainMenuHands");
                renderMainMenuHands(partialTicks);
                this.mc.profiler.endSection();
            }

            this.mc.profiler.endSection();
        }


        // Minecrift - handle notification text
        handleNotificationText();

        this.renderEndNanoTime = System.nanoTime();

    }

    @Shadow
    public boolean isDrawBlockOutline() {
        return false;
    }

    @Inject(method = "isDrawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void isDrawBlockOutlineMixinTop(CallbackInfoReturnable<Boolean> cir)
    {
        if ((mc.gameSettings.hideGUI && ((IMinecraftVR)mc).getVRSettings().renderBlockOutlineMode == VRSettings.RENDER_BLOCK_OUTLINE_MODE_HUD) || ((IMinecraftVR)mc).getVRSettings().renderBlockOutlineMode == VRSettings.RENDER_BLOCK_OUTLINE_MODE_NEVER)  //VIVE EDIT
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isDrawBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;"), cancellable = true)
    private void isDrawBlockOutlineMixinBeforeEntity(CallbackInfoReturnable<Boolean> cir)
    {
        if (((IMinecraftVR)mc).getTeleportTracker().isAiming())
            cir.setReturnValue(false);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    public void renderWorldMixinTop(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        this.renderWorldPass(2, partialTicks, finishTimeNano);
        if (true) ci.cancel(); // yeah no
    }

    @Shadow
    public void renderWorld(float partialTicks, long finishTimeNano) {

    }


    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;getMouseOver(F)V"))
    public void renderWorldMixinMouse(EntityRenderer renderer, float partialTicks) {
        this.getMouseOverVR(partialTicks);
    }

    @Overwrite
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano)
    {
        pass = 2;//what... oh right, red/blue stereo

        /** MINECRIFT */

        cacheRVEPos((EntityLivingBase) mc.getRenderViewEntity());

        setupRVE();

        this.mc.profiler.startSection("lightTex");

        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT) {	//i think this only needs to be done once.
            this.updateLightmap(partialTicks);

            this.getMouseOverVR(partialTicks);
        }

//		this.mc.mcProfiler.endStartSection("getPointedBlock");
//
//			if( this.mc.currentScreen == null )
//			{
//				if(mc.currentPass == RenderPass.LEFT)
//					getPointedBlock(partialTicks);
//
//					// Set up crosshair position
//					float SLIGHTLY_CLOSER = -0.1f;
//					Vec3d eye = mc.vrPlayer.vrdata_world_render.getEye(mc.currentPass).getPosition();
//					Vec3d pos = mc.vrPlayer.vrdata_world_render.hmd.getPosition();
//					Vec3d centerEyePosToCrossDirection = eye.subtract(crossVec).normalize();   // VIVE use camerapos
//					crossX = (float)(crossVec.x - (centerEyePosToCrossDirection.x*SLIGHTLY_CLOSER) -  pos.x);
//					crossY = (float)(crossVec.y - (centerEyePosToCrossDirection.y*SLIGHTLY_CLOSER) -  pos.y);
//					crossZ = (float)(crossVec.z - (centerEyePosToCrossDirection.z*SLIGHTLY_CLOSER) -  pos.z);
//				// information for the entire frame, not individual eye pos camRelX, Y, Z
//			}

//        boolean shadersMod = Config.isShaders();

//        if (shadersMod)
//        {
//            this.mc.profiler.endStartSection("shadersModBeginRender");
//            Shaders.beginRender(this.mc, partialTicks, finishTimeNano);
//        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);

//        if (shadersMod)
//        {
//            Shaders.beginRenderPass(pass, partialTicks, finishTimeNano);
//        }

        RenderGlobal renderglobal = this.mc.renderGlobal;
        ParticleManager particlemanager = this.mc.effectRenderer;
        GlStateManager.enableCull();

        this.mc.profiler.endStartSection("clear");

//        if (shadersMod)
//        {
//            Shaders.setViewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
//        }
//        else
//        {
            GlStateManager.viewport(0, 0, this.mc.getFramebuffer().framebufferWidth, this.mc.getFramebuffer().framebufferHeight);
//        }

        this.updateFogColor(partialTicks);

        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

//        if (shadersMod)
//        {
//            Shaders.clearRenderBuffer();
//        }

        this.mc.profiler.endStartSection("stencil");
        if(((IMinecraftVR)mc).getCurrentPass() != RenderPass.THIRD && ((IMinecraftVR)mc).getCurrentPass() != RenderPass.CENTER && ((IMinecraftVR)mc).getVRSettings().vrUseStencil && MCOpenVR.isHMDTracking()){
//            if(shadersMod){
//                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, Shaders.dfb);
//                Shaders.useProgram(Shaders.ProgramNone);
//            }
            ((IMinecraftVR)mc).getStereoProvider().doStencilForEye(((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT? 0 : 1); //TODO: dont render this every damn frame.
        }
        else{
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }

        this.mc.profiler.endStartSection("camera");

        this.setupCameraTransform(partialTicks, pass);
        applyCameraDepth(false); //neat

//        if (shadersMod)
//        {
//            Shaders.setCamera(partialTicks);
//        }

//			/** MINECRIFT */ // Save our projection and modelview matrices ---- why...
//			GL11.glMatrixMode(GL11.GL_PROJECTION);
//			GL11.glPushMatrix();
//			GL11.glMatrixMode(GL11.GL_MODELVIEW);
//			GL11.glPushMatrix();
//			/** END MINECRIFT */


//        if (Reflector.ActiveRenderInfo_updateRenderInfo2.exists())
//        {
//            Reflector.call(Reflector.ActiveRenderInfo_updateRenderInfo2, this.mc.getRenderViewEntity(), this.mc.gameSettings.thirdPersonView == 2);
//        }
//        else
//        {
            ActiveRenderInfo.updateRenderInfo(this.mc.player, this.mc.gameSettings.thirdPersonView == 2);
//        }

        //TODO: more with this ^^

        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();

        this.mc.profiler.endStartSection("frustum");
        //	ClippingHelperImpl.getInstance();// redundant
        this.mc.profiler.endStartSection("culling");
//        ClippingHelperImpl.getInstance().disabled = shadersMod && !Shaders.isFrustumCulling();
        ICamera icamera = new Frustum();
        Entity entity = this.mc.getRenderViewEntity();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;

//        if (shadersMod)
//        {
//            ShadersRender.setFrustrumPosition(icamera, d0, d1, d2);
//        }
//        else
//        {
            icamera.setPosition(d0, d1, d2);
//        }

        this.mc.profiler.endStartSection("sky");

//        if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass)
//        {///uuuh what... are...u...doing....
//            this.setupFog(-1, partialTicks);
//            GlStateManager.matrixMode(GL11.GL_PROJECTION);
//            GlStateManager.pushMatrix();
////	            GlStateManager.matrixMode(5889);
////	            GlStateManager.loadIdentity();
////	            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
//            GlStateManager.matrixMode(5888);
//
//            if (shadersMod)
//            {
//                Shaders.beginSky();
//            }
//
//            renderglobal.renderSky(partialTicks, pass);
//
//            if (shadersMod)
//            {
//                Shaders.endSky();
//            }
//
//            GlStateManager.matrixMode(GL11.GL_PROJECTION);
//            GlStateManager.popMatrix();
//
////	            GlStateManager.matrixMode(5889);
////	            GlStateManager.loadIdentity();
////	            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
//            GlStateManager.matrixMode(5888);
//
//
//        }
//        else
//        {
            GlStateManager.disableBlend();
//        }

        this.setupFog(0, partialTicks);

        GlStateManager.shadeModel(7425);

        //VIVE NO EYE HEIGHT
        if (entity.posY < 128.0D + (double)(this.mc.gameSettings.clouds * 128.0F))
        {
            this.renderCloudsCheck(renderglobal, partialTicks, pass, d0, d1, d2);
        }

        GlStateManager.enableCull();
        OpenGlHelper.glBlendFunc(GlStateManager.SourceFactor.SRC_ALPHA.factor, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.factor, GlStateManager.SourceFactor.ONE.factor, GlStateManager.DestFactor.ZERO.factor);

        this.mc.profiler.endStartSection("prepareterrain");
        this.setupFog(0, partialTicks);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        this.mc.profiler.endStartSection("terrain_setup");
//        this.checkLoadVisibleChunks(entity, partialTicks, icamera, this.mc.player.isSpectator());

//        if (shadersMod)
//        {
//            ShadersRender.setupTerrain(renderglobal, entity, (double)partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());
//        }
//        else
//        {
            renderglobal.setupTerrain(entity, (double)partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());
//        }

        //      if (mc.currentPass == RenderPass.Left)
        {
            this.mc.profiler.endStartSection("updatechunks");
//            Lagometer.timerChunkUpload.start();
            this.mc.renderGlobal.updateChunks(finishTimeNano);
//            Lagometer.timerChunkUpload.end();
        }

        this.mc.profiler.endStartSection("terrain");
//        Lagometer.timerTerrain.start();

	        /*if (this.mc.gameSettings.ofSmoothFps && pass > 0)
        {
            this.mc.mcProfiler.endStartSection("finish");
	            GL11.glFinish(); //do not turn this on.
            this.mc.mcProfiler.endStartSection("terrain");
	        }*/

        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();

//        if (shadersMod)
//        {
//            ShadersRender.beginTerrainSolid();
//        }

        renderglobal.renderBlockLayer(BlockRenderLayer.SOLID, (double)partialTicks, pass, entity);
        GlStateManager.enableAlpha();

//        if (shadersMod)
//        {
//            ShadersRender.beginTerrainCutoutMipped();
//        }

        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, this.mc.gameSettings.mipmapLevels > 0);
        renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, (double)partialTicks, pass, entity);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);

//        if (shadersMod)
//        {
//            ShadersRender.beginTerrainCutout();
//        }

        renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT, (double)partialTicks, pass, entity);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

//        if (shadersMod)
//        {
//            ShadersRender.endTerrain();
//        }

//        Lagometer.timerTerrain.end();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);

        this.mc.profiler.endStartSection("entities");

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        RenderHelper.enableStandardItemLighting();

//        if (Reflector.ForgeHooksClient_setRenderPass.exists())
//        {
//            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, Integer.valueOf(0));
//        }
        ForgeHooksClient.setRenderPass(0);

        renderglobal.renderEntities(entity, icamera, partialTicks);

//        if (Reflector.ForgeHooksClient_setRenderPass.exists())
//        {//forge diff sets this to 0, optifine has it as -1 ...
//            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, Integer.valueOf(-1));
//        }

        ForgeHooksClient.setRenderPass(-1);


        this.mc.profiler.endStartSection("outline");

        boolean renderOutline = this.isDrawBlockOutline();

        if (renderOutline && this.mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.WATER))
        {
            EntityPlayer entityplayer = (EntityPlayer)entity;
            GlStateManager.disableAlpha();

//            if (!Reflector.ForgeHooksClient_onDrawBlockHighlight.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawBlockHighlight, new Object[] {renderglobal, entityplayer, this.mc.objectMouseOver, Integer.valueOf(0), Float.valueOf(partialTicks)}))
//            {
//                renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);
//            }
            ForgeHooksClient.onDrawBlockHighlight(renderglobal, entityplayer, this.mc.objectMouseOver, 0, partialTicks);

            GlStateManager.enableAlpha();
        }

        if (this.mc.debugRenderer.shouldRender())
        {
            this.mc.debugRenderer.renderDebug(partialTicks, finishTimeNano);
        }

        this.mc.profiler.endStartSection("destroyProgress");
        if (!((IRenderGlobalVR)renderglobal).getDamagedBlocks().isEmpty())
        {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), entity, partialTicks);
            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }

        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableBlend();


        this.mc.profiler.endStartSection("VR");

//        if(shadersMod)
//            Shaders.useProgram(Shaders.ProgramEntities); //entities

        renderCrosshairAtDepth();

//        if(shadersMod)
//            Shaders.useProgram(Shaders.ProgramEntities); //entities

        renderGuiLayer(partialTicks);
        if(KeyboardHandler.Showing) {
            if (((IMinecraftVR)mc).getVRSettings().physicalKeyboard)
                renderPhysicalKeyboard(partialTicks);
            else
                render2D(partialTicks, KeyboardHandler.Framebuffer, KeyboardHandler.Pos_room, KeyboardHandler.Rotation_room);
        }

        if(RadialHandler.Showing)
            render2D(partialTicks, RadialHandler.Framebuffer, RadialHandler.Pos_room, RadialHandler.Rotation_room);

//        if(shadersMod){
//            GlStateManager.enableLighting();
//            GlStateManager.colorMask(true, true, true, true);
//        }

        if (((IMinecraftVR)mc).getCurrentPass() != RenderPass.THIRD || ((IMinecraftVR)mc).getVRSettings().displayMirrorMode != VRSettings.MIRROR_THIRD_PERSON)
        {
            this.renderVRThings(partialTicks);
        }


        this.mc.profiler.endStartSection("hands");
        boolean forgeHands = false;

        // !Main.viewonly &&
        if((((IMinecraftVR)mc).getCurrentPass() != RenderPass.THIRD || ((IMinecraftVR)mc).getVRSettings().displayMirrorMode != VRSettings.MIRROR_THIRD_PERSON))
        {
            this.enableLightmap();
            RenderHelper.enableStandardItemLighting();
            ((IItemRendererVR)itemRenderer).triggerSetLightmap();
            GlStateManager.enableRescaleNormal();

            if (KeyboardHandler.Showing && ((IMinecraftVR)mc).getVRSettings().physicalKeyboard) {
                renderMainMenuHands(partialTicks);
            } else {
                this.renderHand(partialTicks, 0);
                this.renderLeftHand(partialTicks, 0);
            }
            //
//            forgeHands = ReflectorForge.renderFirstPersonHand(this.mc.renderGlobal, partialTicks, pass);
            ForgeHooksClient.renderFirstPersonHand(mc.renderGlobal, partialTicks, pass);
            //
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            this.disableLightmap();
            GlStateManager.enableTexture2D();
        }

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();



        this.mc.profiler.endStartSection("litParticles");
        if (!this.debugView)
        {
            this.enableLightmap();

//            if (shadersMod)
//            {
//                Shaders.beginLitParticles();
//            }

            particlemanager.renderLitParticles(entity, partialTicks);
            RenderHelper.disableStandardItemLighting();
            this.setupFog(0, partialTicks);

            this.mc.profiler.endStartSection("particles");

//            if (shadersMod)
//            {
//                Shaders.beginParticles();
//            }

            particlemanager.renderParticles(entity, partialTicks);

//            if (shadersMod)
//            {
//                Shaders.endParticles();
//            }

            this.disableLightmap();
        }

        GlStateManager.depthMask(false);

//        if (Config.isShaders())
//        {
//            GlStateManager.depthMask(Shaders.isRainDepth());
//        }

        GlStateManager.enableCull();
        this.mc.profiler.endStartSection("weather");

//        if (shadersMod)
//        {
//            Shaders.beginWeather();
//        }

        this.renderRainSnow(partialTicks);

//        if (shadersMod)
//        {
//            Shaders.endWeather();
//        }

        GlStateManager.depthMask(true);
        renderglobal.renderWorldBorder(entity, partialTicks);


//        if (shadersMod)
//        {
//            Shaders.preWater();
//        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.alphaFunc(516, 0.1F);
        this.setupFog(0, partialTicks);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.shadeModel(7425);
        this.mc.profiler.endStartSection("translucent");

//        if (shadersMod)
//        {
//            Shaders.beginWater();
//        }

        renderglobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, (double)partialTicks, pass, entity);

//        if (shadersMod)
//        {
//            Shaders.endWater();
//        }

        this.mc.profiler.endStartSection("forgeEntities");

//        if (Reflector.ForgeHooksClient_setRenderPass.exists() && !this.debugView)
//        {
            RenderHelper.enableStandardItemLighting();
//            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass,Integer.valueOf(1));
        ForgeHooksClient.setRenderPass(1);
            this.mc.renderGlobal.renderEntities(entity, icamera, partialTicks);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
//            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, Integer.valueOf(-1));
            ForgeHooksClient.setRenderPass(-1);
            RenderHelper.disableStandardItemLighting();
//        }

        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();

        this.mc.profiler.endStartSection("aboveClouds");
        //VIVE no eye height
        if (entity.posY >= 128.0D + (double)(this.mc.gameSettings.clouds * 128.0F))
        {
            this.renderCloudsCheck(renderglobal, partialTicks, pass, d0, d1, d2);
        }

//        if (Reflector.ForgeHooksClient_dispatchRenderLast.exists())
//        {
            this.mc.profiler.endStartSection("forge_render_last");
//            Reflector.callVoid(Reflector.ForgeHooksClient_dispatchRenderLast, renderglobal, partialTicks);
        ForgeHooksClient.dispatchRenderLast(renderglobal, partialTicks);
//        }

        eyeCollisionBlock = null;// getEyeCollisionBlock(mc.currentPass);

        eyeCollision = false;//eyeCollisionBlock != null && eyeCollisionBlock.isNormalCube());

//		/** MINECRIFT */ // restore our projection and modelview matrices
//		GL11.glMatrixMode(GL11.GL_PROJECTION);
//		GL11.glPopMatrix();
//		GL11.glMatrixMode(GL11.GL_MODELVIEW);
//		GL11.glPopMatrix();

        if (((IMinecraftVR)mc).getCurrentPass() != RenderPass.THIRD) //no fp overlay for 3rd cam.
        {
            this.renderFaceOverlay(partialTicks);
        }

        this.mc.profiler.endStartSection("ShadersEnd");
//        if ( shadersMod && !forgeHands && this.renderHand && !Shaders.isShadowPass)
//        {
//            GL11.glDisable(GL11.GL_STENCIL_TEST);
//            Shaders.renderCompositeFinal();
//        }

//        if (shadersMod)
//            Shaders.endRender();


        this.mc.profiler.endStartSection("renderGui");

        this.mc.profiler.endSection();

        restoreRVEPos((EntityLivingBase) mc.getRenderViewEntity()); //unhack the RVE position.
    }

    @Overwrite
    private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass, double x, double y, double z)
    {
        if (this.mc.gameSettings.renderDistanceChunks >= 4)
        {
            this.mc.profiler.endStartSection("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();

            //VIVE
            this.setupCameraTransform(partialTicks, pass, true);
            applyCameraDepth(false);
            //Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance * 4.0F);
            //
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.setupFog(0, partialTicks);
            renderGlobalIn.renderClouds(partialTicks, pass, x, y, z);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            //VIVE
            this.setupCameraTransform(partialTicks, pass, false);
            applyCameraDepth(false);
            // Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
            //
            GlStateManager.matrixMode(5888);
        }
    }

    private void updateFogColor(float partialTicks)
    {
        World world = this.mc.world;
        Entity entity = this.mc.getRenderViewEntity();
        float f = 0.25F + 0.75F * (float)this.mc.gameSettings.renderDistanceChunks / 32.0F;
        f = 1.0F - (float)Math.pow((double)f, 0.25D);
        Vec3d vec3d = world.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
        float f1 = (float)vec3d.x;
        float f2 = (float)vec3d.y;
        float f3 = (float)vec3d.z;
        Vec3d vec3d1 = world.getFogColor(partialTicks);
        this.fogColorRed = (float)vec3d1.x;
        this.fogColorGreen = (float)vec3d1.y;
        this.fogColorBlue = (float)vec3d1.z;

        if (this.mc.gameSettings.renderDistanceChunks >= 4)
        {
            double d0 = MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) > 0.0F ? -1.0D : 1.0D;
            Vec3d vec3d2 = new Vec3d(d0, 0.0D, 0.0D);
            float f5 = (float)entity.getLook(partialTicks).dotProduct(vec3d2);

            if (f5 < 0.0F)
            {
                f5 = 0.0F;
            }

            if (f5 > 0.0F)
            {
                float[] afloat = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);

                if (afloat != null)
                {
                    f5 = f5 * afloat[3];
                    this.fogColorRed = this.fogColorRed * (1.0F - f5) + afloat[0] * f5;
                    this.fogColorGreen = this.fogColorGreen * (1.0F - f5) + afloat[1] * f5;
                    this.fogColorBlue = this.fogColorBlue * (1.0F - f5) + afloat[2] * f5;
                }
            }
        }

        this.fogColorRed += (f1 - this.fogColorRed) * f;
        this.fogColorGreen += (f2 - this.fogColorGreen) * f;
        this.fogColorBlue += (f3 - this.fogColorBlue) * f;
        float f8 = world.getRainStrength(partialTicks);

        if (f8 > 0.0F)
        {
            float f4 = 1.0F - f8 * 0.5F;
            float f10 = 1.0F - f8 * 0.4F;
            this.fogColorRed *= f4;
            this.fogColorGreen *= f4;
            this.fogColorBlue *= f10;
        }

        float f9 = world.getThunderStrength(partialTicks);

        if (f9 > 0.0F)
        {
            float f11 = 1.0F - f9 * 0.5F;
            this.fogColorRed *= f11;
            this.fogColorGreen *= f11;
            this.fogColorBlue *= f11;
        }

        //Vivecraft for TE
        IBlockState iblockstate;
        Vec3d pos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition();
        iblockstate = this.mc.world.getBlockState(new BlockPos(pos.x, pos.y, pos.z));
        //

        if (this.cloudFog)
        {
            Vec3d vec3d3 = world.getCloudColour(partialTicks);
            this.fogColorRed = (float)vec3d3.x;
            this.fogColorGreen = (float)vec3d3.y;
            this.fogColorBlue = (float)vec3d3.z;
        }
        else
        {
            //Forge Moved to Block.
            Vec3d viewport = ActiveRenderInfo.projectViewFromEntity(entity, partialTicks);
            BlockPos viewportPos = new BlockPos(viewport);
            IBlockState viewportState = this.mc.world.getBlockState(viewportPos);
            Vec3d inMaterialColor = viewportState.getBlock().getFogColor(this.mc.world, viewportPos, viewportState, entity, new Vec3d(fogColorRed, fogColorGreen, fogColorBlue), partialTicks);
            this.fogColorRed = (float)inMaterialColor.x;
            this.fogColorGreen = (float)inMaterialColor.y;
            this.fogColorBlue = (float)inMaterialColor.z;
        }

        float f13 = this.fogColor2 + (this.fogColor1 - this.fogColor2) * partialTicks;
        this.fogColorRed *= f13;
        this.fogColorGreen *= f13;
        this.fogColorBlue *= f13;
        double d1 = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks) * world.provider.getVoidFogYFactor();

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MobEffects.BLINDNESS))
        {
            int i = ((EntityLivingBase)entity).getActivePotionEffect(MobEffects.BLINDNESS).getDuration();

            if (i < 20)
            {
                d1 *= (double)(1.0F - (float)i / 20.0F);
            }
            else
            {
                d1 = 0.0D;
            }
        }

        if (d1 < 1.0D)
        {
            if (d1 < 0.0D)
            {
                d1 = 0.0D;
            }

            d1 = d1 * d1;
            this.fogColorRed = (float)((double)this.fogColorRed * d1);
            this.fogColorGreen = (float)((double)this.fogColorGreen * d1);
            this.fogColorBlue = (float)((double)this.fogColorBlue * d1);
        }

        if (this.bossColorModifier > 0.0F)
        {
            float f14 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
            this.fogColorRed = this.fogColorRed * (1.0F - f14) + this.fogColorRed * 0.7F * f14;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f14) + this.fogColorGreen * 0.6F * f14;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f14) + this.fogColorBlue * 0.6F * f14;
        }

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MobEffects.NIGHT_VISION))
        {
            float f15 = this.getNightVisionBrightness((EntityLivingBase)entity, partialTicks);
            float f6 = 1.0F / this.fogColorRed;

            if (f6 > 1.0F / this.fogColorGreen)
            {
                f6 = 1.0F / this.fogColorGreen;
            }

            if (f6 > 1.0F / this.fogColorBlue)
            {
                f6 = 1.0F / this.fogColorBlue;
            }

            // Forge: fix MC-4647 and MC-10480
            if (Float.isInfinite(f6)) f6 = Math.nextAfter(f6, 0.0);

            this.fogColorRed = this.fogColorRed * (1.0F - f15) + this.fogColorRed * f6 * f15;
            this.fogColorGreen = this.fogColorGreen * (1.0F - f15) + this.fogColorGreen * f6 * f15;
            this.fogColorBlue = this.fogColorBlue * (1.0F - f15) + this.fogColorBlue * f6 * f15;
        }

        if (this.mc.gameSettings.anaglyph)
        {
            float f16 = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
            float f17 = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
            float f7 = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
            this.fogColorRed = f16;
            this.fogColorGreen = f17;
            this.fogColorBlue = f7;
        }

        net.minecraftforge.client.event.EntityViewRenderEvent.FogColors event = new net.minecraftforge.client.event.EntityViewRenderEvent.FogColors((EntityRenderer)(IEntityRendererVR) this, entity, iblockstate, partialTicks, this.fogColorRed, this.fogColorGreen, this.fogColorBlue);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

        this.fogColorRed = event.getRed();
        this.fogColorGreen = event.getGreen();
        this.fogColorBlue = event.getBlue();

        GlStateManager.clearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
    }

    @Shadow
    public float getNightVisionBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks)
    {
        return 0;
    }

    @Shadow
    private void createWorldIcon()
    {

    }

    @Shadow
    private void renderItemActivation(int p_190563_1_, int p_190563_2_, float p_190563_3_)
    {

    }

    @Shadow
    protected void renderRainSnow(float partialTicks)
    {

    }

    @Shadow
    public void disableLightmap()
    {

    }

    @Shadow
    public void updateLightmap(float partialTicks)
    {

    }

    @Shadow
    private void setupFog(int startCoords, float partialTicks)
    {

    }

    @Shadow
    private FloatBuffer setFogColorBuffer(float red, float green, float blue, float alpha)
    {
        return null;
    }

    @Shadow
    public void enableLightmap()
    {

    }

    //VIVECRAFT ADDITIONS *********************************************************
    public void setupClipPlanes()
    {
        this.farPlaneDistance = (float)(mc.gameSettings.renderDistanceChunks * 16);

//		if (Config.isFogFancy())
//		{
//			this.farPlaneDistance *= 0.95F;
//		}
//
//		if (Config.isFogFast())
//		{
//			this.farPlaneDistance *= 0.83F;
//		}

        this.clipDistance = this.farPlaneDistance * 2.0F;

        if (this.clipDistance < 173.0F)
        {
            this.clipDistance = 173.0F;
        }

        if (mc.world != null && mc.world.provider != null && mc.world.provider.getDimensionType() == DimensionType.THE_END)
        {
            this.clipDistance = 256.0F;
        }
    }


    private void updateMainMenu(GuiMainMenu mainGui)
    {
        try
        {
            String e = null;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int day = calendar.get(5);
            int month = calendar.get(2) + 1;

            if (day == 8 && month == 4)
            {
                e = "Happy birthday, OptiFine!";
            }

            if (day == 14 && month == 8)
            {
                e = "Happy birthday, sp614x!";
            }

            if (e == null)
            {
                return;
            }

            Field[] fs = GuiMainMenu.class.getDeclaredFields();

            for (int i = 0; i < fs.length; ++i)
            {
                if (fs[i].getType() == String.class)
                {
                    fs[i].setAccessible(true);
                    fs[i].set(mainGui, e);
                    break;
                }
            }
        }
        catch (Throwable var8)
        {
            ;
        }
    }

    /** MINECRIFT ADDITIONS BELOW **/


    private float checkCameraCollision(
            double camX,       double camY,       double camZ,
            double camXOffset, double camYOffset, double camZOffset, float distance )
    {
        //This loop offsets at [-.1, -.1, -.1], [.1,-.1,-.1], [.1,.1,-.1] etc... for all 8 directions
        double minDistance = -1d;

        // Lets extend out the test range somewhat
        camXOffset *= 10f;
        camYOffset *= 10f;
        camZOffset *= 10f;

        for (int var20 = 0; var20 < 8; ++var20)
        {
            final float MIN_DISTANCE = 0.06F;
            float var21 = (float)((var20 & 1) * 2 - 1);
            float var22 = (float)((var20 >> 1 & 1) * 2 - 1);
            float var23 = (float)((var20 >> 2 & 1) * 2 - 1);
            var21 *= 0.1F;
            var22 *= 0.1F;
            var23 *= 0.1F;
            RayTraceResult var24 = this.mc.world.rayTraceBlocks(
                    new Vec3d(camX + var21, camY + var22, camZ + var23),
                    new Vec3d(camX - camXOffset + var21, camY - camYOffset + var22, camZ - camZOffset + var23));

            BlockPos bp = var24.getBlockPos();
            if (var24 != null && this.mc.world.isBlockNormalCube(bp, true))
            {
                double var25 = var24.hitVec.distanceTo(new Vec3d(camX, camY, camZ)) - MIN_DISTANCE;

                if (minDistance == -1d)
                {
                    minDistance = var25;
                }
                else if (var25 < minDistance)
                {
                    minDistance = var25;
                }
            }
        }
        if (minDistance == -1d)
            minDistance = distance *= 10d;

        return (float)minDistance;
    }

    public void drawSizedQuad(float displayWidth, float displayHeight, float size, VRGLStateManagerColor color)
    {
        float aspect = displayHeight / displayWidth;

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

        b.pos(-(size / 2f), -(size * aspect) / 2f, 0.0f).tex(0.0f, 0.0f).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(size / 2f, -(size * aspect) / 2f, 0.0f).tex(1.0f, 0.0f).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(size / 2f, (size * aspect) / 2f, 0.0f).tex(1.0f, 1.0f).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(-(size / 2f), (size * aspect) / 2f, 0.0f).tex(0.0f, 1.0f).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();

        t.draw();
    }

    public void drawSizedQuad(float displayWidth, float displayHeight, float size)
    {
        drawSizedQuad(displayWidth, displayHeight, size, new VRGLStateManagerColor());
    }

    public void drawSizedQuadWithLightmap(float displayWidth, float displayHeight, float size, int lightX, int lightY, VRGLStateManagerColor color)
    {
        float aspect = displayHeight / displayWidth;

        this.enableLightmap();

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, VRDefaultVertexFormats.POSITION_TEX_LMAP_COLOR_NORMAL);

        b.pos(-(size / 2f), -(size * aspect) / 2f, 0.0f).tex(0.0f, 0.0f).lightmap(lightX, lightY).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(size / 2f, -(size * aspect) / 2f, 0.0f).tex(1.0f, 0.0f).lightmap(lightX, lightY).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(size / 2f, (size * aspect) / 2f, 0.0f).tex(1.0f, 1.0f).lightmap(lightX, lightY).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();
        b.pos(-(size / 2f), (size * aspect) / 2f, 0.0f).tex(0.0f, 1.0f).lightmap(lightX, lightY).color(color.red, color.green, color.blue, color.alpha).normal(0, 0, 1).endVertex();

        t.draw();

        this.disableLightmap();
    }

    public void drawSizedQuadWithLightmap(float displayWidth, float displayHeight, float size, int lightX, int lightY)
    {
        drawSizedQuadWithLightmap(displayWidth, displayHeight, size, lightX, lightY, new VRGLStateManagerColor());
    }
    public void handleNotificationText()
    {
        String prefix = "";
        String message = "";
        String suffix = "";

        boolean renderTxt = false;

        // error info takes precedence
        if (((IMinecraftVR)mc).getErrorHelper() != null) {
            if (System.currentTimeMillis() < ((IMinecraftVR)mc).getErrorHelper().endTime)
            {
                prefix = ((IMinecraftVR)mc).getErrorHelper().title;
                message = ((IMinecraftVR)mc).getErrorHelper().message;
                suffix = ((IMinecraftVR)mc).getErrorHelper().resolution;
                renderTxt = true;
            }
            else
            {
                ((IMinecraftVR)mc).setErrorHelper(null);
            }
        }
        // otherwise display any calibration info
        if (renderTxt)
            displayNotificationText(prefix, message, suffix,
                    this.mc.displayWidth, this.mc.displayHeight, true, !false);
    }

    public void displayNotificationText(String prefix, String message, String suffix,
                                        int displayWidth, int displayHeight, boolean isStereo, boolean isGuiOrtho)
    {
        final float INITIAL_TEXT_SCALE = isGuiOrtho ? 0.0055f : 0.00375f;
        final int TEXT_WORDWRAP_LEN = 55;
        final int COLUMN_GAP = 12;

        float fade = isGuiOrtho ? 0.85f : 0.80f;
        Vec3d rgb = new Vec3d(0f, 0f, 0f);
        //	renderFadeBlend(rgb, fade);

        // Pass matrici on to OpenGL...
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        if (isStereo)
        {
            int i = ((IMinecraftVR)mc).getCurrentPass().ordinal();
            if(i>1) i = 0;
            GL11.glMultMatrix(((IMinecraftVR)mc).getStereoProvider().eyeproj[i]);
        }
        else
        {
            Project.gluPerspective(90f, (float) displayWidth / (float) displayHeight, minClipDistance, clipDistance);
        }
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        int column = 8;
        ArrayList<String> wrapped = new ArrayList<String>();
        if (message != null)
            Utils.wordWrap(message, TEXT_WORDWRAP_LEN, wrapped);
        float rows = wrapped.size();
        float shift = rows / 2f;

        float x = isGuiOrtho ? 0f : 0; // : -((IMinecraftVR)mc).getVRSettings().getHalfIPD(EyeType.ovrEye_Center);
        float y = shift * COLUMN_GAP * 0.003f; // Move up
        float z = -0.6f;
        boolean d = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.disableDepth();
        GL11.glTranslatef(x, y, z);
        GL11.glRotatef(180f, 0.0F, 1.0F, 0.0F);
        float textScale = (float) Math.sqrt((x * x + y * y + z * z));
        GL11.glScalef(-INITIAL_TEXT_SCALE * textScale, -INITIAL_TEXT_SCALE * textScale, -INITIAL_TEXT_SCALE * textScale);
        if (prefix != null)
            mc.fontRenderer.drawStringWithShadow(prefix, -mc.fontRenderer.getStringWidth(prefix) / 2, -8, /*white*/16777215);

        for (String line : wrapped)
        {
            mc.fontRenderer.drawStringWithShadow(line, -mc.fontRenderer.getStringWidth(line) / 2, column, /*white*/16777215);
            column += COLUMN_GAP;
        }
        column += COLUMN_GAP;
        if (suffix != null)
            mc.fontRenderer.drawStringWithShadow(suffix, -mc.fontRenderer.getStringWidth(suffix) / 2, column, /*white*/16777215);

        if(d)	GlStateManager.enableDepth();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }



    public void getPointedBlock(float par1)
    {
        /** END MINECRIFT */
        if (this.mc.getRenderViewEntity() != null && this.mc.world != null)
        {
            /** MINECRIFT */
            // Lets choose to use the head position for block / entity distance hit / miss calcs for now. Lean
            // forward, you can hit further away...

            this.mc.pointedEntity = null;
            double blockReachDistance = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale * (double)this.mc.playerController.getBlockReachDistance();
            double entityReachDistance = (double)this.mc.playerController.getBlockReachDistance();
            // Darktemp's crosshair fix
            // VIVE START - interact source

            Vec3d aimsource = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getPosition();
            Vec3d aimsoucecopy = new Vec3d(aimsource.x, aimsource.y, aimsource.z);
            Vec3d aimsourcecopy2 = new Vec3d(aimsource.x, aimsource.y, aimsource.z);
            Vec3d eyePos = getRVEPositionEyes(par1);
            // VIVE END - interact source

            Vec3d aim = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getDirection();
            Vec3d endPos = aimsource.add(aim.x*blockReachDistance,aim.y*blockReachDistance ,aim.z*blockReachDistance );
            crossVec=aimsource.add(aim.x*blockReachDistance,aim.y*blockReachDistance ,aim.z*blockReachDistance );

            this.mc.objectMouseOver = this.mc.world.rayTraceBlocks(aimsource, endPos, false, false, true);

            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit != RayTraceResult.Type.MISS) {
                if (eyePos.distanceTo(this.mc.objectMouseOver.hitVec) > blockReachDistance) {
                    this.mc.objectMouseOver.typeOfHit = RayTraceResult.Type.MISS;
                }
            }

            //JRBUDDA - i dunno what any of this does that option isnt used.

            double maxreach = MAX_CROSSHAIR_DISTANCE;

            //   System.out.println(this.mc.objectMouseOver.toString());
            if (this.mc.objectMouseOver == null || this.mc.objectMouseOver.typeOfHit == RayTraceResult.Type.MISS)
            {
                endPos = aimsoucecopy.add(aim.x * MAX_CROSSHAIR_DISTANCE, aim.y * MAX_CROSSHAIR_DISTANCE, aim.z * MAX_CROSSHAIR_DISTANCE);
                RayTraceResult crossPos = this.mc.world.rayTraceBlocks(aimsoucecopy, endPos, false, false, true);
                if (crossPos != null) {
                    crossVec = crossPos.hitVec;
                    maxreach = crossVec.distanceTo(eyePos);
                } else {
                    crossVec = new Vec3d(endPos.x, endPos.y, endPos.z);
                    maxreach = crossVec.distanceTo(eyePos);
                }

                this.mc.objectMouseOver = null;
            }
            else
            {
                // Get HIT distance
                maxreach = this.mc.objectMouseOver.hitVec.distanceTo(eyePos); // Set entityreach here - we can't hit an entity behind whatever this is...
                crossVec = this.mc.objectMouseOver.hitVec;
            }

            Entity pointedEntity = null;
            Vec3d hitLocation = null;

            AxisAlignedBB bb = new AxisAlignedBB(
                    -maxreach, -maxreach, -maxreach,
                    maxreach, maxreach, maxreach)
                    .offset(aimsourcecopy2.x, aimsourcecopy2.y,  aimsourcecopy2.z).expand(1, 1, 1);

            List entitiesWithinCrosshairDist = this.mc.world.getEntitiesWithinAABBExcludingEntity(this.mc.getRenderViewEntity(), bb);

            double reach = maxreach;
            double dist = 0;

            for (int i = 0; i < entitiesWithinCrosshairDist.size(); ++i)
            {
                Entity entity = (Entity)entitiesWithinCrosshairDist.get(i);

                if (entity.canBeCollidedWith())
                {
                    float borderSize = entity.getCollisionBorderSize();
                    AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand((double)borderSize, (double)borderSize, (double)borderSize);
                    RayTraceResult collision = boundingBox.calculateIntercept(aimsourcecopy2, crossVec);

                    if (boundingBox.contains(aimsourcecopy2))
                    {
                        if (0.0D < reach || reach == 0.0D)
                        {
                            pointedEntity = entity;
                            hitLocation = collision == null ? aimsourcecopy2 : collision.hitVec;
                            reach = 0.0D;
                        }
                    }
                    else if (collision != null)
                    {
                        dist = eyePos.distanceTo(collision.hitVec);

                        if (dist < maxreach || reach == 0.0D)
                        {
                            boolean canRiderInteract = false;


                            canRiderInteract = entity.canRiderInteract();

                            if (entity == this.mc.player.getRidingEntity() && !canRiderInteract)
                            {
                                if (reach == 0.0D)
                                {
                                    pointedEntity = entity;
                                    hitLocation = collision.hitVec;
                                }
                            }
                            else
                            {
                                pointedEntity = entity;
                                hitLocation = collision.hitVec;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && hitLocation !=null )
                crossVec = hitLocation;

            if (pointedEntity != null && (dist < entityReachDistance) ) {
                this.mc.objectMouseOver = new RayTraceResult(pointedEntity, hitLocation);
                crossVec = this.mc.objectMouseOver.hitVec;

                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    this.mc.pointedEntity = pointedEntity;
                }
            }


            /** END MINECRIFT */
        }
    }


    /** Renders the pre-generated 2-d framebuffer into the world at the appropriate location..
     * @param par1
     */
    void render2D(float par1, VRFrameBuffer framebuffer, Vec3d pos, org.vivecraft.utils.Matrix4f rot)
    {

        if(((IMinecraftVR)mc).getBowTracker().isDrawing) return;

        boolean inMenuRoom = isInMenuRoom();
        // Pass matrici on to OpenGL...
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        // Minecrift - use correct projection
        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT || ((IMinecraftVR)mc).getCurrentPass() ==RenderPass.RIGHT)
        {
            int i = ((IMinecraftVR)mc).getCurrentPass().ordinal();
            if(i > 1) i = 0;
            GL11.glMultMatrix(((IMinecraftVR)mc).getStereoProvider().cloudeyeproj[i]);
        }
        else if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD) {
            Project.gluPerspective(((IMinecraftVR)mc).getVRSettings().mixedRealityFov, ((IMinecraftVR)mc).getVRSettings().mixedRealityAspectRatio, minClipDistance, clipDistance * 4);
        } else {
            Project.gluPerspective(this.getFOVModifier(par1, true), (float)this.mc.displayWidth / (float) this.mc.displayHeight, minClipDistance, clipDistance * 4);
        }


        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD && this.mc.world == null) {
            matrixBuffer.rewind();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
            matrixBuffer.rewind();
            this.thirdPassProjectionMatrix.load(matrixBuffer);
            matrixBuffer.rewind();
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        // VIVE START - custom GUI position
        GL11.glPushMatrix();
        applyMenuRoomModelView(((IMinecraftVR)mc).getCurrentPass());

        GlStateManager.loadIdentity();
        apply2DModelView(((IMinecraftVR)mc).getCurrentPass(), pos, rot);

        framebuffer.bindFramebufferTexture();

        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();

        // Prevent black border at top / bottom of GUI
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        // Set texture filtering
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);

        if (!inMenuRoom)
        {
            GlStateManager.enableBlend();
            GL14.glBlendColor(1, 1, 1, ((IMinecraftVR)mc).getVRSettings().hudOpacity);
            if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.isSneaking()){
                GL14.glBlendColor(1, 1, 1, ((IMinecraftVR)mc).getVRSettings().hudOpacity * 0.75f);
            }
            GlStateManager.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
        }
        else{
            GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1f);
            GL14.glBlendColor(1, 1, 1, 1);
        }

        Vec3d poseye = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition();

        if (inMenuRoom || mc.currentScreen != null || RadialHandler.isShowing() || KeyboardHandler.Showing ||
                !((IMinecraftVR)mc).getVRSettings().hudOcclusion
                || ((IItemRendererVR)itemRenderer).isInsideOpaqueBlock(poseye, false)){
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
        } else {
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
        }

        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD){
            GlStateManager.depthFunc(GL11.GL_LEQUAL);					}

        //the framebuffer has to be drawn with color blending transparency to support non-alpha cursors. Always has a black background.
        GlStateManager.alphaFunc(GL11.GL_GREATER, 1f/255f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();

        //Render framebuffer onto world projection
        ScaledResolution s = new ScaledResolution(mc);

        if(inMenuRoom)
            GlStateManager.disableAlpha();
        else
            GlStateManager.enableAlpha();

        GlStateManager.disableLighting();

        drawSizedQuad(s.getScaledWidth(), s.getScaledHeight(), 1.5f);

        GL14.glBlendColor(1, 1, 1, 1);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableCull();


        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    void renderPhysicalKeyboard(float partialTicks) {
        if(((IMinecraftVR)mc).getBowTracker().isDrawing) return;

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();

        // Minecrift - use correct projection
        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT || ((IMinecraftVR)mc).getCurrentPass() == RenderPass.RIGHT)
        {
            int i = ((IMinecraftVR)mc).getCurrentPass().ordinal();
            if(i > 1) i = 0;
            GL11.glMultMatrix(((IMinecraftVR)mc).getStereoProvider().cloudeyeproj[i]);
        }
        else if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD) {
            Project.gluPerspective(((IMinecraftVR)mc).getVRSettings().mixedRealityFov, ((IMinecraftVR)mc).getVRSettings().mixedRealityAspectRatio, minClipDistance, clipDistance * 4);
        } else {
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float) this.mc.displayHeight, minClipDistance, clipDistance * 4);
        }

        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD && this.mc.world == null) {
            matrixBuffer.rewind();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
            matrixBuffer.rewind();
            this.thirdPassProjectionMatrix.load(matrixBuffer);
            matrixBuffer.rewind();
        }

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting(); // Do this exactly here or it looks wrong
        applyPhysicalKeyboardModelView(((IMinecraftVR)mc).getCurrentPass(), KeyboardHandler.Pos_room, KeyboardHandler.Rotation_room);

        KeyboardHandler.physicalKeyboard.render();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }


    /** Renders the pre-generated 2-d framebuffer into the world at the appropriate location..
     * @param par1
     */
    public void renderGuiLayer(float par1)
    {
        if(((IMinecraftVR)mc).getBowTracker().isDrawing) return;

        if(mc.currentScreen ==null && mc.gameSettings.hideGUI) return;
        if(RadialHandler.isShowing()) return;

        boolean inMenuRoom = isInMenuRoom();
        // Pass matrici on to OpenGL...
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        // Minecrift - use correct projection
        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.LEFT || ((IMinecraftVR)mc).getCurrentPass() ==RenderPass.RIGHT)
        {
            int i = ((IMinecraftVR)mc).getCurrentPass().ordinal();
            if(i > 1) i = 0;
            GL11.glMultMatrix(((IMinecraftVR)mc).getStereoProvider().cloudeyeproj[i]);
        }
        else if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD) {
            Project.gluPerspective(((IMinecraftVR)mc).getVRSettings().mixedRealityFov, ((IMinecraftVR)mc).getVRSettings().mixedRealityAspectRatio, minClipDistance, clipDistance * 4);
        } else {
            Project.gluPerspective(this.getFOVModifier(par1, true), (float)this.mc.displayWidth / (float) this.mc.displayHeight, minClipDistance, clipDistance * 4);
        }


        if (((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD && this.mc.world == null) {
            matrixBuffer.rewind();
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
            matrixBuffer.rewind();
            this.thirdPassProjectionMatrix.load(matrixBuffer);
            matrixBuffer.rewind();
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // VIVE START - custom GUI position
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        applyMenuRoomModelView(((IMinecraftVR)mc).getCurrentPass());

        ///MAIN MENU ENVIRONMENT
        if(inMenuRoom){
            if (((IMinecraftVR)mc).getMenuWorldRenderer() != null && ((IMinecraftVR)mc).getMenuWorldRenderer().isReady()) {
                try {
                    renderTechjarsAwesomeMainMenuRoom();
                } catch (Exception e) {
                    System.out.println("Error rendering main menu world, unloading to prevent more errors");
                    e.printStackTrace();
                    ((IMinecraftVR)mc).getMenuWorldRenderer().destroy();
                }
            } else {
                renderJrbuddasAwesomeMainMenuRoom();
            }
        }
        //END AWESOME MAIN MENU ENVIRONMENT

        GlStateManager.loadIdentity();
        Vec3d guipos = GuiHandler.applyGUIModelView(((IMinecraftVR)mc).getCurrentPass());

        GuiHandler.guiFramebuffer.bindFramebufferTexture();

        GlStateManager.disableCull();
        //	RenderHelper.disableStandardItemLighting();
        GlStateManager.enableTexture2D();

        // Prevent black border at top / bottom of GUI
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        // Set texture filtering
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16.0f);

        VRGLStateManagerColor color = new VRGLStateManagerColor();
        if (!inMenuRoom)
        {
            if (mc.currentScreen == null)
                color.alpha = ((IMinecraftVR)mc).getVRSettings().hudOpacity;
            if (mc.player != null && mc.player.isSneaking())
                color.alpha *= 0.75f;
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA); // Mods can just stop being dumb, m'kay?
        }
        else{
            GlStateManager.disableBlend();
        }

        Vec3d pos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition();

        boolean iamlazy = ((IMinecraftVR)mc).getVRSettings().vrHudLockMode == ((IMinecraftVR)mc).getVRSettings().HUD_LOCK_WRIST && MCOpenVR.hudPopup;

        if (inMenuRoom || mc.currentScreen != null || iamlazy ||
                !((IMinecraftVR)mc).getVRSettings().hudOcclusion
                || ((IItemRendererVR)itemRenderer).isInsideOpaqueBlock(pos, false)){
            // Never use depth test for in game menu - so you can always see it!
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
        } else {
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
        }

        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD){
            GlStateManager.depthFunc(GL11.GL_LEQUAL);					}

        //the framebuffer has to be drawn with color blending transparency to support non-alpha cursors. Always has a black background.
        GlStateManager.alphaFunc(GL11.GL_GREATER,1f/255f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();

        //Render framebuffer onto world projection
        ScaledResolution s = new ScaledResolution(mc);

        if(inMenuRoom)
            GlStateManager.disableAlpha();
        else
            GlStateManager.enableAlpha();

        GlStateManager.disableLighting();

        if(mc.world != null){
            if (((IItemRendererVR)itemRenderer).isInsideOpaqueBlock(guipos, false))
                guipos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getPosition();

            // Config.isShaders() ? 8 :
            int minLight = 4;
            int i = mc.world.getCombinedLight(new BlockPos(guipos), minLight);
            int j = i >> 16 & 65535;
            int k = i & 65535;
            //OpenGlHelper.glMultiTexCoord2f(OpenGlHelper.GL_TEXTURE1, (float)j, (float)k);

            // what?
			            /*if(!Config.isShaders()){
			                float b = ((float)k) / 255;
			                if (j>k) b = ((float)j) / 255;
			                GlStateManager.color3f(b, b, b); // \_(oo)_/
			            }*/

            drawSizedQuadWithLightmap(s.getScaledWidth(), s.getScaledHeight(), 1.5f, j, k, color);
        } else {
            drawSizedQuad(s.getScaledWidth(), s.getScaledHeight(), 1.5f, color);
        }

        GL14.glBlendColor(1, 1, 1, 1);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableCull();


        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

    }


    //VIVE
    public void renderDebugAxes(int r, int g, int b, float radius){
        setupPolyRendering(true);
        renderCircle(new Vec3d(0, 0, 0), (float) radius, 32, r, g, b	, 255, 0);
        renderCircle(new Vec3d(0, .01, 0), (float) radius * .75f, 32, r, g, b	, 255, 0);
        renderCircle(new Vec3d(0,0.02, 0), (float) radius * .25f, 32, r, g, b	, 255, 0);
        renderCircle(new Vec3d(0, 0, .15), (float) radius *.5f, 32, r, g, b	, 255, 2);
        setupPolyRendering(false);
    }

    public void drawScreen(float par1, GuiScreen screen) {
        int mouseX = 0;
        int mouseY = 0;
        final ScaledResolution var15 = new ScaledResolution(this.mc);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        //Render all UI elements into guiFBO
        GlStateManager.clearColor(0, 0, 0, 0);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, var15.getScaledWidth_double(), var15.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        screen.drawScreen(0, 0, par1);

        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        //inventory messes up fog color sometimes... This fixes

        //update mipmaps for Gui layer
        ((IMinecraftVR)mc).getFramebuffer().bindFramebufferTexture();
        ((IMinecraftVR)mc).getFramebuffer().genMipMaps();
        ((IMinecraftVR)mc).getFramebuffer().unbindFramebufferTexture();
        GL11.glEnable(GL11.GL_STENCIL_TEST);

    }

    //TODO: move this into Guiingame where it belongs.
    /** This draws the normal 2d menu/GUI elements to the framebuffer, to be rendered into the world projection later (in renderFramebufferIntoWorld)
     * @param renderPartialTicks
     * @param tickDuration
     */
    public void drawFramebuffer(float renderPartialTicks, long tickDuration)    // VIVE - added parameter for debug info
    {
        int mouseX = 0;
        int mouseY = 0;
        final ScaledResolution var15 = new ScaledResolution(this.mc);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        if (((IMinecraftVR)mc).getShowSplashScreen())
        {
            ((IMinecraftVR)mc).setShowSplashScreen(false);
            //this.mc.showSplash(((IMinecraftVR)mc).getFramebuffer());
        }
        else if (false)//(this.mc.isIntegratedServerLaunching())
        {
            //			this.guiScreenShowingThisFrame = true;
            //			this.mc.loadingScreen.render();
        }
        else //always for vive, never for mono
        {
            if (
                    (this.mc.world != null && this.mc.player.getSleepTimer() == 0)
                            ||
                            this.mc.currentScreen != null
            ) //draw a thing
            {
                //Render all UI elements into guiFBO
                GlStateManager.clearColor(0, 0, 0, 0);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, var15.getScaledWidth_double(), var15.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0.0F, 0.0F, -2000.0F);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

                // Display loading / progress window if necessary
                if (this.mc.world != null  /*&& !this.blankGUIUntilWorldValid*/)
                {
                    //Draw in game HUD overlay
                    GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

                    FMLCommonHandler.instance().onRenderTickStart(renderPartialTicks);

//                    if(!Main.viewonly){
                        //Render HUD elements
                        renderViveHudIcons();
                        this.mc.ingameGUI.renderGameOverlay(renderPartialTicks);
//                    }

//                    if (this.mc.gameSettings.ofShowFps && !this.mc.gameSettings.showDebugInfo)
//                    {
//                        Config.drawFps();
//                    }

                    //mc.guiAchievement.updateAchievementWindow();

                    FMLCommonHandler.instance().onRenderTickEnd(renderPartialTicks);

                    GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                }

                //        if (this.blankGUIUntilWorldValid) {
                //            if (this.mc.world != null)
                //                this.blankGUIUntilWorldValid = false;
                //        }

                if (this.mc.currentScreen != null /*&& !this.blankGUIUntilWorldValid*/)
                {
                    final int mouseX1 = mouseX = ((IMinecraftVR)mc).getMouseXPos();
                    final int mouseY1 = mouseY = ((IMinecraftVR)mc).getMouseYPos();

                    try
                    {
                        if(mc.currentScreen instanceof GuiContainer)
                            GlStateManager.enableDepth(); //fixes inventory you.

                        ForgeHooksClient.drawScreen(mc.currentScreen, mouseX1, mouseY1, renderPartialTicks);

                    }
                    catch (Throwable throwable)
                    {
                        //duhhh.... wat
                    }

                }
                //	Draw GUI crosshair
                if(GuiHandler.controllerMouseValid) {
                    if(mc.currentScreen !=null){
                        ((IGUIIngameVR)mc.ingameGUI).drawMouseMenuQuad(((IMinecraftVR)mc).getMouseXPos(),((IMinecraftVR)mc).getMouseYPos());
                    }else {
//						ScaledResolution temp = new ScaledResolution(mc);
//						mc.ingameGUI.drawMouseMenuQuad(
//								(int) GuiHandler.controllerMouseX * temp.getScaledWidth() / mc.displayWidth ,
//								(int) (temp.getScaledHeight() - GuiHandler.controllerMouseY *  temp.getScaledHeight() / mc.displayHeight- 1));
                    }
                }



            }

            GlStateManager.enableColorMaterial();
            GlStateManager.disableBlend();
            GlStateManager.disableLighting();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            //inventory messes up fog color sometimes... This fixes


            ((IMinecraftVR)mc).getToastGUI().drawToast(new ScaledResolution(mc));


            // VIVE added debug info to HUD
            if (mc.gameSettings.showDebugInfo && mc.gameSettings.showDebugProfilerChart)
            {
                ((IMinecraftVR)mc).triggerDisplayDebugInfo(tickDuration);
            }
        }

        //update mipmaps for Gui layer
        ((IMinecraftVR)mc).getFramebuffer().bindFramebufferTexture();
        ((IMinecraftVR)mc).getFramebuffer().genMipMaps();
        ((IMinecraftVR)mc).getFramebuffer().unbindFramebufferTexture();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    Vec3i tpUnlimitedColor = new Vec3i((byte)173, (byte)216, (byte)230);
    Vec3i tpLimitedColor = new Vec3i((byte)205, (byte)169, (byte)205);
    Vec3i tpInvalidColor = new Vec3i((byte)83, (byte)83, (byte)83);


    private void renderTeleportArc(OpenVRPlayer vrPlayer) {

        if ( ((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.showBeam && ((IMinecraftVR)mc).getTeleportTracker().isAiming()
                && ((IMinecraftVR)mc).getTeleportTracker().movementTeleportArcSteps > 1)
        {
            mc.profiler.startSection("teleportArc");

            boolean isShader = false;
            GlStateManager.enableCull();

            Tessellator tes = Tessellator.getInstance();
            tes.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            double VOffset = ((IMinecraftVR)mc).getTeleportTracker().lastTeleportArcDisplayOffset;
            Vec3d dest = ((IMinecraftVR)mc).getTeleportTracker().getDestination();

            boolean validLocation = (dest.x != 0
                    || dest.y != 0
                    || dest.z != 0);


            Vec3i color;
            byte alpha = (byte) 255;

            if (!validLocation)
            {
                // invalid location
                color = new Vec3i(83, 75, 83);
                alpha = (byte) 128;
            }
            else
            {
                if(((IMinecraftVR)mc).getVRSettings().vrLimitedSurvivalTeleport && !mc.player.capabilities.allowFlying)
                    color = tpLimitedColor;
                else
                    color = tpUnlimitedColor;

                VOffset = ((IMinecraftVR)mc).getStereoProvider().getCurrentTimeSecs()*((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.textureScrollSpeed * 0.6;
                ((IMinecraftVR)mc).getTeleportTracker().lastTeleportArcDisplayOffset = VOffset;
            }

            float segmentHalfWidth = ((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.beamHalfWidth * 0.15f;
            int segments = ((IMinecraftVR)mc).getTeleportTracker().movementTeleportArcSteps - 1;
            if (((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.beamGrow)
            {
                segments = (int) ((double) segments * ((IMinecraftVR)mc).getTeleportTracker().movementTeleportProgress);
            }
            double segmentProgress = 1.0 / (double) segments;

            Vec3d up = new Vec3d(0,1,0);

            for (int i=0;i<segments;i++)
            {
                double progress = ((double)i / (double)segments) + VOffset * segmentProgress;
                int progressBase = (int)MathHelper.floor(progress);
                progress -= (float) progressBase;

                Vec3d start = ((IMinecraftVR)mc).getTeleportTracker().getInterpolatedArcPosition((float)(progress - segmentProgress * 0.4f))
                        .subtract(mc.getRenderViewEntity().getPositionVector());


                Vec3d end = ((IMinecraftVR)mc).getTeleportTracker().getInterpolatedArcPosition((float)progress)
                        .subtract(mc.getRenderViewEntity().getPositionVector());

                float shift = (float)progress * 2.0f;
                renderBox(tes, start, end, -segmentHalfWidth, segmentHalfWidth, (-1.0f + shift ) * segmentHalfWidth, (1.0f + shift) * segmentHalfWidth, up, color, alpha);
            }

            tes.draw();
            GlStateManager.disableCull();

            if (validLocation && ((IMinecraftVR)mc).getTeleportTracker().movementTeleportProgress >=1){ //draw landing splash
                Vec3d circlePos = new Vec3d(dest.x, dest.y, dest.z)
                        .subtract(mc.getRenderViewEntity().getPositionVector());

                int side = 1; //vrPlayer.movementTeleportDestinationSideHit;

                float o = 0.01f;

                double x = 0;
                double y = 0;
                double z = 0;

                if (side ==0)   y -= o;
                if (side ==1)   y += o;
                if (side ==2)   z -= o;
                if (side ==3)   z += o;
                if (side ==4)   x -= o;
                if (side ==5)   x += o;
                renderFlatQuad(circlePos.add(x, y, z), .6f,.6f, 0,(int)(color.getX()*1.03), (int)(color.getY()*1.03), (int)(color.getZ()*1.03), isShader ? 255 : 64);
                if (side ==0)   y -= o;
                if (side ==1)   y += o;
                if (side ==2)   z -= o;
                if (side ==3)   z += o;
                if (side ==4)   x -= o;
                if (side ==5)   x += o;
                renderFlatQuad(circlePos.add(x, y, z), .4f,.4f, 0,(int)(color.getX()*1.04), (int)(color.getY()*1.04), (int)(color.getZ()*1.04), isShader ? 255 : 64);
                if (side ==0)   y -= o;
                if (side ==1)   y += o;
                if (side ==2)   z -= o;
                if (side ==3)   z += o;
                if (side ==4)   x -= o;
                if (side ==5)   x += o;
                renderFlatQuad(circlePos.add(x, y, z), .2f,.2f, 0,(int)(color.getX()*1.05), (int)(color.getY()*1.05), (int)(color.getZ()*1.05), isShader ? 255 : 64);
            }

            mc.profiler.endSection(); // teleport arc
        }
    }


    //please push your matrix first. and pop after.
    public void SetupRenderingAtController(int controller){

        Vec3d aimSource = getControllerRenderPos(controller);
        aimSource = aimSource.subtract(mc.getRenderViewEntity().getPositionVector());

        if (aimSource!=null)
        { //move from head to hand origin.
            GL11.glTranslatef(
                    (float) (aimSource.x ),
                    (float) (aimSource.y ),
                    (float) (aimSource.z ));
        }


        GL11.glScalef(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale , ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale , ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale);

        //    	Vector3f fore = new Vector3f(0,0,1);
        //     	Matrix4f rotation = ((IMinecraftVR)mc).getVRPlayer().get.getAimRotation(controller);

        FloatBuffer buf = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(controller).getMatrix().transposed().toFloatBuffer();
        //I have no idea why this has to be transposed.

        if(!((IMinecraftVR)mc).getBowTracker().isDrawing || controller == 0){ //doing this elsewhere
            GL11.glMultMatrix(buf);
        }

    }

    boolean okshader = true;

    // VIVE START - render functions
    public void renderLeftHand(float nano, int pass)
    {
        boolean shadersMod = false;
        boolean shadersModShadowPass = false;

        //from player face to HMD
        setupCameraTransform(nano, 0);
        applyCameraDepth(false);
//        if(shadersMod){
//            shadersModShadowPass = Shaders.isShadowPass;
//            Shaders.beginHand(true);
////        }
//        else {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
//        }

        GL11.glPushMatrix();

        SetupRenderingAtController(1);	//does not push

        mc.getTextureManager().bindTexture(mc.player.getLocationSkin());

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManager.enableRescaleNormal();
        ItemStack item = mc.player.getHeldItemOffhand();

        if(((IMinecraftVR)mc).getClimbTracker().isClimbeyClimb() && (item==null || item.getItem() != Items.SHEARS)){
            itemRenderer.renderItemInFirstPerson(mc.player, nano, 0, EnumHand.OFF_HAND, mc.player.getSwingProgress(nano), mc.player.getHeldItemMainhand(), 0);
        }

        if(BowTracker.isHoldingBow(mc.player, EnumHand.MAIN_HAND)){ //render bow
            int c = 1;
            if (((IMinecraftVR)mc).getVRSettings().vrReverseShootingEye) c = 0;
            itemRenderer.renderItemInFirstPerson(mc.player, nano, 0, EnumHand.OFF_HAND, mc.player.getSwingProgress(nano), mc.player.getHeldItemMainhand(), 0);
        }
        else //just hand
            itemRenderer.renderItemInFirstPerson(mc.player, nano, 0, EnumHand.OFF_HAND, mc.player.getSwingProgress(nano), item, 0);
        GlStateManager.disableRescaleNormal();


        GL11.glPopMatrix();//back to hmd rendering


        setupPolyRendering(true);

        //	TP energy
        if (((IMinecraftVR)mc).getVRSettings().vrLimitedSurvivalTeleport && !((IMinecraftVR)mc).getVRPlayer().getFreeMove() && mc.playerController.isNotCreative() && ((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.arcAiming && !((IMinecraftVR)mc).getBowTracker().isActive(mc.player)){
            GL11.glPushMatrix();
            SetupRenderingAtController(1);	//does not push

            Vec3d start = new Vec3d(0,0.005,.03);

            float r;
            float max = .03f;
            if (((IMinecraftVR)mc).getTeleportTracker().isAiming()) {
                r = 2*(float) ( ((IMinecraftVR)mc).getTeleportTracker().getTeleportEnergy() - 4 * ((IMinecraftVR)mc).getTeleportTracker().movementTeleportDistance  ) / 100 * max;
            } else {
                r = 2*((IMinecraftVR)mc).getTeleportTracker().getTeleportEnergy() / 100 * max;
            }

            if(r<0){r=0;}
            renderFlatQuad(start.add(0, .05001, 0), r,r,0, tpLimitedColor.getX(), tpLimitedColor.getY(), tpLimitedColor.getZ(), 128);
            renderFlatQuad(start.add(0, .05, 0), max,max,0, tpLimitedColor.getX(), tpLimitedColor.getY(), tpLimitedColor.getZ(), 50);

            GL11.glPopMatrix();
        }

        if(!((IMinecraftVR)mc).getVRPlayer().getFreeMove()){ //actually rendered from the head, not hand.
            GL11.glPushMatrix();
            GlStateManager.enableDepth();
            if(((IMinecraftVR)mc).getTeleportTracker().vrMovementStyle.arcAiming) {
                renderTeleportArc(((IMinecraftVR)mc).getVRPlayer());
            } else {
                //renderTeleportLine(((IMinecraftVR)mc).getVRPlayer());
            }
            GL11.glPopMatrix();
        }

        setupPolyRendering(false);

//        if(shadersMod)
//            Shaders.endHand();
//        else {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
//        }
    }

    private void renderMainMenuHands(float partialTicks)
    {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        if (((IMinecraftVR) mc).getCurrentPass() != RenderPass.THIRD && ((IMinecraftVR) mc).getCurrentPass() != RenderPass.CENTER)
        {
            int i = ((IMinecraftVR) mc).getCurrentPass().ordinal();
            if(i>1) i = 0;
            GL11.glMultMatrix(((IMinecraftVR)mc).getStereoProvider().eyeproj[i]);
        } else {
            if (((IMinecraftVR) mc).getCurrentPass() == RenderPass.THIRD) {
                Project.gluPerspective(((IMinecraftVR)mc).getVRSettings().mixedRealityFov, ((IMinecraftVR)mc).getVRSettings().mixedRealityAspectRatio, minClipDistance, clipDistance);
            } else {
                Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float) this.mc.displayHeight, minClipDistance, clipDistance);
            }
        }
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GlStateManager.disableTexture2D();
        if (isInMenuRoom()) {
            GlStateManager.disableAlpha();
            GlStateManager.disableDepth();
        }

        // counter head rotation
        if (((IMinecraftVR) mc).getCurrentPass() != RenderPass.THIRD) {
            GL11.glMultMatrix(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR) mc).getCurrentPass()).getMatrix().toFloatBuffer());
        } else {
            applyMRCameraRotation(false);				}


        //OK SO when world == null the origin is at your face.

        Tessellator tes = Tessellator.getInstance();

        for (int c=0;c<2;c++)
        {

            Vec3i color = new Vec3i((byte)(255 - 127 * c), (byte)(255 - 127 * c), (byte)(255 - 127 * c));
            byte alpha = (byte) 255;
            Vec3d controllerPos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR) mc).getCurrentPass()).getPosition()
                    .subtract(getControllerRenderPos(c));

            GL11.glPushMatrix();

            GL11.glTranslatef(
                    (float)-controllerPos.x,
                    (float)-controllerPos.y,
                    (float)-controllerPos.z);

            Vec3d start = new Vec3d(0,0,0);

//						Matrix4f controllerRotation = mc.lookaimController.getAimRotation(c);
//						Vector3f forward = new Vector3f(0,0,-0.17f);
//						Vector3f dir = controllerRotation.transform(forward);
            Vec3d dir = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(c).getDirection();
            Vec3d up = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(c).getCustomVector(new Vec3d(0, 1, 0));

            float sc = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale;

            Vec3d end = new Vec3d(
                    start.x - dir.x*.18*sc,
                    start.y - dir.y*.18*sc,
                    start.z - dir.z*.18*sc);


            tes.getBuffer().begin(7, DefaultVertexFormats.POSITION_COLOR);
            renderBox(tes, start, end, -0.02f*sc, 0.02f*sc, -0.025f*sc, 0.00f*sc, up, color, alpha);
            tes.draw();
            GL11.glPopMatrix();
        }


        GlStateManager.enableTexture2D();

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
    }

    public void renderFlatQuad(Vec3d pos, float width, float height,float yaw, int r, int g, int b, int a)
    {
        Tessellator tes = Tessellator.getInstance();

        tes.getBuffer().begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);

        Vec3d lr = new Vec3d(-width/2,0, height/2).rotateYaw((float) Math.toRadians(-yaw));
        Vec3d ls = new Vec3d(-width/2,0, -height/2).rotateYaw((float) Math.toRadians(-yaw));
        Vec3d lt = new Vec3d(width/2,0, -height/2).rotateYaw((float) Math.toRadians(-yaw));
        Vec3d lu = new Vec3d(width/2,0, height/2).rotateYaw((float) Math.toRadians(-yaw));

        tes.getBuffer().pos(pos.x + lr.x, pos.y, pos.z + lr.z).color(r, g, b, a).endVertex();
        tes.getBuffer().pos(pos.x + ls.x, pos.y, pos.z + ls.z).color(r, g, b, a).endVertex();
        tes.getBuffer().pos(pos.x + lt.x, pos.y, pos.z + lt.z).color(r, g, b, a).endVertex();
        tes.getBuffer().pos(pos.x + lu.x, pos.y, pos.z + lu.z).color(r, g, b, a).endVertex();

        tes.draw();
    }

    public void renderCircle(Vec3d pos, float radius, int edges, int r, int g, int b, int a, int side)
    {
        Tessellator tes = Tessellator.getInstance();

        tes.getBuffer().begin(GL11.GL_TRIANGLE_FAN,DefaultVertexFormats.POSITION_COLOR);

        tes.getBuffer().pos(pos.x, pos.y, pos.z).color(r, g, b, a).endVertex();

        for (int i=0;i<edges + 1;i++)
        {
            float startAngle;
            startAngle = ( (float) (i) / (float) edges ) * (float) Math.PI * 2.0f;

            if (side == 0 || side == 1) { //y
                float x = (float) pos.x + (float) Math.cos(startAngle) * radius;
                float y = (float) pos.y;
                float z = (float) pos.z + (float) Math.sin(startAngle) * radius;
                tes.getBuffer().pos(x, y, z).color(r, g, b, a).endVertex();
            } else if (side == 2 || side == 3) { //z
                float x = (float) pos.x + (float) Math.cos(startAngle) * radius;
                float y = (float) pos.y + (float) Math.sin(startAngle) * radius;
                float z = (float) pos.z;
                tes.getBuffer().pos(x, y, z).color(r, g, b, a).endVertex();
            } else if (side == 4 || side == 5){ //x
                float x = (float) pos.x ;
                float y = (float) pos.y + (float) Math.cos(startAngle) * radius;
                float z = (float) pos.z + (float) Math.sin(startAngle) * radius;
                tes.getBuffer().pos(x, y, z).color(r, g, b, a).endVertex();
            } else{}

        }

        tes.draw();

    }

    private void renderBox(Tessellator tes, Vec3d start, Vec3d end, float minX, float maxX, float minY, float maxY, Vec3d up, Vec3i color, byte alpha)
    {
        Vec3d forward = start.subtract(end).normalize();
        Vec3d right = forward.crossProduct(up);
        up = right.crossProduct(forward);

        Vec3d left = new Vec3d(
                right.x * minX,
                right.y * minX,
                right.z * minX);

        right = right.scale(maxX);


        Vec3d down = new Vec3d(
                up.x * minY,
                up.y * minY,
                up.z * minY);

        up = up.scale(maxY);


        Vec3d backRightBottom    = start.add(   right.x+down.x,   right.y+down.y,   right.z+down.z);
        Vec3d backRightTop       = start.add(   right.x+up.x,     right.y+up.y,     right.z+up.z);
        Vec3d backLeftBottom     = start.add(   left.x+down.x,    left.y+down.y,    left.z+down.z);
        Vec3d backLeftTop        = start.add(   left.x+up.x,      left.y+up.y,      left.z+up.z);
        Vec3d frontRightBottom   = end.add(     right.x+down.x,   right.y+down.y,   right.z+down.z);
        Vec3d frontRightTop      = end.add(     right.x+up.x,     right.y+up.y,     right.z+up.z);
        Vec3d frontLeftBottom    = end.add(     left.x+down.x,    left.y+down.y,    left.z+down.z);
        Vec3d frontLeftTop       = end.add(     left.x+up.x,      left.y+up.y,      left.z+up.z);

        BufferBuilder b = tes.getBuffer();

        b.pos(backRightBottom.x, backRightBottom.y, backRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backRightTop.x, backRightTop.y, backRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backLeftTop.x, backLeftTop.y, backLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backLeftBottom.x, backLeftBottom.y, backLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();    // back

        b.pos(frontLeftBottom.x, frontLeftBottom.y, frontLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();   // front
        b.pos(frontLeftTop.x, frontLeftTop.y, frontLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontRightTop.x, frontRightTop.y, frontRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontRightBottom.x, frontRightBottom.y, frontRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();

        b.pos(frontRightBottom.x, frontRightBottom.y, frontRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();    // right
        b.pos(frontRightTop.x, frontRightTop.y, frontRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backRightTop.x, backRightTop.y, backRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backRightBottom.x, backRightBottom.y, backRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();

        b.pos(backLeftBottom.x, backLeftBottom.y, backLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex(); // left
        b.pos(backLeftTop.x, backLeftTop.y, backLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontLeftTop.x, frontLeftTop.y, frontLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontLeftBottom.x, frontLeftBottom.y, frontLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();

        b.pos(backLeftTop.x, backLeftTop.y, backLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backRightTop.x, backRightTop.y, backRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontRightTop.x, frontRightTop.y, frontRightTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(frontLeftTop.x, frontLeftTop.y, frontLeftTop.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();       // top

        b.pos(frontLeftBottom.x, frontLeftBottom.y, frontLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();       // bottom
        b.pos(frontRightBottom.x, frontRightBottom.y, frontRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backRightBottom.x, backRightBottom.y, backRightBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
        b.pos(backLeftBottom.x, backLeftBottom.y, backLeftBottom.z).color(color.getX(), color.getY(), color.getZ(), alpha).endVertex();
    }

    //awesome.
    private void renderJrbuddasAwesomeMainMenuRoom() {
        GlStateManager.clearColor(.1f, .1f, .1f, 0.1f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        GlStateManager.disableBlend();
        GlStateManager.color(0.5f, 0.5f, 0.5f, 1f);

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableLight(0);
        GlStateManager.enableCull();
        GlStateManager.enableColorMaterial();
        GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        Minecraft.getMinecraft().renderEngine.bindTexture(Gui.OPTIONS_BACKGROUND);

        //float yo = -camRelY;
        int repeat = 4; // texture wraps per meter
        float height = 2.5f;
        float oversize = 1f;

        float[] area = MCOpenVR.getPlayAreaSize();
        if (area != null) {
            float width = area[0] + oversize;
            float length = area[1] + oversize;
            GL11.glPushMatrix();
            GL11.glTranslatef(-width / 2, 0, -length / 2);
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, setFogColorBuffer(width / 2, 1.8f, length / 2, 1));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, setFogColorBuffer(1.0F, 1.0F, 1.0F, 1.0F));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, setFogColorBuffer(0.2F, 0.2F, 0.2F, 1.0F));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, setFogColorBuffer(1.0F, 1.0F, 1.0F, 1.0F));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_CONSTANT_ATTENUATION, setFogColorBuffer(1.0F, 0, 0, 0));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_LINEAR_ATTENUATION, setFogColorBuffer(0.0F, 0, 0, 0));
            GL11.glLight(GL11.GL_LIGHT0, GL11.GL_QUADRATIC_ATTENUATION, setFogColorBuffer(0.0F, 0, 0, 0));
            GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, setFogColorBuffer(0, 0, 0, 1.0F));
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0, 1, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(0, repeat * length);
            GL11.glVertex3f(0, 0, length);
            GL11.glTexCoord2f(repeat * width, repeat * length);
            GL11.glVertex3f(width, 0, length);
            GL11.glTexCoord2f(repeat * width, 0);
            GL11.glVertex3f(width, 0, 0);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0, -1, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, height, 0);
            GL11.glTexCoord2f(repeat * width, 0);
            GL11.glVertex3f(width, height, 0);
            GL11.glTexCoord2f(repeat * width, repeat * length);
            GL11.glVertex3f(width, height, length);
            GL11.glTexCoord2f(0, repeat * length);
            GL11.glVertex3f(0, height, length);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(1, 0, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(0, repeat * height);
            GL11.glVertex3f(0, height, 0);
            GL11.glTexCoord2f(repeat * length, repeat * height);
            GL11.glVertex3f(0, height, length);
            GL11.glTexCoord2f(repeat * length, 0);
            GL11.glVertex3f(0, 0, length);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(-1, 0, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(width, 0, 0);
            GL11.glTexCoord2f(repeat * length, 0);
            GL11.glVertex3f(width, 0, length);
            GL11.glTexCoord2f(repeat * length, repeat * height);
            GL11.glVertex3f(width, height, length);
            GL11.glTexCoord2f(0, repeat * height);
            GL11.glVertex3f(width, height, 0);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0, 0, 1);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(repeat * width, 0);
            GL11.glVertex3f(width, 0, 0);
            GL11.glTexCoord2f(repeat * width, repeat * height);
            GL11.glVertex3f(width, height, 0);
            GL11.glTexCoord2f(0, repeat * height);
            GL11.glVertex3f(0, height, 0);
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0, 0, -1);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, 0, length);
            GL11.glTexCoord2f(0, repeat * height);
            GL11.glVertex3f(0, height, length);
            GL11.glTexCoord2f(repeat * width, repeat * height);
            GL11.glVertex3f(width, height, length);
            GL11.glTexCoord2f(repeat * width, 0);
            GL11.glVertex3f(width, 0, length);
            GL11.glEnd();
            GL11.glPopMatrix();
        }

        RenderHelper.disableStandardItemLighting();
    }

    private void renderTechjarsAwesomeMainMenuRoom() {
        GlStateManager.color(1f, 1f, 1f, 1f);

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();

        GL11.glPushMatrix();

//        boolean shaders = Shaders.shaderPackLoaded;
//        Shaders.shaderPackLoaded = false;

        int tzOffset = Calendar.getInstance().get(Calendar.ZONE_OFFSET);
        ((IMinecraftVR)mc).getMenuWorldRenderer().time = menuWorldFastTime ? (long)((((IMinecraftVR)mc).getTickCounter() * 10) + 10 * ((IMinecraftVR)mc).getTimer().renderPartialTicks) : (long)((System.currentTimeMillis() + tzOffset - 21600000) / 86400000D * 24000D);
        Vec3d hmd = ((IMinecraftVR)mc).getVRPlayer().vrdata_room_post.hmd.getPosition();
        float posX = (float) hmd.x;
        float posY = (float) (hmd.y + ((IMinecraftVR)mc).getMenuWorldRenderer().getWorld().getGround());
        float posZ = (float) hmd.z;

        Vec3d fogColor = ((IMinecraftVR)mc).getMenuWorldRenderer().getFogColor();
        GlStateManager.clearColor((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        GlStateManager.enableFog();
        GlStateManager.glFog(GL11.GL_FOG_COLOR, setFogColorBuffer((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, 1));
        GlStateManager.setFogStart(((IMinecraftVR)mc).getMenuWorldRenderer().getWorld().getXSize() / 2 - 32);
        GlStateManager.setFogEnd(((IMinecraftVR)mc).getMenuWorldRenderer().getWorld().getXSize() / 2);
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        if (GLContext.getCapabilities().GL_NV_fog_distance) {
            // Makes fog look nicer, but only works on NVIDIA cards. AMD cards require a shader.
            GlStateManager.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, NVFogDistance.GL_EYE_RADIAL_NV);
        }

        ((IMinecraftVR)mc).getMenuWorldRenderer().renderSky(posX, posY, posZ, 2);

        if (posY < 128.0D + (double)(this.mc.gameSettings.clouds * 128.0F)) {
            ((IMinecraftVR)mc).getMenuWorldRenderer().renderClouds(2, posX, posY, posZ);
        }

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();

        ((IMinecraftVR)mc).getMenuWorldRenderer().updateLightmap();
        ((IMinecraftVR)mc).getMenuWorldRenderer().render();

        if (posY >= 128.0D + (double)(this.mc.gameSettings.clouds * 128.0F)) {
            ((IMinecraftVR)mc).getMenuWorldRenderer().renderClouds(2, posX, posY, posZ);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableBlend();
        }

        float[] area = MCOpenVR.getPlayAreaSize();
        if (area != null) {
            float width = (float)Math.ceil(area[0]);
            float length = (float)Math.ceil(area[1]);

            Minecraft.getMinecraft().renderEngine.bindTexture(Gui.OPTIONS_BACKGROUND);
            float sun = ((IMinecraftVR)mc).getMenuWorldRenderer().getSunBrightness();
            GlStateManager.color(sun, sun, sun, 0.3f);
            GL11.glTranslatef(-width / 2, 0, -length / 2);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0, 1, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, 0.001f, 0);
            GL11.glTexCoord2f(0, length);
            GL11.glVertex3f(0, 0.001f, length);
            GL11.glTexCoord2f(width, length);
            GL11.glVertex3f(width, 0.001f, length);
            GL11.glTexCoord2f(width, 0);
            GL11.glVertex3f(width, 0.001f, 0);
            GL11.glEnd();
        }

//        Shaders.shaderPackLoaded = shaders;

        GL11.glPopMatrix();
        GlStateManager.disableFog();
    }

    public Vec3d getEyeBlock(RenderPass eye){
        //this will crash if called before rendering.
        Vec3d cam = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(eye).getPosition();
        return new Vec3d(Math.floor(cam.x), Math.floor(cam.y), Math.floor(cam.z));
    }


    public void renderCrosshairAtDepth()
    {
//        if(Main.viewonly) return;

        if(mc.world == null)
            return;

        if(this.mc.currentScreen != null || RadialHandler.isShowing())
            return;

        boolean crosshairSettings = ((IMinecraftVR)mc).getVRSettings().renderInGameCrosshairMode == VRSettings.RENDER_CROSSHAIR_MODE_ALWAYS ||
                (((IMinecraftVR)mc).getVRSettings().renderInGameCrosshairMode == VRSettings.RENDER_CROSSHAIR_MODE_HUD && !this.mc.gameSettings.hideGUI);

        if(!crosshairSettings)
            return;
        if (((IMinecraftVR) mc).getCurrentPass() == RenderPass.THIRD) //it doesn't look very good.
            return;

        if(KeyboardHandler.Showing)
            return;

        if(RadialHandler.isUsingController(ControllerType.RIGHT))
            return;

        if(this.mc.gameSettings.thirdPersonView != 0)
            return;

        if (((IMinecraftVR)mc).getBowTracker().isDrawing)
            return;

        if (((IMinecraftVR)mc).getTeleportTracker().isAiming())
            return;

        if (((IMinecraftVR)mc).getClimbTracker().isGrabbingLadder(0))
            return;

        if (((IMinecraftVR)mc).getClimbTracker().isClimbeyClimb() && ((IMinecraftVR) mc).getClimbTracker().isGrabbingLadder() && (mc.objectMouseOver==null || mc.objectMouseOver.entityHit == null))
            return;

        this.mc.profiler.endStartSection("crosshair");

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f); //white crosshair, with blending

        Vec3d crosshairRenderPos = crossVec;

        if (this.mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit != RayTraceResult.Type.MISS)
            crosshairRenderPos = this.mc.objectMouseOver.hitVec;

        Vec3d aim = crosshairRenderPos.subtract(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getPosition());

        float crossDepth =(float)aim.length();

        //if (crossDepth > MAX_CROSSHAIR_DISTANCE) crossDepth = MAX_CROSSHAIR_DISTANCE;

        float scale = (float) (0.15f* ((IMinecraftVR)mc).getVRSettings().crosshairScale * Math.sqrt(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale));

        crosshairRenderPos = crosshairRenderPos.add((aim.normalize().scale(-0.01))); //scooch closer a bit for light calc.

        GL11.glPushMatrix();
        Vec3d translate = crosshairRenderPos.subtract(mc.getRenderViewEntity().getPositionVector());
        GL11.glTranslated(translate.x , translate.y , translate.z );

        if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK ) {
            GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getYaw(), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getPitch(), 1.0F, 0.0F, 0.0F);
        } else {
            switch (mc.objectMouseOver.sideHit) {
                case DOWN:
                    GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getYaw(), 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-90, 1.0F, 0.0F, 0.0F);
                    break;
                case EAST:
                    GL11.glRotatef(90, 0.0F, 1.0F, 0.0F);
                    //		GL11.glRotatef(0, 1.0F, 0.0F, 0.0F);
                    break;
                case NORTH:
                    //	GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getYaw(), 0.0F, 1.0F, 0.0F);
                    //		GL11.glRotatef(0, 1.0F, 0.0F, 0.0F);
                    break;
                case SOUTH:
                    //		GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getYaw(), 0.0F, 1.0F, 0.0F);
                    //		GL11.glRotatef(0, 1.0F, 0.0F, 0.0F);
                    break;
                case UP:
                    GL11.glRotatef(-((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getYaw(), 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-90, 1.0F, 0.0F, 0.0F);
                    break;
                case WEST:
                    GL11.glRotatef(90, 0.0F, 1.0F, 0.0F);
                    //GL11.glRotatef(0, 1.0F, 0.0F, 0.0F);
                    break;
                default:
                    break;

            }
        }

        if (false)//(((IMinecraftVR)mc).getVRSettings().crosshairRollsWithHead)
            GL11.glRotated(0, 0.0F, 0.0F, 1.0F);

        if (((IMinecraftVR)mc).getVRSettings().crosshairScalesWithDistance)
        {
            float depthscale = .3f + 0.2f * crossDepth;
            scale *=(depthscale);
        }

        this.enableLightmap();
        GL11.glScalef(scale, scale, scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GL11.glDisable(GL11.GL_CULL_FACE); //goddamnit Lambdalib.

        if (!((IMinecraftVR)mc).getVRSettings().useCrosshairOcclusion)
            GlStateManager.disableDepth();
        else
            GlStateManager.enableAlpha();

//        boolean shadersMod = Config.isShaders();

        GlStateManager.enableBlend(); // Fuck it, we want a proper crosshair
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.colorMask(true, true, true, true);

        int light = mc.world.getCombinedLight(new BlockPos(crosshairRenderPos), 0);

        int j = light >> 16 & 65535;
        int k = light & 65535;

        float brightness = 1.0F;
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit == RayTraceResult.Type.MISS) {
            brightness = 0.5F;
        }

        //System.out.println(light + " " + j + " " + k);

        GlStateManager.color(1, 1, 1, 1);
        this.mc.getTextureManager().bindTexture(Gui.ICONS);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        float var7 = 0.00390625F;
        float var8 = 0.00390625F;

        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_LMAP_COLOR);

        b.pos(- 1, + 1, 0).tex( 0     , 15* var8).lightmap(j, k).color(brightness, brightness, brightness, 1.0F).endVertex();
        b.pos(+ 1, + 1, 0).tex( 15*var7, 15* var8).lightmap(j, k).color(brightness, brightness, brightness, 1.0F).endVertex();
        b.pos(+ 1, - 1, 0).tex( 15*var7, 0       ).lightmap(j, k).color(brightness, brightness, brightness, 1.0F).endVertex();
        b.pos(- 1, - 1, 0).tex( 0      , 0       ).lightmap(j, k).color(brightness, brightness, brightness, 1.0F).endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.color(1, 1, 1, 1);
        GL11.glPopMatrix();

    }

    public void renderFadeEffects()
    {
        //        float overallFade = 0f;
        //        Color3f rgb = new Color3f(0f, 0f, 0f);
        //
        //        // Determine pos track based fade
        //        float posTrackFade = 0f;
        //        if (this.mc.world != null && ((IMinecraftVR)mc).getVRSettings().posTrackBlankOnCollision == true)
        //        {
        //            if (this.headCollision)
        //            {
        //                posTrackFade = 1f;
        //                //this.mc.printChatMessage("Collision");
        //            }
        //            else if (this.headCollisionDistance != -1f && this.headCollisionDistance < this.headCollisionThresholdDistance)
        //            {
        //                posTrackFade = 1f - ((1f / this.headCollisionThresholdDistance) * this.headCollisionDistance);
        //                //this.mc.printChatMessage("Collision in " + fadeBlend);
        //            }
        //            //else
        //             //this.mc.printChatMessage("No collision");
        //        }
        //
        //        float vrComfortFade = 0f;
        //        if (this.mc.world != null && this.mc.lookaimController != null && ((IMinecraftVR)mc).getVRSettings().useVrComfort != VRSettings.VR_COMFORT_OFF)
        //        {
        //            float yawRatchet = (float)this.mc.lookaimController.ratchetingYawTransitionPercent();
        //            float pitchRatchet = (float)this.mc.lookaimController.ratchetingPitchTransitionPercent();
        //
        //            if (((IMinecraftVR)mc).getVRSettings().vrComfortTransitionBlankingMode == VRSettings.VR_COMFORT_TRANS_BLANKING_MODE_BLANK)
        //            {
        //                if (yawRatchet > -1f || pitchRatchet > -1f)
        //                {
        //                    vrComfortFade = 1f;
        //                }
        //            }
        //            else if(((IMinecraftVR)mc).getVRSettings().vrComfortTransitionBlankingMode == VRSettings.VR_COMFORT_TRANS_BLANKING_MODE_FADE)
        //            {
        //                if (yawRatchet > -1f || pitchRatchet > -1f)
        //                {
        //                    vrComfortFade = Math.max(yawRatchet, pitchRatchet);
        //                    if (vrComfortFade < 40f)
        //                    {
        //                        vrComfortFade = (vrComfortFade / 40f);
        //                    }
        //                    else if (vrComfortFade > 60f)
        //                    {
        //                        vrComfortFade = ((100f - vrComfortFade) / 40f);
        //                    }
        //                    else
        //                    {
        //                        vrComfortFade = 1f;
        //                    }
        //                }
        //            }
        //        }
        //
        //        overallFade = Math.max(posTrackFade, vrComfortFade);
        //
        //        if (overallFade > 0f)
        //            renderFadeBlend(rgb, overallFade);
    }


    public Vec3d getControllerRenderPos(int c){
        Vec3d out ;

        if(((IMinecraftVR)mc).getVRSettings().seated){
            if(mc.getRenderViewEntity() != null && mc.world != null){
                Vec3d dir = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getDirection();
                dir = dir.rotateYaw((float) Math.toRadians(c==0?-35:35));
                dir = new Vec3d(dir.x, 0, dir.z);
                dir = dir.normalize();
                RenderPass p = RenderPass.CENTER;
                out = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(p).getPosition().add(dir.x*0.3 * ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale, -0.4* ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale ,dir.z*0.3* ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale);
            } else { //main menu
                Vec3d dir = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getDirection();
                dir = dir.rotateYaw((float) Math.toRadians(c==0?-35:35));
                dir = new Vec3d(dir.x, 0, dir.z);
                dir = dir.normalize();
                out = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getPosition().add(dir.x*0.3 , -0.4 ,dir.z*0.3);
            }
            return out;
        } else {
            return ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(c).getPosition();
        }
    }

    private  void apply2DModelView(RenderPass currentPass, Vec3d guipos, org.vivecraft.utils.Matrix4f guirot)
    {
        mc.profiler.startSection("applyKeyboardModelView");

        Vec3d eye =((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(currentPass).getPosition();

        Vec3d guiLocal = new Vec3d(0, 0, 0);

        float scale = GuiHandler.guiScale;

        //convert previously calculated coords to world coords
        guipos = ((IMinecraftVR)mc).getVRPlayer().room_to_world_pos(guipos, ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render);
        org.vivecraft.utils.Matrix4f rot = org.vivecraft.utils.Matrix4f.rotationY(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.rotation_radians);
        guirot = org.vivecraft.utils.Matrix4f.multiply(rot, guirot);


        // counter head rotation
        if (currentPass != RenderPass.THIRD) {
            GL11.glMultMatrix(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(currentPass).getMatrix().toFloatBuffer());
        } else {
            applyMRCameraRotation(false);
        }


        GL11.glTranslatef((float) (guipos.x - eye.x), (float)(guipos.y - eye.y), (float)(guipos.z - eye.z));
//
//  			// offset from eye to gui pos
        GL11.glMultMatrix(guirot.transposed().toFloatBuffer());
        GL11.glTranslatef((float)guiLocal.x, (float) guiLocal.y, (float)guiLocal.z);

        float thescale = scale * ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale; // * this.mc.vroptions.hudscale
        GlStateManager.scale(thescale, thescale, thescale);


        // Config.isShaders() ? 8 :
        int minLight = 4;
        if(mc.world != null){
            if (((IItemRendererVR)itemRenderer).isInsideOpaqueBlock(guipos, false))
                guipos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getPosition();

            int i = mc.world.getCombinedLight(new BlockPos(guipos), minLight);
            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);

//            if(!Config.isShaders()){
                float b = ((float)k) / 255;
                if (j>k) b = ((float)j) / 255;
                GlStateManager.color(b, b, b); // \_(oo)_/
//            }
        }

        mc.profiler.endSection();

    }

    private Vec3d applyPhysicalKeyboardModelView(RenderPass currentPass, Vec3d guipos, org.vivecraft.utils.Matrix4f guirot) {
        mc.profiler.startSection("applyPhysicalKeyboardModelView");

        Vec3d eye = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(currentPass).getPosition();

        //convert previously calculated coords to world coords
        guipos = ((IMinecraftVR)mc).getVRPlayer().room_to_world_pos(guipos, ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render);
        org.vivecraft.utils.Matrix4f rot = org.vivecraft.utils.Matrix4f.rotationY(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.rotation_radians);
        guirot = org.vivecraft.utils.Matrix4f.multiply(rot, guirot);

        // counter head rotation
        if (currentPass != RenderPass.THIRD) {
            GL11.glMultMatrix(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(currentPass).getMatrix().toFloatBuffer());
        } else {
            applyMRCameraRotation(false);
        }

        // offset from eye to gui pos
        GlStateManager.translate((float) (guipos.x - eye.x), (float) (guipos.y - eye.y), (float) (guipos.z - eye.z));
        GlStateManager.multMatrix(guirot.transposed().toFloatBuffer());

        float scale = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale;
        GlStateManager.scale(scale, scale, scale);

        mc.profiler.endSection();

        return guipos;
    }

    private void applyMenuRoomModelView(RenderPass currentPass)
    {
        Vec3d eye = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR) mc).getCurrentPass()).getPosition();

        // counter head rotation
        if (currentPass != RenderPass.THIRD) {
            GL11.glMultMatrix(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(currentPass).getMatrix().toFloatBuffer());
        } else {
            applyMRCameraRotation(false);		}

        GL11.glTranslatef((float)(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.origin.x - eye.x), (float)(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.origin.y - eye.y), (float)(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.origin.z - eye.z));
    }

    private int polyblendsrc, polyblenddst;
//    private Program prog;
    private boolean polyblend, polytex, polylight, polycull;

    private void setupPolyRendering(boolean enable){
//        boolean shadersMod = Config.isShaders();
        boolean shadersModShadowPass = false;
        if(enable){
            //TODO: SOLVE HOW TO GET THESE
//            polyblendsrc = GlStateManager.blendState.srcFactor;
//            polyblenddst = GlStateManager.blendState.dstFactor;
            polyblend = GL11.glIsEnabled(GL11.GL_BLEND);
            polytex =  GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            polylight =  GL11.glIsEnabled(GL11.GL_LIGHTING);
            polycull =  GL11.glIsEnabled(GL11.GL_CULL_FACE);

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
//            if(shadersMod){
//                prog  = Shaders.activeProgram;
//                Shaders.useProgram(Shaders.ProgramNone);
//            }
        } else {
            GlStateManager.blendFunc(polyblendsrc, polyblenddst);
            if (!polyblend) GlStateManager.disableBlend();
            if (polytex) GlStateManager.enableTexture2D();
            if (polylight) GlStateManager.enableLighting();
            if (polycull) GlStateManager.enableCull();
//            if(shadersMod && polytex)
//                Shaders.useProgram(prog);
        }
    }

    public double rveX, rveY, rveZ, rvelastX, rvelastY, rvelastZ, rveprevX, rveprevY, rveprevZ;
    private float rveyaw, rvepitch, rvelastyaw, rvelastpitch;
    private boolean cached;

    public void setupRVE() {
        if (!cached)
            return;

        Vec3d f;
        if(((IMinecraftVR) mc).getCurrentPass() == RenderPass.THIRD){
            f = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.THIRD).getPosition();
        }
        else{
            f = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.CENTER).getPosition();
        }

        EntityLivingBase player = (EntityLivingBase) this.mc.getRenderViewEntity();

        player.posX = f.x;
        player.posY = f.y;
        player.posZ = f.z;
        player.lastTickPosX = f.x;
        player.lastTickPosY = f.y;
        player.lastTickPosZ = f.z;
        player.prevPosX = f.x;
        player.prevPosY = f.y;
        player.prevPosZ = f.z;
        player.rotationPitch =player.prevRotationPitch = -((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getPitch();
        player.prevRotationYawHead	= player.prevRotationYaw  = player.rotationYaw = player.rotationYawHead = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getYaw();
        if(player instanceof EntityPlayerSP)
            ((IEntityPlayerSPVR)((EntityPlayerSP)player)).setOverrideEyeHeight(true);
    }


    public void cacheRVEPos(EntityLivingBase e){
        if (mc.getRenderViewEntity() == null) return;
        if (cached)
            return;
        rveX = e.posX;
        rveY = e.posY;
        rveZ = e.posZ;
        rvelastX = e.lastTickPosX;
        rvelastY = e.lastTickPosY;
        rvelastZ = e.lastTickPosZ;
        rveprevX = e.prevPosX;
        rveprevY = e.prevPosY;
        rveprevZ = e.prevPosZ;
        rveyaw = e.rotationYawHead;
        rvepitch = e.rotationPitch;
        rvelastyaw = e.prevRotationYawHead;
        rvelastpitch = e.prevRotationPitch;
        cached = true;
    }

    public void restoreRVEPos(EntityLivingBase e){
        if (e == null) return;
        e.posX = rveX;
        e.posY = rveY;
        e.posZ = rveZ;
        e.lastTickPosX = rvelastX;
        e.lastTickPosY = rvelastY;
        e.lastTickPosZ = rvelastZ;
        e.prevPosX = rveprevX;
        e.prevPosY = rveprevY;
        e.prevPosZ = rveprevZ;
        e.rotationYaw = rveyaw;
        e.rotationPitch = rvepitch;
        e.prevRotationYaw = rvelastyaw;
        e.prevRotationPitch = rvelastpitch;
        e.rotationYawHead = rveyaw;
        e.prevRotationYawHead = rvelastyaw;
        if(e instanceof EntityPlayerSP)
            ((IEntityPlayerSPVR)((EntityPlayerSP)e)).setOverrideEyeHeight(false);

        cached = false;
    }

    private Vec3d getRVEPositionEyes(float partialTicks) {
        if (mc.getRenderViewEntity() == null) return new Vec3d(0, 0, 0);
        if (cached) {
            if (partialTicks == 1.0F) {
                return new Vec3d(rveX, rveY + mc.getRenderViewEntity().getEyeHeight(), rveZ);
            } else {
                double d0 = rveprevX + (rveX - rveprevX) * partialTicks;
                double d1 = rveprevY + (rveY - rveprevY) * partialTicks + mc.getRenderViewEntity().getEyeHeight();
                double d2 = rveprevZ + (rveZ - rveprevZ) * partialTicks;
                return new Vec3d(d0, d1, d2);
            }
        } else {
            return mc.getRenderViewEntity().getPositionEyes(partialTicks);
        }
    }

    public boolean isInMenuRoom() {
        return this.mc.world == null || mc.currentScreen instanceof GuiWinGame || ((IMinecraftVR)mc).getIntegratedServerLaunchInProgress();
    }

    private void renderFaceOverlay(float par1){ //replaced with shader
//        boolean shadersMod = Config.isShaders();
//        if (shadersMod) {
//            //just disables caps
//            Shaders.beginFPOverlay();
//        }

        this.itemRenderer.renderOverlays(par1);

//        if (shadersMod) {
//            //does nothing at all.
//            Shaders.endFPOverlay();
//        }
    }


    private void renderVRThings(float par1){

        if (this.mc.gameSettings.thirdPersonView == 0)
        {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            renderItemActivation(0, 0, par1); //totem of undying

            GlStateManager.disableCull();

            AxisAlignedBB bb = mc.player.getEntityBoundingBox();
            if(((IMinecraftVR)mc).getVRSettings().vrShowBlueCircleBuddy && bb != null){ 	//blue circle buddy	  - have to draw here so it sits on top of overlays (face in block)
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GlStateManager.loadIdentity();

                setupCameraTransform(par1, 0);
                applyCameraDepth(false);

                Vec3d o = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.CENTER).getPosition();
                if (((IMinecraftVR) mc).getCurrentPass() == RenderPass.THIRD)
                    o=((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.THIRD).getPosition();

                EntityPlayerSP player = mc.player;

                Vec3d interpolatedPlayerPos = new Vec3d(
                        rvelastX + (rveX - rvelastX) * (double)par1,
                        rvelastY + (rveY - rvelastY) * (double)par1,
                        rvelastZ + (rveZ - rvelastZ) * (double)par1
                );

                Vec3d pos = interpolatedPlayerPos.subtract(o).add(0, 0.005, 0);
                setupPolyRendering(true);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.disableDepth();
                renderFlatQuad(pos,
                        (float)(bb.maxX - bb.minX),
                        (float) (bb.maxZ - bb.minZ),
                        0, 0, 255, 255, 64);
                GlStateManager.enableDepth();
                setupPolyRendering(false);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
        }


        if(((IMinecraftVR) mc).getCurrentPass() == RenderPass.LEFT || ((IMinecraftVR) mc).getCurrentPass() == RenderPass.RIGHT){
            if(((IMinecraftVR)mc).getVRSettings().displayMirrorMode == ((IMinecraftVR)mc).getVRSettings().MIRROR_MIXED_REALITY || ((IMinecraftVR)mc).getVRSettings().displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) {

                //render the camera
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();

                setupCameraTransform(par1, 0);
                applyCameraDepth(false);

                Vec3d cam = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.THIRD).getPosition();
                Vec3d o = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(RenderPass.CENTER).getPosition();
                Vec3d pos = cam.subtract(o);

                GL11.glTranslated(pos.x, pos.y, pos.z);
                applyMRCameraRotation(true);
                GL11.glRotated(180, 0, 1, 0);
                renderDebugAxes(0, 0, 0, 0.08f * ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.worldScale); //TODO: camera model?

                GL11.glTranslatef(0, 0.125f, 0);

                ((IMinecraftVR)mc).getStereoProvider().framebufferMR.bindFramebufferTexture();
                drawSizedQuad(320, 200, 0.25f);

                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
        }

    }

    public void applyMRCameraRotation(boolean invert){
        ((IMinecraftVR)mc).getMRTransform();
        org.lwjgl.util.vector.Matrix4f view = new org.lwjgl.util.vector.Matrix4f(((IMinecraftVR)mc).getThirdPassViewMatrix());
        view.m30 = 0;
        view.m31 = 0;
        view.m32 = 0;
        if(!invert) view = (org.lwjgl.util.vector.Matrix4f) view.invert(); //yea yea i know.
        view.store(matrixBuffer);
        matrixBuffer.rewind();
        GlStateManager.multMatrix(matrixBuffer);
        matrixBuffer.rewind();
    }

    private void renderViveHudIcons(){
        //VIVE SPRINTDICATOR
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer)
        {
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            FontRenderer fontrenderer = mc.ingameGUI.getFontRenderer();
            this.mc.entityRenderer.setupOverlayRendering();
            EntityPlayer entityplayer = (EntityPlayer)this.mc.getRenderViewEntity();
            int iconp = 0;
            if(entityplayer.isSprinting()) iconp = 10;
            if(entityplayer.isSneaking()) iconp = 13;
            if(entityplayer.isElytraFlying()) iconp = -1;
            if(iconp!=0){
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                if(iconp ==-1){
                    int w = scaledresolution.getScaledWidth() / 2 - 109;
                    int h = scaledresolution.getScaledHeight() -39;
                    TextureAtlasSprite textureatlassprite = this.mc.getTextureMapBlocks().getAtlasSprite("minecraft:items/elytra");
                    this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                    mc.ingameGUI.drawTexturedModalRect(w, h, textureatlassprite, 16, 16);
                }else{
                    this.mc.getTextureManager().bindTexture(((IGUIIngameVR)mc.ingameGUI).getInventoryBackground());
                    int w = scaledresolution.getScaledWidth() / 2 - 109;
                    int h = scaledresolution.getScaledHeight() -39;
                    mc.ingameGUI.drawTexturedModalRect(w, h, 0 + iconp % 8 * 18, 198 + iconp / 8 * 18, 18, 18);
                }
            }
        }
        //
    }

    private void updateCameraAndRender_OnePass(float par1) {

//        this.frameInit();

        setupClipPlanes();

        this.mc.mouseHelper.deltaX = 1;
        this.mc.mouseHelper.deltaY = 1;
        this.mc.getTutorial().handleMouse(this.mc.mouseHelper);

//        Config.checkDisplayMode();

        if (this.mc.getRenderViewEntity() == null)
        {
            this.mc.setRenderViewEntity(this.mc.player);
        }

        if(this.mc.getIntegratedServer() != null)
            if (!this.mc.getIntegratedServer().isWorldIconSet())
            {
                this.createWorldIcon();
            }

        if(mc.currentScreen == null ){
            // VIVE START - teleport movement
            ((IMinecraftVR)mc).getTeleportTracker().updateTeleportDestinations((EntityRenderer)(IEntityRendererVR) this, mc, mc.getRenderViewEntity());
            // VIVE END - teleport movement
        }
    }

    private static void drawText(FontRenderer fontRendererIn, String str, float x, float y, float z, float viewerYaw, float viewerPitch)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) viewerPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        int i = fontRendererIn.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos((double)(-i - 1), (double)(-1 ), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos((double)(-i - 1), (double)(8 ), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos((double)(i + 1), (double)(8 ), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        bufferbuilder.pos((double)(i + 1), (double)(-1 ), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        if (true)
        {
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, 0, 553648127);
            GlStateManager.enableDepth();
        }

        GlStateManager.depthMask(true);
        fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, 0, -1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    // VIVE END - render functions


}
