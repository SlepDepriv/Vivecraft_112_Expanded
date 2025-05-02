package org.vivecraft.mixin;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

public class MixinLoader implements IFMLLoadingPlugin {
    static {
        // This block runs when the class is loaded, early during startup
        MixinBootstrap.init(); // Initialize Mixin system
        Mixins.addConfiguration("mixins.vivecraft.json"); // Load your mixin config
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0]; // No transformers needed here
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
//        return "org.vivecraft.asm.VivecraftASMTransformer";
        return "";
    }
}
