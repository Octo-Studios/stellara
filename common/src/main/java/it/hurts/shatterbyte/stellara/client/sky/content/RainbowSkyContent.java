package it.hurts.shatterbyte.stellara.client.sky.content;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxContent;
import it.hurts.shatterbyte.stellara.client.sky.api.SkyboxRenderContext;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.render.SkyGeometry;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

public class RainbowSkyContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final List<SkyColor> colors;
    private final float yaw;
    private final float basePitch;
    private final float radius;
    private final float thickness;
    private final int segments;
    private final float arc;
    private final float opacity;
    private final float blur;

    public RainbowSkyContent(ResourceLocation id, int priority, List<SkyColor> colors, float yaw, float basePitch, float radius, float thickness, int segments, float arc, float opacity, float blur) {
        this.id = id;
        this.priority = priority;
        this.colors = List.copyOf(colors);
        this.yaw = yaw;
        this.basePitch = basePitch;
        this.radius = radius;
        this.thickness = thickness;
        this.segments = Math.max(8, segments);
        this.arc = arc;
        this.opacity = opacity;
        this.blur = Math.max(0F, blur);
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
    public float targetAlpha(SkyboxRenderContext context) {
        return Mth.clamp((1F - context.nightFactor()) * context.weatherFactor(), 0F, 1F);
    }

    @Override
    public void render(SkyboxRenderContext context, float alpha) {
        if (colors.isEmpty())
            return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        context.poseStack().pushPose();
        context.poseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-context.skyAngle()));

        var matrix = context.poseStack().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        var rainbowAlpha = alpha * opacity;

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        renderGradient(buffer, matrix, radius, radius - thickness, rainbowAlpha);

        Tesselator.getInstance().end();
        context.poseStack().popPose();
        RenderSystem.enableCull();
    }

    private void renderGradient(BufferBuilder buffer, Matrix4f matrix, float outerRadius, float innerRadius, float alpha) {
        var radialSteps = Math.max(24, colors.size() * 12);

        for (var radial = 0; radial < radialSteps; radial++) {
            var outer = radial / (float) radialSteps;
            var inner = (radial + 1F) / radialSteps;
            var outerArcRadius = Mth.lerp(outer, outerRadius, innerRadius);
            var innerArcRadius = Mth.lerp(inner, outerRadius, innerRadius);
            var outerColor = radialColor(outer);
            var innerColor = radialColor(inner);
            var outerRadialAlpha = radialAlpha(outer);
            var innerRadialAlpha = radialAlpha(inner);

            for (var segment = 0; segment < segments; segment++) {
                var left = segment / (float) segments;
                var right = (segment + 1F) / segments;
                var leftArcAlpha = alpha * edgeFade(left);
                var rightArcAlpha = alpha * edgeFade(right);

                addBand(buffer, matrix,
                        point(left, outerArcRadius),
                        point(left, innerArcRadius),
                        point(right, innerArcRadius),
                        point(right, outerArcRadius),
                        outerColor,
                        innerColor,
                        leftArcAlpha * outerRadialAlpha,
                        leftArcAlpha * innerRadialAlpha,
                        rightArcAlpha * innerRadialAlpha,
                        rightArcAlpha * outerRadialAlpha);
            }
        }
    }

    private SkyPoint point(float progress, float arcRadius) {
        var angle = (progress - 0.5F) * arc;
        var horizontal = Mth.sin(angle) * arcRadius;
        var vertical = basePitch + Mth.cos(angle) * arcRadius;
        var normalX = Mth.sin(yaw);
        var normalZ = Mth.cos(yaw);
        var sideX = Mth.cos(yaw);
        var sideZ = -Mth.sin(yaw);
        var x = normalX + sideX * horizontal;
        var y = vertical;
        var z = normalZ + sideZ * horizontal;
        var scale = SkyGeometry.SKY_RADIUS / Mth.sqrt(x * x + y * y + z * z);

        return new SkyPoint(x * scale, y * scale, z * scale);
    }

    private static float edgeFade(float progress) {
        return Mth.clamp(Mth.sin(progress * Mth.PI) * 1.35F, 0F, 1F);
    }

    private float radialAlpha(float progress) {
        if (blur <= 0F)
            return 1F;

        var fadeWidth = Mth.clamp(blur * 0.5F, 0.001F, 0.5F);
        var edgeProgress = Mth.clamp(Math.min(progress, 1F - progress) / fadeWidth, 0F, 1F);

        return Mth.sin(edgeProgress * Mth.HALF_PI);
    }

    private SkyColor color(float progress) {
        if (colors.size() == 1)
            return colors.get(0);

        var scaled = Mth.clamp(progress, 0F, 1F) * (colors.size() - 1);
        var index = Mth.clamp((int) Math.floor(scaled), 0, colors.size() - 2);
        var blend = scaled - index;
        var from = colors.get(index);
        var to = colors.get(index + 1);

        return new SkyColor(
                Mth.lerp(blend, from.red(), to.red()),
                Mth.lerp(blend, from.green(), to.green()),
                Mth.lerp(blend, from.blue(), to.blue())
        );
    }

    private SkyColor radialColor(float progress) {
        if (blur <= 0F)
            return colors.get(Mth.clamp((int) Math.floor(Mth.clamp(progress, 0F, 0.999999F) * colors.size()), 0, colors.size() - 1));

        return color(progress);
    }

    private static void addBand(BufferBuilder buffer, Matrix4f matrix, SkyPoint outerLeft, SkyPoint innerLeft, SkyPoint innerRight, SkyPoint outerRight, SkyColor outerColor, SkyColor innerColor, float outerLeftAlpha, float innerLeftAlpha, float innerRightAlpha, float outerRightAlpha) {
        buffer.vertex(matrix, outerLeft.x(), outerLeft.y(), outerLeft.z()).color(outerColor.red(), outerColor.green(), outerColor.blue(), outerLeftAlpha).endVertex();
        buffer.vertex(matrix, innerLeft.x(), innerLeft.y(), innerLeft.z()).color(innerColor.red(), innerColor.green(), innerColor.blue(), innerLeftAlpha).endVertex();
        buffer.vertex(matrix, innerRight.x(), innerRight.y(), innerRight.z()).color(innerColor.red(), innerColor.green(), innerColor.blue(), innerRightAlpha).endVertex();
        buffer.vertex(matrix, outerRight.x(), outerRight.y(), outerRight.z()).color(outerColor.red(), outerColor.green(), outerColor.blue(), outerRightAlpha).endVertex();
    }
}
