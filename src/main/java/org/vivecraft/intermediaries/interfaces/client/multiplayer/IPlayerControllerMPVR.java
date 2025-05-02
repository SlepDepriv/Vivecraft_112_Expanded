package org.vivecraft.intermediaries.interfaces.client.multiplayer;

import net.minecraft.util.math.Vec3d;

public interface IPlayerControllerMPVR {

    void setHitVecOverride(Vec3d vec);
    Vec3d getHitVecOverride();
}
