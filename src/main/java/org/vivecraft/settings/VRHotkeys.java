/**
* Copyright 2013 Mark Browning, StellaArtois
* Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
*/
package org.vivecraft.settings;

import net.minecraft.client.resources.I18n;
import org.vivecraft.api.VRData;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings.VrOptions;
import org.vivecraft.utils.Angle;
import org.vivecraft.utils.Axis;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector3;

import org.vivecraft.utils.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

public class VRHotkeys {

	static long nextRead = 0;
	static final long COOLOFF_PERIOD_MILLIS = 500;
	static boolean debug = false;

	private static int startController;
	private static VRData.VRDevicePose startControllerPose;
	private static float startCamposX;
	private static float startCamposY;
	private static float startCamposZ;
	private static Quaternion startCamrotQuat;
	
	public static boolean handleKeyboardGUIInputs(Minecraft mc)
	{
		// Support cool-off period for key presses - otherwise keys can get spammed...
		if (nextRead != 0 && System.currentTimeMillis() < nextRead)
		return false;

		// Capture Minecrift key events
		boolean gotKey = false;
		
		if(Keyboard.getEventKey() == Keyboard.KEY_F5 && Keyboard.getEventKeyState() == true) {
			((IMinecraftVR)mc).getVRSettings().setOptionValue(VrOptions.MIRROR_DISPLAY, ((IMinecraftVR)mc).getVRSettings().displayMirrorMode);
			((IMinecraftVR)mc).notifyMirror(((IMinecraftVR)mc).getVRSettings().getKeyBinding(VrOptions.MIRROR_DISPLAY), false, 3000);
			gotKey = true;
		}
		// VIVE END - hotkeys

		if (gotKey) {
			((IMinecraftVR)mc).getVRSettings().saveOptions();
		}

		return gotKey;
	}
	
	public static boolean handleKeyboardInputs(Minecraft mc)
	{
		// Support cool-off period for key presses - otherwise keys can get spammed...
		if (nextRead != 0 && System.currentTimeMillis() < nextRead)
		return false;

		// Capture Minecrift key events
		boolean gotKey = false;

		// Debug aim
		if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			((IMinecraftVR)mc).getVRSettings().storeDebugAim = true;
			((IMinecraftVR)mc).printChatMessage(I18n.format("vivecraft.messages.showaim"));
			gotKey = true;
		}

		// Walk up blocks
		if (Keyboard.getEventKey() == Keyboard.KEY_B && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			((IMinecraftVR)mc).getVRSettings().walkUpBlocks = !((IMinecraftVR)mc).getVRSettings().walkUpBlocks;
			((IMinecraftVR)mc).printChatMessage(LangHelper.get("vivecraft.messages.walkupblocks", ((IMinecraftVR)mc).getVRSettings().walkUpBlocks ? LangHelper.getYes() : LangHelper.getNo()));
			gotKey = true;
		}

