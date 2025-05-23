package org.vivecraft.gameplay.screenhandlers;

import org.lwjgl.util.vector.Vector3f;
import org.vivecraft.control.ControllerType;
import org.vivecraft.gui.GuiKeyboard;
import org.vivecraft.gui.PhysicalKeyboard;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.replacements.VRFrameBuffer;
import org.vivecraft.intermediaries.interfaces.client.gui.IGuiScreenVR;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector3;
import org.vivecraft.utils.Matrix4f;
import org.vivecraft.utils.OpenVRUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class KeyboardHandler {
	//keyboard
	public static IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
	public static boolean Showing = false;
	public static GuiKeyboard UI = new GuiKeyboard();
	public static PhysicalKeyboard physicalKeyboard = new PhysicalKeyboard();
	public static Vec3d Pos_room = new Vec3d(0,0,0);
	public static Matrix4f Rotation_room = new Matrix4f();
	private static boolean lpl, lps, PointedL, PointedR;
	public static boolean keyboardForGui;
	public static VRFrameBuffer Framebuffer = null;
	private static boolean lastPressedClickL, lastPressedClickR, lastPressedShift;

	public static boolean setOverlayShowing(boolean showingState) {
		// TODO: Deal with this later
//		if (Main.kiosk) return false;
		if(mc.getVRSettings().seated) showingState = false;
		int ret = 1;
		if (showingState) {		
			ScaledResolution s = new ScaledResolution((Minecraft)mc);
			int i = s.getScaledWidth();
			int j = s.getScaledHeight();
            if (mc.getVRSettings().physicalKeyboard)
				physicalKeyboard.show();
            else
            	UI.setWorldAndResolution(Minecraft.getMinecraft(), i, j);
			Showing = true;
      		orientOverlay(((Minecraft)mc).currentScreen!=null);
      		RadialHandler.setOverlayShowing(false, null);
		} else {
			Showing = false;
		}
		return Showing;
	}

	public static void processGui() {
	
		PointedL = false;
		PointedR = false;
		
		if(!Showing) {
			return;
		}
		if(mc.getVRSettings().seated) return;
		if(Rotation_room == null) return;

		if (mc.getVRSettings().physicalKeyboard) {
			physicalKeyboard.process();
			return; // Skip the rest of this
		}
		
		Vec2f tex1 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, ((Minecraft)mc).currentScreen, GuiHandler.guiScale, mc.getVRPlayer().vrdata_room_pre.getController(1));
		Vec2f tex2 = GuiHandler.getTexCoordsForCursor(Pos_room, Rotation_room, ((Minecraft)mc).currentScreen, GuiHandler.guiScale, mc.getVRPlayer().vrdata_room_pre.getController(0));
	
		float u = tex2.x;
		float v = 1 - tex2.y;
		
		if (u<0 || v<0 || u>1 || v>1)
		{
			// offscreen
			UI.cursorX2 = -1.0f;
			UI.cursorY2 = -1.0f;
			PointedR = false;
		}
		else if (UI.cursorX2 == -1.0f)
		{
			UI.cursorX2 = (int) (u * ((Minecraft)mc).displayWidth);
			UI.cursorY2 = (int) (v * ((Minecraft)mc).displayHeight);
			PointedR = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * ((Minecraft)mc).displayWidth);
			float newY = (int) (v * ((Minecraft)mc).displayHeight);
			UI.cursorX2 = UI.cursorX2 * 0.7f + newX * 0.3f;
			UI.cursorY2 = UI.cursorY2 * 0.7f + newY * 0.3f;
			PointedR = true;
		}
		
		 u = tex1.x;
		 v = 1-tex1.y;
		
		if (u<0 || v<0 || u>1 || v>1)
		{
			// offscreen
			UI.cursorX1 = -1.0f;
			UI.cursorY1 = -1.0f;
			PointedL = false;
		}
		else if (UI.cursorX1 == -1.0f)
		{
			UI.cursorX1 = (int) (u * ((Minecraft)mc).displayWidth);
			UI.cursorY1 = (int) (v * ((Minecraft)mc).displayHeight);
			PointedL = true;
		}
		else
		{
			// apply some smoothing between mouse positions
			float newX = (int) (u * ((Minecraft)mc).displayWidth);
			float newY = (int) (v * ((Minecraft)mc).displayHeight);
			UI.cursorX1 = UI.cursorX1 * 0.7f + newX * 0.3f;
			UI.cursorY1 = UI.cursorY1 * 0.7f + newY * 0.3f;
			PointedL = true;
		}
	}
	
	public static void orientOverlay(boolean guiRelative) {
		
		keyboardForGui = false;
		if (!Showing) return;
		keyboardForGui = guiRelative;
		
		org.lwjgl.util.vector.Matrix4f matrix = new org.lwjgl.util.vector.Matrix4f();
		if (mc.getVRSettings().physicalKeyboard) {
			Vec3d pos = mc.getVRPlayer().vrdata_room_pre.hmd.getPosition();
			Vec3d offset = new Vec3d(0, -0.5, 0.3);
			offset = offset.rotateYaw((float)Math.toRadians(-mc.getVRPlayer().vrdata_room_pre.hmd.getYaw()));
			Pos_room = new Vec3d(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);

			float yaw = (float)Math.PI + (float)Math.toRadians(-mc.getVRPlayer().vrdata_room_pre.hmd.getYaw());
			Rotation_room = Matrix4f.rotationY(yaw);
			Rotation_room = Matrix4f.multiply(Rotation_room, OpenVRUtil.rotationXMatrix((float)Math.PI * 0.8f));
		} else if (guiRelative && GuiHandler.guiRotation_room != null) {
			org.lwjgl.util.vector.Matrix4f guiRot = Utils.convertOVRMatrix(GuiHandler.guiRotation_room);
			Vec3d guiUp = new Vec3d(guiRot.m10, guiRot.m11, guiRot.m12);
			Vec3d guiFwd = new Vec3d(guiRot.m20, guiRot.m21, guiRot.m22).scale(0.25f);
			guiUp = guiUp.scale(0.80f);
			matrix.translate(new Vector3f((float)(GuiHandler.guiPos_room.x - guiUp.x), (float)(GuiHandler.guiPos_room.y - guiUp.y), (float)(GuiHandler.guiPos_room.z - guiUp.z)));
			matrix.translate(new Vector3f((float)(guiFwd.x), (float)(guiFwd.y), (float)(guiFwd.z)));
			org.lwjgl.util.vector.Matrix4f.mul(matrix, guiRot, matrix);
			matrix.rotate((float)Math.toRadians(30), new Vector3f(-1, 0, 0)); // tilt it a bit
			Rotation_room =   Utils.convertToOVRMatrix(matrix);
			Pos_room = new Vec3d(Rotation_room.M[0][3],Rotation_room.M[1][3],Rotation_room.M[2][3]);
			Rotation_room.M[0][3] = 0;
			Rotation_room.M[1][3] = 0;
			Rotation_room.M[2][3] = 0;

		} else { //copied from vrplayer.onguiuscreenchanged
			Vec3d v = mc.getVRPlayer().vrdata_room_pre.hmd.getPosition();
			Vec3d adj = new Vec3d(0,-0.5,-2);
			Vec3d e = mc.getVRPlayer().vrdata_room_pre.hmd.getCustomVector(adj);
			Pos_room = new Vec3d(
					(e.x  / 2 + v.x),
					(e.y / 2 + v.y),
					(e.z / 2 + v.z));

			Vec3d pos = mc.getVRPlayer().vrdata_room_pre.hmd.getPosition();
			Vector3 look = new Vector3();
			look.setX((float) (Pos_room.x - pos.x));
			look.setY((float) (Pos_room.y - pos.y));
			look.setZ((float) (Pos_room.z - pos.z));

			float pitch = (float) Math.asin(look.getY()/look.length());
			float yaw = (float) ((float) Math.PI + Math.atan2(look.getX(), look.getZ()));    
			Rotation_room = Matrix4f.rotationY((float) yaw);
		}
	}

	public static void processBindings() {
		if (Showing) {
			if (mc.getVRSettings().physicalKeyboard) {
				physicalKeyboard.processBindings();
				return;
			}

			ScaledResolution s = new ScaledResolution(((Minecraft)mc));
			
			double d0 = UI.cursorX1;	
			double d1 = UI.cursorY1;
			double d2 = UI.cursorX2;
			double d3 = UI.cursorY2;


			if (PointedL && GuiHandler.keyKeyboardClick.isPressed(ControllerType.LEFT)) {
				((IGuiScreenVR)UI).mouseDown((int)d0, (int)d1, 0, false);
				lastPressedClickL = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.LEFT) && lastPressedClickL) {
				((IGuiScreenVR)UI).mouseUp((int)d0, (int)d1, 0, false);
				lastPressedClickL = false;
			}

			if (PointedR && GuiHandler.keyKeyboardClick.isPressed(ControllerType.RIGHT)) {
				((IGuiScreenVR)UI).mouseDown((int)d2, (int)d3, 0, false);
				lastPressedClickR = true;
			}
			if (!GuiHandler.keyKeyboardClick.isKeyDown(ControllerType.RIGHT) && lastPressedClickR) {
				((IGuiScreenVR)UI).mouseUp((int)d2, (int)d3, 0, false);
				lastPressedClickR = false;
			}

			if (GuiHandler.keyKeyboardShift.isPressed()) {
				UI.setShift(true);
				lastPressedShift = true;
			}
			if (!GuiHandler.keyKeyboardShift.isKeyDown() && lastPressedShift) {
				UI.setShift(false);
				lastPressedShift = false;
			}
		}
	}

	public static boolean isUsingController(ControllerType type) {
		if (type == ControllerType.LEFT)
			return PointedL;
		else
			return PointedR;
	}
}
