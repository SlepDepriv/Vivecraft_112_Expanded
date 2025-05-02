package org.vivecraft.intermediaries.interfaces.util;

import net.minecraft.client.settings.KeyBinding;
import org.vivecraft.control.VRInputAction;
import org.vivecraft.provider.MCOpenVR;

public interface IMovementInputFromOptionsVR {

    static float getMovementAxisValue(KeyBinding keyBinding) {
        VRInputAction action = MCOpenVR.getInputAction(keyBinding);
        return Math.abs(action.getAxis1DUseTracked());
    }

}
