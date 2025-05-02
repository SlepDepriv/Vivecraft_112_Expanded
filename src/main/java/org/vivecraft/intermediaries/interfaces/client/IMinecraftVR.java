package org.vivecraft.intermediaries.interfaces.client;

import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.util.Timer;
import net.minecraft.util.math.Vec3d;
import org.vivecraft.api.ErrorHelper;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.gameplay.trackers.*;
import org.vivecraft.provider.OpenVRStereoRenderer;
import org.vivecraft.render.MenuWorldRenderer;
import org.vivecraft.render.RenderPass;
import org.vivecraft.intermediaries.replacements.VRFrameBuffer;
import org.vivecraft.settings.VRSettings;

import java.util.LinkedList;

public interface IMinecraftVR {

OpenVRPlayer getVRPlayer();

///  Trackers

BackpackTracker getBackpackTracker();
BowTracker getBowTracker();
ClimbTracker getClimbTracker();
EatingTracker getEatingTracker();
HorseTracker getHorseTracker();
InteractTracker getInteractTracker();
JumpTracker getJumpTracker();
RowTracker getRowTracker();
RunTracker getRunTracker();
SneakTracker getSneakTracker();
SwimTracker getSwimTracker();
SwingTracker getSwingTracker();
TeleportTracker getTeleportTracker();
VehicleTracker getVehicleTracker();
OpenVRStereoRenderer getStereoProvider();

RenderPass getCurrentPass();

void setFramebuffer(VRFrameBuffer buffer);
VRFrameBuffer getFramebuffer();
MenuWorldRenderer getMenuWorldRenderer();

void notifyMirror( String text, boolean clear, int lengthMs);

void setShowSplashScreen(Boolean bool);
boolean getShowSplashScreen();


int getMouseXPos();
int getMouseYPos();
int getTickCounter();
Timer getTimer();


GuiToast getToastGUI();



void triggerDisplayDebugInfo(long elapsedTicksTime);


boolean getIntegratedServerLaunchInProgress();

Matrix4f getMRTransform();


org.lwjgl.util.vector.Matrix4f getThirdPassViewMatrix();


VRSettings getVRSettings();

void setVRSettings(VRSettings settings);

void print(String s);


void printChatMessage(String msg);


void triggerRightClickMouse();

ErrorHelper getErrorHelper();

void setErrorHelper(ErrorHelper helper);
LinkedList<Vec3d> getHmdPosSamples();

LinkedList<Float> getHmdYawSamples();

float getPumpkinEffect();

void setPumpkinEffect(float pumpkinEffect);



}