		// Player inertia
		if (Keyboard.getEventKey() == Keyboard.KEY_I && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
		{
			((IMinecraftVR)mc).getVRSettings().inertiaFactor += 1;
			if (((IMinecraftVR)mc).getVRSettings().inertiaFactor > VRSettings.INERTIA_MASSIVE)
				((IMinecraftVR)mc).getVRSettings().inertiaFactor = VRSettings.INERTIA_NONE;
			switch (((IMinecraftVR)mc).getVRSettings().inertiaFactor)
			{
			case VRSettings.INERTIA_NONE:
				((IMinecraftVR)mc).printChatMessage(LangHelper.get("vivecraft.messages.playerinertia", I18n.format("vivecraft.options.inertia.none")));
				break;
			case VRSettings.INERTIA_NORMAL:
				((IMinecraftVR)mc).printChatMessage(LangHelper.get("vivecraft.messages.playerinertia", I18n.format("vivecraft.options.inertia.normal")));
				break;
			case VRSettings.INERTIA_LARGE:
				((IMinecraftVR)mc).printChatMessage(LangHelper.get("vivecraft.messages.playerinertia", I18n.format("vivecraft.options.inertia.large")));
				break;
			case VRSettings.INERTIA_MASSIVE:
				((IMinecraftVR)mc).printChatMessage(LangHelper.get("vivecraft.messages.playerinertia", I18n.format("vivecraft.options.inertia.massive")));
				break;
			}
			gotKey = true;
		}

		// Render full player model or just an disembodied hand...
		/*if (Keyboard.getEventKey() == Keyboard.KEY_H && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.renderFullFirstPersonModelMode++;
			if (mc.vrSettings.renderFullFirstPersonModelMode > VRSettings.RENDER_FIRST_PERSON_NONE)
			mc.vrSettings.renderFullFirstPersonModelMode = VRSettings.RENDER_FIRST_PERSON_FULL;

				switch (mc.vrSettings.renderFullFirstPersonModelMode)
			{
			case VRSettings.RENDER_FIRST_PERSON_FULL:
				mc.printChatMessage("First person model (RCTRL-H): Full");
				break;
			case VRSettings.RENDER_FIRST_PERSON_HAND:
				mc.printChatMessage("First person model (RCTRL-H): Hand");
				break;
			case VRSettings.RENDER_FIRST_PERSON_NONE:
				mc.printChatMessage("First person model (RCTRL-H): None");
				break;
			}
			gotKey = true;
		}*/
		// VIVE START - hotkeys

		// Testing different movement styles
//		if (Keyboard.getEventKey() == Keyboard.KEY_M && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
//		{
//			// JRBUDDA ADDED all dis.
//			if (mc.vrPlayer.getFreeMoveMode()) {
//				//cycle restricted movement styles
//				if (mc.vrPlayer.useLControllerForRestricedMovement) {
//					mc.vrPlayer.useLControllerForRestricedMovement = false;
//					mc.printChatMessage("Restricted movement mode set to gaze");
//				} else {
//					mc.vrPlayer.useLControllerForRestricedMovement = true;
//					mc.printChatMessage("Restricted movement mode set to left controller");
//				}
//			} else {				
//				OpenVRPlayer vrp = mc.vrPlayer;				
//				// cycle VR movement styles
//				if (vrp.vrMovementStyle.name == "Minimal") vrp.vrMovementStyle.setStyle("Beam");
//				else if (vrp.vrMovementStyle.name == "Beam") vrp.vrMovementStyle.setStyle("Tunnel");
//				else if (vrp.vrMovementStyle.name == "Tunnel") vrp.vrMovementStyle.setStyle("Grapple");
//				else if (vrp.vrMovementStyle.name == "Grapple") vrp.vrMovementStyle.setStyle("Arc");
//				else vrp.vrMovementStyle.setStyle("Minimal");			
//			}
//					
//			gotKey = true;
//		}

		if (Keyboard.getEventKey() == Keyboard.KEY_R && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			// for testing restricted client mode
			if (((IMinecraftVR)mc).getVRPlayer().isTeleportOverridden()) {
				((IMinecraftVR)mc).getVRPlayer().setTeleportOverride(false);
				((IMinecraftVR)mc).printChatMessage(I18n.format("vivecraft.messages.teleportdisabled"));
			} else {
				((IMinecraftVR)mc).getVRPlayer().setTeleportOverride(true);
				((IMinecraftVR)mc).printChatMessage(I18n.format("vivecraft.messages.teleportenabled"));
			}

			gotKey = true;
		}
		
		
		if (Keyboard.getEventKey() == Keyboard.KEY_HOME && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			snapMRCam(0);
			gotKey = true;
		}
		
		if(Keyboard.getEventKey() == Keyboard.KEY_F12){
            //mc.displayGuiScreen(new GuiWinGame(false, Runnables.doNothing()));
			gotKey = true;
		}
		// VIVE END - hotkeys

		if (gotKey) {
			((IMinecraftVR)mc).getVRSettings().saveOptions();
		}

		return gotKey;
	}

