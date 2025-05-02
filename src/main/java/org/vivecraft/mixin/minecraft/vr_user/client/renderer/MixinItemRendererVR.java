package org.vivecraft.mixin.minecraft.vr_user.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.*;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.control.ControllerType;
import org.vivecraft.gameplay.trackers.BowTracker;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IItemRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IRenderItemVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.render.RenderPass;
import org.vivecraft.render.RenderVRPlayer;
import org.vivecraft.utils.Vector3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mixin(ItemRenderer.class)
public class MixinItemRendererVR implements IItemRendererVR {

    private RenderVRPlayer myRenderVRPlayer;
    private Map<String, RenderVRPlayer> renderVRPlayerSkinMap = new HashMap<>();

    @Shadow
    private Minecraft mc;
    @Shadow
    private ItemStack itemStackMainHand;
    @Shadow
    private ItemStack itemStackOffHand;
    @Shadow
    private float equippedProgressMainHand;
    @Shadow
    private float prevEquippedProgressMainHand;
    @Shadow
    private float equippedProgressOffHand;
    @Shadow
    private float prevEquippedProgressOffHand;
    @Shadow
    private RenderManager renderManager;
    @Shadow
    private RenderItem itemRenderer;

    @Override
    public void triggerSetLightmap() {
        setLightmap();
    }

    public float getEquipProgress(EnumHand hand, float partialTicks){
        if(hand == EnumHand.MAIN_HAND)
            return 1.0f- (this.prevEquippedProgressMainHand + (this.equippedProgressMainHand - this.prevEquippedProgressMainHand) * partialTicks);
        else
            return 1.0F - (this.prevEquippedProgressOffHand + (this.equippedProgressOffHand - this.prevEquippedProgressOffHand) * partialTicks);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void addToConstructor(Minecraft mcIn, CallbackInfo ci) {
        myRenderVRPlayer = new RenderVRPlayer(this.renderManager);
        renderVRPlayerSkinMap.put("default", this.myRenderVRPlayer);
        renderVRPlayerSkinMap.put("slim", new RenderVRPlayer(this.renderManager, true));
    }

    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform)
    {
        this.renderItemSide(0,entityIn, heldStack, transform, false);
    }

    public void renderItemSide(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform, boolean rightSide)
    {
        this.renderItemSide(0,entityIn, heldStack, transform, rightSide);
    }

    public void renderItemSide(float par1, EntityLivingBase entitylivingbaseIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform, boolean rightSide)
    {
        if (!heldStack.isEmpty())
        {
            Item item = heldStack.getItem();
            Block block = Block.getBlockFromItem(item);
            GlStateManager.pushMatrix();
            boolean flag = this.itemRenderer.shouldRenderItemIn3D(heldStack) && block.getRenderLayer() == BlockRenderLayer.TRANSLUCENT;

//            if (flag)
//            {
//                GlStateManager.depthMask(false);
//            }

            ((IRenderItemVR)this.itemRenderer).renderItem(par1, heldStack, entitylivingbaseIn, transform, rightSide);

//            if (flag)
//            {
//                GlStateManager.depthMask(true);
//            }

            GlStateManager.popMatrix();
        }
    }

