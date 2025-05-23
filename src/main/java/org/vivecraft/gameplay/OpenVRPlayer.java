package org.vivecraft.gameplay;

import java.util.ArrayList;
import java.util.Random;

import org.vivecraft.api.NetworkHelper;
import org.vivecraft.api.VRData;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.gameplay.trackers.BowTracker;
import org.vivecraft.gameplay.trackers.Tracker;
import org.vivecraft.intermediaries.interfaces.client.entity.IEntityPlayerSPVR;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.AutoCalibration;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;


// VIVE
public class OpenVRPlayer
{
	IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();

	//loop start
	public VRData vrdata_room_pre; //just latest polling data, origin = 0,0,0, rotation = 0, scaleXZ = walkMultiplier
	//handle server messages
	public VRData vrdata_world_pre; //latest polling data but last tick's origin, rotation, scale
	//tick here
	public VRData vrdata_room_post; //recalc here in the odd case the walk multiplier changed
	public VRData vrdata_world_post; //this is used for rendering and the server. _render is interpolated between this and _pre.
	//interpolate here between post and pre
	public VRData vrdata_world_render; // using interpolated origin, scale, rotation
	//loop end

	private long errorPrintTime = Minecraft.getSystemTime();

	ArrayList<Tracker> trackers=new ArrayList<>();
	public void registerTracker(Tracker tracker){
		trackers.add(tracker);
	}

	public OpenVRPlayer() {
		this.vrdata_room_pre = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
		this.vrdata_room_post = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
		this.vrdata_world_post = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
		this.vrdata_world_pre = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
	}

	public float worldScale =  mc.getVRSettings().vrWorldScale;
	private boolean noTeleportClient = true;
	private boolean teleportOverride = false;
	public int teleportWarningTimer = -1;

	public Vec3d roomOrigin = new Vec3d(0,0,0);
	private boolean isFreeMoveCurrent = true; // based on a heuristic of which locomotion type was last used

	//for overriding the world scale settings with wonder foods.
	public double wfMode = 0;
	public int wfCount = 0;
	//

	private int roomScaleMovementDelay = 0;
	public float vrot = 0;
	boolean initdone =false;

	public static OpenVRPlayer get()
	{
		return ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer();
	}

	public Vec3d room_to_world_pos(Vec3d pos, VRData data){
		Vec3d out = new Vec3d(pos.x*data.worldScale, pos.y*data.worldScale, pos.z*worldScale);
		out =out.rotateYaw(data.rotation_radians);
		return out.add(data.origin.x, data.origin.y, data.origin.z);
	}

	public Vec3d world_to_room_pos(Vec3d pos, VRData data){
		Vec3d out = pos.add(-data.origin.x, -data.origin.y, -data.origin.z);
		out = new Vec3d(out.x/data.worldScale, out.y/data.worldScale, out.z/data.worldScale);
		return out.rotateYaw(-data.rotation_radians);
	}

	public void postPoll(){
		this.vrdata_room_pre = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
		GuiHandler.processGui();
		KeyboardHandler.processGui();
		RadialHandler.processGui();
	}

	public void preTick(){

		//adjust world scale
		if (((Minecraft)mc).world == null || ((Minecraft)mc).currentScreen instanceof GuiWinGame)
			this.worldScale = 1.0f;
		else if (this.wfCount > 0 && !((Minecraft)mc).isGamePaused()) {
			if(this.wfCount < 40){
				this.worldScale-=this.wfMode / 2;
				if(this.worldScale >  mc.getVRSettings().vrWorldScale && this.wfMode <0) this.worldScale = mc.getVRSettings().vrWorldScale;
				if(this.worldScale <  mc.getVRSettings().vrWorldScale && this.wfMode >0) this.worldScale = mc.getVRSettings().vrWorldScale;
			} else {
				this.worldScale+=this.wfMode / 2;
				if(this.worldScale >  mc.getVRSettings().vrWorldScale*20) this.worldScale = 20;
				if(this.worldScale <  mc.getVRSettings().vrWorldScale/10) this.worldScale = 0.1f;
			}
			this.wfCount--;
		} else {
			this.worldScale = mc.getVRSettings().vrWorldScale;
		}

		this.vrdata_world_pre = new VRData(this.roomOrigin, mc.getVRSettings().walkMultiplier, worldScale, (float) Math.toRadians(mc.getVRSettings().vrWorldRotation));

		if(mc.getVRSettings().seated)
			mc.getVRSettings().vrWorldRotation = MCOpenVR.seatedRot;

		//Vivecraft - setup the player entity with the correct view for the logic tick.
		doLookOverride(vrdata_world_pre);
		////

	}

