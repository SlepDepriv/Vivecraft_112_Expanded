package org.vivecraft.mixin.minecraft.vr_user.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.provider.MCOpenVR;

import java.io.File;

@Mixin(GameSettings.class)
public class MixinGameSettingsVR {


    public int guiScaleUser;
    @Shadow
    public KeyBinding[] keyBindings;

    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/settings/GameSettings;difficulty:Lnet/minecraft/world/EnumDifficulty;",
            opcode = Opcodes.PUTFIELD))
    private void initVRBinding(Minecraft mcIn, File optionsFileIn, CallbackInfo ci) {
        this.keyBindings = MCOpenVR.initializeBindings(this.keyBindings);
    }

}
