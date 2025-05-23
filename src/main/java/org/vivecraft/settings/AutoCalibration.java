package org.vivecraft.settings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.provider.MCOpenVR;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

public class AutoCalibration {

	static double magnetDistance=0.05;
	static int weightCap =100;
	public static MagnetPoint currentCalibration;
	public static final float defaultHeight=1.65f;

	static Set<MagnetPoint> magnetSet=new HashSet<MagnetPoint>();

	public static void logHeadPos(Vec3d headpos){
		double y=headpos.y;
		MagnetPoint p =findPoint(y);
		if(p==null){
			p=new MagnetPoint();
			p.center=y;
			p.weight=1;
			magnetSet.add(p);
		}else{
			p.center=(p.center*0.75+y*0.25);
			applySucc(p);
			applyHeightBonus();
			if(p.weight > weightCap)
				p.weight = weightCap;
		}

		getBestEstimate();
	}

	private static void applyHeightBonus() {
		double highest=-1;
		MagnetPoint href=null;
		for (MagnetPoint m: magnetSet) {
			if(highest<m.center){
				href=m;
				highest=m.center;
			}
		}
		Iterator<MagnetPoint> i=magnetSet.iterator();
		while(i.hasNext()){
			MagnetPoint m=i.next();
			if(m.equals(href)){
				if(m.succ>0)
					m.weight++;
				else
					if(m.weight<weightCap){
						m.weight--;
						if(m.weight<0)
							i.remove();
					}
			} else {
				m.weight--;
				if(m.weight<0)
					i.remove();
			}
		}
	}

	static void applySucc(MagnetPoint p){
		for (MagnetPoint m: magnetSet) {
			if(m.equals(p))
				m.succ++;
			else
				m.succ=0;
		}
	}

	static MagnetPoint findPoint(double y){
		for (MagnetPoint m: magnetSet) {
			if(Math.abs(y-m.center)<magnetDistance){
				return m;
			}
		}
		return null;
	}


	static void getBestEstimate(){
		double highest=-1;
		MagnetPoint href=null;

		for (MagnetPoint m : magnetSet) {
			if (highest < m.weight) {
				href = m;
				highest = m.weight;
			}
		}

		currentCalibration=href;
	}

	public static void calibrateManual() {
		IMinecraftVR mc= (IMinecraftVR) Minecraft.getMinecraft();
		mc.getVRSettings().manualCalibration=(float) MCOpenVR.hmdPivotHistory.latest().y;
		mc.printChatMessage(String.format("User height set to %.2fm",mc.getVRSettings().manualCalibration));
	}

	public static class MagnetPoint{
		public double center;
		public int weight;
		int succ;
	}

	public static float getPlayerHeight(){
		IMinecraftVR mc= (IMinecraftVR) Minecraft.getMinecraft();
		if(mc.getVRSettings().manualCalibration!=-1)
			return mc.getVRSettings().manualCalibration;
		if(currentCalibration==null || currentCalibration.weight<weightCap) {
			if (mc.getVRSettings().autoCalibration != -1)
				return mc.getVRSettings().autoCalibration;
			else
				return defaultHeight;
		}
		return (float) currentCalibration.center;
	}
}
