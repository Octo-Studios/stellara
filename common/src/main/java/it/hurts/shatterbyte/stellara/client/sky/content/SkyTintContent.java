package it.hurts.shatterbyte.stellara.client.sky.content;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxContent;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRenderContext;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SkyTintContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final SkyColor color;
    private final float opacity;

    public SkyTintContent(ResourceLocation id, int priority, SkyColor color, float opacity) {
        this.id = id;
        this.priority = priority;
        this.color = color;
        this.opacity = opacity;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public void render(SkyboxRenderContext context, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        var matrix = context.poseStack().last().pose();
        var radius = 100F;
        var tintAlpha = alpha * opacity;
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addFace(buffer, matrix, -radius, -radius, -radius, -radius, -radius, radius, radius, -radius, radius, radius, -radius, -radius, tintAlpha);
        addFace(buffer, matrix, -radius, radius, radius, -radius, radius, -radius, radius, radius, -radius, radius, radius, radius, tintAlpha);
        addFace(buffer, matrix, -radius, -radius, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, radius, tintAlpha);
        addFace(buffer, matrix, radius, -radius, -radius, radius, radius, -radius, -radius, radius, -radius, -radius, -radius, -radius, tintAlpha);
        addFace(buffer, matrix, -radius, -radius, -radius, -radius, radius, -radius, -radius, radius, radius, -radius, -radius, radius, tintAlpha);
        addFace(buffer, matrix, radius, -radius, radius, radius, radius, radius, radius, radius, -radius, radius, -radius, -radius, tintAlpha);

        var mesh = buffer.build();
        if (mesh != null)
            BufferUploader.drawWithShader(mesh);
    }

    private void addFace(com.mojang.blaze3d.vertex.BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float alpha) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, x3, y3, z3).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, x4, y4, z4).setColor(color.red(), color.green(), color.blue(), alpha);
    }
}
