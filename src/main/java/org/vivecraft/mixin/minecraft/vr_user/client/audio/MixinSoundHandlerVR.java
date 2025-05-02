package org.vivecraft.mixin.minecraft.vr_user.client.audio;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.vivecraft.intermediaries.interfaces.client.audio.ISoundHandlerVR;

@Mixin(SoundHandler.class)
public class MixinSoundHandlerVR implements ISoundHandlerVR {

    @Shadow
    private SoundManager sndManager;

    @Override
    public SoundManager getSndManager() {
        return sndManager;
    }
}
