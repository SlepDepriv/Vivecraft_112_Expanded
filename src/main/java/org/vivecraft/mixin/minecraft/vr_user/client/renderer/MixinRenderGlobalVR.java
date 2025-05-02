package org.vivecraft.mixin.minecraft.vr_user.client.renderer;

import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.util.vector.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.intermediaries.interfaces.client.renderer.IRenderGlobalVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.render.RenderPass;
import org.vivecraft.settings.VRSettings;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobalVR implements IRenderGlobalVR {
    @Shadow
    public Minecraft mc;
    @Shadow
    private Framebuffer entityOutlineFramebuffer;
    @Shadow
    private Map<Integer, DestroyBlockProgress> damagedBlocks;
    @Shadow
    private boolean displayListEntitiesDirty;
    @Shadow
    private WorldClient world;

    @Override
    public Map<Integer, DestroyBlockProgress> getDamagedBlocks() {
        return damagedBlocks;
    }

    @Override
    public void triggerRenderSky(BufferBuilder bufferBuilderIn, float posY, boolean reverseX) {
        renderSky(bufferBuilderIn, posY, reverseX);
    }

    @Override
    public void triggerRenderStars(BufferBuilder bufferBuilderIn) {
        renderStars(bufferBuilderIn);
    }

    @Inject(method = "makeEntityOutlineShader", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;entityOutlineShader:Lnet/minecraft/client/shader/ShaderGroup;", opcode = Opcodes.PUTFIELD))
    private void addEntityOutlineIf(CallbackInfo ci) {
        if (this.entityOutlineFramebuffer != null) {
            this.entityOutlineFramebuffer.deleteFramebuffer();
            this.entityOutlineFramebuffer = null;
        }
    }

    @Overwrite
    public void renderEntityOutlineFramebuffer()
    {
        if (this.isRenderEntityOutlines())
        {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            this.entityOutlineFramebuffer.framebufferRenderExt(this.mc.getFramebuffer().framebufferWidth, this.mc.getFramebuffer().framebufferHeight, false);
            GlStateManager.disableBlend();
        }
    }

    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE", ordinal = 13))
    private boolean modifyFlag1(boolean original) {
        return ((IMinecraftVR)mc).getCurrentPass() == RenderPass.THIRD && ((IMinecraftVR)mc).getVRSettings().displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)V", ordinal = 1, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void alterEntity2First(CallbackInfo ci, Entity entity2) {
        if(entity2 == this.mc.getRenderViewEntity()) { //fix shaders shadow location.
            ((IEntityRendererVR)mc.entityRenderer).restoreRVEPos((EntityLivingBase) entity2);
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)V", ordinal = 1, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void alterEntity2Second(CallbackInfo ci, Entity entity2) {
        if(entity2 == this.mc.getRenderViewEntity()) {
            ((IEntityRendererVR)mc.entityRenderer).cacheRVEPos((EntityLivingBase) entity2);
            ((IEntityRendererVR)mc.entityRenderer).setupRVE();
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebuffer(Z)V"))
    private void changeBindFrameBuffer(Framebuffer framebuffer, boolean p_147610_1_) {
        this.mc.getFramebuffer().bindFramebuffer(true);
    }

    @Redirect(method = "setupTerrain", at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos"))
    private BlockPos redirectBlockPos(double x, double y, double z) {
        return new BlockPos(x, y, z);
    }

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;displayListEntitiesDirty:Z", opcode = Opcodes.PUTFIELD))
    private void overrideDisplayListDirty(RenderGlobal instance, boolean originalValue) {
        this.displayListEntitiesDirty = true;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;getFacingFromVector(FFF)Lnet/minecraft/util/EnumFacing;"))
    private EnumFacing changeVecToDir(float x, float y, float z) {
        Vec3d dir = ((IMinecraftVR)Minecraft.getMinecraft()).getVRPlayer().vrdata_world_render.hmd.getDirection();
        return EnumFacing.getFacingFromVector((float)dir.x, (float)dir.y, (float)dir.z).getOpposite();
    }

    /**
     * @reason I couldn't be bothered injecting every single one of the thumps
     * @author SlepDepriv
     * @param player
     * @param type
     * @param blockPosIn
     * @param data
     */
    @Overwrite
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data)
    {
        Random random = this.world.rand;

        boolean playernear =  mc.player !=null && !mc.player.isDead && (mc.player.getPosition().distanceSq(blockPosIn) < 25); ///hmm sure why not.

        switch (type)
        {
            case 1000:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1001:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;

            case 1002:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;

            case 1003:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ENDEREYE_LAUNCH, SoundCategory.NEUTRAL, 1.0F, 1.2F, false);
                break;

            case 1004:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_FIREWORK_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.2F, false);
                break;

            case 1005:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1006:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1007:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1008:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_FENCE_GATE_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1009:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);
                break;

            case 1010:
                if (Item.getItemById(data) instanceof ItemRecord)
                {
                    this.world.playRecord(blockPosIn, ((ItemRecord)Item.getItemById(data)).getSound());
                }
                else
                {
                    this.world.playRecord(blockPosIn, (SoundEvent)null);
                }

                break;

            case 1011:
                if(playernear)
                    MCOpenVR.triggerHapticPulse(0,250); //VIVECRAFT go thump.

                this.world.playSound(blockPosIn, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1012:
                if(playernear)
                    MCOpenVR.triggerHapticPulse(0,250); //VIVECRAFT go thump.

                this.world.playSound(blockPosIn, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1013:
                if(playernear)
                    MCOpenVR.triggerHapticPulse(0,250); //VIVECRAFT go thump.

                this.world.playSound(blockPosIn, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1014:
                if(playernear)
                    MCOpenVR.triggerHapticPulse(0,250); //VIVECRAFT go thump.

                this.world.playSound(blockPosIn, SoundEvents.BLOCK_FENCE_GATE_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1015:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_GHAST_WARN, SoundCategory.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1016:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1017:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ENDERDRAGON_SHOOT, SoundCategory.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1018:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1019:
                if(playernear){ //this is just mean.
                    MCOpenVR.triggerHapticPulse(0,750);
                    MCOpenVR.triggerHapticPulse(1,750);
                }
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1020:
                if(playernear){ //this is just mean.
                    MCOpenVR.triggerHapticPulse(0,750);
                    MCOpenVR.triggerHapticPulse(1,750);
                }
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1021:
                if(playernear){ //this is just mean.
                    MCOpenVR.triggerHapticPulse(0,1250);
                    MCOpenVR.triggerHapticPulse(1,1250);
                }
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1022:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1024:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1025:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_BAT_TAKEOFF, SoundCategory.NEUTRAL, 0.05F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1026:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1027:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.NEUTRAL, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;

            case 1029:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1030:
                if(playernear){
                    MCOpenVR.triggerHapticPulse(0,500);
                }
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1031:
                if(playernear){
                    MCOpenVR.triggerHapticPulse(0,1250);
                    MCOpenVR.triggerHapticPulse(1,1250);
                }
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1032:
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_PORTAL_TRAVEL, random.nextFloat() * 0.4F + 0.8F));
                break;

            case 1033:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1034:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1035:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;

            case 1036:
                if(playernear)
                    MCOpenVR.triggerHapticPulse(0,250); //VIVECRAFT go thump.

                this.world.playSound(blockPosIn, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 1037:
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 2000:
                int k1 = data % 3 - 1;
                int l1 = data / 3 % 3 - 1;
                double d3 = (double)blockPosIn.getX() + (double)k1 * 0.6D + 0.5D;
                double d4 = (double)blockPosIn.getY() + 0.5D;
                double d5 = (double)blockPosIn.getZ() + (double)l1 * 0.6D + 0.5D;

                for (int k2 = 0; k2 < 10; ++k2)
                {
                    double d18 = random.nextDouble() * 0.2D + 0.01D;
                    double d19 = d3 + (double)k1 * 0.01D + (random.nextDouble() - 0.5D) * (double)l1 * 0.5D;
                    double d20 = d4 + (random.nextDouble() - 0.5D) * 0.5D;
                    double d21 = d5 + (double)l1 * 0.01D + (random.nextDouble() - 0.5D) * (double)k1 * 0.5D;
                    double d22 = (double)k1 * d18 + random.nextGaussian() * 0.01D;
                    double d23 = -0.03D + random.nextGaussian() * 0.01D;
                    double d24 = (double)l1 * d18 + random.nextGaussian() * 0.01D;
                    this.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d19, d20, d21, d22, d23, d24);
                }

                return;

            case 2001:
                Block block = Block.getBlockById(data & 4095);

                if (block.getDefaultState().getMaterial() != Material.AIR)
                {
                    SoundType soundtype = block.getSoundType(Block.getStateById(data), world, blockPosIn, null);
                    this.world.playSound(blockPosIn, soundtype.getBreakSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false);
                }

                this.mc.effectRenderer.addBlockDestroyEffects(blockPosIn, block.getStateFromMeta(data >> 12 & 255));
                break;

            case 2002:
            case 2007:
                double d6 = (double)blockPosIn.getX();
                double d7 = (double)blockPosIn.getY();
                double d8 = (double)blockPosIn.getZ();

                for (int i2 = 0; i2 < 8; ++i2)
                {
                    this.spawnParticle(EnumParticleTypes.ITEM_CRACK, d6, d7, d8, random.nextGaussian() * 0.15D, random.nextDouble() * 0.2D, random.nextGaussian() * 0.15D, Item.getIdFromItem(Items.SPLASH_POTION));
                }

                float f5 = (float)(data >> 16 & 255) / 255.0F;
                float f = (float)(data >> 8 & 255) / 255.0F;
                float f1 = (float)(data >> 0 & 255) / 255.0F;
                EnumParticleTypes enumparticletypes = type == 2007 ? EnumParticleTypes.SPELL_INSTANT : EnumParticleTypes.SPELL;

                for (int l2 = 0; l2 < 100; ++l2)
                {
                    double d10 = random.nextDouble() * 4.0D;
                    double d12 = random.nextDouble() * Math.PI * 2.0D;
                    double d14 = Math.cos(d12) * d10;
                    double d27 = 0.01D + random.nextDouble() * 0.5D;
                    double d29 = Math.sin(d12) * d10;
                    Particle particle1 = this.spawnParticle0(enumparticletypes.getParticleID(), enumparticletypes.getShouldIgnoreRange(), d6 + d14 * 0.1D, d7 + 0.3D, d8 + d29 * 0.1D, d14, d27, d29);

                    if (particle1 != null)
                    {
                        float f4 = 0.75F + random.nextFloat() * 0.25F;
                        particle1.setRBGColorF(f5 * f4, f * f4, f1 * f4);
                        particle1.multiplyVelocity((float)d10);
                    }
                }

                this.world.playSound(blockPosIn, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.NEUTRAL, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 2003:
                double d9 = (double)blockPosIn.getX() + 0.5D;
                double d11 = (double)blockPosIn.getY();
                double d13 = (double)blockPosIn.getZ() + 0.5D;

                for (int j3 = 0; j3 < 8; ++j3)
                {
                    this.spawnParticle(EnumParticleTypes.ITEM_CRACK, d9, d11, d13, random.nextGaussian() * 0.15D, random.nextDouble() * 0.2D, random.nextGaussian() * 0.15D, Item.getIdFromItem(Items.ENDER_EYE));
                }

                for (double d25 = 0.0D; d25 < (Math.PI * 2D); d25 += 0.15707963267948966D)
                {
                    this.spawnParticle(EnumParticleTypes.PORTAL, d9 + Math.cos(d25) * 5.0D, d11 - 0.4D, d13 + Math.sin(d25) * 5.0D, Math.cos(d25) * -5.0D, 0.0D, Math.sin(d25) * -5.0D);
                    this.spawnParticle(EnumParticleTypes.PORTAL, d9 + Math.cos(d25) * 5.0D, d11 - 0.4D, d13 + Math.sin(d25) * 5.0D, Math.cos(d25) * -7.0D, 0.0D, Math.sin(d25) * -7.0D);
                }

                return;

            case 2004:
                for (int i3 = 0; i3 < 20; ++i3)
                {
                    double d26 = (double)blockPosIn.getX() + 0.5D + ((double)this.world.rand.nextFloat() - 0.5D) * 2.0D;
                    double d28 = (double)blockPosIn.getY() + 0.5D + ((double)this.world.rand.nextFloat() - 0.5D) * 2.0D;
                    double d30 = (double)blockPosIn.getZ() + 0.5D + ((double)this.world.rand.nextFloat() - 0.5D) * 2.0D;
                    this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, d26, d28, d30, 0.0D, 0.0D, 0.0D, new int[0]);
                    this.world.spawnParticle(EnumParticleTypes.FLAME, d26, d28, d30, 0.0D, 0.0D, 0.0D, new int[0]);
                }

                return;

            case 2005:
                ItemDye.spawnBonemealParticles(this.world, blockPosIn, data);
                break;

            case 2006:
                for (int j2 = 0; j2 < 200; ++j2)
                {
                    float f2 = random.nextFloat() * 4.0F;
                    float f3 = random.nextFloat() * ((float)Math.PI * 2F);
                    double d15 = (double)(MathHelper.cos(f3) * f2);
                    double d16 = 0.01D + random.nextDouble() * 0.5D;
                    double d17 = (double)(MathHelper.sin(f3) * f2);
                    Particle particle = this.spawnParticle0(EnumParticleTypes.DRAGON_BREATH.getParticleID(), false, (double)blockPosIn.getX() + d15 * 0.1D, (double)blockPosIn.getY() + 0.3D, (double)blockPosIn.getZ() + d17 * 0.1D, d15, d16, d17);

                    if (particle != null)
                    {
                        particle.multiplyVelocity(f2);
                    }
                }

                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ENDERDRAGON_FIREBALL_EPLD, SoundCategory.HOSTILE, 1.0F, this.world.rand.nextFloat() * 0.1F + 0.9F, false);
                break;

            case 3000:
                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, true, (double)blockPosIn.getX() + 0.5D, (double)blockPosIn.getY() + 0.5D, (double)blockPosIn.getZ() + 0.5D, 0.0D, 0.0D, 0.0D, new int[0]);
                this.world.playSound(blockPosIn, SoundEvents.BLOCK_END_GATEWAY_SPAWN, SoundCategory.BLOCKS, 10.0F, (1.0F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F) * 0.7F, false);
                break;

            case 3001:
                this.world.playSound(blockPosIn, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 64.0F, 0.8F + this.world.rand.nextFloat() * 0.3F, false);
        }
    }


    @Shadow
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {

    }
    @Shadow
    private void spawnParticle(EnumParticleTypes particleIn, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {

    }

    @Shadow
    private Particle spawnParticle0(int particleID, boolean ignoreRange, boolean minParticles, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {
        return null;
    }
    @Shadow
    private Particle spawnParticle0(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters)
    {
        return null;
    }

    @Shadow
    protected boolean isRenderEntityOutlines()
    {
        return false;
    }

    @Shadow
    private void renderSky(BufferBuilder bufferBuilderIn, float posY, boolean reverseX)
    {

    }
    @Shadow
    private void renderStars(BufferBuilder bufferBuilderIn)
    {

    }

}
