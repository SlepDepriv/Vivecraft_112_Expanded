package org.vivecraft.mixin.minecraft.vr_user.client.gui;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.RenderItem;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.vivecraft.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.client.gui.IGuiScreenVR;
import org.vivecraft.settings.VRHotkeys;
import org.vivecraft.settings.VRSettings;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

@Mixin(GuiScreen.class)
public class MixinGUIScreenVR extends Gui implements GuiYesNoCallback, IGuiScreenVR {

    @Shadow
    private static Logger LOGGER;
    @Shadow
    private static Set<String> PROTOCOLS;
    @Shadow
    private static Splitter NEWLINE_SPLITTER;
    @Shadow
    public Minecraft mc;
    @Shadow
    protected RenderItem itemRender;
    @Shadow
    public int width;
    @Shadow
    public int height;
    @Shadow
    protected List<GuiButton> buttonList = Lists.<GuiButton>newArrayList();
    @Shadow
    protected List<GuiLabel> labelList = Lists.<GuiLabel>newArrayList();
    @Shadow
    public boolean allowUserInput;
    @Shadow
    protected FontRenderer fontRenderer;
    @Shadow
    protected GuiButton selectedButton;
    @Shadow
    private int eventButton;
    @Shadow
    private long lastMouseEvent;
    @Shadow
    private int touchValue;
    @Shadow
    private URI clickedLinkURI;
    @Shadow
    private boolean focused;
    @Shadow(remap = false)
    protected boolean keyHandled, mouseHandled;

    public boolean pressShiftFake;
    private static boolean isFakeShift(){
        return Minecraft.getMinecraft().currentScreen != null && ((IGuiScreenVR) Minecraft.getMinecraft().currentScreen).getPressShiftFake();
    }

    @Override
    public boolean getPressShiftFake() {
        return pressShiftFake;
    }

    @Override
    public void setPressShiftFake(boolean bool) {
        pressShiftFake = bool;
    }

    @Override
    public boolean getMouseDown() {
        return mouseDown;
    }

    @Override
    public void setMouseDown(boolean bool) {
        mouseDown = bool;
    }

    @Overwrite
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            /** MINECRIFT */
            // Escape pressed - save all
            VRSettings.saveAll(this.mc);
            /** END MINECRIFT */
            this.mc.displayGuiScreen((GuiScreen)null);

