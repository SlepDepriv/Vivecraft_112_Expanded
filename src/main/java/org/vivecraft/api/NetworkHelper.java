package org.vivecraft.api;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.render.PlayerModelController;
import org.vivecraft.utils.Quaternion;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.util.vector.Matrix4f;

public class NetworkHelper {

	public static Map<UUID, VivePlayer> vivePlayers = new HashMap<UUID, VivePlayer>();
	
	public enum PacketDiscriminators {
		VERSION,
		REQUESTDATA,
		HEADDATA,
		CONTROLLER0DATA,
		CONTROLLER1DATA,
		WORLDSCALE,
		DRAW,
		MOVEMODE,
		UBERPACKET,
		TELEPORT,
		CLIMBING
	}
	private final static String channel = "Vivecraft";
	
	public static CPacketCustomPayload getVivecraftClientPacket(PacketDiscriminators command, byte[] payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
        return  (new CPacketCustomPayload(channel, pb));
	}
	
	public static SPacketCustomPayload getVivecraftServerPacket(PacketDiscriminators command, byte[] payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
        return (new SPacketCustomPayload(channel, pb));
	}
	
	public static SPacketCustomPayload getVivecraftServerPacket(PacketDiscriminators command, String payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeString(payload);
        return (new SPacketCustomPayload(channel, pb));
	}
	
	
	public static boolean serverWantsData = false;
	public static boolean serverAllowsClimbey = false;
	public static boolean serverSupportsDirectTeleport = false;
	
	private static float worldScallast = 0;

	public static void sendVRPlayerPositions(OpenVRPlayer player) {
		if(!serverWantsData) return;
		if(Minecraft.getMinecraft().getConnection() == null) return;
		float worldScale = ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().vrdata_world_post.worldScale;
	
		if (worldScale != worldScallast) {
			ByteBuf payload = Unpooled.buffer();
			payload.writeFloat(worldScale);
			byte[] out = new byte[payload.readableBytes()];
			payload.readBytes(out);
			CPacketCustomPayload pack = getVivecraftClientPacket(PacketDiscriminators.WORLDSCALE,out);
			Minecraft.getMinecraft().getConnection().sendPacket(pack);
			
			worldScallast = worldScale;
		}
		byte[] a=null, b = null, c=null;
		{
			FloatBuffer buffer = player.vrdata_world_post.hmd.getMatrix().toFloatBuffer();
			buffer.rewind();
			Matrix4f matrix = new Matrix4f();
			matrix.load(buffer);

			Vec3d headPosition = player.vrdata_world_post.getHeadPivot();
			Quaternion headRotation = new Quaternion(matrix);
			
			ByteBuf payload = Unpooled.buffer();
			payload.writeBoolean(((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings().seated);
			payload.writeFloat((float)headPosition.x);
			payload.writeFloat((float)headPosition.y);
			payload.writeFloat((float)headPosition.z);
			payload.writeFloat((float)headRotation.w);
			payload.writeFloat((float)headRotation.x);
			payload.writeFloat((float)headRotation.y);
			payload.writeFloat((float)headRotation.z);
			byte[] out = new byte[payload.readableBytes()];
			payload.readBytes(out);
			a = out;
			CPacketCustomPayload pack = getVivecraftClientPacket(PacketDiscriminators.HEADDATA,out);
			Minecraft.getMinecraft().getConnection().sendPacket(pack);
			
		}	
		
		for (int i = 0; i < 2; i++) {
			Vec3d controllerPosition = player.vrdata_world_post.getController(i).getPosition();
			FloatBuffer buffer = player.vrdata_world_post.getController(i).getMatrix().toFloatBuffer();
			buffer.rewind();
			Matrix4f matrix = new Matrix4f();
			matrix.load(buffer);
			Quaternion controllerRotation = new Quaternion(matrix);		
			ByteBuf payload = Unpooled.buffer();
			payload.writeBoolean(((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings().vrReverseHands);
			payload.writeFloat((float)controllerPosition.x);
			payload.writeFloat((float)controllerPosition.y);
			payload.writeFloat((float)controllerPosition.z);
			payload.writeFloat((float)controllerRotation.w);
			payload.writeFloat((float)controllerRotation.x);
			payload.writeFloat((float)controllerRotation.y);
			payload.writeFloat((float)controllerRotation.z);
			byte[] out = new byte[payload.readableBytes()];
			if(i == 0) b = out;
			else c = out;
			payload.readBytes(out);
			CPacketCustomPayload pack  = getVivecraftClientPacket(i == 0? PacketDiscriminators.CONTROLLER0DATA : PacketDiscriminators.CONTROLLER1DATA,out);
			Minecraft.getMinecraft().getConnection().sendPacket(pack);
		}
		
		PlayerModelController.getInstance().Update(Minecraft.getMinecraft().player.getUniqueID(), a, b, c, true);
		
	}
	
	
	public static boolean isVive(EntityPlayerMP p){
		if(p == null) return false;
			if(vivePlayers.containsKey(p.getUniqueID())){
				return vivePlayers.get(p.getUniqueID()).isVR();
			}
		return false;
	}
	
	public static void sendPosData(EntityPlayerMP from) {

		VivePlayer v = vivePlayers.get(from.getUniqueID());
		if (v==null || v.isVR() == false || v.player == null || v.player.hasDisconnected()) return;

		for (VivePlayer sendTo : vivePlayers.values()) {

			if (sendTo == null || sendTo.player == null || sendTo.player.hasDisconnected())
				continue; // dunno y but just in case.

			if (v == sendTo || v.player.getEntityWorld() != sendTo.player.getEntityWorld() || v.hmdData == null || v.controller0data == null || v.controller1data == null){
				continue;
			}

			double d = sendTo.player.getPositionVector().squareDistanceTo(v.player.getPositionVector());

			if (d < 256 * 256) {
				SPacketCustomPayload pack  = getVivecraftServerPacket(PacketDiscriminators.UBERPACKET, v.getUberPacket());
				sendTo.player.connection.sendPacket(pack);
			}
		}
	}

	public static void sendVersionInfo() {
		byte[] version = Minecraft.getMinecraft().getVersion().getBytes(Charsets.UTF_8);
		String s = "Vivecraft";
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeBytes(s.getBytes());
		Minecraft.getMinecraft().getConnection().sendPacket(new CPacketCustomPayload("REGISTER", pb ));
		Minecraft.getMinecraft().getConnection().sendPacket(new CPacketCustomPayload("MC|Vive|Version", (new PacketBuffer(Unpooled.buffer())).writeString(Minecraft.getMinecraft().getVersion())));
		Minecraft.getMinecraft().getConnection().sendPacket(NetworkHelper.getVivecraftClientPacket(PacketDiscriminators.VERSION, version));
		((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().teleportWarningTimer = 20 * 3;
	}

	public static void resetServerSettings() {
		worldScallast = 0;
		serverAllowsClimbey = false;
		serverWantsData = false;
		serverSupportsDirectTeleport = false;
	}
	
}
