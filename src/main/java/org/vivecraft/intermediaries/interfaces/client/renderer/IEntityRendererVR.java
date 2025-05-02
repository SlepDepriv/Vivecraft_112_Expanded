package org.vivecraft.intermediaries.interfaces.client.renderer;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

public interface IEntityRendererVR {

    Vec3d getCrossVec();
    void setCrossVec(Vec3d vec);
    float getClipDistance();
    float getMinClipDistance();
    boolean getInWater();
    boolean getWasinwater();
    boolean getInPortal();
    boolean getInBlock();
    boolean getOnFire();
    float getInBlockFloat();
    boolean getMenuWorldFastTime();

    void setInWater(boolean bool);
    void setWasInwater(boolean bool);
    void setInPortal(boolean bool);
    void setInBlock(boolean bool);
    void setOnFire(boolean bool);

    void setInBlockFloat(float num);


    void setMenuWorldFastTime(boolean bool);
    org.lwjgl.util.vector.Matrix4f getThirdPassProjectionMatrix();
    void getMouseOverVR(float partialTicks);
    Vec3d getControllerRenderPos(int c);
    void displayNotificationText(String prefix, String message, String suffix, int displayWidth, int displayHeight, boolean isStereo, boolean isGuiOrtho);
    void drawFramebuffer(float renderPartialTicks, long tickDuration);
    void drawScreen(float par1, GuiScreen screen);
    void setupClipPlanes();
    boolean isInMenuRoom();
    void applyMRCameraRotation(boolean invert);
    void restoreRVEPos(EntityLivingBase e);
    void cacheRVEPos(EntityLivingBase e);
    void setupRVE();

    void renderGuiLayer(float par1);

    double getRveY();
}