            if (this.mc.currentScreen == null)
            {
                this.mc.setIngameFocus();
            }
        }
    }

    public void keyTypedPublic(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    @Overwrite
    public void handleInput() throws IOException
    {
        if (Mouse.isCreated())
        {
            while (Mouse.next())
            {
                if (!GuiHandler.controllerMouseValid) {
                    GuiHandler.controllerMouseValid = true;
                    GuiHandler.controllerMouseTicks = 20;
                }

                this.mouseHandled = false;
                if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre((GuiScreen)(IGuiScreenVR) this))) continue;
                this.handleMouseInput();
                if (this.equals(this.mc.currentScreen) && !this.mouseHandled) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post((GuiScreen)(IGuiScreenVR)this));

            }
        }

        if (Keyboard.isCreated())
        {
            while (Keyboard.next())
            {
                this.keyHandled = false;
                if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Pre((GuiScreen)(IGuiScreenVR)this))) continue;
                this.handleKeyboardInput();
                if (this.equals(this.mc.currentScreen) && !this.keyHandled) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Post((GuiScreen)(IGuiScreenVR)this));
            }
        }
    }

    @Overwrite
    public void handleMouseInput() throws IOException
    {
        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int k = Mouse.getEventButton();

        /** MINECRIFT */
        if(!GuiHandler.controllerMouseValid){
            if (mc.mouseHelper.deltaX > 0 || mc.mouseHelper.deltaY> 0 )
                GuiHandler.controllerMouseValid = true;
        }
        /** END MINECRIFT */

        if (Mouse.getEventButtonState())
        {
            if (this.mc.gameSettings.touchscreen && this.touchValue++ > 0)
            {
                return;
            }

            this.eventButton = k;
            this.lastMouseEvent = Minecraft.getSystemTime();
            this.mouseClicked(i, j, this.eventButton);
        }
        else if (k != -1)
        {
            if (this.mc.gameSettings.touchscreen && --this.touchValue > 0)
            {
                return;
            }

            this.eventButton = -1;
            this.mouseReleased(i, j, k);
        }
        else if (this.eventButton != -1 && this.lastMouseEvent > 0L)
        {
            long l = Minecraft.getSystemTime() - this.lastMouseEvent;
            this.mouseClickMove(i, j, this.eventButton, l);
        }
    }

    @Overwrite
    public void handleKeyboardInput() throws IOException
    {
        if(Keyboard.getEventKey()==1 && KeyboardHandler.Showing){
            KeyboardHandler.setOverlayShowing(false);
            return;
        }
        /** MINECRIFT */

        {
            if (VRHotkeys.handleKeyboardGUIInputs(mc))
                return;
        }
        /** END MINECRIFT */

        char c0 = Keyboard.getEventCharacter();

        if (/*Keyboard.getEventKey() == 0 && c0 >= ' ' ||*/ Keyboard.getEventKeyState()) // wtf mojang
        {
            this.keyTyped(c0, Keyboard.getEventKey());
        }

        this.mc.dispatchKeypresses();
    }

    @Overwrite
    public void drawWorldBackground(int tint)
    {
        if (this.mc.world != null)
        {
            /** MINECRIFT */
            if (((IMinecraftVR)mc).getVRSettings() != null && ((IMinecraftVR)mc).getVRSettings().menuBackground == false)
            {
                this.drawGradientRect(0, 0, this.width, this.height, 0, 0);
            }
            else
            {
                this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
            }
            /** END MINECRIFT */
        }
        else
        {
            this.drawBackground(tint);
        }
    }

    @Overwrite
    public static boolean isShiftKeyDown()
    {
        return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54) || isFakeShift();
    }

    @Shadow
    public void drawBackground(int tint)
    {
    }

    @Shadow
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {

    }

    @Shadow
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
    }

    @Shadow
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {

    }

    /** MINECRIFT ADDITIONS BELOW */
    public boolean mouseDown;
    public void mouseDown( int rawX, int rawY, int button , boolean invertY)
    {
        int var1 = rawX * this.width / this.mc.displayWidth;
        int var2 = 0;

        if(invertY) { //need to figure out wtf is up with this some day.
            var2 = this.height - rawY * this.height / this.mc.displayHeight - 1;
        } else {
            var2 =  rawY * this.height / this.mc.displayHeight - 1;
        }

        this.eventButton = button;
        this.lastMouseEvent = Minecraft.getSystemTime();
        try {
            mouseClicked(var1, var2, button);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mouseDown = true;
    }

    public void mouseUp( int rawX, int rawY, int button, boolean invertY )
    {
        mouseDown = false;
        int var1 = rawX * this.width / this.mc.displayWidth;

        int var2 = 0;

        if(invertY) { //need to figure out wtf is up with this some day.
            var2 = this.height - rawY * this.height / this.mc.displayHeight - 1;
        } else {
            var2 =  rawY * this.height / this.mc.displayHeight - 1;
        }
        mouseReleased(var1, var2, button);
    }

    public void mouseDrag( int rawX, int rawY )
    {
        int var1 = rawX * this.width / this.mc.displayWidth;
        int var2 = this.height - rawY * this.height / this.mc.displayHeight - 1;
        long var3 = Minecraft.getSystemTime() - this.lastMouseEvent;
        this.mouseClickMove(var1, var2, this.eventButton, var3);
    }

    public void mouseGuiDown( int guiX, int guiY, int button )
    {
        this.eventButton = button;
        this.lastMouseEvent = Minecraft.getSystemTime();
        try {
            mouseClicked(guiX, guiY, button);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void mouseGuiUp( int guiX, int guiY, int button )
    {
        mouseDown = false;
        mouseReleased(guiX, guiY, button);
    }

    public void mouseGuiDrag( int guiX, int guiY )
    {
        long var3 = Minecraft.getSystemTime() - this.lastMouseEvent;
        this.mouseClickMove(guiX, guiY, this.eventButton, var3);
    }

    @Shadow
    public void confirmClicked(boolean result, int id) {

    }
}
