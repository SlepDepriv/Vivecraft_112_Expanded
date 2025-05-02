package org.vivecraft.mixin;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.util.Collections;
import java.util.Set;

public class MixinConfig implements IMixinConfig {
    @Override
    public MixinEnvironment getEnvironment() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public IMixinConfigSource getSource() {
        return null;
    }

    @Override
    public String getCleanSourceId() {
        return "";
    }

    @Override
    public String getMixinPackage() {
        return "";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public IMixinConfigPlugin getPlugin() {
        return null;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public Set<String> getTargets() {
        return Collections.emptySet();
    }

    @Override
    public <V> void decorate(String s, V v) {

    }

    @Override
    public boolean hasDecoration(String s) {
        return false;
    }

    @Override
    public <V> V getDecoration(String s) {
        return null;
    }
}
