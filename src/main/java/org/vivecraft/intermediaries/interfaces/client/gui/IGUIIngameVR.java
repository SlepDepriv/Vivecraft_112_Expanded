package org.vivecraft.intermediaries.interfaces.client.gui;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ResourceLocation;

public interface IGUIIngameVR {

    ResourceLocation getInventoryBackground();
    void drawMouseMenuQuad(int mouseX, int mouseY);
}
