package org.vivecraft.mixin.minecraft.vr_user.client.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.gui.IGUIIngameVR;

import java.util.Random;

@Mixin(GuiIngame.class)
public class MixinGUIIngameVR extends Gui implements IGUIIngameVR {

    @Shadow
    private static ResourceLocation VIGNETTE_TEX_PATH;
    @Shadow
    private static ResourceLocation WIDGETS_TEX_PATH;
    @Shadow
    private static ResourceLocation PUMPKIN_BLUR_TEX_PATH;
    @Shadow
    private Random rand;
    @Shadow
    private Minecraft mc;
    @Shadow
    private RenderItem itemRenderer;
    @Shadow
    private GuiNewChat persistantChatGUI;
    @Shadow
    private int updateCounter;
    @Shadow
    private GuiSpectator spectatorGui;
    @Shadow
    private GuiPlayerTabOverlay overlayPlayerList;
    @Shadow
    private GuiBossOverlay overlayBoss;

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

    // Vivecraft
    public boolean showPlayerList;

    @Override
    public ResourceLocation getInventoryBackground() {
        return INVENTORY_BACKGROUND;
    }

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;armorItemInSlot(I)Lnet/minecraft/item/ItemStack;", shift = At.Shift.AFTER))
    private void zeroPumpkinEffect(CallbackInfo ci) {
        ((IMinecraftVR)mc).setPumpkinEffect(0);
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderPumpkinOverlay(Lnet/minecraft/client/gui/GuiIngame;Lnet/minecraft/client/gui/ScaledResolution;)V"))
    private void onePumpkinEffect(ScaledResolution scaledRes) {
        ((IMinecraftVR)mc).setPumpkinEffect(1);
    }

    @ModifyExpressionValue(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean disableNausea(boolean original) {
        return true;
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderAttackIndicator(Lnet/minecraft/client/gui/GuiIngame;FLnet/minecraft/client/gui/ScaledResolution;)V"))
    private void disableAttackIndicator(ScaledResolution sr, float partialTicks) {
        // Do nothing
    }

    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", ordinal = 0))
    private void addEnableAlpha(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        GlStateManager.enableAlpha();
    }

    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", ordinal = 1, shift = At.Shift.AFTER))
    private void addHotbarContextSelection(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        EntityPlayer entityplayer2 = (EntityPlayer)this.mc.getRenderViewEntity();
        int i2 = sr.getScaledWidth() / 2;
        //Vivecraft - render hotbar context selection.
        if(((IMinecraftVR)mc).getInteractTracker().hotbar >= 0 && ((IMinecraftVR)mc).getInteractTracker().hotbar < 9 && entityplayer2.inventory.currentItem != ((IMinecraftVR)mc).getInteractTracker().hotbar) {
            GlStateManager.color(0.0F, 1.0F, 0.0F, 1.0F);
            this.drawTexturedModalRect(i2 - 91 - 1 + ((IMinecraftVR)mc).getInteractTracker().hotbar * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        //
    }

    @ModifyExpressionValue(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean changeIf(boolean original) {
        return original || ((IMinecraftVR)mc).getVRSettings().vrTouchHotbar;
    }

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", ordinal = 3))
    private void addInteractTracker1(GuiIngame instance, int x, int y, int textureX, int textureY, int width, int height) {
        if(((IMinecraftVR)mc).getInteractTracker().hotbar == 9 ) {
            GlStateManager.color(0.0F, 0.0F, 1.0F, 1.0F);
            instance.drawTexturedModalRect(x, y, textureX, textureY, width, height);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            instance.drawTexturedModalRect(x, y, textureX, textureY, width, height);
        }
    }
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V", ordinal = 5))
    private void addInteractTracker2(GuiIngame instance, int x, int y, int textureX, int textureY, int width, int height) {
        if(((IMinecraftVR)mc).getInteractTracker().hotbar == 9 ) {
            GlStateManager.color(0.0F, 0.0F, 1.0F, 1.0F);
            instance.drawTexturedModalRect(x, y, textureX, textureY, width, height);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            instance.drawTexturedModalRect(x, y, textureX, textureY, width, height);
        }
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void disableRenderVignette(float lightLevel, ScaledResolution scaledRes, CallbackInfo ci) {
        if (true) ci.cancel();
    }

    @Overwrite
    protected void renderPortal(float timeInPortal, ScaledResolution scaledRes)
    {

    }

    @Shadow
    protected void renderPlayerStats(ScaledResolution scaledRes)
    {

    }

    @Shadow
    protected void renderMountHealth(ScaledResolution p_184047_1_)
    {

    }
    @Shadow
    public void renderHorseJumpBar(ScaledResolution scaledRes, int x)
    {

    }

    @Shadow
    public void renderExpBar(ScaledResolution scaledRes, int x)
    {

    }

    @Shadow
    public void renderSelectedItem(ScaledResolution scaledRes)
    {

    }

    @Shadow
    public FontRenderer getFontRenderer()
    {
        return this.mc.fontRenderer;
    }



    //VIVECRAFT ADDITIONS ***********************************

    public void drawMouseMenuQuad(int mouseX, int mouseY)
    {
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.color(1, 1, 1, 1);

        this.mc.getTextureManager().bindTexture(Gui.ICONS);
        float menuMousePointerSize = 16f * ((IMinecraftVR)mc).getVRSettings().menuCrosshairScale;

        //Why didnt we think of this sooner?
		/*GlStateManager.colorMask(false, false, false, true);
		GlStateManager.blendFunc(SourceFactor.CONSTANT_ALPHA, DestFactor.ZERO);
		drawCentredTexturedModalRect(mouseX, mouseY, menuMousePointerSize, menuMousePointerSize, 0, 0, 15, 15);

		GlStateManager.blendFunc(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR);
		GlStateManager.colorMask(true, true, true, false);
		drawCentredTexturedModalRect(mouseX, mouseY, menuMousePointerSize, menuMousePointerSize, 0, 0, 15, 15);*/

        // Turns out all we needed was some blendFuncSeparate magic :)
        // Also color DestFactor of ZERO produces better results with non-white crosshairs
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        drawCentredTexturedModalRect(mouseX, mouseY, menuMousePointerSize, menuMousePointerSize, 0, 0, 15, 15);

        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        //GlStateManager.colorMask(true, true, true, false);
    }
    /**
     * Draws a centred textured rectangle at the stored z-value. Args: x, y, width, height, u, v, texwidth, texheight
     */
    public void drawCentredTexturedModalRect(int centreX, int centreY, float width, float height, int u, int v, int texWidth, int texHeight)
    {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        tessellator.getBuffer().pos(0, 0, 0).tex(u, v);
        tessellator.getBuffer().pos((double)(centreX - (width / 2f)), (double)(centreY + (height / 2f)), (double)this.zLevel).tex((double)((float)(u + 0) * f), (double)((float)(v + texHeight) * f1)).endVertex();
        tessellator.getBuffer().pos((double)(centreX + (width / 2f)), (double)(centreY + (height / 2f)), (double)this.zLevel).tex( (double)((float)(u + texWidth) * f), (double)((float)(v + texHeight) * f1)).endVertex();
        tessellator.getBuffer().pos((double)(centreX + (width / 2f)), (double)(centreY - (height / 2f)), (double)this.zLevel).tex( (double)((float)(u + texWidth) * f), (double)((float)(v + 0) * f1)).endVertex();
        tessellator.getBuffer().pos((double)(centreX - (width / 2f)), (double)(centreY - (height / 2f)), (double)this.zLevel).tex( (double)((float)(u + 0) * f), (double)((float)(v + 0) * f1)).endVertex();
        tessellator.draw();
    }
}
