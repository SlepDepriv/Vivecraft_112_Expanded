package org.vivecraft.intermediaries.replacements;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class VRFrameBuffer {
    public int framebufferTextureWidth;
    public int framebufferTextureHeight;
    public int framebufferWidth;
    public int framebufferHeight;
    public boolean useDepth;
    public int framebufferObject;
    public int framebufferTexture;
    public int depthBuffer;
    public float[] framebufferColor;
    public int framebufferFilter;

    //Vivecraft
    private static final int NO_TEXTURE_ID = -1;
    private boolean generatedFramebufferTexture;
    private boolean genMipMaps;
    private boolean linearFilter;
    private String name;
    private boolean useTextureInsteadofRenderBuffer = false;
    //

    public VRFrameBuffer(int width, int height, boolean useDepthIn)
    {
        this.useDepth = useDepthIn;
        this.framebufferObject = -1;
        this.framebufferTexture = -1;
        this.depthBuffer = -1;
        this.framebufferColor = new float[4];
        this.framebufferColor[0] = 1.0F;
        this.framebufferColor[1] = 1.0F;
        this.framebufferColor[2] = 1.0F;
        this.framebufferColor[3] = 0.0F;
        this.createBindFramebuffer(width, height);
    }

    public void createBindFramebuffer(int width, int height, int textureId)
    {
        if (!OpenGlHelper.isFramebufferEnabled())
        {
            this.framebufferWidth = width;
            this.framebufferHeight = height;
        }
        else
        {
            GlStateManager.enableDepth();

            if (this.framebufferObject >= 0)
            {
                this.deleteFramebuffer();
            }

            this.createFramebuffer(width, height, textureId);
            this.checkFramebufferComplete();
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        }
    }

    public void deleteFramebuffer()
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            this.unbindFramebufferTexture();
            this.unbindFramebuffer();

            if (this.depthBuffer > -1)
            {
                OpenGlHelper.glDeleteRenderbuffers(this.depthBuffer);
                this.depthBuffer = -1;
            }

            if (this.framebufferTexture > -1)
            {
                TextureUtil.deleteTexture(this.framebufferTexture);
                this.framebufferTexture = -1;
            }

            if (this.framebufferObject > -1)
            {
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
                OpenGlHelper.glDeleteFramebuffers(this.framebufferObject);
                this.framebufferObject = -1;
            }
        }
    }

    public void createFramebuffer(int width, int height, int textureId)
    {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        this.framebufferTextureWidth = width;
        this.framebufferTextureHeight = height;

        if (!OpenGlHelper.isFramebufferEnabled())
        {
            this.framebufferClear();
        }
        else
        {
            this.framebufferObject = OpenGlHelper.glGenFramebuffers();

            //Vivecraft allow making framebuffer from provided texture
            if (textureId == NO_TEXTURE_ID) {
                // generate texture
                this.framebufferTexture = TextureUtil.glGenTextures();
                this.generatedFramebufferTexture = true;
            }
            else {
                // Use supplied texture ID
                this.framebufferTexture = textureId;
                this.generatedFramebufferTexture = false;
            }
            //

            if (this.useDepth)
            {
                if (this.useTextureInsteadofRenderBuffer)
                    this.depthBuffer = TextureUtil.glGenTextures();
                else
                    this.depthBuffer = OpenGlHelper.glGenRenderbuffers();
            }


            if (linearFilter)
                this.setFramebufferFilter(GL11.GL_LINEAR);
            else
                this.setFramebufferFilter(9728);
            GlStateManager.bindTexture(this.framebufferTexture);
            GlStateManager.glTexImage2D(3553, 0, 32856, this.framebufferTextureWidth, this.framebufferTextureHeight, 0, 6408, 5121, (IntBuffer)null);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, 3553, this.framebufferTexture, 0);

            if (this.useDepth)
            {
                if (this.useTextureInsteadofRenderBuffer) {
                    //Vivecraft TODO: Re-evaluate this block.
                    GL11.glBindTexture((int)3553, this.depthBuffer );
                    GL11.glTexParameteri((int)3553, (int)10242, (int)10496);
                    GL11.glTexParameteri((int)3553, (int)10243, (int)10496);
                    GL11.glTexParameteri((int)3553, (int)10241, linearFilter ? GL11.GL_LINEAR : (int)9728);
                    GL11.glTexParameteri((int)3553, (int)10240, linearFilter ? GL11.GL_LINEAR : (int)9728);
                    GL11.glTexParameteri((int)3553, (int)34891, (int)6409);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, (int)0, GL30.GL_DEPTH24_STENCIL8, framebufferWidth, framebufferHeight, (int)0, GL30.GL_DEPTH_STENCIL,  GL30.GL_UNSIGNED_INT_24_8, (ByteBuffer)null);
                    GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depthBuffer, (int)0);
                    //
                } else {
                    OpenGlHelper.glBindRenderbuffer(OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
                    OpenGlHelper.glRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER, 33190, this.framebufferTextureWidth, this.framebufferTextureHeight);
                    OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
                }

