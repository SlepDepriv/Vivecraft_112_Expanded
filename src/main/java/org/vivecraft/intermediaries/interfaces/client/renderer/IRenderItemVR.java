package org.vivecraft.intermediaries.interfaces.client.renderer;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public interface IRenderItemVR {

    boolean getIsMainhand();
    boolean getIsFPhand();

    void setIsMainhand(boolean bool);
    void setIsFPhand(boolean bool);

    void renderItem(float par1, ItemStack stack, EntityLivingBase entitylivingbaseIn, ItemCameraTransforms.TransformType transform, boolean leftHanded);
}
