package org.vivecraft.gameplay.trackers;

import net.minecraft.client.entity.EntityPlayerSP;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.provider.MCOpenVR;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class BackpackTracker extends Tracker {
	public boolean[] wasIn = new boolean[2];
	public boolean[] hystersis = new boolean[2];

	public int previousSlot = 0;

	public BackpackTracker(IMinecraftVR mc) {
		super(mc);
	}

	public boolean isActive(EntityPlayerSP p){
		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
		if(mc.getVRSettings().seated) return false;
		if(!mc.getVRSettings().backpackSwitching) return false;
		if(p == null) return false;
		if(p.isDead) return false;
		if(p.isPlayerSleeping()) return false;
		if(mc.getBowTracker().isDrawing) return false;
		return true;
	}

	
	private Vec3d down = new Vec3d(0, -1, 0);
	
	public void doProcess(EntityPlayerSP player){
		OpenVRPlayer provider = mc.getVRPlayer();

		Vec3d hmdPos=provider.vrdata_room_pre.getHeadRear();
		
		for(int c=0; c<2; c++) {
			Vec3d controllerPos = provider.vrdata_room_pre.getController(c).getPosition();//.add(provider.getCustomControllerVector(c, new Vec3(0, 0, -0.1)));
			Vec3d controllerDir = provider.vrdata_room_pre.getHand(c).getDirection();
			Vec3d hmddir = provider.vrdata_room_pre.hmd.getDirection();
			Vec3d delta = hmdPos.subtract(controllerPos);
			double dot = controllerDir.dotProduct(down);
			double dotDelta = delta.dotProduct(hmddir);
			
			boolean below  = ((Math.abs(hmdPos.y - controllerPos.y)) < 0.25);
			boolean behind = (dotDelta > 0); 
			boolean aimdown = (dot > .6);
			
			boolean zone = below && behind && aimdown;

			IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
			if (zone){
				if(!wasIn[c]){
					if(c==0){ //mainhand
						if((mc.getClimbTracker().isGrabbingLadder() &&
								mc.getClimbTracker().isClaws(((Minecraft)mc).player.getHeldItemMainhand()))){}
						else{
						if(player.inventory.currentItem != 0){
							previousSlot = player.inventory.currentItem;
							player.inventory.currentItem = 0;	
						} else {
							player.inventory.currentItem = previousSlot;
							previousSlot = 0;
						}}
					}
					else { //offhand
						if((mc.getClimbTracker().isGrabbingLadder() &&
								mc.getClimbTracker().isClaws(((Minecraft)mc).player.getHeldItemOffhand()))){}
						else {
							player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
					}
					}
					MCOpenVR.triggerHapticPulse(c, 1500);
					wasIn[c] = true;
					hystersis[c] = true;
				}
			} else {
				if(hystersis[c]) {
					wasIn[c] = !behind && !aimdown;
					hystersis[c] = wasIn[c];
				} else {
				wasIn[c] = false;
			}
		}
}
}

}
