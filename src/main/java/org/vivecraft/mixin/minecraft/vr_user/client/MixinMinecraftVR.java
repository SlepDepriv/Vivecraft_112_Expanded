package org.vivecraft.mixin.minecraft.vr_user.client;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.profiler.Profiler;
import net.minecraft.profiler.Snooper;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.*;
import net.minecraft.util.Timer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.api.ErrorHelper;
import org.vivecraft.control.VRInputAction;
import org.vivecraft.gameplay.OpenVRPlayer;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.gameplay.trackers.*;
import org.vivecraft.intermediaries.interfaces.client.renderer.IEntityRendererVR;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.audio.ISoundHandlerVR;
import org.vivecraft.intermediaries.replacements.VRScreenShotHelper;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.provider.OpenVRStereoRenderer;
import org.vivecraft.render.*;
import org.vivecraft.intermediaries.replacements.VRFrameBuffer;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.CrashReportScreenName;
import org.vivecraft.utils.LangHelper;
import org.vivecraft.utils.MCReflection;
import org.vivecraft.utils.Utils;
import paulscode.sound.SoundSystem;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.FutureTask;

// The horrors oh my god, I shouldn't be allowed to do this.
// Someone please stop me from this... mangling

@Mixin(Minecraft.class)
public class MixinMinecraftVR implements IMinecraftVR {

    // VIVE START - teleport movement
    public OpenVRPlayer vrPlayer;
    public BackpackTracker backpackTracker = new BackpackTracker(this);
    public BowTracker bowTracker = new BowTracker(this);
    public SwimTracker swimTracker = new SwimTracker(this);
    public EatingTracker autoFood=new EatingTracker(this);
    public JumpTracker jumpTracker=new JumpTracker(this);
    public SneakTracker sneakTracker=new SneakTracker(this);
    public ClimbTracker climbTracker = new ClimbTracker(this);
    public RunTracker runTracker  = new RunTracker(this);
    public RowTracker rowTracker  = new RowTracker(this);
    public TeleportTracker teleportTracker = new TeleportTracker(this);
    public SwingTracker swingTracker = new SwingTracker(this);
    public InteractTracker interactTracker = new InteractTracker(this);
    public HorseTracker horseTracker = new HorseTracker(this);
    public VehicleTracker vehicleTracker = new VehicleTracker(this);
    // VIVE END - teleport movement


    public boolean minecriftDebug = false;
    public final float PIOVER180 = (float)(Math.PI/180);

    public int lastShaderIndex = -1;
    public Object displayImpl = null;
    public Field fieldHwnd = null;
    public Field fieldDisplay = null;
    public Field fieldWindow = null;
    public Field fieldResized = null;
    public Method fieldResizedMethod = null;
    public OpenVRStereoRenderer stereoProvider;
    public VRSettings vrSettings;
    public long lastIntegratedServerLaunchCheck = 0;
    public boolean integratedServerLaunchInProgress = false;
    public boolean lastEnableVsync = true;
    public boolean grabScreenShot = false;
    public Cursor nativeMouseCursor = null;
    public boolean lastShowMouseNative = true;
    public Cursor invisibleMouseCursor = null;
    public long lastWindow = 0;
    public int lastRenderDistanceChunks = -1;
    public boolean lastFogFancy = true;
    public boolean lastFogFast = false;
    public float lastWorldScale = 0f;
    public boolean enableWorldExport = false;
    public DimensionType lastDimensionId = DimensionType.OVERWORLD;
    public SoundManager sndManager = null;
    public MenuWorldRenderer menuWorldRenderer;


    private FloatBuffer matrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private FloatBuffer matrixBuffer2 = GLAllocation.createDirectFloatBuffer(16);

    private boolean firstInit = true;
    public boolean showSplashScreen = true;
    public long splashTimer1 = 0;
    public long splashTimer2 = 0;
    private Framebuffer splash;
    private float splashFadeAlpha = 0;
    public Deque<Long> runTickTimeNanos = new ArrayDeque<Long>();
    public long medianRunTickTimeNanos = 0;
    public long frameIndex = 0;
    public boolean visible = true;
    public ErrorHelper errorHelper;
//    public static final String RENDER_SETUP_FAILURE_MESSAGE = "Failed to initialise stereo rendering plugin: ";
//    public static final int ERROR_DISPLAY_TIME_SECS = 10;



    public RenderPass currentPass;
    private boolean lastClick;

    public int hmdAvgLength = 90;
    public LinkedList<Vec3d> hmdPosSamples = new LinkedList<Vec3d>();
    public LinkedList<Float> hmdYawSamples = new LinkedList<Float>();
    private float hmdYawTotal;
    private float hmdYawLast;
    public int tickCounter;
    private boolean trigger;

    // Welcome to the shadow wall of hell
    // Stay awhile
    @Shadow
    private static Logger LOGGER;
    @Shadow
    public Profiler profiler;
    private VRFrameBuffer vrFrameBuffer;
    @Shadow
    public File gameDir;
    @Shadow
    public GuiScreen currentScreen;
    @Shadow
    public LoadingScreenRenderer loadingScreen;
    @Shadow
    public EntityRenderer entityRenderer;
    @Shadow
    public DebugRenderer debugRenderer;
    @Shadow
    private boolean isGamePaused;
    @Shadow
    public GameSettings gameSettings;
    @Shadow
    private int fpsCounter;
    @Shadow
    private boolean actionKeyF3;
    @Shadow
    private Tutorial tutorial;
    @Shadow
    private long debugUpdateTime;
    @Shadow
    private static int debugFPS;
    @Shadow
    private Snooper usageSnooper;
    @Shadow
    public WorldClient world;
    @Shadow
    public RenderGlobal renderGlobal;
    @Shadow
    private RenderManager renderManager;
    @Shadow
    private RenderItem renderItem;
    @Shadow
    private ItemRenderer itemRenderer;
    @Shadow
    public EntityPlayerSP player;
    @Shadow
    private Timer timer;
    @Shadow
    private Queue<FutureTask<? >> scheduledTasks;
    @Shadow
    private float renderPartialTicksPaused;
    @Shadow
    private SoundHandler soundHandler;
    @Shadow
    private MusicTicker musicTicker;
    @Shadow
    public int displayWidth;
    @Shadow
    public int displayHeight;
    @Shadow
    public GuiIngame ingameGUI;
    @Shadow
    private IntegratedServer integratedServer;
    @Shadow
    public FrameTimer frameTimer;
    @Shadow
    long startNanoTime = System.nanoTime();
    @Shadow
    public String debug;
    @Shadow
    private int rightClickDelayTimer;
    @Shadow
    public RayTraceResult objectMouseOver;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public TextureManager renderEngine;
    @Shadow
    private int leftClickCounter;
    @Shadow
    private int joinPlayerCounter;
    @Shadow
    public ParticleManager effectRenderer;
    @Shadow
    public NetworkManager networkManager;
    @Shadow
    long systemTime = getSystemTime();
    @Shadow
    public boolean skipRenderWorld;
    @Shadow
    private ItemColors itemColors;
    @Shadow
    private SearchTreeManager searchTreeManager;
    @Shadow
    public FontRenderer fontRenderer;
    @Shadow
    private long debugCrashKeyPressTime;
    @Shadow
    public boolean inGameHasFocus;
    @Shadow
    public static boolean IS_RUNNING_ON_MAC;
    @Shadow
    public MouseHelper mouseHelper;
    @Shadow
    private GuiToast toastGui;

    // Getters and Setter nonsense
    @Override
    public OpenVRPlayer getVRPlayer() {
        return vrPlayer;
    }
    @Override
    public BackpackTracker getBackpackTracker() {
        return backpackTracker;
    }
    @Override
    public BowTracker getBowTracker() {
        return bowTracker;
    }
    @Override
    public ClimbTracker getClimbTracker() {
        return climbTracker;
    }
    @Override
    public EatingTracker getEatingTracker() {
        return autoFood;
    }
    @Override
    public HorseTracker getHorseTracker() {
        return horseTracker;
    }
    @Override
    public InteractTracker getInteractTracker() {
        return interactTracker;
    }
    @Override
    public JumpTracker getJumpTracker() {
        return jumpTracker;
    }
    @Override
    public RowTracker getRowTracker() {
        return rowTracker;
    }
    @Override
    public RunTracker getRunTracker() {
        return runTracker;
    }
    @Override
    public SneakTracker getSneakTracker() {
        return sneakTracker;
    }
    @Override
    public SwimTracker getSwimTracker() {
        return swimTracker;
    }
    @Override
    public SwingTracker getSwingTracker() {
        return swingTracker;
    }
    @Override
    public TeleportTracker getTeleportTracker() {
        return teleportTracker;
    }
    @Override
    public VehicleTracker getVehicleTracker() {
        return vehicleTracker;
    }
    @Override
    public OpenVRStereoRenderer getStereoProvider() {
        return stereoProvider;
    }
    @Override
    public RenderPass getCurrentPass() {
        return currentPass;
    }
    @Override
    public VRSettings getVRSettings() {
        return vrSettings;
    }
    @Override
    public void setVRSettings(VRSettings settings) {
        vrSettings = settings;
    }
    @Override
    public void triggerRightClickMouse() {
        this.rightClickMouse();
    }
    @Override
    public int getTickCounter() {
        return tickCounter;
    }
    @Override
    public Timer getTimer() {
        return timer;
    }
    @Override
    public MenuWorldRenderer getMenuWorldRenderer() {
        return menuWorldRenderer;
    }

