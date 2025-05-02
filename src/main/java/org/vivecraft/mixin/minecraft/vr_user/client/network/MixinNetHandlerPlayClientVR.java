package org.vivecraft.mixin.minecraft.vr_user.client.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.debug.DebugRendererNeighborsUpdate;
import net.minecraft.client.renderer.debug.DebugRendererPathfinding;
import net.minecraft.entity.IMerchant;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.village.MerchantRecipeList;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.api.NetworkHelper;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.render.PlayerModelController;
import org.vivecraft.utils.BlockWithData;
import org.vivecraft.utils.LangHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.UUID;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClientVR {

    @Shadow
    private static Logger LOGGER;
    @Shadow
    private Minecraft client;
    @Shadow
    private NetworkManager netManager;
    @Shadow
    private GameProfile profile;
    @Shadow
    private WorldClient world;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addToConstructor(Minecraft mcIn, GuiScreen p_i46300_2_, NetworkManager networkManagerIn, GameProfile profileIn, CallbackInfo ci) {
        //VIVECRAFT
        NetworkHelper.resetServerSettings();
        //
    }

    @Inject(method = "handleJoinGame", at = @At("TAIL"))
    private void addToHandleJoinGame(SPacketJoinGame packetIn, CallbackInfo ci) {
        // VIVE START - ask server if it's running this mod
        NetworkHelper.sendVersionInfo();
        // VIVE END - ask server if it's running this mod
    }

    @Inject(method = "handleDisconnect", at = @At("TAIL"))
    private void addToHandleJoinGame(SPacketDisconnect packetIn, CallbackInfo ci) {
        // VIVE START - no longer on a vanilla server, reset restricted state
        ((IMinecraftVR)this.client).getVRPlayer().setTeleportSupported(false);
        ((IMinecraftVR)this.client).getVRPlayer().setTeleportOverride(false);
        // VIVE END - no longer on a vanilla server, reset restricted state
    }

    @Overwrite
    public void handleChat(SPacketChat packetIn)
    {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, this.client);
        ITextComponent message = packetIn.getChatComponent();

        //Forge
        message = net.minecraftforge.event.ForgeEventFactory.onClientChat(packetIn.getType(), packetIn.getChatComponent());
        //

        if (packetIn.getType() == ChatType.GAME_INFO)
        {
            this.client.ingameGUI.setOverlayMessage(message, false);
        }
        else
        {
            this.client.ingameGUI.getChatGUI().printChatMessage(message);
        }
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    public void addToHandleRespawn(SPacketRespawn packetIn, CallbackInfo ci)
    {
        //VIVECRAFT
        NetworkHelper.resetServerSettings();
        NetworkHelper.sendVersionInfo();
        //
    }

    @Overwrite
    public void handleCustomPayload(SPacketCustomPayload packetIn)
    {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, this.client);

        if ("MC|TrList".equals(packetIn.getChannelName()))
        {
            PacketBuffer packetbuffer = packetIn.getBufferData();

            try
            {
                int k = packetbuffer.readInt();
                GuiScreen guiscreen = this.client.currentScreen;

                if (guiscreen != null && guiscreen instanceof GuiMerchant && k == this.client.player.openContainer.windowId)
                {
                    IMerchant imerchant = ((GuiMerchant)guiscreen).getMerchant();
                    MerchantRecipeList merchantrecipelist = MerchantRecipeList.readFromBuf(packetbuffer);
                    imerchant.setRecipes(merchantrecipelist);
                }
            }
            catch (IOException ioexception)
            {
                LOGGER.error("Couldn't load trade info", (Throwable)ioexception);
            }
            finally
            {
                if (false) // Forge: let packet handle releasing buffer
                    packetbuffer.release();
            }
        }
        else if ("MC|Brand".equals(packetIn.getChannelName()))
        {
            this.client.player.setServerBrand(packetIn.getBufferData().readString(32767));
        }
        else if ("MC|BOpen".equals(packetIn.getChannelName()))
        {
            EnumHand enumhand = (EnumHand)packetIn.getBufferData().readEnumValue(EnumHand.class);
            ItemStack itemstack = enumhand == EnumHand.OFF_HAND ? this.client.player.getHeldItemOffhand() : this.client.player.getHeldItemMainhand();

            if (itemstack.getItem() == Items.WRITTEN_BOOK)
            {
                this.client.displayGuiScreen(new GuiScreenBook(this.client.player, itemstack, false));
            }
        }
        else if ("MC|DebugPath".equals(packetIn.getChannelName()))
        {
            PacketBuffer packetbuffer1 = packetIn.getBufferData();
            int l = packetbuffer1.readInt();
            float f1 = packetbuffer1.readFloat();
            Path path = Path.read(packetbuffer1);
            ((DebugRendererPathfinding)this.client.debugRenderer.pathfinding).addPath(l, path, f1);
        }
        else if ("MC|DebugNeighborsUpdate".equals(packetIn.getChannelName()))
        {
            PacketBuffer packetbuffer2 = packetIn.getBufferData();
            long i1 = packetbuffer2.readVarLong();
            BlockPos blockpos = packetbuffer2.readBlockPos();
            ((DebugRendererNeighborsUpdate)this.client.debugRenderer.neighborsUpdate).addUpdate(i1, blockpos);
        }
        else if ("MC|StopSound".equals(packetIn.getChannelName()))
        {
            PacketBuffer packetbuffer3 = packetIn.getBufferData();
            String s = packetbuffer3.readString(32767);
            String s1 = packetbuffer3.readString(256);
            this.client.getSoundHandler().stop(s1, SoundCategory.getByName(s));
        }// VIVE START - server told us that it has this mod too, allow extended reach, etc.
        else if ("MC|ViveOK".equals(packetIn.getChannelName()))
        { //allowed, set to user preference.
            ((IMinecraftVR)this.client).getVRPlayer().setTeleportSupported(true);
            ((IMinecraftVR)this.client).getVRPlayer().teleportWarningTimer = -1;
        }
        else if ("Vivecraft".equals(packetIn.getChannelName()))
        {
            PacketBuffer packetbuffer = packetIn.getBufferData();
            byte db = packetbuffer.readByte();
            NetworkHelper.PacketDiscriminators dis = NetworkHelper.PacketDiscriminators.values()[db];

            switch (dis){
                case VERSION:
                    String v = packetbuffer.readString(1024);
                    ((IMinecraftVR)this.client).getVRPlayer().setTeleportSupported(true);
                    ((IMinecraftVR)this.client).getVRPlayer().teleportWarningTimer = -1;
                    ((IMinecraftVR)this.client).printChatMessage(LangHelper.get("vivecraft.messages.serverplugin", v));
                    break;
                case REQUESTDATA:
                    NetworkHelper.serverWantsData = true;
                    break;
                case CLIMBING:
                    NetworkHelper.serverAllowsClimbey = true;
                    if(packetbuffer.readableBytes() > 0){
                        byte[] b = new byte[packetbuffer.readableBytes()];
                        packetbuffer.readBytes(b);
                        final ByteArrayInputStream byteArrayInputStream =
                                new ByteArrayInputStream(b);
                        ObjectInputStream objectInputStream = null;
                        try {
                            objectInputStream = new ObjectInputStream(byteArrayInputStream);
                            ((IMinecraftVR)Minecraft.getMinecraft()).getClimbTracker().serverblockmode = objectInputStream.readByte();
                            ArrayList<String> temp = (ArrayList<String>) objectInputStream.readObject();
                            objectInputStream.close();
                            ((IMinecraftVR)Minecraft.getMinecraft()).getClimbTracker().blocklist.clear();
                            for (String string : temp) {
                                String[] parts = string.split(":");
                                String id, data = null;
                                if(parts.length == 1){
                                    id = string;
                                } else if(parts.length ==2){
                                    id = parts[0];
                                    data = parts[1];
                                } else {
                                    //wut
                                    continue;
                                }

                                if(data != null && !tryParseInt(data)){
                                    continue;
                                }

                                Block test;
                                if(tryParseInt(id)){
                                    test = Block.getBlockById(Integer.parseInt(id));
                                } else {
                                    test = Block.getBlockFromName(id);
                                }

                                if(test == null){
                                    continue;
                                }
                                BlockWithData bd = null;

                                if(data == null)
                                    bd = new BlockWithData(test);
                                else
                                    bd = new BlockWithData(test, Integer.parseInt(data));

                                ((IMinecraftVR)Minecraft.getMinecraft()).getClimbTracker().blocklist.add(bd);
                            }
                        } catch (Exception e) {
                            System.out.println("Something went amiss processing climbey blocks: " + e.getMessage());
                        }
                    }
                    break;
                case TELEPORT:
                    NetworkHelper.serverSupportsDirectTeleport = true;
                    break;
                case UBERPACKET:
                    Long hi = packetbuffer.readLong();
                    Long low = packetbuffer.readLong();
                    byte[] hmd = new byte[29];
                    byte[] c0 = new byte[29];
                    byte[] c1 = new byte[29];
                    packetbuffer.readBytes(29).getBytes(0, hmd);
                    packetbuffer.readBytes(29).getBytes(0, c0);
                    packetbuffer.readBytes(29).getBytes(0, c1);
                    UUID u = new UUID(hi, low);

                    PlayerModelController.getInstance().Update(u, hmd, c0, c1);
                    break;
                default:
                    break;
            }

            //packetbuffer.release(); // Causes error spam with Forge
        }
        else if ("REGISTER".equals(packetIn.getChannelName())){ // TODO: wtf is this? oh, nothing? and it spews an error? fuck it!
            //PacketBuffer packetbuffer = packetIn.getBufferData();
            //String v = new String(packetbuffer.array(),Charsets.UTF_8);
            //System.out.println("REGISTER " + v );
        }
        // VIVE END
    }

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
