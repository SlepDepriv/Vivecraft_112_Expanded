package org.vivecraft.intermediaries.interfaces.client.entity;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface IEntityPlayerSPVR {

    void setMovementTeleportTimer(int i);
    int getMovementTeleportTimer();
    void setTeleported(boolean bool);
    boolean getTeleported();
    void setItemInUseClient(ItemStack item);
    void setItemInUseCountClient(int count);
    boolean isClimbeyClimbEquipped();
    boolean isClimbeyJumpEquipped();
    void stepSound(BlockPos blockforNoise, Vec3d soundPos);
    boolean getOverrideEyeHeight();
    void setOverrideEyeHeight(boolean bool);

    boolean getInitFromServer();
    void setInitFromServer(boolean bool);

}