	public void postTick(){
		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();

		//Handle all room translations up to this point and then rotate it around the hmd.
		VRData temp = new VRData(roomOrigin, mc.getVRSettings().walkMultiplier, this.worldScale, vrdata_world_pre.rotation_radians);
		float end = mc.getVRSettings().vrWorldRotation;
		float start = (float) Math.toDegrees(vrdata_world_pre.rotation_radians);
		rotateOriginAround(-end+start, temp.getHeadPivot());
		//

		this.vrdata_room_post = new VRData(new Vec3d(0, 0, 0), mc.getVRSettings().walkMultiplier, 1, 0);
		this.vrdata_world_post = new VRData(this.roomOrigin, mc.getVRSettings().walkMultiplier, worldScale, (float) Math.toRadians(mc.getVRSettings().vrWorldRotation));

		//Vivecraft - setup the player entity with the correct view for the logic tick.
		doLookOverride(vrdata_world_post);
		////

		NetworkHelper.sendVRPlayerPositions(this);

	}

	public void preRender(float par1){
		Minecraft mc = Minecraft.getMinecraft();

		//do some interpolatin'

		float interpolatedWorldScale = vrdata_world_post.worldScale*par1 + vrdata_world_pre.worldScale*(1-par1);

		float end = vrdata_world_post.rotation_radians;
		float start = vrdata_world_pre.rotation_radians;

		float difference = Math.abs(end - start);

		if (difference > Math.PI)
			if (end > start)
				start += 2*Math.PI;
			else
				end += 2*Math.PI;

		float interpolatedWorldRotation_Radians = (float) (end*par1 + start*(1-par1));
		//worldRotationRadians += 0.01;
		Vec3d interPolatedRoomOrigin = new Vec3d(
				vrdata_world_pre.origin.x + (vrdata_world_post.origin.x - vrdata_world_pre.origin.x) * (double)par1,
				vrdata_world_pre.origin.y + (vrdata_world_post.origin.y - vrdata_world_pre.origin.y) * (double)par1,
				vrdata_world_pre.origin.z + (vrdata_world_post.origin.z - vrdata_world_pre.origin.z) * (double)par1
		);

		//System.out.println(vrdata_world_post.origin.x + " " + vrdata_world_pre.origin.x + " = " + interPolatedRoomOrigin.x);

		this.vrdata_world_render = new VRData(interPolatedRoomOrigin, ((IMinecraftVR)mc).getVRSettings().walkMultiplier, interpolatedWorldScale, interpolatedWorldRotation_Radians);

		//handle special items
		for (Tracker tracker : trackers) {
			if (tracker.getEntryPoint() == Tracker.EntryPoint.SPECIAL_ITEMS) {
				tracker.idleTick(mc.player);
				if (tracker.isActive(mc.player)){
					tracker.doProcess(mc.player);
				}else{
					tracker.reset(mc.player);
				}
			}
		}


	}

	public void postRender(float par1){
		//insurance.
		vrdata_world_render = null;
	}


	public void setRoomOrigin(double x, double y, double z, boolean reset) {

		if(!reset && vrdata_world_render != null){
			if (Minecraft.getSystemTime() - errorPrintTime >= 1000) { // Only print once per second, since this might happen every frame
				System.out.println("Vivecraft Warning: Room origin set too late! Printing call stack:");
				Thread.dumpStack();
				errorPrintTime = Minecraft.getSystemTime();
			}
			return;
		}

		if (reset){
			if(vrdata_world_pre!=null)
				vrdata_world_pre.origin = new Vec3d(x, y, z);
		}
		roomOrigin = new Vec3d(x, y, z);
	}

	//set room
	public void snapRoomOriginToPlayerEntity(EntityPlayerSP player, boolean reset, boolean instant)
	{
		if (Thread.currentThread().getName().equals("Server thread"))
			return;

		if(player.posX == 0 && player.posY == 0 &&player.posZ == 0) return;

		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();

		if(mc.getSneakTracker().sneakCounter > 0) return; //avoid relocating the view while roomscale dismounting.

		VRData temp = vrdata_world_pre;

		if(instant) temp = new VRData(roomOrigin, mc.getVRSettings().walkMultiplier, this.worldScale, (float) Math.toRadians(mc.getVRSettings().vrWorldRotation));

		Vec3d campos = temp.getHeadPivot().subtract(temp.origin);

		double x,y,z;

		x = player.posX - campos.x;
		z = player.posZ - campos.z;
		y = player.posY;

		setRoomOrigin(x, y, z, reset);
	}