    @Override
    public void setFramebuffer(VRFrameBuffer buffer) {
        this.vrFrameBuffer = buffer;
    }
    @Override
    public VRFrameBuffer getFramebuffer() {
        return vrFrameBuffer;
    }
    @Override
    public ErrorHelper getErrorHelper() {
        return errorHelper;
    }

    @Override
    public void setShowSplashScreen(Boolean bool) {
        showSplashScreen = bool;
    }

    @Override
    public boolean getShowSplashScreen() {
        return showSplashScreen;
    }
    @Override
    public void setErrorHelper(ErrorHelper helper) {
        errorHelper = helper;
    }

    @Override
    public GuiToast getToastGUI() {
        return toastGui;
    }

    @Override
    public void triggerDisplayDebugInfo(long elapsedTicksTime) {
        displayDebugInfo(elapsedTicksTime);
    }

    @Override
    public boolean getIntegratedServerLaunchInProgress() {
        return integratedServerLaunchInProgress;
    }

    @Override
    public org.lwjgl.util.vector.Matrix4f getThirdPassViewMatrix() {
        return thirdPassViewMatrix;
    }

    @Override
    public LinkedList<Vec3d> getHmdPosSamples() {
        return hmdPosSamples;
    }

    @Override
    public LinkedList<Float> getHmdYawSamples() {
        return hmdYawSamples;
    }

    @Override
    public float getPumpkinEffect() {
        return pumpkineffect;
    }

    @Override
    public void setPumpkinEffect(float pumpkinEffect) {
        this.pumpkineffect = pumpkinEffect;
    }

    //

    // The actual mixining bit of the mixin

