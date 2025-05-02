package org.vivecraft.gui.settings;

import java.io.IOException;

import org.vivecraft.gui.framework.BaseGuiSettings;
import org.vivecraft.gui.framework.GuiButtonEx;
import org.vivecraft.gui.framework.GuiSmallButtonEx;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

public class GuiRadialConfiguration extends BaseGuiSettings
{
    // VIVE START - hide options not supported by tracked controller UI
    static VRSettings.VrOptions[] options = new VRSettings.VrOptions[] {
            VRSettings.VrOptions.RADIAL_MODE_HOLD,

    };
    // VIVE END - hide options not supported by tracked controller UI

    public GuiRadialConfiguration(GuiScreen guiScreen, VRSettings guivrSettings) {
        super( guiScreen, guivrSettings );
        screenTitle = "vivecraft.options.screen.radialmenu";
    }
    
    private String[] arr;
    private boolean isShift = false;
    private int selectedIndex = -1;
    private GuiRadialItemsList list;
    private boolean isselectmode = false;
    
    public void setKey(KeyBinding key) {
    	
    	if(key != null)
    		arr[selectedIndex] = key.getKeyDescription();
    	else
    		arr[selectedIndex] = "";

    	this.selectedIndex = -1;
    	this.isselectmode = false;
    	this.reinit = true;
		this.list.setEnabled(false);

    	if(!this.isShift)
			((IMinecraftVR)mc).getVRSettings().vrRadialItems = ArrayUtils.clone(arr);
    	else
			((IMinecraftVR)mc).getVRSettings().vrRadialItemsAlt = ArrayUtils.clone(arr);

		((IMinecraftVR)mc).getVRSettings().saveOptions();
    }
    
    
    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
    	list = new GuiRadialItemsList(this, mc);
    	this.buttonList.clear();

