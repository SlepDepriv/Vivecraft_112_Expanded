package org.vivecraft.intermediaries.replacements;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

import java.lang.reflect.Field;

public class VRDefaultVertexFormats {
    public static final VertexFormatElement POSITION_3F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3);
    public static final VertexFormatElement COLOR_4UB = new VertexFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.COLOR, 4);
    public static final VertexFormatElement TEX_2F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.UV, 2);
    public static final VertexFormatElement TEX_2S = new VertexFormatElement(1, VertexFormatElement.EnumType.SHORT, VertexFormatElement.EnumUsage.UV, 2);
    public static final VertexFormatElement NORMAL_3B = new VertexFormatElement(0, VertexFormatElement.EnumType.BYTE, VertexFormatElement.EnumUsage.NORMAL, 3);
    public static final VertexFormatElement PADDING_1B = new VertexFormatElement(0, VertexFormatElement.EnumType.BYTE, VertexFormatElement.EnumUsage.PADDING, 1);
    public static final VertexFormat POSITION_COLOR_NORMAL = (new VertexFormat()).addElement(POSITION_3F).addElement(COLOR_4UB).addElement(NORMAL_3B).addElement(PADDING_1B);
    public static final VertexFormat POSITION_TEX_LMAP_COLOR_NORMAL = (new VertexFormat()).addElement(POSITION_3F).addElement(TEX_2F).addElement(TEX_2S).addElement(COLOR_4UB).addElement(NORMAL_3B).addElement(PADDING_1B);

}
