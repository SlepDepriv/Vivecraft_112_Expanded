package org.vivecraft.mixin.minecraft.vr_user.client;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LoadingScreenRenderer.class)
public class MixinLoadingScreenRendererVR {

    @Shadow
    private Minecraft mc;
    @Shadow
    private Framebuffer framebuffer;


    @Redirect(method = "setLoadingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V"))
    private void changeFrameBufferRender(Framebuffer instance ,int width, int height) {
        this.framebuffer.framebufferRender(mc.displayWidth, mc.displayHeight);
    }
}
