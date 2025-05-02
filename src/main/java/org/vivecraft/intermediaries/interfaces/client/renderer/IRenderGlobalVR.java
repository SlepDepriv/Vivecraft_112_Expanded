package org.vivecraft.intermediaries.interfaces.client.renderer;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.DestroyBlockProgress;

import java.util.Map;

public interface IRenderGlobalVR {

    Map<Integer, DestroyBlockProgress> getDamagedBlocks();

    void triggerRenderSky(BufferBuilder bufferBuilderIn, float posY, boolean reverseX);
    void triggerRenderStars(BufferBuilder bufferBuilderIn);

}
