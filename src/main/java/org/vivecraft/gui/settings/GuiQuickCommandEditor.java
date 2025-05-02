/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package org.vivecraft.gui.settings;

import java.io.IOException;

import net.minecraft.client.resources.I18n;
import org.vivecraft.gui.framework.BaseGuiSettings;
import org.vivecraft.gui.framework.GuiButtonEx;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.settings.VRSettings;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;


public class GuiQuickCommandEditor extends BaseGuiSettings {

	private GuiQuickCommandsList guiList;
	
	public GuiQuickCommandEditor(GuiScreen par1GuiScreen, VRSettings par2vrSettings) {
		super(par1GuiScreen, par2vrSettings);
        screenTitle = "vivecraft.options.screen.quickcommands";
	}

    /**
     * Fired when a key is typed (except F11 who toggle full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode)
    {
		for (int i = 0; i < 12; i++) {
			GuiTextField txt = ((GuiQuickCommandsList.CommandEntry)this.guiList.getListEntry(i)).txt;
			if (txt.isFocused()){
				txt.textboxKeyTyped(typedChar, keyCode);
			}
		}
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui() {
    	this.guiList = new GuiQuickCommandsList(this, mc);
        this.buttonList.clear();
        this.buttonList.add(new GuiButtonEx(ID_GENERIC_DEFAULTS, this.width / 2 - 155 ,  this.height -25 ,150,20, I18n.format("vivecraft.gui.loaddefaults")));
        this.buttonList.add(new GuiButtonEx(ID_GENERIC_DONE, this.width / 2 - 155  + 160, this.height -25,150,20, I18n.format("gui.done")));
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int par1, int par2, float par3) {
        if (reinit) {
            initGui();
            reinit = false;
        }
        this.guiList.drawScreen(par1, par2, par3);
        super.drawScreen(par1,par2,par3,false);
    }

    /**
     * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
     */
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if (par1GuiButton.id == ID_GENERIC_DONE) {
    		for (int i = 0; i < 12; i++) {
    			String c = ((GuiQuickCommandsList.CommandEntry)this.guiList.getListEntry(i)).txt.getText();
				((IMinecraftVR)mc).getVRSettings().vrQuickCommands[i] = c;
			}
    		
            this.guivrSettings.saveOptions();
            this.mc.displayGuiScreen(this.parentGuiScreen);
        } else if (par1GuiButton.id == ID_GENERIC_DEFAULTS){
            ((IMinecraftVR)mc).getVRSettings().vrQuickCommands = ((IMinecraftVR)mc).getVRSettings().getQuickCommandsDefaults();
        	this.initGui();
        }
    }
    
    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
		for (int i = 0; i < 12; i++) {
			GuiTextField txt = ((GuiQuickCommandsList.CommandEntry)this.guiList.getListEntry(i)).txt;
			txt.setFocused(false);
		}
    	
      if (mouseButton != 0 || !this.guiList.mouseClicked(mouseX, mouseY, mouseButton))
        {
            try {
				super.mouseClicked(mouseX, mouseY, mouseButton);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    /**
     * Called when a mouse button is released.  Args : mouseX, mouseY, releaseButton\n \n@param state Will be negative
     * to indicate mouse move and will be either 0 or 1 to indicate mouse up.
     */
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (state != 0 || !this.guiList.mouseReleased(mouseX, mouseY, state))
        {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    
}