    @Overwrite
    private void renderMapFirstPersonSide(float p_187465_1_, EnumHandSide hand, float p_187465_3_, ItemStack stack)
    {
        float f = hand == EnumHandSide.RIGHT ? 1.0F : -1.0F;

        if (!this.mc.player.isInvisible())
        {
            //            GlStateManager.pushMatrix();
            //            GlStateManager.rotate(f * 10.0F, 0.0F, 0.0F, 1.0F);
            //            this.renderArmFirstPerson(p_187465_1_, p_187465_3_, p_187465_2_);
            //            GlStateManager.popMatrix();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(-f * 0F, 0.225F, -0.1F);
        GlStateManager.rotate(-30.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-30.0F*f, 0.0F, 1.0F, 0.0F);
        //  GlStateManager.translate(f * 0.51F, -0.08F + p_187465_1_ * -1.2F, -0.75F);
        float f1 = MathHelper.sqrt(p_187465_3_);
        float f2 = MathHelper.sin(f1 * (float)Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * MathHelper.sin(f1 * ((float)Math.PI * 2F));
        float f5 = -0.3F * MathHelper.sin(p_187465_3_ * (float)Math.PI);
        //	       GlStateManager.translate(f * f3, f4 - 0.3F * f2, f5);
        //	        GlStateManager.rotate(f2 * -45.0F, 1.0F, 0.0F, 0.0F);
        //	       GlStateManager.rotate(f * f2 * -30.0F, 0.0F, 1.0F, 0.0F);
        this.renderMapFirstPerson(stack);
        GlStateManager.popMatrix();
    }

    @Overwrite
    private void renderMapFirstPerson(float p_187463_1_, float p_187463_2_, float p_187463_3_)
    {
        float f = MathHelper.sqrt(p_187463_3_);
        float f1 = -0.2F * MathHelper.sin(p_187463_3_ * (float)Math.PI);
        float f2 = -0.4F * MathHelper.sin(f * (float)Math.PI);
        //GlStateManager.translate(0.0F, -f1 / 2.0F, f2);
        float f3 = this.getMapAngleFromPitch(p_187463_1_);
        //  GlStateManager.translate(0.0F, 0.04F + p_187463_2_ * -1.2F + f3 * -0.5F, -0.72F);
        //GlStateManager.rotate(f3 * -85.0F, 1.0F, 0.0F, 0.0F);
        // this.renderArms();
        float f4 = MathHelper.sin(f * (float)Math.PI);
        // GlStateManager.rotate(f4 * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-f * 0F, 0.225F, -0.5F);
        GlStateManager.scale(1.5F, 1.5F, 1.5F);
        GlStateManager.rotate(-30.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-30.0F*f, 0.0F, 1.0F, 0.0F);
        this.renderMapFirstPerson(this.itemStackMainHand);
    }

    @Overwrite
    private void renderArmFirstPerson(float equipProgress, float swingProgress, EnumHandSide side)
    { //render arm

        boolean flag = side != EnumHandSide.LEFT;
        float f = flag ? 1.0F : -1.0F;
        AbstractClientPlayer abstractclientplayer = this.mc.player;
        this.mc.getTextureManager().bindTexture(abstractclientplayer.getLocationSkin());

//        float f1 = MathHelper.sqrt_float(swingProgress);
//        float f2 = -0.3F * MathHelper.sin(f1 * (float)Math.PI);
//        float f3 = 0.4F * MathHelper.sin(f1 * ((float)Math.PI * 2F));
//        float f4 = -0.4F * MathHelper.sin(swingProgress * (float)Math.PI);
//        GlStateManager.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equipProgress * -0.6F, f4 + -0.71999997F);
//        GlStateManager.rotate(f * 45.0F, 0.0F, 1.0F, 0.0F);
//        float f5 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
//        float f6 = MathHelper.sin(f1 * (float)Math.PI);
//        GlStateManager.rotate(f * f6 * 70.0F, 0.0F, 1.0F, 0.0F);
//        GlStateManager.rotate(f * f5 * -20.0F, 0.0F, 0.0F, 1.0F);
//        GlStateManager.translate(f * -1.0F, 3.6F, 3.5F);
//        GlStateManager.rotate(f * 120.0F, 0.0F, 0.0F, 1.0F);
//        GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
//        GlStateManager.rotate(f * -135.0F, 0.0F, 1.0F, 0.0F);
//        GlStateManager.translate(f * 5.6F, 0.0F, 0.0F);
        //VIVE this is all thats needed to align hands with controllers.
        //TODO: Animation.

        GlStateManager.pushMatrix();

        this.transformFirstPerson(side, swingProgress);

        GlStateManager.scale(0.4f, 0.4F, 0.4F);
        GlStateManager.translate(0.375*-f, 0, .75);
        GlStateManager.rotate(-90, 1, 0,0);

        GlStateManager.rotate(180, 0, 1, 0);

        RenderVRPlayer renderVRPlayer = renderVRPlayerSkinMap.get(abstractclientplayer.getSkinType());
        if (renderVRPlayer == null) renderVRPlayer = myRenderVRPlayer;
        if (flag)
        {
            renderVRPlayer.renderRightArm(abstractclientplayer);
        }
        else
        {
            renderVRPlayer.renderLeftArm(abstractclientplayer);
        }
        GlStateManager.popMatrix();
        GlStateManager.enableCull();
    }

    @Overwrite
    private void transformEatFirstPerson(float progress, EnumHandSide side, ItemStack item)
    {
        float f = (float)this.mc.player.getItemInUseCount() - progress + 1.0F;
        float f1 = f / (float)item.getMaxItemUseDuration();

        if (f1 < 0.8F)
        {
            float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float)Math.PI) * 0.1F);
            //      GlStateManager.translate(0.0F, f2, 0.0F);
        }