    @Inject(method = "<init>", at = @At(value = "HEAD"))
    private static void MinecraftHeadMixin(GameConfiguration gameConfiguration, CallbackInfo ci) {
        loadClassPath();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void MinecraftTailMixin(GameConfiguration gameConfiguration, CallbackInfo ci) {
        this.displayWidth = 1280;
        this.displayHeight = 720;

        VRSettings.initSettings((Minecraft)(IMinecraftVR)this, this.gameDir);
        if (!vrSettings.badStereoProviderPluginID.isEmpty()) {
            vrSettings.stereoProviderPluginID = vrSettings.badStereoProviderPluginID;
            vrSettings.badStereoProviderPluginID = "";
            vrSettings.saveOptions();
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createDisplay()V", shift = At.Shift.AFTER))
    private void initAfterCreateDisplay(CallbackInfo ci) {
        Display.setTitle("Minecraft VR");
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;registerMetadataSerializers()V", shift = At.Shift.AFTER))
    private void initActuallySetupFrameBufferMyGod(CallbackInfo ci) {
        this.vrFrameBuffer = new VRFrameBuffer(this.displayWidth, this.displayHeight, true);
        this.vrFrameBuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;standardGalacticFontRenderer:Lnet/minecraft/client/gui/FontRenderer;", opcode = Opcodes.PUTFIELD, shift = At.Shift.BEFORE))
    private void initActuallyStartVivecraft(CallbackInfo ci) {
        /** MINECRIFT */
        try {
            System.out.println("Initialzing Vivecraft");
            initMinecrift();
            System.out.println("Vivecraft Initialized");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("Error Initializing Vivecraft " + e.getMessage());
            e.printStackTrace();
        }
        /** END MINECRIFT */
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;serverName:Ljava/lang/String;", shift = At.Shift.BEFORE, opcode = Opcodes.GETFIELD))
    private void initStartMenuWorldRenderer(CallbackInfo ci) {
        // VIVE: Main menu world initialization
        this.menuWorldRenderer = new MenuWorldRenderer();

        if (stereoProvider != null && stereoProvider.isInitialized())
            this.menuWorldRenderer.init();
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;debugRenderer:Lnet/minecraft/client/renderer/debug/DebugRenderer;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void initAfterDebugRenderer(CallbackInfo ci) {
        this.gameSettings.enableVsync = false;
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/Display;setVSyncEnabled(Z)V"), remap = false)
    private void initDisableVSync(boolean enabled) {
        Display.setVSyncEnabled(false);
    }

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void initAtEnd(CallbackInfo ci) {
        LangHelper.registerResourceListener();
        MCOpenVR.initInputAndApplication();
        vrSettings.firstRun = false;
        vrSettings.saveOptions();
    }

    private static void loadClassPath(){
        File resourceRoot=new File("../src/resources");
        if(!resourceRoot.exists() || !resourceRoot.isDirectory())
            return;
        Method method = null;
        try {
            method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(ClassLoader.getSystemClassLoader(), resourceRoot.toURI().toURL());
        } catch (NoSuchMethodException | IllegalAccessException | MalformedURLException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.BEFORE))
    public void DisplayGUIScreenMixin(@Nullable GuiScreen guiScreenIn, CallbackInfo cir) {
        // VIVE START - notify stereo provider that we're about to change screen
        GuiHandler.onGuiScreenChanged(this.currentScreen, guiScreenIn, true);
        // VIVE END - notify stereo provider that we're about to change screen
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/Display;destroy()V", shift = At.Shift.AFTER, remap = false))
    public void shutdownMinecraftAppletMixin(CallbackInfo cir) {
        MCOpenVR.destroy();
    }


    /**
     * @reason I most likely shouldn't just be overwriting methods for compatibility reasons but god I'm lazy
     * @author SlepDepriv
     * @throws IOException
     */

    @Overwrite
    private void runGameLoop() throws IOException
    {
        long i = System.nanoTime();

        if (this.gameSettings.showDebugInfo && this.gameSettings.showDebugProfilerChart && !this.gameSettings.hideGUI)
        {
            if (!this.profiler.profilingEnabled)
            {
                this.profiler.clearProfiling();
            }

            this.profiler.profilingEnabled = true;
        }
        else
        {
            this.profiler.profilingEnabled = false;
        }

        this.profiler.startSection("root");

        long time = System.nanoTime();

        if (Display.isCreated() && Display.isCloseRequested())
        {
            this.shutdown();
        }

        {
            //avoid having to changed OpenGLHelper
            gameSettings.fboEnable = true;
            OpenGlHelper.framebufferSupported = true;
        }

        /** MINECRIFT */ // setup the display, render buffers, shaders etc.
        this.frameIndex++;

        try {
            stereoProvider.setupRenderConfiguration();
        } catch (RenderConfigException e) {
            GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
            GlStateManager.clearColor(0, 0, 0, 1);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            ((IEntityRendererVR)entityRenderer).displayNotificationText(LangHelper.get("vivecraft.messages.rendersetupfailed", e.error), "", "", this.displayWidth, this.displayHeight, false, true);
            this.updateDisplay();
            if (this.frameIndex % 300 == 0)
                System.out.println(e.title + " " + e.error);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
            }
            return;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /** END MINECRIFT */

        this.timer.updateTimer();

        //VIVECRAFT
        this.profiler.startSection("Poll");
        // Poll sensors
        MCOpenVR.poll(frameIndex);
        this.profiler.endSection();

        vrPlayer.postPoll();

        this.profiler.startSection("scheduledExecutables");

        synchronized (this.scheduledTasks)
        {
            while (!this.scheduledTasks.isEmpty())
            {
                Util.runTask(this.scheduledTasks.poll(), LOGGER);
            }
        }

        this.profiler.endSection();



        long l = System.nanoTime();
        this.profiler.startSection("tick");

        for (int j = 0; j < Math.min(10, this.timer.elapsedTicks); ++j)
        {
            //VIVECRAFT
            vrPlayer.preTick();
            //
            this.runTick();
            //VIVECRAFT
            vrPlayer.postTick();
            //
        }


        //VIVECRAFT
        try {
            stereoProvider.setupRenderConfiguration();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //


        this.profiler.endStartSection("preRenderErrors");
        long i1 = System.nanoTime() - l;
        this.checkGLError("Pre render");

        float par1 = this.isGamePaused ? this.renderPartialTicksPaused : this.timer.renderPartialTicks;

        this.profiler.endStartSection("Gui");

        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);

        // Render GUI to FBO if necessary
        this.vrFrameBuffer = GuiHandler.guiFramebuffer; //draw to 2d gui.
        this.vrFrameBuffer.bindFramebuffer(true);

        ((IEntityRendererVR)entityRenderer).drawFramebuffer(par1, i1);   // VIVE - added param for debug info

        if(KeyboardHandler.Showing && !this.vrSettings.physicalKeyboard) {
            this.vrFrameBuffer = KeyboardHandler.Framebuffer;
            this.vrFrameBuffer.bindFramebuffer(true);
            ((IEntityRendererVR)entityRenderer).drawScreen(par1, KeyboardHandler.UI);
        }

        if(RadialHandler.Showing) {
            this.vrFrameBuffer = RadialHandler.Framebuffer;
            this.vrFrameBuffer.bindFramebuffer(true);
            ((IEntityRendererVR)entityRenderer).drawScreen(par1, RadialHandler.UI);
        }

        checkGLError("post 2d ");
        this.profiler.endSection();

        //VIVECRAFT
        this.profiler.startSection("preRender");
        vrPlayer.preRender(par1);
        this.profiler.endSection();
        //

        this.profiler.startSection("sound");
        //this.mcSoundHandler.setListener(this.player, this.timer.renderPartialTicks);
        updateSoundListener(); // we update the sound listener from the HMD info
        this.profiler.endSection();


        this.profiler.startSection("hmdSampling");

        if (hmdPosSamples.size() == hmdAvgLength)
            hmdPosSamples.removeFirst();
        if (hmdYawSamples.size() == hmdAvgLength)
            hmdYawSamples.removeFirst();

        float yaw = vrPlayer.vrdata_room_pre.hmd.getYaw();
        if (yaw < 0) yaw += 360;
        hmdYawTotal += angleDiff(yaw, hmdYawLast);
        hmdYawLast = yaw;
        if (Math.abs(angleNormalize(hmdYawTotal) - hmdYawLast) > 1 || hmdYawTotal > 100000) {
            hmdYawTotal = hmdYawLast;
            System.out.println("HMD yaw desync/overflow corrected");
        }

        hmdPosSamples.add(vrPlayer.vrdata_room_pre.hmd.getPosition());
        float yawAvg = 0;
        if(hmdYawSamples.size() > 0){
            for (float f : hmdYawSamples) {
                yawAvg += f;
            }
            yawAvg /= hmdYawSamples.size();
        }
        if( Math.abs((hmdYawTotal - yawAvg)) > 20) trigger = true;
        if( Math.abs((hmdYawTotal - yawAvg)) < 1) trigger = false;
        if(trigger || hmdYawSamples.isEmpty())
            hmdYawSamples.add(hmdYawTotal);

        this.profiler.endSection(); //hmd sampling

        //VIVECRAFT RENDERING MAIN
        if (minecriftDebug) print("FrameIndex: " + frameIndex);

//			int passes = 2;
//
//			if( this.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON ){
//				passes = 3;
//			} else if (this.vrSettings.displayMirrorMode == vrSettings.MIRROR_MIXED_REALITY || this.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) {
//				passes = 4;
//			}
//
        int w, h;

        List<RenderPass> passes = this.stereoProvider.getRenderPasses();

        /** Minecrift - main stereo render loop **/
        for (RenderPass pass : passes)
        {
            this.currentPass = pass;
            // Bye bye switch, mixin doesn't like you
            if (pass == RenderPass.LEFT || pass == RenderPass.RIGHT) {
                this.vrFrameBuffer = stereoProvider.framebufferVrRender;
            } else if (pass == RenderPass.CENTER) {
                this.vrFrameBuffer = stereoProvider.framebufferUndistorted;
            } else if (pass == RenderPass.THIRD) {
                this.vrFrameBuffer = stereoProvider.framebufferMR;
            }

            this.profiler.startSection("Eye:" + currentPass.ordinal());
            this.profiler.startSection("setup");
            this.vrFrameBuffer.bindFramebuffer(true);	//draw to main texture for every pass
            this.profiler.endSection();
            renderSingleView(pass.ordinal(), par1);
            this.profiler.endSection(); //eye

            if (grabScreenShot) {
                boolean inPass;
                if (passes.contains(RenderPass.CENTER)) {
                    inPass = (pass == RenderPass.CENTER);
                } else {
                    inPass = vrSettings.displayMirrorLeftEye ? (pass == RenderPass.LEFT) : (pass == RenderPass.RIGHT);
                }

                if(inPass){
                    this.vrFrameBuffer.unbindFramebuffer();
                    //OpenGlHelper.fbo = false; // why the hell?
                    this.ingameGUI.getChatGUI().printChatMessage(VRScreenShotHelper.saveScreenshot(this.gameDir, this.getFramebuffer().framebufferWidth, this.getFramebuffer().framebufferHeight, this.getFramebuffer()));
                    //OpenGlHelper.fbo = true; // i'm don't understand
                    grabScreenShot = false;
                }
            }
        } //end per eye rendering.

        profiler.startSection("Display/Reproject");
        try {
            this.stereoProvider.endFrame();
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }

        this.profiler.startSection("mirror");
        vrFrameBuffer.unbindFramebuffer(); // draw directly to window
        copyToMirror();
        drawNotifyMirror();
        checkGLError("post-mirror");
        this.profiler.endSection();

        //VIVECRAFT
        vrPlayer.postRender(par1);
        //

        profiler.startSection("GameWindowEvents");
        Display.processMessages();
        Display.update(false);
        profiler.endSection();

        profiler.endSection();


        ////END MAIN VIVECRAFT RENDERING

        ++this.fpsCounter;
        boolean flag = this.isSingleplayer() && ((this.currentScreen != null && this.currentScreen.doesGuiPauseGame()) || MCOpenVR.paused) && !this.integratedServer.getPublic();

        if (this.isGamePaused != flag)
        {
            if (this.isGamePaused)
            {
                this.renderPartialTicksPaused = this.timer.renderPartialTicks;
            }
            else
            {
                this.timer.renderPartialTicks = this.renderPartialTicksPaused;
            }

            this.isGamePaused = flag;
        }

        long k = System.nanoTime();
        this.frameTimer.addFrame(k - this.startNanoTime);
        this.startNanoTime = k;

        while (getSystemTime() >= this.debugUpdateTime + 1000L)
        {
            debugFPS = this.fpsCounter;
            this.debug = String.format("%d fps (%d chunk update%s) T: %s%s%s%s%s", debugFPS, RenderChunk.renderChunksUpdated, RenderChunk.renderChunksUpdated == 1 ? "" : "s", (float)this.gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : this.gameSettings.limitFramerate, this.gameSettings.enableVsync ? " vsync" : "", this.gameSettings.fancyGraphics ? "" : " fast", this.gameSettings.clouds == 0 ? "" : (this.gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"), OpenGlHelper.useVbo() ? " vbo" : "");
            RenderChunk.renderChunksUpdated = 0;
            this.debugUpdateTime += 1000L;
            this.fpsCounter = 0;
            this.usageSnooper.addMemoryStatsToSnooper();

            if (!this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.startSnooper();
            }
        }

//        if (this.isFramerateLimitBelowMax())
//        {
//            this.mcProfiler.startSection("fpslimit_wait");
//            Display.sync(this.getLimitFramerate());
//            this.mcProfiler.endSection();
//        }

        this.checkWindowResize(); // fuck it just call this directly, idk why updateDisplay blows up

        this.profiler.endSection(); //root
    }

    /**
     * @reason  So much, so much code- I can't be bothered I'm sorry. Maybe future Slep will hate me for this and redo it in the future, but for now this should be fine
     * @author SlepDepriv
     * @throws IOException
     */

    @Overwrite
    public void runTick() throws IOException
    {
        this.tickCounter++;

        if (this.rightClickDelayTimer > 0)
        {
            --this.rightClickDelayTimer;
        }

        FMLCommonHandler.instance().onPreClientTick();

        this.profiler.startSection("gui");

        if (!this.isGamePaused)
        {
            this.ingameGUI.updateTick();
        }

        this.profiler.endSection();
        ((IEntityRendererVR)entityRenderer).getMouseOverVR(1.0F);
        this.tutorial.onMouseHover(this.world, this.objectMouseOver);
        this.profiler.startSection("gameMode");

        if (!this.isGamePaused && this.world != null)
        {
            this.playerController.updateController();
        }

        this.profiler.endStartSection("textures");

        // VanillaFix support
        if (this.world == null && this.menuWorldRenderer != null) {
            this.menuWorldRenderer.pushVisibleTextures();
        }
        // End VanillaFix support

        // VIVE: nah we wanna tick textures on the main menu too
        //if (this.world != null)
        //{
        this.renderEngine.tick();
        //}

        if (this.currentScreen == null && this.player != null)
        {
            if (this.player.getHealth() <= 0.0F && !(this.currentScreen instanceof GuiGameOver))
            {
                this.displayGuiScreen((GuiScreen)null);
            }
            else if (this.player.isPlayerSleeping() && this.world != null)
            {
                this.displayGuiScreen(new GuiSleepMP());
            }
        }
        else if (this.currentScreen != null && this.currentScreen instanceof GuiSleepMP && !this.player.isPlayerSleeping())
        {
            this.displayGuiScreen((GuiScreen)null);
        }

        if (this.currentScreen != null)
        {
            this.leftClickCounter = 10000;
        }

        if (this.currentScreen != null)
        {
            try
            {
                this.currentScreen.handleInput();
            }
            catch (Throwable throwable1)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Updating screen events");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Affected screen");
                crashreportcategory.addDetail("Screen name", new CrashReportScreenName(currentScreen));
                throw new ReportedException(crashreport);
            }

            if (this.currentScreen != null)
            {
                try
                {
                    this.currentScreen.updateScreen();
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport1 = CrashReport.makeCrashReport(throwable, "Ticking screen");
                    CrashReportCategory crashreportcategory1 = crashreport1.makeCategory("Affected screen");
                    crashreportcategory1.addDetail("Screen name", new CrashReportScreenName(currentScreen));
                    throw new ReportedException(crashreport1);
                }
            }
        }

        //Vivecraft
        this.profiler.endStartSection("vrProcessBindings");
        MCOpenVR.processBindings();
        ///

        if (this.currentScreen == null || this.currentScreen.allowUserInput)
        {
            this.profiler.endStartSection("mouse");
            this.runTickMouse();

            if (this.leftClickCounter > 0)
            {
                --this.leftClickCounter;
            }

            this.profiler.endStartSection("keyboard");
            this.runTickKeyboard();
        }

        //Vivecraft
        this.profiler.endStartSection("vrInputActionsTick");
        for (VRInputAction action : MCOpenVR.getInputActions()) {
            action.tick();
        }

        if(vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY || vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON)
            VRHotkeys.handleMRKeys();

        ///

        if (this.world != null)
        {
            if (this.player != null)
            {

                ++this.joinPlayerCounter;

                if (this.joinPlayerCounter == 30)
                {
                    this.joinPlayerCounter = 0;
                    this.world.joinEntityInSurroundings(this.player);
                }
            }

            // Vivecraft - weird place for this but whatev, should be in openvrplayer
            this.vrPlayer.updateFreeMove();
            if (this.vrPlayer.teleportWarningTimer >= 0) {
                if (--this.vrPlayer.teleportWarningTimer == 0) {
                    printChatMessage(I18n.format("vivecraft.messages.noserverplugin"));
                }
            }

            this.profiler.endStartSection("gameRenderer");

            if (!this.isGamePaused)
            {
                this.entityRenderer.updateRenderer();
            }

            this.profiler.endStartSection("levelRenderer");

            if (!this.isGamePaused)
            {
                this.renderGlobal.updateClouds();
            }

            this.profiler.endStartSection("level");

            if (!this.isGamePaused)
            {
                if (this.world.getLastLightningBolt() > 0)
                {
                    this.world.setLastLightningBolt(this.world.getLastLightningBolt() - 1);
                }

                this.world.updateEntities();
            }
        }
        else if (this.entityRenderer.isShaderActive())
        {
            this.entityRenderer.stopUseShader();
        }

        if (this.menuWorldRenderer != null) this.menuWorldRenderer.updateTorchFlicker();
        PlayerModelController.getInstance().tick();

        if (!this.isGamePaused)
        {
            this.musicTicker.update();
            this.soundHandler.update();
        }

        if (this.world != null)
        {
            if (!this.isGamePaused)
            {
                this.world.setAllowedSpawnTypes(this.world.getDifficulty() != EnumDifficulty.PEACEFUL, true);
                this.tutorial.update();

                try
                {
                    this.world.tick();
                }
                catch (Throwable throwable2)
                {
                    CrashReport crashreport2 = CrashReport.makeCrashReport(throwable2, "Exception in world tick");

                    if (this.world == null)
                    {
                        CrashReportCategory crashreportcategory2 = crashreport2.makeCategory("Affected level");
                        crashreportcategory2.addCrashSection("Problem", "Level is null!");
                    }
                    else
                    {
                        this.world.addWorldInfoToCrashReport(crashreport2);
                    }

                    throw new ReportedException(crashreport2);
                }
            }

            this.profiler.endStartSection("animateTick");

            if (!this.isGamePaused && this.world != null)
            {
                this.world.doVoidFogParticles(MathHelper.floor(this.player.posX), MathHelper.floor(this.player.posY), MathHelper.floor(this.player.posZ));
            }

            this.profiler.endStartSection("particles");

            if (!this.isGamePaused)
            {
                this.effectRenderer.updateEffects();
            }
        }
        else if (this.networkManager != null)
        {
            this.profiler.endStartSection("pendingConnection");
            this.networkManager.processReceivedPackets();
        }


        this.profiler.endSection();
        FMLCommonHandler.instance().onPostClientTick();

        this.systemTime = getSystemTime();
    }

    /**
     * @reason Yada yada, I'm a lazy bastard
     * @author SlepDepriv
     * @throws IOException
     */

    @Overwrite
    private void runTickKeyboard() throws IOException
    {
        while (Keyboard.next())
        {
            int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();

            if (this.debugCrashKeyPressTime > 0L)
            {
                if (getSystemTime() - this.debugCrashKeyPressTime >= 6000L)
                {
                    throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
                }

                if (!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61))
                {
                    this.debugCrashKeyPressTime = -1L;
                }
            }
            else if (Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61))
            {
                this.actionKeyF3 = true;
                this.debugCrashKeyPressTime = getSystemTime();
            }

            this.dispatchKeypresses();

            if (this.currentScreen != null)
            {
                this.currentScreen.handleKeyboardInput();
            }

            /** MINECRIFT */

            {
                if (VRHotkeys.handleKeyboardInputs((Minecraft)(IMinecraftVR)this))
                    continue;
            }
            /** END MINECRIFT */

            boolean flag = Keyboard.getEventKeyState();

            if (flag)
            {
                if (i == 62 && this.entityRenderer != null)
                {
                    this.entityRenderer.switchUseShader();
                }

                if(i==1) KeyboardHandler.setOverlayShowing(false);

                boolean flag1 = false;

                if (this.currentScreen == null)
                {
                    if (i == 1)
                    {
                        this.displayInGameMenu();

                    }

                    flag1 = Keyboard.isKeyDown(61) && this.processKeyF3(i);
                    this.actionKeyF3 |= flag1;

                    if (i == 59)
                    {
                        this.gameSettings.hideGUI = !this.gameSettings.hideGUI;
                    }
                }

                if (flag1)
                {
                    KeyBinding.setKeyBindState(i, false);
                }
                else
                {
                    KeyBinding.setKeyBindState(i, true);
                    KeyBinding.onTick(i);
                }

                if (this.gameSettings.showDebugProfilerChart)
                {
                    if (i == 11)
                    {
                        this.updateDebugProfilerName(0);
                    }

                    for (int j = 0; j < 9; ++j)
                    {
                        if (i == 2 + j)
                        {
                            this.updateDebugProfilerName(j + 1);
                        }
                    }
                }
            }
            else
            {
                KeyBinding.setKeyBindState(i, false);

                if (i == 61)
                {
                    if (this.actionKeyF3)
                    {
                        this.actionKeyF3 = false;
                    }
                    else
                    {
                        this.gameSettings.showDebugInfo = !this.gameSettings.showDebugInfo;
                        this.gameSettings.showDebugProfilerChart = this.gameSettings.showDebugInfo && GuiScreen.isShiftKeyDown();
                        this.gameSettings.showLagometer = this.gameSettings.showDebugInfo && GuiScreen.isAltKeyDown();
                    }
                }
            }

            net.minecraftforge.fml.common.FMLCommonHandler.instance().fireKeyInput();
        }

        this.processKeyBinds();
    }

    /**
     * @reason More of the same, more code I'm converting from the original Vivecraft 1.12
     * @author SlepDepriv
     */
    @Overwrite
    private void processKeyBinds()
    {
        for (; this.gameSettings.keyBindTogglePerspective.isPressed(); this.renderGlobal.setDisplayListEntitiesDirty())
        {
            vrSettings.setOptionValue(VRSettings.VrOptions.MIRROR_DISPLAY, vrSettings.displayMirrorMode);
            notifyMirror(vrSettings.getKeyBinding(VRSettings.VrOptions.MIRROR_DISPLAY), false, 3000);
        }

        while (this.gameSettings.keyBindSmoothCamera.isPressed())
        {
            this.gameSettings.smoothCamera = !this.gameSettings.smoothCamera;
        }

        for (int i = 0; i < 9; ++i)
        {
            boolean flag = this.gameSettings.keyBindSaveToolbar.isKeyDown();
            boolean flag1 = this.gameSettings.keyBindLoadToolbar.isKeyDown();

            if (this.gameSettings.keyBindsHotbar[i].isPressed())
            {
                if (this.player.isSpectator())
                {
                    this.ingameGUI.getSpectatorGui().onHotbarSelected(i);
                }
                else if (!this.player.isCreative() || this.currentScreen != null || !flag1 && !flag)
                {
                    this.player.inventory.currentItem = i;
                }
                else
                {
                    GuiContainerCreative.handleHotbarSnapshots((Minecraft)(IMinecraftVR) this, i, flag1, flag);
                }
            }
        }

        while (this.gameSettings.keyBindInventory.isPressed())
        {
            if (this.playerController.isRidingHorse())
            {
                this.player.sendHorseInventory();
            }
            else
            {
                this.tutorial.openInventory();
                this.displayGuiScreen(new GuiInventory(this.player));
            }
        }

        while (this.gameSettings.keyBindAdvancements.isPressed())
        {
            this.displayGuiScreen(new GuiScreenAdvancements(this.player.connection.getAdvancementManager()));
        }

        while (this.gameSettings.keyBindSwapHands.isPressed())
        {
            if (!this.player.isSpectator())
            {
                this.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }

        while (this.gameSettings.keyBindDrop.isPressed())
        {
            if (!this.player.isSpectator())
            {
                this.player.dropItem(GuiScreen.isCtrlKeyDown());
            }
        }

        boolean flag2 = this.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN;

        if (flag2)
        {
            if (this.currentScreen == null && this.gameSettings.keyBindChat.isPressed())
            {
                this.displayGuiScreen(new GuiChat());
            }

            if (this.currentScreen == null && this.gameSettings.keyBindCommand.isPressed())
            {
                this.displayGuiScreen(new GuiChat("/"));
            }
        }

        if (this.player.isHandActive())
        {
            if (!this.gameSettings.keyBindUseItem.isKeyDown() && (bowTracker.isActive(player) == false || vrSettings.seated))
            {
                if(!autoFood.isEating())
                    this.playerController.onStoppedUsingItem(this.player);
            }

            label109:

            while (true)
            {
                if (!this.gameSettings.keyBindAttack.isPressed())
                {
                    while (this.gameSettings.keyBindUseItem.isPressed())
                    {
                        ;
                    }

                    while (true)
                    {
                        if (this.gameSettings.keyBindPickBlock.isPressed())
                        {
                            continue;
                        }

                        break label109;
                    }
                }
            }
        }
        else //not using item
        {
            //VIVE SUPPORT HAND SWINGING
            if (this.gameSettings.keyBindAttack.isPressed() && currentScreen == null)
            {
                this.clickMouse();
                lastClick = true;
            } else if (!this.gameSettings.keyBindAttack.isKeyDown()){
                this.leftClickCounter = 0;
                if (lastClick)
                {
                    this.playerController.resetBlockRemoving();
                }
                lastClick = false;
            }
            ///END VIVE

            while (this.gameSettings.keyBindUseItem.isPressed())
            {
                this.rightClickMouse();
            }

            while (this.gameSettings.keyBindPickBlock.isPressed())
            {
                this.middleClickMouse();
            }
        }

        if (this.gameSettings.keyBindUseItem.isKeyDown() && this.rightClickDelayTimer == 0 && !this.player.isHandActive() && currentScreen == null)
        { //someone tell me what this is for.
            this.rightClickMouse();
        }

        this.sendClickBlockToController(this.currentScreen == null && this.gameSettings.keyBindAttack.isKeyDown());
    }

    /**
     * @reason I really need more caffeine, urgh
     * @author SlepDepriv
     * @throws IOException
     */
    @Overwrite
    private void runTickMouse() throws IOException
    {
        while (Mouse.next())
        {
            if (net.minecraftforge.client.ForgeHooksClient.postMouseEvent()) continue;

            int i = Mouse.getEventButton();
            KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());

            if (Mouse.getEventButtonState())
            {
                if (this.player.isSpectator() && i == 2)
                {
                    this.ingameGUI.getSpectatorGui().onMiddleClick();
                }
                else
                {
                    KeyBinding.onTick(i - 100);
                }
            }

            if(!(GuiHandler.controllerMouseValid)){
                if (mouseHelper.deltaX > 0 || mouseHelper.deltaY> 0 )
                    GuiHandler.controllerMouseValid = true;
            }

            long j = getSystemTime() - this.systemTime;

            if (j <= 200L)
            {
                int k = Mouse.getEventDWheel();

                if (k != 0)
                {
                    if (this.player.isSpectator())
                    {
                        k = k < 0 ? -1 : 1;

                        if (this.ingameGUI.getSpectatorGui().isMenuActive())
                        {
                            this.ingameGUI.getSpectatorGui().onMouseScroll(-k);
                        }
                        else
                        {
                            float f = MathHelper.clamp(this.player.capabilities.getFlySpeed() + (float)k * 0.005F, 0.0F, 0.2F);
                            this.player.capabilities.setFlySpeed(f);
                        }
                    }
                    else
                    {
                        this.player.inventory.changeCurrentItem(k);
                    }
                }

                if (this.currentScreen == null)
                {
                    if (!this.inGameHasFocus && Mouse.getEventButtonState())
                    {
                        this.setIngameFocus();
                    }
                    else if (this.inGameHasFocus && !Display.isActive())
                    {
                        this.setIngameNotInFocus();
                    }
                }
                else if (this.currentScreen != null)
                {
                    this.currentScreen.handleMouseInput();
                }
            }
        }
    }

    /**
     * @reason Oh wait I have a cup of coffee right next to me, I am a fool.
     * @author SlepDepriv
     */
    @Overwrite
    public void setIngameFocus()
    {
        if (Display.isActive())
        {
            if (!this.inGameHasFocus)
            {
                if (!IS_RUNNING_ON_MAC)
                {
                    KeyBinding.updateKeyBindState();
                }

                this.inGameHasFocus = true;
                if(vrSettings.seated)
                    this.mouseHelper.grabMouseCursor(); // NO. BAD.
                this.displayGuiScreen((GuiScreen)null);
                this.leftClickCounter = 10000;
            }
        }
    }

    @Inject(method = "launchIntegratedServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V", shift = At.Shift.BEFORE))
    public void addToLaunchIntegratedServerFirst(CallbackInfo cir) {
        integratedServerLaunchInProgress = true;
    }

    @Inject(method = "launchIntegratedServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", shift = At.Shift.BEFORE, ordinal = 1))
    public void addToLaunchIntegratedServerSecond(CallbackInfo cir) {
        integratedServerLaunchInProgress = false;
    }

    @Inject(method = "loadWorld", at = @At(value = "HEAD"))
    public void addToLoadWorld(CallbackInfo cir) {
        vrPlayer.setRoomOrigin(0, 0, 0, true);
    }

    @Shadow
    private void displayDebugInfo(long elapsedTicksTime)
    {

    }

    @Shadow
    public static long getSystemTime()
    {
        return Sys.getTime() * 1000L / Sys.getTimerResolution();
    }


    @Shadow
    public void setIngameNotInFocus()
    {

    }

    @Shadow
    private void sendClickBlockToController(boolean inGame)
    {
    }

    @Shadow
    public void middleClickMouse()
    {
    }

    @Shadow
    public void rightClickMouse()
    {
    }

    @Shadow
    private void clickMouse()
    {
    }

    @Shadow
    @Nullable
    public NetHandlerPlayClient getConnection()
    {
        return this.player == null ? null : this.player.connection;
    }

    @Shadow
    private boolean processKeyF3(int auxKey)
    {
        return false;
    }

    @Shadow
    public void dispatchKeypresses()
    {

    }

    @Shadow
    protected void checkWindowResize()
    {
    }

    @Shadow
    public void displayInGameMenu()
    {
    }

    @Shadow
    public void shutdown()
    {
    }

    @Shadow
    public boolean isSingleplayer()
    {
        return true;
    }


    @Shadow
    public void displayGuiScreen(@Nullable GuiScreen guiScreenIn)
    {

    }
    @Shadow
    public void checkGLError(String message)
    {

    }

    @Shadow
    public void updateDisplay()
    {
        this.profiler.startSection("display_update");
        Display.update();
        this.profiler.endSection();
        this.checkWindowResize();
    }

    @Shadow
    private void updateDebugProfilerName(int keyCount)
    {

    }

    //VIVECRAFT ADDITIONS **************************************************************************


    public void printChatMessage(String msg)
    {
        if (this.world != null) {
            ITextComponent chatText = new TextComponentString(msg);
            this.ingameGUI.getChatGUI().printChatMessage(chatText);
        }
    }

    public Matrix4f getMRTransform(){
        //I swear to god this should be correct for column-major and post-multiplication for view matrix

        Vec3d roomo = vrPlayer.vrdata_world_render.origin;

        FloatBuffer conrot = null;

        Object temp;
        if(MCOpenVR.mrMovingCamActive){
            org.vivecraft.utils.Matrix4f temp2 = MCOpenVR.getAimRotation(2);
            conrot = temp2.inverted().toFloatBuffer();
        }else {
            //reconstruct from vrsettings
            Matrix4f m = (Matrix4f) vrSettings.vrFixedCamrotQuat.getMatrix();

            //m=m.rotate((float) Math.toRadians(-vrSettings.vrFixedCamrotYaw), new org.vivecraft.utils.lwjgl.Vector3f(0, 1, 0));
            //m=m.rotate((float) Math.toRadians(-vrSettings.vrFixedCamrotPitch), new org.vivecraft.utils.lwjgl.Vector3f(1, 0, 0));
            //m=m.rotate((float) Math.toRadians(vrSettings.vrFixedCamrotRoll), new org.vivecraft.utils.lwjgl.Vector3f(0, 0, 1));
            // idk why this one was here
            //m=m.rotate((float) Math.toRadians(180), new org.vivecraft.utils.lwjgl.Vector3f(0, 1, 0));

            matrixBuffer2.rewind();
            m.store(matrixBuffer2);
            matrixBuffer2.rewind();
            conrot = matrixBuffer2;

        }

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        //Room pose
        GlStateManager.translate(-roomo.x, -roomo.y, -roomo.z);
        GlStateManager.rotate((float) Math.toDegrees(vrPlayer.vrdata_world_render.rotation_radians), 0, 1, 0);
        //
        //scale everything in the room
        GlStateManager.scale(vrPlayer.vrdata_world_render.worldScale,
                vrPlayer.vrdata_world_render.worldScale,
                vrPlayer.vrdata_world_render.worldScale);
        ///

        //Device Pose
        Vec3d cp = vrPlayer.vrdata_room_pre.getController(2).getPosition();

        GlStateManager.translate(-cp.x, -cp.y, -cp.z);

        //this is here because.
        GlStateManager.multMatrix(conrot);

        //local offsets
        GlStateManager.translate(-vrSettings.mrMovingCamOffsetX, -vrSettings.mrMovingCamOffsetY, -vrSettings.mrMovingCamOffsetZ);
        GlStateManager.multMatrix(Utils.convertToOVRMatrix(vrSettings.mrMovingCamOffsetRotQuat.getMatrix()).toFloatBuffer());



        //put back scale
        GlStateManager.scale(1/vrPlayer.vrdata_world_render.worldScale, 1/vrPlayer.vrdata_world_render.worldScale, 1/vrPlayer.vrdata_world_render.worldScale);
        //

        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        GlStateManager.popMatrix();

        matrixBuffer.rewind();
        this.thirdPassViewMatrix.load(matrixBuffer);
        matrixBuffer.rewind();
        return (Matrix4f) thirdPassViewMatrix;
    }


    public void printGLMatrix(String derp){
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        matrixBuffer.rewind();
        Matrix4f temp = new Matrix4f();
        temp.load(matrixBuffer);
        System.out.println(derp + "\r\n" + temp.toString());
        matrixBuffer.rewind();
    }

    public void clearGLError() //bad bad bad
    {
        int var2 = GL11.glGetError();
    }
    //public org.lwjgl.util.vector.Matrix4f thirdPassInverseViewMatrix = new org.lwjgl.util.vector.Matrix4f();
    public org.lwjgl.util.vector.Matrix4f thirdPassViewMatrix = new org.lwjgl.util.vector.Matrix4f();

    private void copyToMirror()
    {
        // VIVE start - render eye buffers to the desktop window

        if (this.vrSettings.displayMirrorMode < VRSettings.MIRROR_OFF) // new values
            this.vrSettings.displayMirrorMode = VRSettings.MIRROR_ON_CROPPED;

        if (this.vrSettings.displayMirrorMode > VRSettings.MIRROR_ON_CROPPED) // new values
            this.vrSettings.displayMirrorMode = VRSettings.MIRROR_ON_CROPPED;

        if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_OFF && MCOpenVR.isHMDTracking()) {
            notifyMirror("Mirror is OFF", true, 1000);
        } else if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_MIXED_REALITY)  {
            if (VRShaders._DepthMask_shaderProgramId != 0) {
                doMixedRealityMirror();
            } else {
                notifyMirror("Shader compile failed, see log", true, 10000);
            }
        } else if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_ON_DUAL){
            VRFrameBuffer source = stereoProvider.framebufferEye0;
            VRFrameBuffer source2 = stereoProvider.framebufferEye1;

            if (source != null)
                source.framebufferRenderExt(0,
                        displayWidth/ 2, displayHeight, 0, true,0,0, false);

            if (source2 != null)
                source2.framebufferRenderExt((displayWidth / 2),
                        displayWidth / 2, displayHeight, 0, true,0,0, false);
        }
        else {
            float xcrop = 0;
            float ycrop = 0;
            boolean ar = false;
            VRFrameBuffer source = stereoProvider.framebufferEye0;
            if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_FIRST_PERSON) {
                source = stereoProvider.framebufferUndistorted;
            } else if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_THIRD_PERSON) {
                source = stereoProvider.framebufferMR;
            } else if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_ON_SINGLE || this.vrSettings.displayMirrorMode == VRSettings.MIRROR_OFF) {
                if (!this.vrSettings.displayMirrorLeftEye)
                    source = stereoProvider.framebufferEye1;
            } else if (this.vrSettings.displayMirrorMode == VRSettings.MIRROR_ON_CROPPED) {
                if (!this.vrSettings.displayMirrorLeftEye)
                    source = stereoProvider.framebufferEye1;
                xcrop = 0.15f;
                ycrop = 0.15f;
                ar = true;
            }
            //debug
            //	source = GuiHandler.guiFramebuffer;
            //	source = stereoProvider.framebufferEye0;
            //	source = stereoProvider.framebufferEye1;
            //	source = GuiHandler.guiFramebuffer;

            if (source != null)
                source.framebufferRenderExt(0, displayWidth, displayHeight, 0, true, xcrop, ycrop, ar);
        }
    }

    private void doMixedRealityMirror() {
//        boolean hasShaders = Config.isShaders();
        boolean alphaMask = this.vrSettings.mixedRealityUnityLike && this.vrSettings.mixedRealityAlphaMask;

        if (!alphaMask) GlStateManager.clearColor(vrSettings.mixedRealityKeyColor.getRed() / 255F, vrSettings.mixedRealityKeyColor.getGreen() / 255F, vrSettings.mixedRealityKeyColor.getBlue() / 255F, 1);
        else GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        Vec3d camplayer = new Vec3d(-thirdPassViewMatrix.m30, -thirdPassViewMatrix.m31, -thirdPassViewMatrix.m32).subtract(vrPlayer.vrdata_world_render.getEye(RenderPass.CENTER).getPosition());

        camplayer = camplayer.rotateYaw((float) Math.PI);
        org.lwjgl.util.vector.Matrix4f viewMatrix = new org.lwjgl.util.vector.Matrix4f(thirdPassViewMatrix);
        viewMatrix.m33 =1;
        viewMatrix.m30 =0;
        viewMatrix.m31 =0;
        viewMatrix.m32 =0;

        viewMatrix = (Matrix4f) viewMatrix.invert();

        org.lwjgl.util.vector.Vector3f CameraLook = Utils.directionFromMatrix(viewMatrix, 0, 0, 1);

        OpenGlHelper.glUseProgram(VRShaders._DepthMask_shaderProgramId);

        // set projection matrix
        ((IEntityRendererVR)entityRenderer).getThirdPassProjectionMatrix().store(matrixBuffer);
        matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix4ARB(VRShaders._DepthMask_projectionMatrix, false, matrixBuffer);

        // set view matrix
        viewMatrix.store(matrixBuffer);
        matrixBuffer.rewind();
        ARBShaderObjects.glUniformMatrix4ARB(VRShaders._DepthMask_viewMatrix, false, matrixBuffer);

        ARBShaderObjects.glUniform1iARB(VRShaders._DepthMask_colorTexUniform, 1);
        ARBShaderObjects.glUniform1iARB(VRShaders._DepthMask_depthTexUniform, 2);
        ARBShaderObjects.glUniform3fARB(VRShaders._DepthMask_hmdViewPosition, (float)camplayer.x, (float)camplayer.y, (float)camplayer.z);
        ARBShaderObjects.glUniform3fARB(VRShaders._DepthMask_hmdPlaneNormal, (float)-CameraLook.x, 0, (float) CameraLook.z);
        ARBShaderObjects.glUniform3fARB(VRShaders._DepthMask_keyColorUniform, vrSettings.mixedRealityKeyColor.getRed() / 255F, vrSettings.mixedRealityKeyColor.getGreen() / 255F, vrSettings.mixedRealityKeyColor.getBlue() / 255F);
        ARBShaderObjects.glUniform1iARB(VRShaders._DepthMask_alphaModeUniform, alphaMask ? 1 : 0);

        // bind color and depth textures
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        stereoProvider.framebufferMR.bindFramebufferTexture();
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE2);

