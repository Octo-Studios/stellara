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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.joml.Matrix4f;

import java.util.List;

public class SkyRibbonContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final List<SkyColor> colors;
    private final Ribbon ribbon;
    private final int segments;
    private final int gradientSteps;
    private final float centerYaw;
    private final float basePitch;
    private final float spacing;
    private final float thickness;
    private final float arc;
    private final float tilt;
    private final float waveAmplitude;
    private final float waveFrequency;
    private final float wobbleAmplitude;
    private final float opacity;
    private final float speed;
    private final float edgeSoftness;
    private final float blur;

    public SkyRibbonContent(ResourceLocation id, int priority, List<SkyColor> colors, int segments, int gradientSteps, float centerYaw, float basePitch, float spacing, float thickness, float arc, float tilt, float waveAmplitude, float waveFrequency, float wobbleAmplitude, float opacity, float speed, float edgeSoftness, float blur, long seed) {
        this.id = id;
        this.priority = priority;
        this.colors = List.copyOf(colors);
        this.ribbon = makeRibbon(seed);
        this.segments = Math.max(8, segments);
        this.gradientSteps = Math.max(2, gradientSteps);
        this.centerYaw = centerYaw;
        this.basePitch = basePitch;
        this.spacing = spacing;
        this.thickness = thickness;
        this.arc = arc;
        this.tilt = tilt;
        this.waveAmplitude = waveAmplitude;
        this.waveFrequency = waveFrequency;
        this.wobbleAmplitude = wobbleAmplitude;
        this.opacity = opacity;
        this.speed = speed;
        this.edgeSoftness = edgeSoftness;
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
    public void render(SkyboxRenderContext context, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        var matrix = context.poseStack().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        var time = context.renderTime() * speed;

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        renderRibbon(buffer, matrix, ribbon, time, alpha * opacity);

        Tesselator.getInstance().end();
        RenderSystem.enableCull();
    }

    private void renderRibbon(BufferBuilder buffer, Matrix4f matrix, Ribbon ribbon, float time, float alpha) {
        var ribbonOffset = spacing + ribbon.offset();
        var ribbonAlpha = alpha * ribbon.alpha() * (0.78F + 0.22F * Mth.sin(time * 0.73F + ribbon.phaseA()));

        for (var segment = 0; segment < segments; segment++) {
            var left = segment / (float) segments;
            var right = isClosedArc() && segment == segments - 1 ? 0F : (segment + 1F) / segments;
            var leftFade = arcFade(left);
            var rightFade = arcFade(right);

            for (var step = 0; step < gradientSteps; step++) {
                var bottom = step / (float) gradientSteps;
                var top = (step + 1F) / gradientSteps;
                var bottomAlpha = gradientAlpha(bottom);
                var topAlpha = gradientAlpha(top);

                addQuad(buffer, matrix,
                        point(ribbon, ribbonOffset, left, bottom, time),
                        point(ribbon, ribbonOffset, left, top, time),
                        point(ribbon, ribbonOffset, right, top, time),
                        point(ribbon, ribbonOffset, right, bottom, time),
                        ribbonColor(bottom + ribbon.colorShift()),
                        ribbonColor(top + ribbon.colorShift()),
                        ribbonAlpha * leftFade * bottomAlpha,
                        ribbonAlpha * leftFade * topAlpha,
                        ribbonAlpha * rightFade * topAlpha,
                        ribbonAlpha * rightFade * bottomAlpha);
            }
        }
    }

    private SkyPoint point(Ribbon ribbon, float ribbonOffset, float progress, float widthProgress, float time) {
        var angle = (progress - 0.5F) * arc;
        var widthOffset = (widthProgress - 0.5F) * thickness * ribbon.thicknessScale();
        var primaryFrequency = periodicFrequency(waveFrequency);
        var detailFrequency = periodicFrequency(waveFrequency * 2.7F);
        var wobbleFrequency = periodicFrequency(waveFrequency * 1.35F);
        var largeWave = Mth.sin(progress * Mth.TWO_PI * primaryFrequency + time + ribbon.phaseA()) * waveAmplitude;
        var smallWave = Mth.sin(progress * Mth.TWO_PI * detailFrequency - time * 0.64F + ribbon.phaseB()) * waveAmplitude * 0.34F;
        var sideWobble = Mth.sin(progress * Mth.TWO_PI * wobbleFrequency + time * 0.41F + ribbon.phaseC()) * wobbleAmplitude;
        var pitch = clampPitch(basePitch + ribbonOffset + widthOffset + largeWave + smallWave);
        var yaw = angle + sideWobble;
        var point = SkyGeometry.point(yaw, pitch);

        var cosTilt = Mth.cos(tilt);
        var sinTilt = Mth.sin(tilt);
        var tilted = Math.abs(tilt) < 0.001F
                ? point
                : new SkyPoint(point.x(), point.y() * cosTilt - point.z() * sinTilt, point.y() * sinTilt + point.z() * cosTilt);
        var cosYaw = Mth.cos(centerYaw);
        var sinYaw = Mth.sin(centerYaw);

        return new SkyPoint(tilted.x() * cosYaw + tilted.z() * sinYaw, tilted.y(), tilted.z() * cosYaw - tilted.x() * sinYaw);
    }

    private float arcFade(float progress) {
        if (isClosedArc())
            return 1F;

        var left = Mth.clamp(progress / Math.max(0.001F, edgeSoftness), 0F, 1F);
        var right = Mth.clamp((1F - progress) / Math.max(0.001F, edgeSoftness), 0F, 1F);

        return Mth.sin(Math.min(left, right) * Mth.HALF_PI);
    }

    private float periodicFrequency(float frequency) {
        if (!isClosedArc())
            return frequency;

        return Math.max(1F, Math.round(frequency));
    }

    private boolean isClosedArc() {
        return arc >= Mth.TWO_PI - 0.02F;
    }

    private float gradientAlpha(float progress) {
        if (blur <= 0F)
            return 1F;

        var fadeWidth = Mth.clamp(blur * 0.5F, 0.001F, 0.5F);
        var edgeProgress = Mth.clamp(Math.min(progress, 1F - progress) / fadeWidth, 0F, 1F);

        return Mth.sin(edgeProgress * Mth.HALF_PI);
    }

    private SkyColor color(float progress) {
        if (colors.size() == 1)
            return colors.get(0);

        var wrapped = progress - Mth.floor(progress);
        var scaled = wrapped * colors.size();
        var index = Mth.floor(scaled);
        var next = (index + 1) % colors.size();
        var blend = scaled - index;
        var from = colors.get(index);
        var to = colors.get(next);

        return new SkyColor(Mth.lerp(blend, from.red(), to.red()), Mth.lerp(blend, from.green(), to.green()), Mth.lerp(blend, from.blue(), to.blue()));
    }

    private SkyColor ribbonColor(float progress) {
        if (colors.size() == 1)
            return colors.get(0);

        if (blur <= 0F) {
            var wrapped = progress - Mth.floor(progress);

            return colors.get(Mth.clamp((int) Math.floor(wrapped * colors.size()), 0, colors.size() - 1));
        }

        return color(progress);
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix, SkyPoint lowerLeft, SkyPoint upperLeft, SkyPoint upperRight, SkyPoint lowerRight, SkyColor lowerColor, SkyColor upperColor, float lowerLeftAlpha, float upperLeftAlpha, float upperRightAlpha, float lowerRightAlpha) {
        var center = normalize(
                lowerLeft.x() + upperLeft.x() + upperRight.x() + lowerRight.x(),
                lowerLeft.y() + upperLeft.y() + upperRight.y() + lowerRight.y(),
                lowerLeft.z() + upperLeft.z() + upperRight.z() + lowerRight.z()
        );
        var centerColor = new SkyColor((lowerColor.red() + upperColor.red()) * 0.5F, (lowerColor.green() + upperColor.green()) * 0.5F, (lowerColor.blue() + upperColor.blue()) * 0.5F);
        var centerAlpha = (lowerLeftAlpha + upperLeftAlpha + upperRightAlpha + lowerRightAlpha) * 0.25F;

        addTriangle(buffer, matrix, lowerLeft, lowerColor, lowerLeftAlpha, upperLeft, upperColor, upperLeftAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, upperLeft, upperColor, upperLeftAlpha, upperRight, upperColor, upperRightAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, upperRight, upperColor, upperRightAlpha, lowerRight, lowerColor, lowerRightAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, lowerRight, lowerColor, lowerRightAlpha, lowerLeft, lowerColor, lowerLeftAlpha, center, centerColor, centerAlpha);
    }

    private static SkyPoint normalize(float x, float y, float z) {
        var length = SkyGeometry.SKY_RADIUS / Mth.sqrt(x * x + y * y + z * z);

        return new SkyPoint(x * length, y * length, z * length);
    }

    private static void addTriangle(BufferBuilder buffer, Matrix4f matrix, SkyPoint first, SkyColor firstColor, float firstAlpha, SkyPoint second, SkyColor secondColor, float secondAlpha, SkyPoint third, SkyColor thirdColor, float thirdAlpha) {
        buffer.vertex(matrix, first.x(), first.y(), first.z()).color(firstColor.red(), firstColor.green(), firstColor.blue(), firstAlpha).endVertex();
        buffer.vertex(matrix, second.x(), second.y(), second.z()).color(secondColor.red(), secondColor.green(), secondColor.blue(), secondAlpha).endVertex();
        buffer.vertex(matrix, third.x(), third.y(), third.z()).color(thirdColor.red(), thirdColor.green(), thirdColor.blue(), thirdAlpha).endVertex();
    }

    private static float clampPitch(float pitch) {
        return Mth.clamp(pitch, -1.42F, 1.42F);
    }

    private static Ribbon makeRibbon(long seed) {
        var random = new LegacyRandomSource(seed);

        return new Ribbon(
                randomRange(-0.035F, 0.035F, random),
                randomRange(0.72F, 1.18F, random),
                randomRange(0.65F, 1.35F, random),
                random.nextFloat() * Mth.TWO_PI,
                random.nextFloat() * Mth.TWO_PI,
                random.nextFloat() * Mth.TWO_PI,
                random.nextFloat()
        );
    }

    private static float randomRange(float min, float max, LegacyRandomSource random) {
        return min + random.nextFloat() * (max - min);
    }

    private record Ribbon(float offset, float alpha, float thicknessScale, float phaseA, float phaseB, float phaseC, float colorShift) {
    }
}
