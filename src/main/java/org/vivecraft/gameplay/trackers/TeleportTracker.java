package org.vivecraft.gameplay.trackers;

import java.util.Random;

import org.vivecraft.api.NetworkHelper;
import org.vivecraft.gameplay.VRMovementStyle;
import org.vivecraft.intermediaries.interfaces.client.entity.IEntityPlayerSPVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IItemRendererVR;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.utils.Angle;
import org.vivecraft.utils.Matrix4f;
import org.vivecraft.utils.OpenVRUtil;
import org.vivecraft.utils.Quaternion;
import org.vivecraft.utils.Vector3;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TeleportTracker extends Tracker{
    private float teleportEnergy;
    private Vec3d movementTeleportDestination = new Vec3d(0.0,0.0,0.0);
    private EnumFacing movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public double movementTeleportDistance;
    private Vec3d[] movementTeleportArc = new Vec3d[50];
    public int movementTeleportArcSteps = 0;
    public double lastTeleportArcDisplayOffset = 0;
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();

	public TeleportTracker(IMinecraftVR mc) {
		super(mc);
	}

	public float getTeleportEnergy () {return teleportEnergy;	}

    public boolean isAiming(){
    	return movementTeleportProgress > 0;
    }
    
    public Vec3d getDestination(){
    	return movementTeleportDestination;
    }
    
	public boolean isActive(EntityPlayerSP p){
		if(p == null) return false;
		if(p.isDead) return false;
		if(p.isPlayerSleeping()) return false;
		return true;
	}


	@Override
	public void reset(EntityPlayerSP player) {
		movementTeleportDestination=new Vec3d(0,0,0);
		movementTeleportArcSteps = 0;
		movementTeleportProgress = 0;
	}

	public void doProcess(EntityPlayerSP player){ //on tick


		Random rand = new Random();

        if (teleportEnergy < 100) { teleportEnergy++;}
        
        boolean doTeleport = false;
        Vec3d dest = null;

        boolean bindingTeleport = MCOpenVR.keyTeleport.isKeyDown() && mc.getVRPlayer().isTeleportEnabled();
        boolean seatedTeleport = mc.getVRSettings().seated && !mc.getVRPlayer().getFreeMove() && (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0);

        if ((bindingTeleport || seatedTeleport) && !player.isRiding())
        {
            dest = movementTeleportDestination;

            if (vrMovementStyle.teleportOnRelease)
            {
                if (((IEntityPlayerSPVR)player).getMovementTeleportTimer()==0)
                {
                    String sound = vrMovementStyle.startTeleportingSound;
                    if (sound != null)
                    {
//                        player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
//                                1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                    }
                }
                ((IEntityPlayerSPVR)player).setMovementTeleportTimer(((IEntityPlayerSPVR)player).getMovementTeleportTimer() + 1);
                if (((IEntityPlayerSPVR)player).getMovementTeleportTimer() > 0)
                {
                    movementTeleportProgress = (float) ((IEntityPlayerSPVR)player).getMovementTeleportTimer() / 1.0f;
                    if (movementTeleportProgress>=1.0f)
                    {
                        movementTeleportProgress = 1.0f;
                    }

                    if (dest.x != 0 || dest.y != 0 || dest.z != 0)
                    {
                        Vec3d eyeCenterPos = mc.getVRPlayer().vrdata_world_pre.hmd.getPosition();

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.add(-eyeCenterPos.x, -eyeCenterPos.y, -eyeCenterPos.z).normalize();
                        Vec3d forward;
						
						forward	= player.getLookVec();

                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;

                                Vec3d sparkPos = new Vec3d(eyeCenterPos.x + forward.x * forwardDist,
                                        eyeCenterPos.y + forward.y * forwardDist,
                                        eyeCenterPos.z + forward.z * forwardDist);
                                sparkPos = sparkPos.add(right.x * rightDist, right.y * rightDist, right.z * rightDist);
                                sparkPos = sparkPos.add(up.x * upDist, up.y * upDist, up.z * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.x, sparkPos.y, sparkPos.z,
//                                        motionDir.x * speed, motionDir.y * speed, motionDir.z * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    }
                }
            }
            else
            {
                if (((IEntityPlayerSPVR)player).getMovementTeleportTimer() >= 0 && (dest.x != 0 || dest.y != 0 || dest.z != 0))
                {
                    if (((IEntityPlayerSPVR)player).getMovementTeleportTimer() == 0)
                    {
                        String sound = vrMovementStyle.startTeleportingSound;
                        if (sound != null)
                        {
//                            player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
//                                    1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                        }
                    }
                    ((IEntityPlayerSPVR)player).setMovementTeleportTimer(((IEntityPlayerSPVR)player).getMovementTeleportTimer() + 1);

                    Vec3d playerPos = new Vec3d(player.posX, player.posY, player.posZ);
                    double dist = dest.distanceTo(playerPos);
                    double progress = (((IEntityPlayerSPVR)player).getMovementTeleportTimer() * 1.0) / (dist + 3.0);

                    if (((IEntityPlayerSPVR)player).getMovementTeleportTimer() > 0)
                    {
                        movementTeleportProgress = progress;

                        // spark at dest point
                        if (vrMovementStyle.destinationSparkles)
                        {
                          //  player.world.spawnParticle("instantSpell", dest.x, dest.y, dest.z, 0, 1.0, 0);
                        }

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.add(-player.posX, -player.posY, -player.posZ).normalize();
                        Vec3d forward = player.getLookVec();
                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;
                                Vec3d sparkPos = new Vec3d(player.posX + forward.x * forwardDist,
                                        player.posY + forward.y * forwardDist,
                                        player.posZ + forward.z * forwardDist);
                                sparkPos = sparkPos.add(right.x * rightDist, right.y * rightDist, right.z * rightDist);
                                sparkPos = sparkPos.add(up.x * upDist, up.y * upDist, up.z * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.x, sparkPos.y, sparkPos.z,
//                                        motionDir.x * speed, motionDir.y * speed, motionDir.z * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    } else
                    {
                        movementTeleportProgress = 0;
                    }

                    if (progress >= 1.0)
                    {
                        doTeleport = true;
                    }
                }
            }
        }
        else //not holding down Ltrigger
        {
            if (vrMovementStyle.teleportOnRelease && movementTeleportProgress>=1.0f)
            {
                dest = movementTeleportDestination;
                doTeleport = true;
            }
            ((IEntityPlayerSPVR)player).setMovementTeleportTimer(0);
            movementTeleportProgress = 0;
        }

        if (doTeleport && dest!=null && (dest.x != 0 || dest.y !=0 || dest.z != 0)) //execute teleport
        {
            movementTeleportDistance = (float)MathHelper.sqrt(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playCustomTeleportSound = movementTeleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
            Block block = null;

            if (playCustomTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
//                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }

     	   //execute teleport               
            if(!mc.getVRPlayer().isTeleportSupported()){
            	String tp = "/tp " + ((Minecraft)mc).player.getName() + " " + dest.x + " " +dest.y + " " + dest.z;
                ((Minecraft)mc).player.sendChatMessage(tp);
            } else {
            	if(NetworkHelper.serverSupportsDirectTeleport)	((IEntityPlayerSPVR)player).setTeleported(true);
            	player.setLocationAndAngles(dest.x, dest.y, dest.z, player.rotationYaw, player.rotationPitch);
            }

            doTeleportCallback();
            
          //  System.out.println("teleport " + dest.toString());

            if (playCustomTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
//                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                ((IEntityPlayerSPVR)((Minecraft)mc).player).stepSound(new BlockPos(dest), dest);
            }
        }
  
	}
	
    public void updateTeleportDestinations(EntityRenderer renderer, Minecraft mc, Entity player)
    { //called every frame
        mc.profiler.startSection("updateTeleportDestinations");

        // no teleporting if on a server that disallows teleporting

        if (vrMovementStyle.arcAiming)
        {
            movementTeleportDestination=new Vec3d(0,0,0);

            if (movementTeleportProgress>0.0f)
            {
                updateTeleportArc(mc, player);
            }
        }
        else //non-arc modes.
        {
//            Vec3d start = mc.entityRenderer.getControllerRenderPos(1);
//            Vec3d aimDir = mc.vrPlayer.vrdata_world_render.getController(1).getDirection();
//            
//            // setup teleport forwards to the mouse cursor
//            double movementTeleportDistance = 250.0;
//            Vec3d movementTeleportPos = start.addVector(
//                    aimDir.x * movementTeleportDistance,
//                    aimDir.y * movementTeleportDistance,
//                    aimDir.z * movementTeleportDistance);
//            RayTraceResult collision = mc.world.rayTraceBlocks(start, movementTeleportPos, !mc.player.isInWater(), true, false);
//            Vec3d traceDir = start.subtract(movementTeleportPos).normalize();
//            Vec3d reverseEpsilon = new Vec3d(-traceDir.x * 0.02, -traceDir.y * 0.02, -traceDir.z * 0.02);
//
//            // don't update while charging up a teleport
//            if (movementTeleportProgress != 0)
//                return;
//
//            if (collision != null && collision.typeOfHit != Type.MISS)
//            {
//                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
//            }
        }
        mc.profiler.endSection();
    }

    private void updateTeleportArc(Minecraft mc, Entity player)
    {
        Vec3d start = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(1).getPosition(); //and here i was just thinking there was never a need to use the render positions for logic.
        Vec3d tiltedAim = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(1).getDirection();
        Matrix4f handRotation = MCOpenVR.getAimRotation(1);
        
        if(((IMinecraftVR)mc).getVRSettings().seated){
        	start = ((IEntityRendererVR)mc.entityRenderer).getControllerRenderPos(0);
        	tiltedAim = ((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.getController(0).getDirection();
        	handRotation =MCOpenVR.getAimRotation(0);
        }
        
        Matrix4f rot = Matrix4f.rotationY(((IMinecraftVR)mc).getVRPlayer().vrdata_world_render.rotation_radians);
        handRotation = Matrix4f.multiply(rot, handRotation);
        
        // extract hand roll
        Quaternion handQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(handRotation);
        Angle euler = handQuat.toEuler();
        //TODO: use vrdata for this
        
        int maxSteps = 50;
        movementTeleportArc[0] = new Vec3d(
        		start.x,
        		start.y,
        		start.z);
        
        movementTeleportArcSteps = 1;

        // calculate gravity vector for arc
        float gravityAcceleration = 0.098f;
        Matrix4f rollCounter = OpenVRUtil.rotationZMatrix((float)Math.toRadians(-euler.getRoll()));
        Matrix4f gravityTilt = OpenVRUtil.rotationXMatrix((float)Math.PI * -.8f);
        Matrix4f gravityRotation = Matrix4f.multiply(handRotation, rollCounter);
        
        Vector3 forward = new Vector3(0,1,0);
        Vector3 gravityDirection = gravityRotation.transform(forward);
        Vec3d gravity = gravityDirection.multiply(-1).toVec3d();
        
        gravity = gravity.scale(gravityAcceleration);

        
     //   gravity.rotateAroundY(this.worldRotationRadians);

        // calculate initial move step	
        float speed = 0.5f;
        Vec3d velocity = new Vec3d(
                tiltedAim.x * speed,
                tiltedAim.y * speed,
                tiltedAim.z * speed);

        Vec3d pos = new Vec3d(start.x, start.y, start.z);
        Vec3d newPos;

        // trace arc
        for (int i=movementTeleportArcSteps;i<maxSteps;i++)
        {
        	if (i*4 > teleportEnergy) {
        		break;
        		}
        	newPos = new Vec3d(
            pos.x + velocity.x,
            pos.y + velocity.y,
            pos.z + velocity.z);

      	
            boolean	water =false;
            if(((IMinecraftVR)mc).getVRSettings().seated )
            	water = ((IEntityRendererVR)mc.entityRenderer).getInWater();
            else{
                water = ((IItemRendererVR)mc.entityRenderer.itemRenderer).isInsideOfMaterial(start, Material.WATER);
            }
            
            RayTraceResult collision = tpRaytrace(player.world, pos, newPos, !water, true, false);
			
            if (collision != null && collision.typeOfHit != Type.MISS)
            {
        		
                movementTeleportArc[i] = new Vec3d(
                		collision.hitVec.x,
                		collision.hitVec.y,
                		collision.hitVec.z);

                movementTeleportArcSteps = i + 1;

                Vec3d traceDir = pos.subtract(newPos).normalize();
                Vec3d reverseEpsilon = new Vec3d(-traceDir.x * 0.02, -traceDir.y * 0.02, -traceDir.z * 0.02);

                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
                          
                
    			Vec3d diff = mc.player.getPositionVector().subtract(movementTeleportDestination);
       
        		double yDiff = diff.y;
        		movementTeleportDistance = diff.length();
        		double xzdiff = Math.sqrt(diff.x * diff.x + diff.z*diff.z);
        		
        		boolean ok = true;
        		
            	if(mc.player.isSneaking()) {
            		if(yDiff > 0.2)
            			ok = false;
            	}        	

            	if (!mc.player.capabilities.allowFlying && ((IMinecraftVR)mc).getVRSettings().vrLimitedSurvivalTeleport) { //survival mode mode
        			if(((IMinecraftVR)mc).getVRSettings().vrTeleportDownLimit > 0 && yDiff > ((IMinecraftVR)mc).getVRSettings().vrTeleportDownLimit + 0.2)
        	    		ok = false;
        			else if(((IMinecraftVR)mc).getVRSettings().vrTeleportUpLimit > 0 && -yDiff > ((IMinecraftVR)mc).getVRSettings().vrTeleportUpLimit + 0.2)
        	    		ok = false;  			
        			else if(((IMinecraftVR)mc).getVRSettings().vrTeleportHorizLimit > 0 && xzdiff > ((IMinecraftVR)mc).getVRSettings().vrTeleportHorizLimit + 0.2)
        	    		ok = false;
            	}
                
            	if(!ok) { //u fail.
            		movementTeleportDestination = new Vec3d(0, 0, 0);
            		movementTeleportDistance = 0;
            	}
            	
                break;
            }

            pos = new Vec3d(newPos.x, newPos.y, newPos.z);


            movementTeleportArc[i] = new Vec3d(
            		newPos.x,
            		newPos.y,
            		newPos.z);

            movementTeleportArcSteps = i + 1;

            velocity = velocity.add(gravity);

        }
    }

    private RayTraceResult tpRaytrace(World w, Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z))
        {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z))
            {
                int i = MathHelper.floor(vec32.x);
                int j = MathHelper.floor(vec32.y);
                int k = MathHelper.floor(vec32.z);
                int l = MathHelper.floor(vec31.x);
                int i1 = MathHelper.floor(vec31.y);
                int j1 = MathHelper.floor(vec31.z);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = w.getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                if(iblockstate.getBlock() == Blocks.WATER){
                	ignoreBlockWithoutBoundingBox = !stopOnLiquid;
                }
                if (block == Blocks.WATERLILY || (!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(w, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid))
                {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace(w, blockpos, vec31, vec32);

                    if (raytraceresult != null)
                    {
                        return raytraceresult;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z))
                    {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k)
                    {
                        return returnLastUncollidableBlock ? raytraceresult2 : null;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l)
                    {
                        d0 = (double)l + 1.0D;
                    }
                    else if (i < l)
                    {
                        d0 = (double)l + 0.0D;
                    }
                    else
                    {
                        flag2 = false;
                    }

                    if (j > i1)
                    {
                        d1 = (double)i1 + 1.0D;
                    }
                    else if (j < i1)
                    {
                        d1 = (double)i1 + 0.0D;
                    }
                    else
                    {
                        flag = false;
                    }

                    if (k > j1)
                    {
                        d2 = (double)j1 + 1.0D;
                    }
                    else if (k < j1)
                    {
                        d2 = (double)j1 + 0.0D;
                    }
                    else
                    {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.x - vec31.x;
                    double d7 = vec32.y - vec31.y;
                    double d8 = vec32.z - vec31.z;

                    if (flag2)
                    {
                        d3 = (d0 - vec31.x) / d6;
                    }

                    if (flag)
                    {
                        d4 = (d1 - vec31.y) / d7;
                    }

                    if (flag1)
                    {
                        d5 = (d2 - vec31.z) / d8;
                    }

                    if (d3 == -0.0D)
                    {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D)
                    {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D)
                    {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5)
                    {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
                    }
                    else if (d4 < d5)
                    {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
                    }
                    else
                    {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
                    }

                    l = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = w.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(w, blockpos) != Block.NULL_AABB)
                    {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid))
                        {
                            RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(w, blockpos, vec31, vec32);

                            if (raytraceresult1 != null)
                            {
                                return raytraceresult1;
                            }
                        }
                        else
                        {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }

                return returnLastUncollidableBlock ? raytraceresult2 : null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }
    
    private void doTeleportCallback(){ //not really a callback anymore, is it?
        Minecraft mc = Minecraft.getMinecraft();

        ((IMinecraftVR)mc).getSwingTracker().disableSwing = 3;

        if(((IMinecraftVR)mc).getVRSettings().vrLimitedSurvivalTeleport){
          mc.player.addExhaustion((float) (movementTeleportDistance / 16 * 1.2f));    
          
          if (mc.playerController.isNotCreative() && vrMovementStyle.arcAiming){
          	teleportEnergy -= movementTeleportDistance * 4;	
          }       
        }
        
        mc.player.fallDistance = 0.0F;

        ((IEntityPlayerSPVR)mc.player).setMovementTeleportTimer(-1);
        
    }
	
    // look for a valid place to stand on the block that the trace collided with
    private boolean checkAndSetTeleportDestination(Minecraft mc, Entity player, Vec3d start, RayTraceResult collision, Vec3d reverseEpsilon)
    {

    	BlockPos bp = collision.getBlockPos();
    	IBlockState testClimb = player.world.getBlockState(bp);
    	
    	
    	if (testClimb.getBlock() == Blocks.WATER){
    		Vec3d hitVec = new Vec3d(collision.hitVec.x, bp.getY(), collision.hitVec.z );

    		Vec3d offset = hitVec.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getEntityBoundingBox().offset(offset.x, offset.y, offset.z);
    		boolean emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty();

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(bp).add(0.5, 0, 0.5);
    			offset = center.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    			bb = player.getEntityBoundingBox().offset(offset.x, offset.y, offset.z);
    			emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty(); 	
    		}
    		float ex = 0;
    		if(((IMinecraftVR)mc).getVRSettings().seated)ex = 0.5f;
    		if(emptySpotReq){
    			movementTeleportDestination = new Vec3d(bb.getCenter().x,bb.minY+ex, bb.getCenter().z);
    			movementTeleportDestinationSideHit = collision.sideHit;
    			return true;
    		}

    	} else if (collision.sideHit != EnumFacing.UP) 
    	{ //sides  		    		
    		//jrbudda require arc hitting top of block.	unless ladder or vine or creative or limits off.

    		if (testClimb.getBlock() instanceof BlockLadder|| testClimb.getBlock() instanceof BlockVine) {
    			Vec3d dest = new Vec3d(bp.getX()+0.5, bp.getY() + 0.5, bp.getZ()+0.5);

    			Block playerblock = mc.world.getBlockState(bp.down()).getBlock();
    			if(playerblock == testClimb.getBlock()) dest = dest.add(0,-1,0);

    			movementTeleportDestination = dest.scale(1);
    			movementTeleportDestinationSideHit = collision.sideHit;
    			
    			return true; //really should check if the block above is passable. Maybe later.
    		} else {
    			if (!mc.player.capabilities.allowFlying && ((IMinecraftVR)mc).getVRSettings().vrLimitedSurvivalTeleport)
    				return false; //if creative, check if can hop on top.
    		}
    	}

    	double y = 0;
    	BlockPos hitBlock = collision.getBlockPos().down();

    	for(int k = 0; k<2; k++){

    		testClimb = player.world.getBlockState(hitBlock);
    		Vec3d hitVec = new Vec3d(collision.hitVec.x, hitBlock.getY() + testClimb.getBoundingBox(mc.world, hitBlock).maxY, collision.hitVec.z );
    		Vec3d offset = hitVec.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getEntityBoundingBox().offset(offset.x, offset.y, offset.z);
    		double ex = 0;
    		if (testClimb.getBlock() == Blocks.SOUL_SAND) ex = 0.05;

    		boolean emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty() &&
    				!mc.world.getCollisionBoxes(player,bb.grow(0, .125 + ex, 0)).isEmpty();     

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(hitBlock).add(0.5, testClimb.getBoundingBox(mc.world, hitBlock).maxY, 0.5);
    			offset = center.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    			bb = player.getEntityBoundingBox().offset(offset.x, offset.y, offset.z);
    			emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty() &&
    					!mc.world.getCollisionBoxes(player,bb.grow(0, .125 + ex, 0)).isEmpty();     	
    		}

    		if(emptySpotReq){
    			Vec3d dest = new Vec3d(bb.getCenter().x, hitBlock.getY() + testClimb.getBoundingBox(mc.world, hitBlock).maxY, bb.getCenter().z);


    			movementTeleportDestination = dest.scale(1);
    			
    			return true;
    		}

    		hitBlock = hitBlock.up();
    	}

    	return false;
    }

    // rough interpolation between arc locations
    public Vec3d getInterpolatedArcPosition(float progress)
    {
        // not enough points to interpolate or before start
        if (movementTeleportArcSteps == 1 || progress <= 0.0f)
        {
            return new Vec3d(
                    movementTeleportArc[0].x,
                    movementTeleportArc[0].y,
                    movementTeleportArc[0].z);
        }

        // past end of arc
        if (progress>=1.0f)
        {
            return new Vec3d(
                    movementTeleportArc[movementTeleportArcSteps-1].x,
                    movementTeleportArc[movementTeleportArcSteps-1].y,
                    movementTeleportArc[movementTeleportArcSteps-1].z);
        }

        // which two points are we between?
        float stepFloat = progress * (float)(movementTeleportArcSteps - 1);
        int step = (int) Math.floor(stepFloat);

        double deltaX = movementTeleportArc[step+1].x - movementTeleportArc[step].x;
        double deltaY = movementTeleportArc[step+1].y - movementTeleportArc[step].y;
        double deltaZ = movementTeleportArc[step+1].z - movementTeleportArc[step].z;

        float stepProgress = stepFloat - step;

        return new Vec3d(
                movementTeleportArc[step].x + deltaX * stepProgress,
                movementTeleportArc[step].y + deltaY * stepProgress,
                movementTeleportArc[step].z + deltaZ * stepProgress);
    }

}