//                Config.checkGlError("Create FBO");
            }

            this.checkFramebufferComplete();
            this.framebufferClear();
            this.unbindFramebufferTexture();
        }
    }

    public void setFramebufferFilter(int framebufferFilterIn)
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            this.framebufferFilter = framebufferFilterIn;
            GlStateManager.bindTexture(this.framebufferTexture);
            GlStateManager.glTexParameteri(3553, 10241, framebufferFilterIn);
            GlStateManager.glTexParameteri(3553, 10240, framebufferFilterIn);
            GlStateManager.glTexParameteri(3553, 10242, 10496);
            GlStateManager.glTexParameteri(3553, 10243, 10496);
            GlStateManager.bindTexture(0);
        }
    }


    public void checkFramebufferComplete()
    {
        int i = OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);

        if (i != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE)
        {
            if (i == OpenGlHelper.GL_FB_INCOMPLETE_ATTACHMENT)
            {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            }
            else if (i == OpenGlHelper.GL_FB_INCOMPLETE_MISS_ATTACH)
            {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            }
            else if (i == OpenGlHelper.GL_FB_INCOMPLETE_DRAW_BUFFER)
            {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            }
            else if (i == OpenGlHelper.GL_FB_INCOMPLETE_READ_BUFFER)
            {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            }
            else
            {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
            }
        }
    }

    public void bindFramebufferTexture()
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            GlStateManager.bindTexture(this.framebufferTexture);
        }
    }

    public void unbindFramebufferTexture()
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            GlStateManager.bindTexture(0);
        }
    }

    public void bindFramebuffer(boolean p_147610_1_)
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);

            if (p_147610_1_)
            {
                GlStateManager.viewport(0, 0, this.framebufferWidth, this.framebufferHeight);
            }
        }
    }

    public void unbindFramebuffer()
    {
        if (OpenGlHelper.isFramebufferEnabled())
        {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
        }
    }

    public void setFramebufferColor(float red, float green, float blue, float alpha)
    {
        this.framebufferColor[0] = red;
        this.framebufferColor[1] = green;
        this.framebufferColor[2] = blue;
        this.framebufferColor[3] = alpha;
    }

    public void framebufferRender(int width, int height)
    {
        this.framebufferRenderExt(width, height, true);
    }

