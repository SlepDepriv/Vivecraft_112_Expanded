package org.vivecraft.mixin.minecraft.vr_user.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IRenderItemVR;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(RenderItem.class)
public class MixinRenderItemVR implements IRenderItemVR {

    private static boolean ismainhand = false;
    private static boolean isfphand = false;
    @Shadow
    private ItemModelMesher itemModelMesher;
    @Shadow
    private TextureManager textureManager;
    @Shadow
    private ItemColors itemColors;

    @Override
    public boolean getIsMainhand() {
        return ismainhand;
    }

    @Override
    public boolean getIsFPhand() {
        return isfphand;
    }

    @Override
    public void setIsMainhand(boolean bool) {
        ismainhand = bool;
    }

    @Override
    public void setIsFPhand(boolean bool) {
        isfphand = bool;
    }

    @Overwrite
    public void renderItem(ItemStack stack, IBakedModel model) {
        if (stack != null && !stack.isEmpty())
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);

            EntityPlayerSP p = Minecraft.getMinecraft().player;

            if(p!=null && isfphand){

                fade= p.getCooledAttackStrength(0)*.75f + .25f;

                if(p.isSneaking())
                    fade =0.75f;

                if(p.isActiveItemStackBlocking() && p.getActiveItemStack() != stack)
                    fade =0.75f;

                if(stack.getItem() == Items.SHIELD) {
                    if (p.isActiveItemStackBlocking())
                        fade = 1;
                    else
                        fade = 0.75f;
                }

                if(fade < 0.1) fade = 0.1f;
                if(fade > 1) fade = 1f;
                GlStateManager.enableBlend();
                GL14.glBlendColor(1, 1, 1, fade);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            }

            if (model.isBuiltInRenderer())
            {
                //VIVECRAFT
                GlStateManager.color(1.0F, 1.0F, 1.0F, fade );

                GlStateManager.enableRescaleNormal();

                stack.getItem().getTileEntityItemStackRenderer().renderByItem(stack);
            }
            else
            {
//                if (Config.isCustomItems())
//                {
//                    model = CustomItems.getCustomItemModel(stack, model, this.modelLocation, false);
//                    this.modelLocation = null;
//                }

//                this.renderModelHasEmissive = false;

                this.renderModel(model, -1, stack);

//                if (this.renderModelHasEmissive)
//                {
//                    float f = OpenGlHelper.lastBrightnessX;
//                    float f1 = OpenGlHelper.lastBrightnessY;
//                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, f1);
//                    this.renderModelEmissive = true;
//                    this.renderModel(model, stack);
//                    this.renderModelEmissive = false;
//                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
//                }

//                if (stack.hasEffect() && (!Config.isCustomItems() || !CustomItems.renderCustomEffect(this, stack, model)))
//                {
//                    this.renderEffect(model);
//                }
            }

            GL14.glBlendColor(1, 1, 1, 1);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.disableBlend();
            fade = 1;
            GlStateManager.popMatrix();
        }
    }

    private int makeColor(int a, int r, int g, int b) {
        return  a << 24 | r << 16 | g << 8 | b;
    }

    float fade = 1;

    @Shadow
    private void renderQuads(BufferBuilder renderer, List<BakedQuad> quads, int color, ItemStack stack)
    {
        boolean flag = color == -1 && !stack.isEmpty();
        int i = 0;

        for (int j = quads.size(); i < j; ++i)
        {
            BakedQuad bakedquad = quads.get(i);
            int k = color;

            if (flag && bakedquad.hasTintIndex())
            {
                k = this.itemColors.colorMultiplier(stack, bakedquad.getTintIndex());

//                if (Config.isCustomColors())
//                {
//                    k = CustomColors.getColorFromItemStack(stack, bakedquad.getTintIndex(), k);
//                }

                if (EntityRenderer.anaglyphEnable)
                {
                    k = TextureUtil.anaglyphColor(k);
                }

                int b = (int) (fade * 255);

                int rev = (255 - b) >> 24;

                k -= rev; //apply fade to tinted color.

                if (((IMinecraftVR)Minecraft.getMinecraft()).getJumpTracker().isBoots(stack))
                {
                    k = makeColor(b, 0, 255, 0);
                } else if (((IMinecraftVR)Minecraft.getMinecraft()).getClimbTracker().isClaws(stack))
                {
                    k = makeColor(b, 130, 0, 75);
                } //override color

                k = k | -16777216; //wtf r u.

            }

            //Optifine ignores this Forge call since it modifies renderQuad heavily.
            //if (Reflector.forgeExists()) Reflector.callVoid(Reflector.LightUtil_renderQuadColor, renderer, bakedquad, k);
            //else
//            this.renderQuad(renderer, bakedquad, k);
            //SD: Actually they must of changed it since this was written, it calls this now instead
            net.minecraftforge.client.model.pipeline.LightUtil.renderQuadColor(renderer, bakedquad, k);
        }

    }

    @Overwrite
    public void renderItem(ItemStack stack, ItemCameraTransforms.TransformType cameraTransformType)
    {
        if (!stack.isEmpty())
        {
            IBakedModel ibakedmodel = this.getItemModelWithOverrides(stack, (World)null, (EntityLivingBase)null);
            this.renderItemModel(0, stack, ibakedmodel, cameraTransformType, false);
        }
    }

    public void renderItem(ItemStack stack, EntityLivingBase entitylivingbaseIn, ItemCameraTransforms.TransformType transform, boolean leftHanded)
    {
        this.renderItem(0, stack, entitylivingbaseIn, transform, leftHanded);
    }

    public void renderItem(float par1, ItemStack stack, EntityLivingBase entitylivingbaseIn, ItemCameraTransforms.TransformType transform, boolean leftHanded)
    {
        if (!stack.isEmpty() && entitylivingbaseIn != null)
        {
            IBakedModel ibakedmodel = this.getItemModelWithOverrides(stack, entitylivingbaseIn.world, entitylivingbaseIn);
            this.renderItemModel(par1, stack, ibakedmodel, transform, leftHanded);
        }
    }

    protected void renderItemModel(float par1, ItemStack stack, IBakedModel bakedmodel, ItemCameraTransforms.TransformType transform, boolean leftHanded)
    {
        if (!stack.isEmpty())
        {
            this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();

            ForgeHooksClient.handleCameraTransforms(bakedmodel, transform, leftHanded);


//            CustomItems.setRenderOffHand(leftHanded);
            this.renderItem(stack, bakedmodel);
//            CustomItems.setRenderOffHand(false);
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    }

    @Shadow
    public IBakedModel getItemModelWithOverrides(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entitylivingbaseIn)
    {
        return null;
    }

    @Shadow
    private void renderModel(IBakedModel model, int color, ItemStack stack)
    {

    }
 }
