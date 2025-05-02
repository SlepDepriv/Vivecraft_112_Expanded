package org.vivecraft.gui.settings;

import net.minecraft.client.resources.I18n;
import org.vivecraft.gui.framework.BaseGuiSettings;
import org.vivecraft.gui.framework.GuiButtonEx;
import org.vivecraft.gui.framework.GuiEventEx;
import org.vivecraft.gui.framework.GuiSliderEx;
import org.vivecraft.gui.framework.GuiSmallButtonEx;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.settings.VRSettings.VrOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiSeatedOptions extends BaseGuiSettings implements GuiEventEx
{
	static VRSettings.VrOptions[] seatedOptions = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.X_SENSITIVITY,
			VRSettings.VrOptions.Y_SENSITIVITY,
			VRSettings.VrOptions.KEYHOLE,
            VRSettings.VrOptions.SEATED_HUD_XHAIR,
			VRSettings.VrOptions.WALK_UP_BLOCKS,
            VRSettings.VrOptions.WORLD_ROTATION_INCREMENT,
            VRSettings.VrOptions.SEATED_FREE_MOVE        
	};
	
	
	public GuiSeatedOptions(GuiScreen guiScreen, VRSettings guivrSettings) {
		super( guiScreen, guivrSettings );
		screenTitle = "vivecraft.options.screen.seated";
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	public void initGui()
	{
		this.buttonList.clear();
		this.buttonList.add(new GuiButtonEx(ID_GENERIC_DEFAULTS, this.width / 2 - 155 ,  this.height -25 ,150,20, I18n.format("vivecraft.gui.loaddefaults")));
		this.buttonList.add(new GuiButtonEx(ID_GENERIC_DONE, this.width / 2 - 155  + 160, this.height -25,150,20, I18n.format("gui.done")));

		VRSettings.VrOptions[] buttons = seatedOptions;

		addButtons(buttons, 0);
		
		this.buttonList.add(new GuiButtonEx(300, this.width / 2 - 155 , this.height / 6 + 102,150,20, I18n.format("vivecraft.options.screen.freemove.button")));
		this.buttonList.add(new GuiButtonEx(301, this.width / 2 + 5 , this.height / 6 + 102,150,20, I18n.format("vivecraft.options.screen.teleport.button")));

	}

	private void addButtons(VRSettings.VrOptions[] buttons, int y) {
		for (int var12 = 2; var12 < buttons.length + 2; ++var12)
		{
			VRSettings.VrOptions var8 = buttons[var12 - 2];
			int width = this.width / 2 - 155 + var12 % 2 * 160;
			int height = this.height / 6 + 21 * (var12 / 2) - 10 + y;

			if (var8 == VRSettings.VrOptions.DUMMY)
				continue;
			
			boolean show = true;
			
			if (var8.getEnumFloat())
			{
				float minValue = 0.0f;
				float maxValue = 1.0f;
				float increment = 0.01f;

				if (var8 == VRSettings.VrOptions.X_SENSITIVITY)
				{
					minValue = 0.1f;
					maxValue = 5f;
					increment = 0.01f;
				}
				else if (var8 == VRSettings.VrOptions.Y_SENSITIVITY)
				{
					minValue = 0.1f;
					maxValue = 5f;
					increment = 0.01f;
				}
				else if (var8 == VRSettings.VrOptions.KEYHOLE)
				{
					minValue = 0f;
					maxValue = 40f;
					increment = 5f;
				}
                else if (var8 == VrOptions.WORLD_ROTATION_INCREMENT){
                    minValue = -1f;
                    maxValue = 4f;
                    increment = 1f;
                }
                else if(var8 == VrOptions.TELEPORT_DOWN_LIMIT){
                    minValue = 0f;
                    maxValue = 16f;
                    increment = 1f;
                    show = this.guivrSettings.vrLimitedSurvivalTeleport;
                }
                else if(var8 == VrOptions.TELEPORT_UP_LIMIT){
                    minValue = 0f;
                    maxValue = 4f;
                    increment = 1f;
                    show = this.guivrSettings.vrLimitedSurvivalTeleport;
                }
                else if(var8 == VrOptions.TELEPORT_HORIZ_LIMIT){
                    minValue = 0f;
                    maxValue = 32f;
                    increment = 1f;
                    show = this.guivrSettings.vrLimitedSurvivalTeleport;
                }
				if(show) {
					GuiSliderEx slider = new GuiSliderEx(var8.returnEnumOrdinal(), width, height , var8, this.guivrSettings.getKeyBinding(var8), minValue, maxValue, increment, this.guivrSettings.getOptionFloatValue(var8));
					slider.setEventHandler(this);
					this.buttonList.add(slider);
				}
			}
			else
			{
				this.buttonList.add(new GuiSmallButtonEx(var8.returnEnumOrdinal(), width, height , var8, this.guivrSettings.getKeyBinding(var8)));
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
				((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings().saveOptions();
				this.mc.displayGuiScreen(this.parentGuiScreen);
			}
			else if (par1GuiButton.id == ID_GENERIC_DEFAULTS)
			{
				VRSettings vrSettings=((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings();
				vrSettings.keyholeX=15;
				vrSettings.xSensitivity=1;
				vrSettings.ySensitivity=1;
				vrSettings.seatedUseHMD = false;
				vrSettings.seatedHudAltMode = false;
				((IMinecraftVR)Minecraft.getMinecraft()).getVRSettings().saveOptions();
				this.reinit = true;
			}
			else if (par1GuiButton.id == 300) {
				this.mc.displayGuiScreen(new GuiFreeMoveSettings(this, guivrSettings));
			}
			else if (par1GuiButton.id == 301) {
				this.mc.displayGuiScreen(this.parentGuiScreen);
				this.mc.displayGuiScreen(new GuiTeleportSettings(this, guivrSettings));
			}

			else if (par1GuiButton instanceof GuiSmallButtonEx)
			{
				VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);
				this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
				par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));
			}
		}
	}

	@Override
	public boolean event(int id, VrOptions enumm) {
		return true;
	}

	@Override
	public boolean event(int id, String s) {
		return true;
	}
}