	public static void handleMRKeys() {
		Minecraft mc = Minecraft.getMinecraft();
		
		boolean gotKey = false;
		
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(-0.01F, 0, 0));
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(0.01F, 0, 0));
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_UP) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(0, 0, -0.01F));
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(0, 0, 0.01F));
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_PRIOR) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(0, 0.01F, 0));
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_NEXT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamPos(new Vector3(0, -0.01F, 0));
			gotKey = true;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_UP) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.PITCH, 0.5F);
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.PITCH, -0.5F);
			gotKey = true;

		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.YAW, 0.5F);
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.YAW, -0.5F);
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_PRIOR) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.ROLL, 0.5F);
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_NEXT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			adjustCamRot(Axis.ROLL, -0.5F);
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_INSERT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.gameSettings.fovSetting +=1 ;
			gotKey = true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DELETE) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.gameSettings.fovSetting -=1 ;
			gotKey = true;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_INSERT) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			((IMinecraftVR)mc).getVRSettings().mixedRealityFov +=1 ;
			gotKey = true;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_DELETE) && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)&& Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
		{
			((IMinecraftVR)mc).getVRSettings().mixedRealityFov -=1 ;
			gotKey = true;
		}
		
		if(gotKey) {
			((IMinecraftVR)mc).getVRSettings().saveOptions();
			if (MCOpenVR.mrMovingCamActive) {
				Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(LangHelper.get("vivecraft.messages.coords", ((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetX, ((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetY, ((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetZ)));
				Angle angle = ((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetRotQuat.toEuler();
				Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(LangHelper.get("vivecraft.messages.angles", angle.getPitch(), angle.getYaw(), angle.getRoll())));
			} else {
				Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(LangHelper.get("vivecraft.messages.coords", ((IMinecraftVR)mc).getVRSettings().vrFixedCamposX, ((IMinecraftVR)mc).getVRSettings().vrFixedCamposY, ((IMinecraftVR)mc).getVRSettings().vrFixedCamposZ)));
				Angle angle = ((IMinecraftVR)mc).getVRSettings().vrFixedCamrotQuat.toEuler();
				Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(LangHelper.get("vivecraft.messages.angles", angle.getPitch(), angle.getYaw(), angle.getRoll())));
			}
		}
	}
	
	private static void adjustCamPos(Vector3 offset) {
		Minecraft mc = Minecraft.getMinecraft();
		if (MCOpenVR.mrMovingCamActive) {
			offset = ((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetRotQuat.multiply(offset);
			((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetX += offset.getX();
			((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetY += offset.getY();
			((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetZ += offset.getZ();
		} else {
			offset = ((IMinecraftVR)mc).getVRSettings().vrFixedCamrotQuat.inverse().multiply(offset);
			((IMinecraftVR)mc).getVRSettings().vrFixedCamposX += offset.getX();
			((IMinecraftVR)mc).getVRSettings().vrFixedCamposY += offset.getY();
			((IMinecraftVR)mc).getVRSettings().vrFixedCamposZ += offset.getZ();
		}
	}

	private static void adjustCamRot(Axis axis, float degrees) {
		Minecraft mc = Minecraft.getMinecraft();
		if (MCOpenVR.mrMovingCamActive) {
			((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetRotQuat.set(((IMinecraftVR)mc).getVRSettings().mrMovingCamOffsetRotQuat.rotate(axis, degrees, true));
		} else {
			((IMinecraftVR)mc).getVRSettings().vrFixedCamrotQuat.set(((IMinecraftVR)mc).getVRSettings().vrFixedCamrotQuat.rotate(axis, degrees, false));
		}
	}
	
	public static void snapMRCam(int controller) {
		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
		Vec3d c = mc.getVRPlayer().vrdata_room_pre.getController(controller).getPosition();
		mc.getVRSettings().vrFixedCamposX =(float) c.x;
		mc.getVRSettings().vrFixedCamposY =(float) c.y;
		mc.getVRSettings().vrFixedCamposZ =(float) c.z;
		
		Quaternion quat = new Quaternion(Utils.convertOVRMatrix(mc.getVRPlayer().vrdata_room_pre.getController(controller).getMatrix()));
		mc.getVRSettings().vrFixedCamrotQuat.set(quat);
	}

	public static void updateMovingThirdPersonCam() {
		Minecraft mc = Minecraft.getMinecraft();

		if (startControllerPose != null) {
			VRData.VRDevicePose controllerPose = ((IMinecraftVR)mc).getVRPlayer().vrdata_room_pre.getController(startController);
			Vec3d startPos = startControllerPose.getPosition();
			Vec3d deltaPos = controllerPose.getPosition().subtract(startPos);

			Matrix4f deltaMatrix = Matrix4f.multiply(controllerPose.getMatrix(), startControllerPose.getMatrix().inverted());
			Vector3 offset = new Vector3(startCamposX - (float)startPos.x, startCamposY - (float)startPos.y, startCamposZ - (float)startPos.z);
			Vector3 offsetRotated = deltaMatrix.transform(offset);

			((IMinecraftVR)mc).getVRSettings().vrFixedCamposX = startCamposX + (float)deltaPos.x + (offsetRotated.getX() - offset.getX());
			((IMinecraftVR)mc).getVRSettings().vrFixedCamposY = startCamposY + (float)deltaPos.y + (offsetRotated.getY() - offset.getY());
			((IMinecraftVR)mc).getVRSettings().vrFixedCamposZ = startCamposZ + (float)deltaPos.z + (offsetRotated.getZ() - offset.getZ());
			((IMinecraftVR)mc).getVRSettings().vrFixedCamrotQuat.set(startCamrotQuat.multiply(new Quaternion(Utils.convertOVRMatrix(deltaMatrix))));
		}
	}

	public static void startMovingThirdPersonCam(int controller) {
		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
		startController = controller;
		startControllerPose = mc.getVRPlayer().vrdata_room_pre.getController(controller);
		startCamposX = mc.getVRSettings().vrFixedCamposX;
		startCamposY = mc.getVRSettings().vrFixedCamposY;
		startCamposZ = mc.getVRSettings().vrFixedCamposZ;
		startCamrotQuat = mc.getVRSettings().vrFixedCamrotQuat.copy();
	}

	public static void stopMovingThirdPersonCam() {
		startControllerPose = null;
	}

	public static boolean isMovingThirdPersonCam() {
		return startControllerPose != null;
	}
}
