package org.vivecraft.utils;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.crash.ICrashReportDetail;


public class CrashReportScreenName implements ICrashReportDetail<String> {
    private final GuiScreen screen;

    public CrashReportScreenName(GuiScreen screen) {
        this.screen = screen;
    }

    @Override
    public String call() throws Exception {
        return screen.getClass().getCanonicalName();
    }
}
