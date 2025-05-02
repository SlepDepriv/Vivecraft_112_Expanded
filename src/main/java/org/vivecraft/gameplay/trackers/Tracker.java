package org.vivecraft.gameplay.trackers;

import net.minecraft.client.entity.EntityPlayerSP;
import org.vivecraft.gameplay.OpenVRPlayer;

import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;

/**
 * register in {@link OpenVRPlayer}
 * */
public abstract class Tracker {
	public IMinecraftVR mc;
	public Tracker(IMinecraftVR mc){
		this.mc=mc;
	}

	public abstract boolean isActive(EntityPlayerSP player);
	public abstract void doProcess(EntityPlayerSP player);
	public void reset(EntityPlayerSP player){}
	public void idleTick(EntityPlayerSP player){}

	public EntryPoint getEntryPoint(){return EntryPoint.LIVING_UPDATE;}

	public enum EntryPoint{
		LIVING_UPDATE, SPECIAL_ITEMS
	}
}
