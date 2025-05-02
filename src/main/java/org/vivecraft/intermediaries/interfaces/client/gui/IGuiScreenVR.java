package org.vivecraft.intermediaries.interfaces.client.gui;

import java.io.IOException;

public interface IGuiScreenVR {

    boolean getPressShiftFake();
    void setPressShiftFake(boolean bool);

    static boolean isFakeShift() {
        return false;
    }

    boolean getMouseDown();
    void setMouseDown(boolean bool);
    void mouseDown( int rawX, int rawY, int button , boolean invertY);
    void mouseUp( int rawX, int rawY, int button, boolean invertY );
    void mouseDrag( int rawX, int rawY );
    void mouseGuiDown( int guiX, int guiY, int button );
    void mouseGuiUp( int guiX, int guiY, int button );
    void mouseGuiDrag( int guiX, int guiY );
    void keyTypedPublic(char typedChar, int keyCode) throws IOException;
}