	public float rotDiff_Degrees(float start, float end){ //calculate shortest difference between 2 angles.

		double x = Math.toRadians(end);
		double y = Math.toRadians(start);

		return (float) Math.toDegrees((Math.atan2(Math.sin(x-y), Math.cos(x - y))));
	}

	public void rotateOriginAround(float degrees, Vec3d o){
		Vec3d pt = roomOrigin;


		float rads = (float) Math.toRadians(degrees); //reverse rotate.

		if(rads!=0)
			setRoomOrigin(
					Math.cos(rads) * (pt.x-o.x) - Math.sin(rads) * (pt.z-o.z) + o.x,
					pt.y,
					Math.sin(rads) * (pt.x-o.x) + Math.cos(rads) * (pt.z-o.z) + o.z
					,false);

		VRData test = new VRData(roomOrigin, mc.getVRSettings().walkMultiplier, this.worldScale, (float) Math.toRadians(mc.getVRSettings().vrWorldRotation));

		Vec3d b = vrdata_world_pre.hmd.getPosition();
		Vec3d a = test.hmd.getPosition();
		double dist = b.distanceTo(a); //should always be 0 (unless in a vehicle)
	}

	public void onLivingUpdate(EntityPlayerSP player, Minecraft mc, Random rand)
	{
		if(!((IEntityPlayerSPVR)player).getInitFromServer()) return;

		if(!initdone){

			System.out.println("<Debug info start>");
			System.out.println("Room object: "+this);
			System.out.println("Room origin: " + vrdata_world_pre.origin);
			System.out.println("Hmd position room: " + vrdata_room_pre.hmd.getPosition());
			System.out.println("Hmd position world: " + vrdata_world_pre.hmd.getPosition());
			System.out.println("<Debug info end>");

			initdone =true;
		}


		AutoCalibration.logHeadPos(MCOpenVR.hmdPivotHistory.latest());

		doPlayerMoveInRoom(player);

		for (Tracker tracker : trackers) {
			if (tracker.getEntryPoint() == Tracker.EntryPoint.LIVING_UPDATE) {
				tracker.idleTick(mc.player);
				if (tracker.isActive(mc.player)){
					tracker.doProcess(mc.player);
				}else{
					tracker.reset(mc.player);
				}
			}
		}

		if(((IMinecraftVR)mc).getVRSettings().vrAllowCrawling){
			//experimental
			//           topofhead = (double) (mc.vrPlayer.getHMDPos_Room().y + .05);
			//
			//           if(topofhead < .5) {topofhead = 0.5f;}
			//           if(topofhead > 1.8) {topofhead = 1.8f;}
			//
			//           player.height = (float) topofhead - 0.05f;
			//           player.spEyeHeight = player.height - 1.62f;
			//           player.boundingBox.setMaxY( player.boundingBox.minY +  topofhead);
		} else {
			//    	   player.height = 1.8f;
			//    	   player.spEyeHeight = 0.12f;
		}

		if(player.isRiding()){
			Entity e = mc.player.getRidingEntity();
			if (e instanceof AbstractHorse) {
				AbstractHorse el = (AbstractHorse) e;
				if (el.canBeSteered() && el.isHorseSaddled() && !((IMinecraftVR)mc).getHorseTracker().isActive((EntityPlayerSP)mc.player)){
					el.renderYawOffset = vrdata_world_pre.getBodyYaw();
				}
			}else if (e instanceof EntityLiving) {
				EntityLiving el = (EntityLiving) e; //this is just pigs in vanilla
				if (el.canBeSteered()){
					el.renderYawOffset = vrdata_world_pre.getBodyYaw();
				}
			}
		}

		mc.profiler.endSection();
	}