    	if(this.isselectmode) {
        	this.buttonList.add(new GuiButtonEx(251, this.width / 2 - 155 ,  this.height -25 ,150,20, I18n.format("vivecraft.gui.clear")));
        	this.buttonList.add(new GuiButtonEx(250, this.width / 2 - 180  + 160, this.height -25,150,20, I18n.format("gui.cancel")));
    	}else {
    		
        	this.buttonList.add(new GuiButtonEx(ID_GENERIC_DEFAULTS, this.width / 2 - 155 ,  this.height -25 ,150,20, I18n.format("vivecraft.gui.loaddefaults")));
        	this.buttonList.add(new GuiButtonEx(ID_GENERIC_DONE, this.width / 2 - 180  + 160, this.height -25,150,20, I18n.format("gui.done")));
        	if(this.isShift)
        		this.buttonList.add(new GuiButton(201, this.width / 2 - 180+160, 32, 150, 20, I18n.format("vivecraft.gui.radialmenu.mainset")));
        	else
        		this.buttonList.add(new GuiButton(201, this.width / 2 - 180+160 ,32, 150, 20, I18n.format("vivecraft.gui.radialmenu.alternateset")));

        	VRSettings.VrOptions[] buttons = options;

        	for (int var12 = 2; var12 < buttons.length + 2; ++var12)
        	{
        		VRSettings.VrOptions var8 = buttons[var12 - 2];
        		int width = this.width / 2 - 180 + var12 % 2 * 160;
        		int height =  32 * (var12 / 2);
        		if (var8 == VRSettings.VrOptions.DUMMY)
        			continue;
        		this.buttonList.add(new GuiSmallButtonEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8)));         
        	}    

        	int numButts = 8;
        	int buttonwidthMin = 120;
        	int degreesPerButt = 360 / numButts;
        	int dist = 48;
        	int centerx = this.width / 2;
        	int centery = this.height / 2;
        	arr = ArrayUtils.clone(((IMinecraftVR)mc).getVRSettings().vrRadialItems);
        	String[] alt = ArrayUtils.clone(((IMinecraftVR)mc).getVRSettings().vrRadialItemsAlt);

        	if(this.isShift)
        		arr = alt;
    		
    	 	for (int i = 0; i < numButts; i++)
        	{
        		KeyBinding b = null;
        		for (KeyBinding kb: mc.gameSettings.keyBindings) {
    				if(kb.getKeyDescription().equalsIgnoreCase(arr[i]))
    					b = kb;				
    			}
        		
        		String str = ""; 
        		if(b!=null)		
        			
        			str = I18n.format(b.getKeyDescription());
        		int buttonwidth =  Math.max(buttonwidthMin, fontRenderer.getStringWidth(str));

        		int x=0,y=0;
        		if(i==0) {
        			x = 0;
        			y = -dist; 				
        		}
        		else if (i==1) {
        			x = buttonwidth/2 + 8;
        			y = -dist/2;
        		}
        		else if (i==2) {
        			x = buttonwidth/2 + 32;
        			y = 0; 	
        		}
        		else if (i==3) {
        			x = buttonwidth/2 + 8;
        			y = dist/2;      	
        		}
        		else if (i==4) {
        			x = 0;
        			y = dist; 	
        		}
        		else if (i==5) {
        			x = -buttonwidth/2 - 8;
        			y = dist/2;      	
        		}
        		else if (i==6) {
        			x = -buttonwidth/2 - 32;
        			y = 0; 	
        		}
        		else if (i==7) {
        			x = -buttonwidth/2 - 8;
        			y = -dist/2;
        		}

        		this.buttonList.add(new GuiButton(i, centerx + x - buttonwidth/2 , centery+y, buttonwidth, 20, str ));    
        	}
    	}
    	
   
    }    

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton)
    {
    	if (par1GuiButton.enabled)
    	{
    		if (par1GuiButton.id == ID_GENERIC_DONE)
    		{
				((IMinecraftVR)mc).getVRSettings().saveOptions();
    			this.mc.displayGuiScreen(this.parentGuiScreen);
    		}
    		else if (par1GuiButton.id == ID_GENERIC_DEFAULTS)
    		{
    			this.guivrSettings.radialModeHold = true;
    			this.guivrSettings.vrRadialItems = this.guivrSettings.getRadialItemsDefault();
    			this.guivrSettings.vrRadialItemsAlt = new String[8];

				((IMinecraftVR)mc).getVRSettings().saveOptions();
    			this.reinit = true;
    		}
    		else if(par1GuiButton.id == 201) {
    			this.isShift = !this.isShift;
    			this.reinit = true;
    		}
    		else if(par1GuiButton.id == 250) {
    			this.isselectmode = false;
    			this.reinit = true;
    		}
    		else if(par1GuiButton.id == 251) {
    			this.setKey(null);
    		}
    		else if (par1GuiButton instanceof GuiSmallButtonEx)
    		{
    			VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);
    			this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
    			par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));
    		}
    		else if(par1GuiButton.id < 200) {
    			this.selectedIndex = par1GuiButton.id;
    			this.isselectmode = true;
    			this.list.setEnabled(true);
    			this.reinit = true;
    		}
    	}
    }


    @Override
    public void drawScreen(int par1, int par2, float par3) {
        this.drawDefaultBackground();
    	
//    	if(!MCOpenVR.isBound(MCOpenVR.keyRadialMenu))
//    		this.drawCenteredString(this.fontRenderer, "The radial menu is not currently bound to a controller button.", this.width / 2, this.height - 50, 13777215);
//
		if (!this.isselectmode)
			this.drawCenteredString(this.fontRenderer, I18n.format("vivecraft.messages.radialmenubind.1"), this.width / 2, this.height - 50, 0x55FF55);

    	if(this.isShift)
    		this.drawCenteredString(this.fontRenderer, I18n.format("vivecraft.messages.radialmenubind.2"), this.width / 2, this.height - 36, 13777015);

    	if(this.isselectmode)
    		list.drawScreen(par1, par2, par3);    
    	
    	super.drawScreen(par1, par2, par3, false);

    }
    
    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {

    	boolean flag = false;

    	if (this.isselectmode) {
    		flag = this.list.mouseClicked(mouseX, mouseY, mouseButton);
    	} 

    	if (!flag)
    	{
    		try {
    			super.mouseClicked(mouseX, mouseY, mouseButton);
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
}
