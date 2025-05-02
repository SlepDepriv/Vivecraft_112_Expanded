package org.vivecraft.intermediaries.replacements;

public class VRGLStateManagerColor {
    public float red;
    public float green;
    public float blue;
    public float alpha;

    public VRGLStateManagerColor()
    {
        this(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public VRGLStateManagerColor(float redIn, float greenIn, float blueIn, float alphaIn)
    {
        this.red = 1.0F;
        this.green = 1.0F;
        this.blue = 1.0F;
        this.alpha = 1.0F;
        this.red = redIn;
        this.green = greenIn;
        this.blue = blueIn;
        this.alpha = alphaIn;
    }
}