//        if (hasShaders)
//            GlStateManager.bindTexture(Shaders.dfbDepthTextures.get(0)); // shadersmod has it's own depth buffer
//        else
            GlStateManager.bindTexture(stereoProvider.framebufferMR.depthBuffer);

        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);


        for(int i = 0; i < (alphaMask ? 3 : 2); i++) {

            int resW = displayWidth / 2;
            int resH = displayHeight;
            int posW = (displayWidth / 2) * i;
            int posH = 0;

            if (this.vrSettings.mixedRealityUnityLike) {
                resW = displayWidth / 2;
                resH = displayHeight / 2;
                if (this.vrSettings.mixedRealityAlphaMask && i == 2) {
                    posW = displayWidth / 2;
                    posH = displayHeight / 2;
                } else {
                    posW = 0;
                    posH = (displayHeight / 2) * (1 - i);
                }
            }

            // set other uniforms
            ARBShaderObjects.glUniform2fARB(VRShaders._DepthMask_resolutionUniform, resW, resH);
            ARBShaderObjects.glUniform2fARB(VRShaders._DepthMask_positionUniform, posW, posH);
            ARBShaderObjects.glUniform1iARB(VRShaders._DepthMask_passUniform, i);


            // draw framebuffer
            (stereoProvider.framebufferMR).framebufferRenderExt(posW, resW, resH, posH, true, 0,0,false);
        }

        OpenGlHelper.glUseProgram(0);

        if (this.vrSettings.mixedRealityUnityLike) {
            if(this.vrSettings.mixedRealityMRPlusUndistorted)
                (stereoProvider.framebufferUndistorted).framebufferRenderExt(displayWidth/ 2,displayWidth/ 2, displayHeight / 2, 0, true,0,0, false);
            else
                (stereoProvider.framebufferEye0).framebufferRenderExt(displayWidth/ 2, displayWidth/ 2, displayHeight/ 2, 0, true,0,0,false);
        }


    }

    private float fov = 1.0f;

    public boolean reinitflag;

    private int dispLastWidth, dispLastHeight;
    public boolean wasDisplayResized()
    {
        int h = Display.getHeight();
        int w = Display.getWidth();

        boolean was = dispLastHeight != h || dispLastWidth != w;
        dispLastHeight = h;
        dispLastWidth = w;
        return was;
    }

    public void initMinecrift() throws Exception
    {
        // Get underlying LWJGL Display implementation
        if (displayImpl == null)
        {
            try {
                Method displayMethod = Display.class.getDeclaredMethod("getImplementation");
                displayMethod.setAccessible(true);
                displayImpl = displayMethod.invoke(null, (java.lang.Object[])null); // VIVE fix warning
                System.out.println(String.format("[Minecrift] LWJGL Display implementation class: %s", new Object[]{displayImpl.getClass().toString()}));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            //Class.forName("org.vivecraft.provider.MCHydra").newInstance();//creates and registers MCHydra if it can be (if the libraries are found)
            //hydraLibsAvailable = true;
        } catch (NoClassDefFoundError e1) {
            System.err.println("Skipping loading: [Razer Hydra library] (Sixense-Java): "+e1.toString());
        } catch( Exception e1) {
            System.err.println("Skipping loading: [Razer Hydra library] (Sixense-Java): "+e1.toString());
        }

        System.out.println("Is this being triggered?");
        System.out.println("[DEBUG] What's going on?");

        new MCOpenVR();
        MCOpenVR.init();
        this.stereoProvider = new OpenVRStereoRenderer();
        this.vrPlayer = new OpenVRPlayer();
        this.vrSettings.vrAllowCrawling = false;
        //hmdInfo = PluginManager.configureHMD("oculus");

        //register Trackers
        vrPlayer.registerTracker(backpackTracker);
        vrPlayer.registerTracker(bowTracker);
        vrPlayer.registerTracker(climbTracker);
        vrPlayer.registerTracker(autoFood);
        vrPlayer.registerTracker(jumpTracker);
        vrPlayer.registerTracker(rowTracker);
        vrPlayer.registerTracker(runTracker);
        vrPlayer.registerTracker(sneakTracker);
        vrPlayer.registerTracker(swimTracker);
        vrPlayer.registerTracker(swingTracker);
        vrPlayer.registerTracker(teleportTracker);
        vrPlayer.registerTracker(horseTracker);
        vrPlayer.registerTracker(vehicleTracker);
        vrPlayer.registerTracker(interactTracker);

        //TODO: init new steroerenderer

        nativeMouseCursor = Mouse.getNativeCursor();
        try {
            invisibleMouseCursor = new Cursor(1, 1, 0, 0, 1, BufferUtils.createIntBuffer(1), null);
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
    }

    public void showNativeMouseCursor(boolean show)
    {
        if (show == lastShowMouseNative)
            return;

        lastShowMouseNative = show;

        try
        {
            if (show)
            {
                Mouse.setNativeCursor(nativeMouseCursor);
            }
            else
            {
                Mouse.setNativeCursor(invisibleMouseCursor);
            }
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
        }
    }

    public double getCurrentTimeSecs()
    {
        return this.stereoProvider.getCurrentTimeSecs();
    }

    boolean w;

    public void drawQuad()
    {
        // this func just draws a perfectly normal box with some texture coordinates
        GL11.glBegin(GL11.GL_QUADS);

        // Front Face
        GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(-1.0f, -1.0f,  0.0f);  // Bottom Left Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f( 1.0f, -1.0f,  0.0f);  // Bottom Right Of The Texture and Quad
        GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f( 1.0f,  1.0f,  0.0f);  // Top Right Of The Texture and Quad
        GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(-1.0f,  1.0f,  0.0f);  // Top Left Of The Texture and Quad

        GL11.glEnd();
    }

    /**
     * Sets the listener of sounds
     */
    public void updateSoundListener() {
        boolean loaded  = (boolean) MCReflection.SoundManager_loaded.get(((ISoundHandlerVR)soundHandler).getSndManager());
        if(loaded){
            SoundSystem sndSystem = (SoundSystem) MCReflection.SoundManager_sndSystem.get(((ISoundHandlerVR)soundHandler).getSndManager());
            Vec3d up = vrPlayer.vrdata_world_render.hmd.getCustomVector(new Vec3d(0, 1, 0));
            Vec3d hmdPos = vrPlayer.vrdata_world_render.getHeadPivot();
            Vec3d hmdDir = vrPlayer.vrdata_world_render.hmd.getDirection();

            if (sndSystem != null)
            {
                sndSystem.setListenerPosition((float)hmdPos.x, (float)hmdPos.y, (float)hmdPos.z);
                sndSystem.setListenerOrientation((float)hmdDir.x, (float)hmdDir.y, (float)hmdDir.z, (float)up.x, (float)up.y, (float)up.z);
            }
        }
    }

    private static void sleepNanos (long nanoDelay)
    {
        final long end = System.nanoTime() + nanoDelay;
        do
        {
            Thread.yield();  // This is a busy wait sadly...
        }
        while (System.nanoTime() < end);
    }

    private void addRunTickTimeNanos(long runTickTime)
    {
        int i = 0;
        medianRunTickTimeNanos = runTickTime;

        if (this.vrSettings.smoothRunTickCount < 1)
            this.vrSettings.smoothRunTickCount = 1;

        if (this.vrSettings.smoothRunTickCount % 2 == 0)
        {
            // Need an odd number for this
            this.vrSettings.smoothRunTickCount++;
        }

        runTickTimeNanos.addFirst(runTickTime);
        while (runTickTimeNanos.size() > this.vrSettings.smoothRunTickCount)
            runTickTimeNanos.removeLast();

        if (runTickTimeNanos.size() == this.vrSettings.smoothRunTickCount)
        {
            Long[] array = new Long[runTickTimeNanos.size()];
            for (Iterator itr = runTickTimeNanos.iterator(); itr.hasNext(); i++)
            {
                array[i] = (Long)itr.next();
            }
            Arrays.sort(array);
            medianRunTickTimeNanos = array[array.length / 2];
        }
    }

    private long getMedianRunTickTimeNanos()
    {
        return medianRunTickTimeNanos;
    }

    public void triggerYawTransition(boolean isPositive) {
        //	this.lookaimController.triggerYawTransition(isPositive);
    }

    public void print(String s)
    {
        s = s.replace("\n", "\n[Minecrift] ");
        System.out.println("[Minecrift] " + s);
    }

    public float watereffect, portaleffect, pumpkineffect;
    private boolean renderSingleView(int eye, float nano) {
        boolean shouldupdate = false;

        GlStateManager.clearColor(0f, 0, 0, 1f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();


        this.profiler.startSection("updateCameraAndRender");
        if (!this.skipRenderWorld)
        {
            //Forge calls onRenderTickStart > move to ER.drawFrameBuffer

            ///THIS IS WHERE EVERYTHING IS RENDERED
            this.entityRenderer.updateCameraAndRender( nano, System.nanoTime());

            //Forge calls onRenderTickEnd > move to ER.drawFrameBuffer
        }
        this.profiler.endSection();
        checkGLError("postucr " + eye);

        if(currentPass == RenderPass.LEFT || currentPass == RenderPass.RIGHT) {
            //copies the rendered scene to eye tex with fsaa and other postprocessing effects.
            this.profiler.startSection("postprocesseye");

            VRFrameBuffer source = this.vrFrameBuffer;

            if (this.vrSettings.useFsaa)
            {
                this.profiler.startSection("fsaa");
                stereoProvider.doFSAA(false);
                source = stereoProvider.fsaaLastPassResultFBO;
                checkGLError("fsaa " + eye);
                this.profiler.endSection();
            }

            if(currentPass == RenderPass.LEFT)
                stereoProvider.framebufferEye0.bindFramebuffer(true); //draw to L eye tex
            else
                stereoProvider.framebufferEye1.bindFramebuffer(true); //draw to R eye tex

            if(vrSettings.useFOVReduction && vrPlayer.getFreeMove()){
                if( player !=null && (Math.abs(player.moveForward) > 0 || Math.abs(player.moveStrafing) > 0)) {
                    fov -=0.05;
                    if(fov < 0.22) fov = 0.22f;
                } else {
                    fov +=0.01;
                    if(fov > 0.8) fov = 0.8f;
                }
            } else {
                fov = 1f;
            }

            ARBShaderObjects.glUseProgramObjectARB(VRShaders._FOVReduction_shaderProgramId);
            ARBShaderObjects.glUniform1iARB(VRShaders._FOVReduction_TextureUniform, 0);

            if(pumpkineffect > 0){
                ARBShaderObjects.glUniform1fARB(VRShaders._FOVReduction_RadiusUniform, 0.25f);
                ARBShaderObjects.glUniform1fARB(VRShaders._FOVReduction_BorderUniform, 0.0f);
            } else{
                ARBShaderObjects.glUniform1fARB(VRShaders._FOVReduction_RadiusUniform, fov);
                ARBShaderObjects.glUniform1fARB(VRShaders._FOVReduction_BorderUniform, 0.06f);
            }

            // VIVE start - screen flash when hurt instead of view tilt
            float r = 0, k = 0;
            // VIVE start - screen flash when hurt instead of view tilt
            float time =  (float) (System.currentTimeMillis() - usageSnooper.getMinecraftStartTimeMillis()) / 1000;
            if (player!=null && world !=null) {

                if(((IEntityRendererVR)entityRenderer).getWasinwater() != ((IEntityRendererVR)entityRenderer).getInWater()) {
                    watereffect = 2.3f;
                } else {
                    if(((IEntityRendererVR)entityRenderer).getInWater()){
                        watereffect -= (1f/120f);
                    } else {
                        watereffect -= (1f/60f);
                    }
                    if(watereffect < 0) watereffect = 0;
                }

                ((IEntityRendererVR)entityRenderer).setWasInwater(((IEntityRendererVR)entityRenderer).getInWater());

                if(false) watereffect = 0; //dont stack.

                if(((IEntityRendererVR)entityRenderer).getInPortal()){
                    portaleffect = 1f;
                } else {
                    portaleffect -= (1f/60f);
                    if(portaleffect < 0) portaleffect = 0;
                }

                float var3 = (float)player.hurtTime - nano;

                float percent = 1 - player.getHealth() / player.getMaxHealth();
                percent = (percent-0.5f) * 0.75f;

                if (var3>0.0f)
                {
                    var3 /= (float) player.maxHurtTime;
                    var3 = percent + MathHelper.sin(var3 * var3 * var3 * var3 * (float) Math.PI) * 0.5f;
                    r = var3;
                } else {
                    r =  (float) (percent * Math.abs(Math.sin(2.5f*time/(1-percent+.1) )));
                    if (player.isCreative()) r = 0;
                }

                if(((IEntityRendererVR)entityRenderer).getInBlock() && player.isDead == false){
                    //k = (float) entityRenderer.itemRenderer.inBlock;

                }

                if (player.isPlayerSleeping()){
                    if(k<0.8)k=.8f;
                }

                if (MCOpenVR.isWalkingAbout){
                    if(k<0.8)k=.5f;
                }

            } else {
                watereffect = 0;
                portaleffect = 0;
            }
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_HealthAlpha, r);
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_BlackAlpha, k);
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_time,time);
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_waterAmplitude, watereffect);
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_portalAmplitutde, portaleffect);
            ARBShaderObjects.glUniform1fARB(VRShaders._Overlay_pumpkinAmplitutde, pumpkineffect);
            ARBShaderObjects.glUniform1iARB(VRShaders._Overlay_eye, currentPass == currentPass.LEFT ? 1 : -1);

            source.framebufferRender(stereoProvider.framebufferEye0.framebufferWidth, stereoProvider.framebufferEye0.framebufferHeight);

            ARBShaderObjects.glUseProgramObjectARB(0);

            checkGLError("post-draw " + eye);

            this.profiler.endSection();

            //this.mcProfiler.startSection("OpenGL Finish");
            //	GL11.glFinish();//DO NOT LEAVE THIS UNCOMMENTED
            //this.mcProfiler.endSection();

        }

        return shouldupdate;
    }

    private float angleNormalize(float angle) {
        angle %= 360;
        if (angle < 0) angle += 360;
        return angle;
    }


    private float angleDiff(float a, float b) {
        float d = Math.abs(a - b) % 360;
        float r = d > 180 ? 360 - d : d;

        int sign = (a - b >= 0 && a - b <= 180) || (a - b <=-180 && a- b>= -360) ? 1 : -1;
        return r * sign;

    }

    public int getMouseXPos()
    {
        if(Display.isCreated() && Display.isActive())
            return Mouse.getX() * currentScreen.width / this.displayWidth;
        else if (currentScreen != null)
            return (int) GuiHandler.controllerMouseX  * this.currentScreen.width / this.displayWidth;

        return 0;
    }

    public int getMouseYPos()
    {
        if(Display.isCreated() && Display.isActive())
            return this.currentScreen.height - Mouse.getY() * this.currentScreen.height / this.displayHeight - 1;
        else if (currentScreen != null)
            return  (int) (this.currentScreen.height - GuiHandler.controllerMouseY * this.currentScreen.height / this.displayHeight - 1);

        return 0;
    }

    // FORGE
    public ItemColors getItemColors()
    {
        return this.itemColors;
    }

    public SearchTreeManager getSearchTreeManager()
    {
        return this.searchTreeManager;
    }
    //
    long mirroNotifyStart;
    String mirrorNotifyText;
    boolean mirrorNotifyClear;
    long mirroNotifyLen;

    public void notifyMirror( String text, boolean clear, int lengthMs) {
        mirroNotifyStart = System.currentTimeMillis();
        mirroNotifyLen = lengthMs;
        mirrorNotifyText = text;
        mirrorNotifyClear = clear;
    }

    private void drawNotifyMirror() {
        if (System.currentTimeMillis() < mirroNotifyStart + mirroNotifyLen) {
            GlStateManager.viewport(0, 0, displayWidth, displayHeight);
            GlStateManager.clear(256);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, displayWidth /4, displayHeight/4, 0.0D, -10, 20);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();

            if(mirrorNotifyClear) {
                GlStateManager.clearColor(0, 0, 0, 0);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
            }
            fontRenderer.drawStringWithShadow(mirrorNotifyText, 0,0, /*white*/16777215);
        }
    }

}