//    public void framebufferRenderExt(int left, int width, int height, int top, boolean disableBlend)
//    {
//        if (OpenGlHelper.isFramebufferEnabled())
//        {
//            GlStateManager.colorMask(true, true, true, false);
//            GlStateManager.disableDepth();
//            GlStateManager.depthMask(false);
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.ortho(0.0D, (double)width, (double)height, 0.0D, 1000.0D, 3000.0D);
//            GlStateManager.matrixMode(5888);
//            GlStateManager.loadIdentity();
//            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
//            GlStateManager.viewport(left, top, width, height);
//            GlStateManager.enableTexture2D();
//            GlStateManager.disableLighting();
//            GlStateManager.disableAlpha();
//
//            if (disableBlend)
//            {
//                GlStateManager.disableBlend();
//                GlStateManager.enableColorMaterial();
//            }
//
//            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//            this.bindFramebufferTexture();
//            float f = (float)width;
//            float f1 = (float)height;
//            float f2 = (float)this.framebufferWidth / (float)this.framebufferTextureWidth;
//            float f3 = (float)this.framebufferHeight / (float)this.framebufferTextureHeight;
//            Tessellator tessellator = Tessellator.getInstance();
//            BufferBuilder bufferbuilder = tessellator.getBuffer();
//            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
//            bufferbuilder.pos(0.0D, (double)f1, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
//            bufferbuilder.pos((double)f, (double)f1, 0.0D).tex((double)f2, 0.0D).color(255, 255, 255, 255).endVertex();
//            bufferbuilder.pos((double)f, 0.0D, 0.0D).tex((double)f2, (double)f3).color(255, 255, 255, 255).endVertex();
//            bufferbuilder.pos(0.0D, 0.0D, 0.0D).tex(0.0D, (double)f3).color(255, 255, 255, 255).endVertex();
//            tessellator.draw();
//            this.unbindFramebufferTexture();
//            GlStateManager.depthMask(true);
//            GlStateManager.colorMask(true, true, true, true);
//        }
//    }

    //Vivecraft extended framebuffer drawing.
    public void framebufferRenderExt(int left, int width, int height, int top, boolean p_227588_3_, float xCropFactor, float yCropFactor, boolean keepAspect)
    {
        GlStateManager.colorMask(true, true, true, false);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, (double)width, (double)height, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
        //Vivecraft add x/y offsets
        GlStateManager.viewport(left, top, width, height);
        //
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();

        if (p_227588_3_)
        {
            GlStateManager.disableBlend();
            GlStateManager.enableColorMaterial();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindFramebufferTexture();

        float displayAspect = (float)width / (float)height;
        float frameAspect = (float)this.framebufferWidth / (float)this.framebufferHeight;


        float rightV = (float)width;
        float bottomV = (float)height;
        float leftV = 0;
        float topV = 0;

        if (keepAspect) {
            if (displayAspect > frameAspect) {
                float factor = (float)width / (float)this.framebufferWidth;
                leftV = 0;
                rightV = width;
                topV = height / 2f - ((this.framebufferHeight / 2f) * factor);
                bottomV = height / 2f + ((this.framebufferHeight / 2f) * factor);
            } else {
                float factor = (float)height / (float)this.framebufferHeight;
                leftV = width / 2f - ((this.framebufferWidth / 2f) * factor);
                rightV = width / 2f + ((this.framebufferWidth / 2f) * factor);
                topV = 0;
                bottomV = height;
            }
        }


        float f2 = (float)this.framebufferWidth / (float)this.framebufferTextureWidth;
        float f3 = (float)this.framebufferHeight / (float)this.framebufferTextureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(leftV, bottomV, 0.0D).tex(xCropFactor, yCropFactor).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(rightV, bottomV, 0.0D).tex(f2 - xCropFactor, yCropFactor).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(rightV, topV, 0.0D).tex(f2 - xCropFactor, f3 - yCropFactor).color(255, 255, 255, 255).endVertex();
        bufferbuilder.pos(leftV, topV, 0.0D).tex(xCropFactor, f3 - yCropFactor).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
        this.unbindFramebufferTexture();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
    }

    public void framebufferClear()
    {
        this.bindFramebuffer(true);
        GlStateManager.clearColor(this.framebufferColor[0], this.framebufferColor[1], this.framebufferColor[2], this.framebufferColor[3]);
        int i = 16384;

        if (this.useDepth)
        {
            GlStateManager.clearDepth(1.0D);
            i |= 256;
        }

        GlStateManager.clear(i);
        this.unbindFramebuffer();
    }
    //Vivecraft Additions
    public VRFrameBuffer(String name, int width, int height, boolean useDepth, boolean generateMipMaps, boolean linearFilter)
    {
        this(name, width, height, useDepth, generateMipMaps, linearFilter, false, 0,-1, true);
    }

    public VRFrameBuffer(String name, int width, int height, boolean useDepth, boolean generateMipMaps, boolean linearFilter, boolean multisample, int multisamplecount, int textureID, boolean textureDepth)
    {
        this.name = name;
        this.useDepth = useDepth;
        this.framebufferObject = -1;
        this.framebufferTexture = NO_TEXTURE_ID;
        this.generatedFramebufferTexture = true;
        this.depthBuffer = -1;
        this.framebufferColor = new float[4];
        this.framebufferColor[0] = 1.0F;
        this.framebufferColor[1] = 1.0F;
        this.framebufferColor[2] = 1.0F;
        this.framebufferColor[3] = 0.0F;
        this.genMipMaps = generateMipMaps;
        this.useTextureInsteadofRenderBuffer = textureDepth;
        this.linearFilter = linearFilter;
//        this.multiSample = multisample;
//        if (this.multiSample) {
//            this.multiSampleCount = multisamplecount;
//            this.textureType = GL32.GL_TEXTURE_2D_MULTISAMPLE;
//        }

        this.createBindFramebuffer(width, height, textureID);
    }
    public void genMipMaps()
    {
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);    // TODO: Minecrift - Check GLContext capabilities
    }
    public void createBindFramebuffer(int width, int height){
        this.createBindFramebuffer(width, height, NO_TEXTURE_ID);
    }

    public void framebufferRenderExt(int width, int height, boolean noblend)
    {
        this.framebufferRenderExt(0,width, height,0, noblend,0,0,false);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        if (name != null) sb.append("Name:   " + name).append("\n");
        sb.append("Size:   " + framebufferWidth + " x " + framebufferHeight).append("\n");
        sb.append("FB ID:  " + framebufferObject).append("\n");
        sb.append("Tex ID: " + framebufferTexture).append("\n");
        return sb.toString();
    }


    //Forge Support
    public boolean isStencilEnabled(){
        return true;
    }

    public boolean enableStencil()
    {
        return true;
    }


}