	public void doPlayerMoveInRoom(EntityPlayerSP player){

		if(roomScaleMovementDelay > 0){
			roomScaleMovementDelay--;
			return;
		}
		IMinecraftVR mc = (IMinecraftVR) Minecraft.getMinecraft();
		if(player == null) return;
		if(player.isSneaking()) {return;} //jrbudda : prevent falling off things or walking up blocks while moving in room scale.
		if(player.isPlayerSleeping()) return; //
		if(mc.getJumpTracker().isjumping()) return; //
		if(mc.getClimbTracker().isGrabbingLadder()) return; //
		if(player.isDead) return; //

		if(mc.getVehicleTracker().canRoomscaleDismount(((Minecraft)mc).player)) {
			Vec3d mountpos = ((Minecraft)mc).player.getRidingEntity().getPositionVector();
			Vec3d tp = vrdata_world_pre.getHeadPivot();
			double dist = Math.sqrt((tp.x - mountpos.x) * (tp.x - mountpos.x) + (tp.z - mountpos.z) *(tp.z - mountpos.z));
			if (dist > 0.85) {
				mc.getSneakTracker().sneakCounter = 5;
			}
			return;
		}

		VRData temp = new VRData(this.roomOrigin, mc.getVRSettings().walkMultiplier, worldScale, this.vrdata_world_pre.rotation_radians);
		//if(Math.abs(player.motionX) > 0.01) return;
		//if(Math.abs(player.motionZ) > 0.01) return;

		float playerHalfWidth = player.width / 2.0F;

		// move player's X/Z coords as the HMD moves around the room

		//OK this is the first place I've found where we reallly need to update the VR data before doing this calculation.

		Vec3d eyePos = temp.getHeadPivot();

		double x = eyePos.x;
		double y = player.posY;
		double z = eyePos.z;

		// create bounding box at dest position
		AxisAlignedBB bb = new AxisAlignedBB(
				x - (double) playerHalfWidth,
				y,
				z - (double) playerHalfWidth,
				x + (double) playerHalfWidth,
				y + (double) player.height,
				z + (double) playerHalfWidth);

		Vec3d torso = null;



		// valid place to move player to?
		float var27 = 0.0625F;
		boolean emptySpot = ((Minecraft)mc).world.getCollisionBoxes(player, bb).isEmpty();

		if (emptySpot)
		{
			// don't call setPosition style functions to avoid shifting room origin
			player.posX = x;
			if (!mc.getVRSettings().simulateFalling)	{
				player.posY = y;
			}
			player.posZ = z;

			player.setEntityBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + player.height, bb.maxZ));
			player.fallDistance = 0.0F;

