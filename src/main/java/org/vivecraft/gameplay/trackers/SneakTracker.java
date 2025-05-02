package org.vivecraft.gameplay.trackers;

import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.AutoCalibration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class SneakTracker extends Tracker {
	public boolean sneakOverride=false;
	public int sneakCounter = 0;

	public SneakTracker(IMinecraftVR mc) {
		super(mc);
	}

	public boolean isActive(EntityPlayerSP p){
		if(mc.getVRSettings().seated)
			return false;
		if(!mc.getVRPlayer().getFreeMove() && !mc.getVRSettings().simulateFalling)
			return false;
		if(!mc.getVRSettings().realisticSneakEnabled)
			return false;
		if(p==null || p.isDead || !p.onGround)
			return false;
		if(p.isRiding())
			return false;
		return true;
	}

	@Override
	public void reset(EntityPlayerSP player) {
		sneakOverride = false;
	}

	public void doProcess(EntityPlayerSP player){

		if(!((Minecraft)mc).isGamePaused()) {
			if (mc.getSneakTracker().sneakCounter > 0)
				mc.getSneakTracker().sneakCounter--;
		}
		
	    if(( AutoCalibration.getPlayerHeight() - MCOpenVR.hmdPivotHistory.latest().y )> mc.getVRSettings().sneakThreshold){
		   sneakOverride=true;
	    }else{
		    sneakOverride=false;
	    }
	}

}
