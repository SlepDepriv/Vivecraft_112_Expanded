package org.vivecraft.intermediaries.interfaces.client.renderer;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.Vec3d;

public interface IItemRendererVR {

    boolean isInsideOfMaterial(Vec3d pos, Material materialIn);
    void triggerSetLightmap();
    boolean isInsideOpaqueBlock(Vec3d in, boolean set);

}