			torso = getEstimatedTorsoPosition(x, y, z);


		}

		//test for climbing up a block
		else if ((mc.getVRSettings().walkUpBlocks || (mc.getClimbTracker().isGrabbingLadder() && mc.getVRSettings().realisticClimbEnabled)) && player.fallDistance == 0)
		{
			if (torso == null)
			{
				torso = getEstimatedTorsoPosition(x, y, z);
			}

			// is the player significantly inside a block?
			float climbShrink = player.width * 0.45f;
			double shrunkClimbHalfWidth = playerHalfWidth - climbShrink;
			AxisAlignedBB bbClimb = new AxisAlignedBB(
					torso.x - shrunkClimbHalfWidth,
					bb.minY,
					torso.z - shrunkClimbHalfWidth,
					torso.x + shrunkClimbHalfWidth,
					bb.maxY,
					torso.z + shrunkClimbHalfWidth);

			boolean iscollided = !((Minecraft)mc).world.getCollisionBoxes(player, bbClimb).isEmpty();

			if(iscollided){
				double xOffset = torso.x - x;
				double zOffset = torso.z - z;

				bb = bb.offset(xOffset, 0, zOffset);

				int extra = 0;
				if(player.isOnLadder() && mc.getVRSettings().realisticClimbEnabled)
					extra = 6;

				for (int i = 0; i <=10+extra ; i++)
				{
					bb = bb.offset(0, .1, 0);

					emptySpot = ((Minecraft)mc).world.getCollisionBoxes(player, bb).isEmpty();
					if (emptySpot)
					{
						x += xOffset;
						z += zOffset;
						y += 0.1f*i;

						player.posX = x;
						player.posY = y;
						player.posZ = z;

						player.setEntityBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));

						Vec3d dest  = roomOrigin.add(xOffset, 0.1f*i, zOffset);

						setRoomOrigin(dest.x, dest.y, dest.z, false);

						Vec3d look = player.getLookVec();
						Vec3d forward = new Vec3d(look.x,0,look.z).normalize();
						player.fallDistance = 0.0F;
						((IEntityPlayerSPVR)((Minecraft)mc).player).stepSound(new BlockPos(player.getPositionVector()), player.getPositionVector());

						break;
					}
				}
			}
		}
	}




	// use simple neck modeling to estimate torso location
	public Vec3d getEstimatedTorsoPosition(double x, double y, double z)
	{
		Entity player = Minecraft.getMinecraft().player;
		Vec3d look = player.getLookVec();
		Vec3d forward = new Vec3d(look.x, 0, look.z).normalize();
		float factor = (float)look.y * 0.25f;
		Vec3d torso = new Vec3d(
				x + forward.x * factor,
				y + forward.y * factor,
				z + forward.z * factor);

		return torso;
	}




	public void blockDust(double x, double y, double z, int count, IBlockState bs){
		Random rand = new Random();
		for (int i = 0; i < count; ++i)
		{
			Minecraft.getMinecraft().world.spawnParticle(EnumParticleTypes.BLOCK_DUST,
					x+ ((double)rand.nextFloat() - 0.5D)*.02f,
					y + ((double)rand.nextFloat() - 0.5D)*.02f,
					z + ((double)rand.nextFloat()- 0.5D)*.02f,
					((double)rand.nextFloat()- 0.5D)*.1f,((double)rand.nextFloat()- 0.5D)*.05f,((double)rand.nextFloat()- 0.5D)*.1f,
					new int[] {Block.getStateId(bs)});
		}
	}








	public void updateFreeMove() {
		if (mc.getTeleportTracker().isAiming())
			isFreeMoveCurrent = false;
		if (((Minecraft)mc).player.movementInput.moveForward != 0 || ((Minecraft)mc).player.movementInput.moveStrafe != 0)
			isFreeMoveCurrent = true;
		updateTeleportKeys();
	}

	/**
	 * Again with the weird logic, see {@link #isTeleportEnabled()}
	 *
	 * @return
	 */
	public boolean getFreeMove() {
		if (mc.getVRSettings().seated)
			return mc.getVRSettings().seatedFreeMove || !isTeleportEnabled();
		else
			return isFreeMoveCurrent || mc.getVRSettings().forceStandingFreeMove;
	}

	@Override
	public String toString() {
		return "VRPlayer: " +
				"\r\n \t origin: " + this.roomOrigin +
				"\r\n \t rotation: " + String.format("%.3f", ((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings().vrWorldRotation) +
				"\r\n \t scale: " + String.format("%.3f", this.worldScale) +
				"\r\n \t room_pre " + this.vrdata_room_pre +
				"\r\n \t world_pre " + this.vrdata_world_pre +
				"\r\n \t world_post " + this.vrdata_world_post +
				"\r\n \t world_render " + this.vrdata_world_render ;
	}


	public void doLookOverride(VRData data){
		EntityPlayerSP entity = ((Minecraft)mc).player;
		if(entity == null)return;
		//This is used for all sorts of things both client and server side.

		if(false){  //hmm, to use HMD? literally never.
			//set model view direction to camera
			//entity.rotationYawHead = entity.rotationYaw = (float)mc.vrPlayer.getHMDYaw_World();
			//entity.rotationPitch = (float)mc.vrPlayer.getHMDPitch_World();
		} else { //default to looking 'at' the crosshair position.
			if(((IEntityRendererVR)((Minecraft)mc).entityRenderer).getCrossVec() != null){
				Vec3d aimPos = ((IEntityRendererVR)((Minecraft)mc).entityRenderer).getCrossVec();
				if (((Minecraft)mc).objectMouseOver != null && ((Minecraft)mc).objectMouseOver.typeOfHit != RayTraceResult.Type.MISS)
					aimPos = ((Minecraft)mc).objectMouseOver.hitVec;
				Vec3d playerToCrosshair = entity.getPositionEyes(1).subtract(aimPos); //backwards
				double what = playerToCrosshair.y/playerToCrosshair.length();
				if(what > 1) what = 1;
				if(what < -1) what = -1;
				float pitch = (float)Math.toDegrees(Math.asin(what));
				float yaw = (float)Math.toDegrees(Math.atan2(playerToCrosshair.x, -playerToCrosshair.z));
				entity.rotationYaw = entity.rotationYawHead = yaw;
				entity.rotationPitch = pitch;
			}
		}

		ItemStack i = ((EntityPlayerSP) entity).inventory.getCurrentItem();

		if((entity.isSprinting() && entity.movementInput.jump) || entity.isElytraFlying() || (entity.isRiding() && entity.moveForward > 0)){
			//us needed for server side movement.
			if(mc.getVRSettings().vrFreeMoveMode == mc.getVRSettings().FREEMOVE_CONTROLLER ){
				entity.rotationYaw = data.getController(1).getYaw();
				entity.rotationPitch = -data.getController(1).getPitch();
			}else{
				entity.rotationYaw = data.hmd.getYaw();
				entity.rotationPitch = -data.hmd.getPitch();
			}
		} else if(i.getItem() == Items.SNOWBALL ||
				i.getItem() == Items.EGG  ||
				i.getItem() == Items.SPAWN_EGG  ||
				i.getItem() == Items.POTIONITEM
		) {
			//use r_hand aim
			entity.rotationYawHead = entity.rotationYaw =  data.getController(0).getYaw();
			entity.rotationPitch = -data.getController(0).getPitch();
		} else if (BowTracker.isHoldingBowEither(entity) && mc.getBowTracker().isNotched()){
			//use bow aim
			Vec3d aim = mc.getBowTracker().getAimVector(); //this is actually reversed
			if (aim != null && aim.lengthSquared() > 0) {
				float pitch = (float)Math.toDegrees(Math.asin(aim.y/aim.length()));
				float yaw = (float)Math.toDegrees(Math.atan2(aim.x, -aim.z));
				entity.rotationYaw = (float)yaw;
				entity.rotationPitch = (float)pitch;
				entity.rotationYawHead = yaw;
			}
		}


		if(mc.getSwingTracker().shouldIlookatMyHand[0]){
			Vec3d playerToMain = entity.getPositionEyes(1).subtract(data.getController(0).getPosition()); //backwards
			float pitch =(float)Math.toDegrees(Math.asin(playerToMain.y/playerToMain.length()));
			float yaw = (float)Math.toDegrees(Math.atan2(playerToMain.x,-playerToMain.z));
			entity.rotationYawHead  = entity.rotationYaw = yaw;
			entity.rotationPitch = pitch;
		}
		else if(mc.getSwingTracker().shouldIlookatMyHand[1]){
			Vec3d playerToMain = entity.getPositionEyes(1).subtract(data.getController(1).getPosition()); //backwards
			float pitch = (float)Math.toDegrees(Math.asin(playerToMain.y/playerToMain.length()));
			float yaw = (float)Math.toDegrees(Math.atan2(playerToMain.x, -playerToMain.z));
			entity.rotationYawHead  = entity.rotationYaw = yaw;
			entity.rotationPitch = pitch;
		}
	}

	public Vec3d AimedPointAtDistance(VRData source, int controller, double distance) {
		Vec3d vec3d = source.getController(controller).getPosition();
		Vec3d vec3d1 = source.getController(controller).getDirection();
		return vec3d.add(vec3d1.x * distance, vec3d1.y * distance, vec3d1.z * distance);
	}

	public RayTraceResult rayTraceVR(VRData source, int controller, double blockReachDistance)
	{
		Vec3d vec3d = source.getController(controller).getPosition();
		Vec3d vec3d2 = AimedPointAtDistance(source, controller, blockReachDistance);
		return ((Minecraft)mc).world.rayTraceBlocks(vec3d, vec3d2, false, false, true);
	}
	public boolean isTeleportSupported() {
		return !noTeleportClient;
	}

	public boolean isTeleportOverridden() {
		return teleportOverride;
	}

	/**
	 * The logic here is a bit weird, because teleport is actually still enabled even in
	 * seated free move mode. You could use it by simply binding it in the vanilla controls.
	 * However, when free move is forced in standing mode, teleport is outright disabled.
	 *
	 * @return
	 */
	public boolean isTeleportEnabled() {
		boolean enabled = !noTeleportClient || teleportOverride;
		if (!mc.getVRSettings().seated)
			return enabled && !mc.getVRSettings().forceStandingFreeMove;
		else
			return enabled;
	}

	public void setTeleportSupported(boolean supported) {
		noTeleportClient = !supported;
		updateTeleportKeys();
	}

	public void setTeleportOverride(boolean override) {
		teleportOverride = override;
		updateTeleportKeys();
	}

	private void updateTeleportKeys() {
		MCOpenVR.getInputAction(MCOpenVR.keyTeleport).setEnabled(isTeleportEnabled());
		MCOpenVR.getInputAction(MCOpenVR.keyTeleportFallback).setEnabled(!isTeleportEnabled());
	}
}