        if(!((IMinecraftVR)Minecraft.getMinecraft()).getEatingTracker().isEating()) {
            float f3 = 1.0F - (float) Math.pow((double) f1, 27.0D);
            int i = side == EnumHandSide.RIGHT ? 1 : -1;
            //  GlStateManager.translate(f3 * 0.6F * (float)i, f3 * -0.5F, f3 * 0.0F);
            GlStateManager.rotate((float) i * f3 * 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate((float) i * f3 * 30.0F, 0.0F, 0.0F, 1.0F);
        } else { //OM NOM NOM NOM
            long t = this.mc.player.getItemInUseCount();
            GlStateManager.translate(0,0.006*Math.sin(t), 0);
        }
    }


    @Overwrite
    private void transformFirstPerson(EnumHandSide side, float swingprogress)
    {
        if(swingprogress == 0) return;
        //VIVE TODO: SOMETHING
        int i = side == EnumHandSide.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingprogress * swingprogress * (float)Math.PI);
        //  GlStateManager.rotate((float)i * (45.0F + f * -20.0F), 0.0F, 1.0F, 0.0F);
        float f1 = 0.5f * MathHelper.sin(MathHelper.sqrt(swingprogress) * (float)Math.PI);
        float 	f2= MathHelper.sin((float) (swingprogress *3*Math.PI));
        if(swingprogress > 0.5) {
            f2= MathHelper.sin((float) (swingprogress *Math.PI + Math.PI));
        }
        GlStateManager.translate(0,0,-f1);
        //        GlStateManager.rotate((float)i * f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate((f2) * 45.0F  , 1.0F, 0.0F, 0.0F);
        //        GlStateManager.rotate((float)i * -45.0F, 0.0F, 1.0F, 0.0F);
    }

    //better name would be doEquipItemAnimation
    @Overwrite
    private void transformSideFirstPerson(EnumHandSide side, float equippingprogress)
    {
        //VIVE NOOP
//        int i = side == EnumHandSide.RIGHT ? 1 : -1;
//        GlStateManager.translate((float)i * 0.56F, -0.52F + equippingprogress * -0.6F, -0.72F);
    }

    @Overwrite
    public void renderItemInFirstPerson(AbstractClientPlayer player, float partialTicks, float bodyPitch, EnumHand hand, float swingProgress, @Nullable ItemStack itemstack, float equippingprogress)
    {
        boolean mainHand = hand == EnumHand.MAIN_HAND;
        EnumHandSide enumhandside = mainHand ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        equippingprogress = getEquipProgress(hand, partialTicks);

        boolean shouldrenderhand = true;
        if(((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD && ((IMinecraftVR)mc).getVRSettings().mixedRealityRenderHands == false)
            shouldrenderhand = false;

        if(BowTracker.isBow(itemstack))
            shouldrenderhand = false;


        if (shouldrenderhand && !player.isInvisible())
        {
            this.renderArmFirstPerson(equippingprogress, player.swingingHand == hand ? swingProgress : 0, enumhandside);
        }

        if(itemstack!=null){
            GlStateManager.pushMatrix();
            boolean thing = false;

            if (itemstack.getItem() instanceof ItemMap)
            {
                if (mainHand && this.itemStackOffHand == null)
                {
                    this.renderMapFirstPerson(bodyPitch, equippingprogress, swingProgress);
                }
                else
                {
                    this.renderMapFirstPersonSide(equippingprogress, enumhandside, swingProgress, itemstack);
                }
            }
            else
            {
                boolean rightSide = enumhandside == EnumHandSide.RIGHT;
                int i = rightSide ? 1 : -1;

                if (player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == hand)
                { //using animations
                    int j = rightSide ? 1 : -1;

                    switch (itemstack.getItemUseAction())
                    {
                        case NONE:
                            this.transformSideFirstPerson(enumhandside, equippingprogress);
                            break;

                        case EAT:
                        case DRINK:
                            this.transformEatFirstPerson(partialTicks, enumhandside, itemstack);
                            this.transformSideFirstPerson(enumhandside, equippingprogress);
                            break;

                        case BLOCK:
                            GlStateManager.scale(1.2, 1.2, 1.2);
                            if(player.isActiveItemStackBlocking()){
                                GlStateManager.rotate(i*90.0F, 0.0F, 1.0F, 0.0F);
                            } else{
                                GlStateManager.rotate((1-equippingprogress)*i*90.0F, 0.0F, 1.0F, 0.0F);
                            }

                            this.transformSideFirstPerson(enumhandside, equippingprogress);
                            break;

                        case BOW:
                            //	    					this.doEquipItemAnimation(enumhandside, equippingprogress);
                            //	    					GlStateManager.translate((float)j * -0.2785682F, 0.18344387F, 0.15731531F);
                            //	    					GlStateManager.rotate(-13.935F, 1.0F, 0.0F, 0.0F);
                            //	    					GlStateManager.rotate((float)j * 35.3F, 0.0F, 1.0F, 0.0F);
                            //	    					GlStateManager.rotate((float)j * -9.785F, 0.0F, 0.0F, 1.0F);
                            //	    					float f5 = (float)itemstack.getMaxItemUseDuration() - ((float)this.mc.player.getItemInUseCount() - partialTicks  1.0F);
                            //	    					float f6 = f5 / 20.0F;
                            //	    					f6 = (f6 * f6  f6 * 2.0F) / 3.0F;
                            //
                            //	    					if (f6 > 1.0F)
                            //	    					{
                            //	    						f6 = 1.0F;
                            //	    					}
                            //
                            //	    					if (f6 > 0.1F)
                            //	    					{
                            //	    						float f7 = MathHelper.sin((f5 - 0.1F) * 1.3F);
                            //	    						float f3 = f6 - 0.1F;
                            //	    						float f4 = f7 * f3;
                            //	    						GlStateManager.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
                            //	    					}
                            //
                            //	    					GlStateManager.translate(f6 * 0.0F, f6 * 0.0F, f6 * 0.04F);
                            //	    					GlStateManager.scale(1.0F, 1.0F, 1.0F  f6 * 0.2F);
                            //	    					GlStateManager.rotate((float)j * 45.0F, 0.0F, -1.0F, 0.0F);
                            break;
                        default:
                            break;
                    }
                }

                {
                    float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
                    float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float)Math.PI * 2F));
                    float f2 = -0.2F * MathHelper.sin(swingProgress * (float)Math.PI);

                    //GlStateManager.translate((float)i * f, f1, f2);
                    this.transformSideFirstPerson(enumhandside, equippingprogress);

                    if(player.swingingHand == hand)
                        this.transformFirstPerson(enumhandside, swingProgress);

                    //VIVE manually adjust items based on type... no other choice :(

                    Item item =  itemstack.getItem();

                    boolean vive = !MCOpenVR.isGunStyle();

                    boolean sword = false, tool = false;
                    boolean bow = BowTracker.isBow(itemstack) && ((IMinecraftVR)mc).getBowTracker().isActive((EntityPlayerSP) player);

                    if(bow && item.getClass().getName().equals("FryPan")){
                        bow = false;
                        tool = true;
                    }

                    // TODO: I'll be adding proper implementation for this kind of thing later
//                    if(!bow && Reflector.forgeExists()){ //tinkers
//                        String t = item.getClass().getSuperclass().getName().toLowerCase();
//                        if (t.contains("weapon") || t.contains("sword")) {
//                            sword = true;
//                            tool = true;
//                        } else 	if 	(t.contains("tool")){
//                            tool = true;
//                        }
//                    }

                    if(item instanceof ItemBlock){

                        if (this.itemRenderer.shouldRenderItemIn3D(itemstack)){
                            GlStateManager.translate(0f, 0f, -0.1f);
                            GlStateManager.scale(0.2, 0.2, 0.2);
                        }else if(((ItemBlock) item).getBlock() == Blocks.TORCH || ((ItemBlock) item).getBlock() == Blocks.REDSTONE_TORCH) {
                            GlStateManager.translate(0f, 0.05f, -0.2f);
                            GlStateManager.scale(0.6, 0.6, 0.6);
                            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                            GlStateManager.rotate(-75.0F, 0.0F, 0.0F, 1.0F);
                        }else{
                            GlStateManager.translate(0f, 0f, -0.15f);
                            GlStateManager.scale(0.3, 0.3, 0.3);
                            GlStateManager.rotate(-i*45.0F, 0.0F, 1.0F, 0.0F);
                        }
                    }
                    else if(item instanceof ItemSword || sword)
                    {
                        if(vive){
                            GlStateManager.translate(0f, 0f, -0.2f);
                            GlStateManager.scale(0.6, 0.6, 0.6);
                            GlStateManager.rotate(-45F, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                        } else {
                            GlStateManager.translate(0f, 0.10f, -0.125f);
                            GlStateManager.scale(0.6, 0.6, 0.6);
                            GlStateManager.rotate(-45F + 39.4f, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                        }

                    }
                    else if(item instanceof ItemTool ||
                            item instanceof ItemArrow ||
                            item instanceof ItemHoe ||
                            item instanceof ItemFishingRod ||
                            item instanceof ItemCarrotOnAStick ||
                            item instanceof ItemShears||
                            item instanceof ItemFlintAndSteel ||
                            tool)

                    {
                        boolean climbClaws = ((IMinecraftVR)mc).getClimbTracker().isClaws(itemstack) && ((IMinecraftVR)mc).getClimbTracker().isClimbeyClimb();

                        if(climbClaws){

                            GlStateManager.scale(0.3, 0.3, 0.3);
                            GlStateManager.translate(-.025f, .12f, .25f);
                            GlStateManager.rotate(90, 0, 0, 1);

                            if((MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.RIGHT) && rightSide) || (MCOpenVR.keyClimbeyGrab.isKeyDown(ControllerType.LEFT) && !rightSide)) {
                                GlStateManager.translate(0f, 0f, -.2f);
                            }

                        }

                        if(vive || climbClaws){
                            GlStateManager.translate(0f, -.025f, -0.1f);
                            GlStateManager.scale(0.6, 0.6, 0.6);
                            if(item instanceof ItemCarrotOnAStick || item instanceof ItemFishingRod) {}
                            else
                                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                            GlStateManager.rotate(-45F, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                        } else {
                            GlStateManager.translate(0f, 0.035, -0.1f);
                            GlStateManager.scale(0.6, 0.6, 0.6);
                            if(item instanceof ItemCarrotOnAStick || item instanceof ItemFishingRod) {
                                GlStateManager.rotate(39.4f, 1.0F, 0.0F, 0.0F);
                            }
                            else
                                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                            GlStateManager.rotate(-45F - 39.4f, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                        }


                    }
                    else if(item instanceof ItemShield){
                        GlStateManager.scale(0.4, 0.4, 0.4);
                        GlStateManager.rotate(i*90.0F, 0.0F, 1.0F, 0.0F);
                        GlStateManager.translate(.5,0.5,.6);
                    }
                    else if(bow){
                        GlStateManager.scale(1.0f, 1.0f, 1.0f);
                        if(((IMinecraftVR)mc).getBowTracker().isDrawing){ //here there be dragons

                            int c = 0;
                            if (((IMinecraftVR)mc).getVRSettings().vrReverseShootingEye) c = 1;

                            Vec3d aim = ((IMinecraftVR)mc).getBowTracker().getAimVector();
                            Vec3d a = new Vec3d(aim.x, aim.y, aim.z);
                            //a.rotateAroundY(-mc.vrSettings.vrWorldRotation);
                            Vector3 aimCopy = new Vector3((float)a.x,(float) a.y, (float)a.z);

                            //Matrix4f left = mc.lookaimController.getAimRotation(1);

                            Vec3d lup = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(1).getCustomVector(new Vec3d(0, -1, 0));
                            Vector3 current = new Vector3((float)lup.x, (float)lup.y, (float)lup.z);
                            Vec3d lback = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(1).getCustomVector(new Vec3d(0, 0, -1));

                            Vector3 currentfore =  new Vector3((float)lback.x, (float)lback.y, (float)lback.z);

                            Vector3 v = aimCopy.cross(current);
                            double d = 180 / Math.PI * Math.acos(aimCopy.dot(current));

                            float pitch = (float)Math.toDegrees(Math.asin(aimCopy.getY()/aimCopy.length()));
                            float yaw = (float)Math.toDegrees(Math.atan2(aimCopy.getX(), aimCopy.getZ()));

                            Vector3 up = new Vector3(0,1,0);

                            Vector3 pAim2 = new Vector3(0,0,0);

                            aimCopy.setY(0) ; // we want the normal to a aiming plane, but vertical.

                            float porjaim = currentfore.dot(aimCopy); //angle between controller up and aim, just for ortho check
                            if(porjaim !=0) { //check to make sure 	we arent holding the bow perfectly straight up.
                                pAim2 = aimCopy.divide(1/porjaim);	 //projection of l_controller_up onto aim vector ... why is there no multiply?
                            }

                            float dot =0;
                            Vector3 proj = currentfore.subtract(pAim2).normalized(); //subtract to get projection of LCU onto vertical aim plane

                            dot = proj.dot(up);		//angle between our projection and straight up (the default bow render pos.)

                            float dot2 = aimCopy.dot(proj.cross(up)); //angle sign test, negative is left roll

                            float angle;

                            if (dot2 < 0)
                                angle = -(float) Math.acos(dot);
                            else angle = (float) Math.acos(dot);

                            float roll = (float) (180 / Math.PI * angle);     //calulate bow model roll.

                            GlStateManager.rotate(yaw, 0.0F,1.0F, 0.0F);
                            GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);

                            GlStateManager.rotate(-roll, 0.0F, 0.0F, 1.0F);
                            GlStateManager.rotate(90f, 1.0F, 0.0F, 0.0F);
                            //	GlStateManager.rotate(-180.0F, 0.0F, 0.0F, 1.0F);

                            if(((IMinecraftVR)mc).getBowTracker().isCharged()){
                                long t = Minecraft.getSystemTime() - ((IMinecraftVR)mc).getBowTracker().startDrawTime;
                                GlStateManager.translate(0.003*Math.sin(t),0, 0);
                            }
                            GlStateManager.scale(1,((IMinecraftVR)mc).getBowTracker().getDrawPercent()*0.15+1,1);
                        }
                        else if(((IMinecraftVR)mc).getVRSettings().seated){
                            GlStateManager.scale(0.5f,0.5f,0.5f);
                            GlStateManager.rotate(90f, 1.0F, 0.0F, 0.0F);
                        }

                        if(vive || ((IMinecraftVR)mc).getBowTracker().isDrawing){
                            GlStateManager.translate(-0.012, 0.2, 00);
                            GlStateManager.rotate(-45F, 1.0F, 0.0F, 0.0F);
                        } else {
                            GlStateManager.translate(-0.012, 0.17, .100);
                            GlStateManager.rotate(-45F + 39.4f, 1.0F, 0.0F, 0.0F	);
                        }

                        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);

                    }
                    else if (item instanceof ItemBow) {
                        //roomscale bow off
                        GlStateManager.scale(0.5f,0.5f,0.5f);
                        GlStateManager.rotate(90f, 1.0F, 0.0F, 0.0F);

                        GlStateManager.translate(-0.012, 0.2, 00);
                        GlStateManager.rotate(-45F, 1.0F, 0.0F, 0.0F);

                        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    }
                    else { //NORMAL ITEMS
                        GlStateManager.translate(0f, 0f, -0.1f);
                        GlStateManager.scale(0.3, 0.3, 0.3);
                        GlStateManager.rotate(-i*45.0F, 0.0F, 1.0F, 0.0F);
                    }
                }

                ((IRenderItemVR)itemRenderer).setIsMainhand(mainHand);
                ((IRenderItemVR)itemRenderer).setIsFPhand(true);

                //VIVE use 'NONE' transforms.
                if(!thing)
                    this.renderItemSide(partialTicks,player, itemstack, rightSide ? ItemCameraTransforms.TransformType.NONE : ItemCameraTransforms.TransformType.NONE, !rightSide);
                else
                    this.renderItemSide(partialTicks,player, itemstack, rightSide ? ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, !rightSide);

                ((IRenderItemVR)itemRenderer).setIsMainhand(false);
                ((IRenderItemVR)itemRenderer).setIsFPhand(false);

            }
            GlStateManager.popMatrix();
        }

    }

    @Overwrite
    public void renderOverlays(float partialTicks)
    {
        ((IEntityRendererVR)mc.entityRenderer).setInPortal(false);
        ((IEntityRendererVR)mc.entityRenderer).setInWater(false);
        ((IEntityRendererVR)mc.entityRenderer).setInBlock(false);
        ((IEntityRendererVR)mc.entityRenderer).setOnFire(false);

        GlStateManager.disableAlpha();
        Vec3d pos = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getEye(((IMinecraftVR)mc).getCurrentPass()).getPosition();
        if (isInsideOpaqueBlock(pos.add(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.hmd.getDirection().scale(0.05)), true))
        {
            TextureAtlasSprite tex = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(this.mc.world.getBlockState(new BlockPos(pos)));
            this.renderFaceInBlock();
            ((IEntityRendererVR)mc.entityRenderer).setInBlock(true);
            ((IEntityRendererVR)mc.entityRenderer).renderGuiLayer(partialTicks);
        }

        if (!this.mc.player.isSpectator())
        {
            if (isInsideOfMaterial(pos, Material.WATER) )
            {
                ((IEntityRendererVR)mc.entityRenderer).setInWater(true);
                //  this.renderWaterOverlayTexture(partialTicks);
            }


            if (this.mc.player.isBurning())
            {
                if (!net.minecraftforge.event.ForgeEventFactory.renderFireOverlay(mc.player, partialTicks)) {
                    this.renderFireInFirstPerson();
                    ((IEntityRendererVR)mc.entityRenderer).setOnFire(true);
                }
            }
        }


        if (!this.mc.player.isPotionActive(MobEffects.NAUSEA))
        {
            float f = this.mc.player.prevTimeInPortal + (this.mc.player.timeInPortal - this.mc.player.prevTimeInPortal) * partialTicks;

            if (f > 0.0F)
            {
                ((IEntityRendererVR)mc.entityRenderer).setInPortal(true);
                // this.renderPortal(f, new ScaledResolution(mc));
            }
        }


        GlStateManager.enableAlpha();
    }

    private void renderPortal(float timeInPortal, ScaledResolution scaledRes)
    {
        if (timeInPortal < 1.0F)
        {
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * timeInPortal;
            timeInPortal = timeInPortal * 0.8F + 0.2F;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, timeInPortal);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite textureatlassprite = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.PORTAL.getDefaultState());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        float f6 = textureatlassprite.getMinU();
        float f7 = textureatlassprite.getMaxU();
        float f8 = textureatlassprite.getMinV();
        float f9 = textureatlassprite.getMaxV();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        vertexbuffer.pos(-1.0D, -1.0D, -.6D).tex((double)( f6), (double)( f8)).endVertex();
        vertexbuffer.pos(1.0D, -1.0D, -.6D).tex((double)(f7), (double)( f8)).endVertex();
        vertexbuffer.pos(1.0D, 1.0D, -.6D).tex((double)(f7), (double)(f9)).endVertex();
        vertexbuffer.pos(-1.0D, 1.0D, -.6D).tex((double)( f6), (double)(f9)).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isInsideOfMaterial(Vec3d pos, Material materialIn)
    {
        BlockPos blockpos = new BlockPos(pos);
        IBlockState iblockstate = mc.world.getBlockState(blockpos);
        IBlockState iblockstateup = mc.world.getBlockState(blockpos.up());

        if (iblockstate.getMaterial() == materialIn)
        {
            float f = BlockLiquid.getLiquidHeightPercent(iblockstate.getBlock().getMetaFromState(iblockstate)) -0.11111111F;;
            if(iblockstateup.getMaterial() != materialIn && materialIn instanceof MaterialLiquid) f+=0.09F;

            //float f1 = (float)(blockpos.getY() + 1) - f;
            boolean flag = (pos.y-blockpos.getY()) < (1-f);
            return flag;
        }
        else
        {
            return false;
        }
    }


    public boolean isInsideOpaqueBlock(Vec3d in, boolean set)
    {

        if (mc.world == null) return false;
        BlockPos bp = new BlockPos(in);

        if(mc.world.getBlockState(bp).isOpaqueCube()) {
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat(1);
            return true;
        }

        if(!set) return false;
        ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat(0);
        Vec3d pos = in.add(0, 0, 0);
        float per = 0;
        float buffer = .07f;

        if((pos.x - Math.floor(pos.x)) < buffer)
        {
            per = (float) (pos.x - Math.floor(pos.x));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.west()).isOpaqueCube()) return true;
        }

        if(pos.x - Math.floor(pos.x) > 1-buffer){
            per = 1f - (float) (pos.x - Math.floor(pos.x));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.east()).isOpaqueCube()) return true;
        }

        if((pos.y - Math.floor(pos.y)) < buffer)
        {
            per = (float) (pos.y - Math.floor(pos.y));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.down()).isOpaqueCube()) return true;
        }

        if(pos.y - Math.floor(pos.y) > 1-buffer){
            per = 1f - (float) (pos.y - Math.floor(pos.y));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.up()).isOpaqueCube()) return true;
        }

        if((pos.z - Math.floor(pos.z)) < buffer)
        {
            per = (float) (pos.z - Math.floor(pos.z));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.north()).isOpaqueCube()) return true;
        }

        if(pos.z - Math.floor(pos.z) > 1-buffer){
            per = 1f - (float) (pos.z - Math.floor(pos.z));
            ((IEntityRendererVR)mc.entityRenderer).setInBlockFloat((buffer - per) / buffer);
            if (mc.world.getBlockState(bp.south()).isOpaqueCube()) return true;
        }

        return false;

    }

    @Overwrite
    private void renderFireInFirstPerson()
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
        //GlStateManager.depthFunc(519);
        //GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        float f = 1.0F;

        for (int i = 0; i < 4; ++i)
        {
            GlStateManager.pushMatrix();
            TextureAtlasSprite textureatlassprite = this.mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            float f1 = textureatlassprite.getMinU();
            float f2 = textureatlassprite.getMaxU();
            float f3 = textureatlassprite.getMinV();
            float f4 = textureatlassprite.getMaxV();
            GlStateManager.rotate((float)(i * 90.0F - ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getBodyYaw()), 0.0F, 1.0F, 0.0F);
            float f5 = 0.3f;
            float f6 = (float) (((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getHeadPivot().y - ((IEntityRendererVR)mc.entityRenderer).getRveY());
            GlStateManager.translate(0, -f6, 0.0F);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(-f5, 0, -f5).tex((double)f2, (double)f4).endVertex();
            bufferbuilder.pos(f5, 0, -f5).tex((double)f1, (double)f4).endVertex();
            bufferbuilder.pos(f5, f6, -f5).tex((double)f1, (double)f3).endVertex();
            bufferbuilder.pos(-f5, f6, -f5).tex((double)f2, (double)f3).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
    }


    private void renderFaceInBlock() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();

        GlStateManager.color(0f, 0F, 0F, ((IEntityRendererVR)mc.entityRenderer).getInBlockFloat());

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.ortho(0.0D, 1, 0, 1, 0, 100);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(-1 ,-1, 0).endVertex();
        bufferbuilder.pos(2, -1, 0).endVertex();
        bufferbuilder.pos(2, 2, 0).endVertex();
        bufferbuilder.pos(-1, 2, 0).endVertex();
        tessellator.draw();


        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        //GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Shadow
    private float getMapAngleFromPitch(float pitch)
    {
        return 0;
    }

    @Shadow
    private void renderMapFirstPerson(ItemStack stack)
    {

    }

    @Shadow
    private void setLightmap()
    {

    }
}
