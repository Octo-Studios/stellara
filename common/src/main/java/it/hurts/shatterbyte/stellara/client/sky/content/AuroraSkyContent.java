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

public class AuroraSkyContent implements SkyboxContent {
    private final ResourceLocation id;
    private final int priority;
    private final List<SkyColor> colors;
    private final int bands;
    private final int segments;
    private final int gradientSteps;
    private final float waveAmplitude;
    private final float waveFrequency;
    private final float speed;
    private final float opacity;
    private final float centerYaw;
    private final float width;
    private final float minPitch;
    private final float maxPitch;
    private final float rayStrength;
    private final float edgeSoftness;
    private final float depthAmplitude;
    private final float depthOffset;
    private final float sphereRadius;

    public AuroraSkyContent(ResourceLocation id, int priority, List<SkyColor> colors, int bands, int segments, int gradientSteps, float waveAmplitude, float waveFrequency, float speed, float opacity, float centerYaw, float width, float minPitch, float maxPitch, float rayStrength, float edgeSoftness, float depthAmplitude, float depthOffset, float sphereRadius) {
        this.id = id;
        this.priority = priority;
        this.colors = List.copyOf(colors);
        this.bands = bands;
        this.segments = segments;
        this.gradientSteps = Math.max(1, gradientSteps);
        this.waveAmplitude = waveAmplitude;
        this.waveFrequency = waveFrequency;
        this.speed = speed;
        this.opacity = opacity;
        this.centerYaw = centerYaw;
        this.width = width;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.rayStrength = rayStrength;
        this.edgeSoftness = edgeSoftness;
        this.depthAmplitude = depthAmplitude;
        this.depthOffset = depthOffset;
        this.sphereRadius = Math.max(1F, sphereRadius);
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

        context.poseStack().pushPose();
        context.poseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-context.skyAngle()));

        var matrix = context.poseStack().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        var time = context.renderTime() * speed;

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        renderCurtains(buffer, matrix, time, alpha);

        Tesselator.getInstance().end();

        context.poseStack().popPose();

        RenderSystem.enableCull();
    }

    private void renderCurtains(BufferBuilder buffer, Matrix4f matrix, float time, float alpha) {
        for (var band = 0; band < bands; band++) {
            var bandProgress = bands <= 1 ? 0.5F : band / (float) (bands - 1);
            var bandWidth = width;
            var bandDepthOffset = (bandProgress - 0.5F) * width * 0.035F;
            var bandVerticalOffset = (bandProgress - 0.5F) * 0.08F;
            var bandMinPitch = minPitch + bandVerticalOffset;
            var bandMaxPitch = maxPitch + bandVerticalOffset;
            var bandAlpha = alpha * opacity * Mth.lerp(0.5F + 0.5F * Mth.sin(time * 0.7F + band * 1.37F), 0.58F, 1F);
            var xStart = -bandWidth * 0.5F;
            var xEnd = bandWidth * 0.5F;

            for (var segment = 0; segment < segments; segment++) {
                var left = segment / (float) segments;
                var right = (segment + 1F) / segments;
                var xLeft = Mth.lerp(left, xStart, xEnd);
                var xRight = Mth.lerp(right, xStart, xEnd);
                var leftRay = ray(left, band, time);
                var rightRay = ray(right, band, time);
                var leftDensity = densityWave(left, band, time);
                var rightDensity = densityWave(right, band, time);
                var leftHeight = heightWave(left, band, time);
                var rightHeight = heightWave(right, band, time);
                var leftBaseline = baselineWave(left, band, time);
                var rightBaseline = baselineWave(right, band, time);
                var leftWave = depthWave(left, band, time) * depthAmplitude;
                var rightWave = depthWave(right, band, time) * depthAmplitude;
                var leftDepth = leftWave * 0.55F + bandDepthOffset;
                var rightDepth = rightWave * 0.55F + bandDepthOffset;
                var leftHorizontal = xLeft + leftWave * 0.22F + leftBaseline * width * 0.34F + horizontalWave(left, band, time) * width * 0.42F;
                var rightHorizontal = xRight + rightWave * 0.22F + rightBaseline * width * 0.34F + horizontalWave(right, band, time) * width * 0.42F;
                var leftBottom = clampPitch(bandMinPitch + wave(left, band, time + 2.1F) * 1.25F + leftBaseline * 0.12F - leftDensity * 0.025F);
                var rightBottom = clampPitch(bandMinPitch + wave(right, band, time + 2.1F) * 1.25F + rightBaseline * 0.12F - rightDensity * 0.025F);
                var leftTop = clampPitch(bandMaxPitch + wave(left, band, time) * 1.25F + leftRay * (0.08F + leftHeight * 0.22F) + leftHeight * 0.16F);
                var rightTop = clampPitch(bandMaxPitch + wave(right, band, time) * 1.25F + rightRay * (0.08F + rightHeight * 0.22F) + rightHeight * 0.16F);
                var horizontalFade = horizontalFade(left) * horizontalFade(right);

                for (var step = 0; step < gradientSteps; step++) {
                    var bottom = step / (float) gradientSteps;
                    var top = (step + 1F) / gradientSteps;
                    var lowerAlpha = bandAlpha * horizontalFade * gradientAlpha(bottom) * Mth.lerp(leftRay, 1F - rayStrength, 1F) * leftDensity;
                    var upperAlpha = bandAlpha * horizontalFade * gradientAlpha(top) * Mth.lerp(rightRay, 1F - rayStrength, 1F) * rightDensity;

                    addCurtain(buffer, matrix,
                            curtainPoint(leftHorizontal, Mth.lerp(bottom, leftBottom, leftTop), leftDepth, bottom),
                            curtainPoint(leftHorizontal, Mth.lerp(top, leftBottom, leftTop), leftDepth, top),
                            curtainPoint(rightHorizontal, Mth.lerp(top, rightBottom, rightTop), rightDepth, top),
                            curtainPoint(rightHorizontal, Mth.lerp(bottom, rightBottom, rightTop), rightDepth, bottom),
                            color(bottom),
                            color(top),
                            lowerAlpha,
                            upperAlpha);
                }
            }
        }
    }

    private SkyPoint curtainPoint(float horizontal, float pitch, float depth, float verticalProgress) {
        var layerRadius = SkyGeometry.SKY_RADIUS * (1F + depthOffset);
        var radius = layerRadius * Math.max(0.05F, 1F + depth * 0.18F);
        var yaw = centerYaw + horizontal / sphereRadius;
        var horizontalRadius = Mth.cos(clampPitch(pitch)) * radius;

        return new SkyPoint(Mth.sin(yaw) * horizontalRadius, Mth.sin(clampPitch(pitch)) * radius, Mth.cos(yaw) * horizontalRadius);
    }

    private float wave(float progress, int band, float time) {
        return Mth.sin(progress * Mth.TWO_PI * waveFrequency + time + band * 1.9F) * waveAmplitude + Mth.sin(progress * Mth.TWO_PI * (waveFrequency * 0.47F) - time * 0.63F + band) * waveAmplitude * 0.45F;
    }

    private float depthWave(float progress, int band, float time) {
        var first = Mth.sin(progress * Mth.TWO_PI * (waveFrequency * 0.22F) + time * 0.42F + band * 0.7F);
        var second = Mth.sin(progress * Mth.TWO_PI * (waveFrequency * 0.11F) - time * 0.25F + band * 1.3F);

        return (first * 0.72F + second * 0.28F) * waveAmplitude;
    }

    private float horizontalWave(float progress, int band, float time) {
        var first = Mth.sin(progress * Mth.PI * 4F + time * 0.9F + band * 2.1F);
        var second = Mth.sin(progress * Mth.PI * 2F - time * 0.58F + band * 1.3F);

        return (first * 0.65F + second * 0.35F) * waveAmplitude;
    }

    private float baselineWave(float progress, int band, float time) {
        var first = Mth.sin(progress * Mth.TWO_PI * 1.15F + time * 0.32F + band * 1.9F);
        var second = Mth.sin(progress * Mth.TWO_PI * 2.1F - time * 0.21F + band * 0.6F);

        return (first * 0.68F + second * 0.32F) * waveAmplitude;
    }

    private float heightWave(float progress, int band, float time) {
        var first = Mth.sin(progress * Mth.TWO_PI * 4.3F - time * 0.46F + band * 2.4F);
        var second = Mth.sin(progress * Mth.TWO_PI * 9.0F + time * 0.18F + band * 1.1F);

        return Mth.clamp((first * 0.62F + second * 0.38F + 1F) * 0.5F, 0F, 1F);
    }

    private float densityWave(float progress, int band, float time) {
        var first = Mth.sin(progress * Mth.TWO_PI * 3.2F + time * 0.27F + band * 1.7F);
        var second = Mth.sin(progress * Mth.TWO_PI * 6.8F - time * 0.19F + band * 0.8F);
        var density = (first * 0.58F + second * 0.42F + 1F) * 0.5F;

        return Mth.lerp(Mth.clamp(density, 0F, 1F), 0.45F, 1F);
    }

    private float horizontalFade(float progress) {
        var left = Mth.clamp(progress / Math.max(0.001F, edgeSoftness), 0F, 1F);
        var right = Mth.clamp((1F - progress) / Math.max(0.001F, edgeSoftness), 0F, 1F);

        return Mth.sin(Math.min(left, right) * Mth.HALF_PI);
    }

    private float ray(float progress, int band, float time) {
        var fast = Mth.sin(progress * Mth.TWO_PI * 17F + band * 2.7F + time * 0.55F);
        var slow = Mth.sin(progress * Mth.TWO_PI * 7F - band * 1.4F - time * 0.31F);

        return Mth.clamp((fast * 0.5F + slow * 0.5F + 1F) * 0.5F, 0F, 1F);
    }

    private static float gradientAlpha(float progress) {
        return Mth.clamp(Mth.sin(progress * Mth.PI) * 1.18F, 0.08F, 1F);
    }

    private static float clampPitch(float pitch) {
        return Mth.clamp(pitch, -1.45F, 1.45F);
    }

    private SkyColor color(float progress) {
        if (colors.size() == 1)
            return colors.get(0);

        var scaled = Mth.clamp(progress, 0F, 1F) * (colors.size() - 1);
        var index = Mth.clamp((int) Math.floor(scaled), 0, colors.size() - 2);
        var blend = scaled - index;
        var from = colors.get(index);
        var to = colors.get(index + 1);

        return new SkyColor(Mth.lerp(blend, from.red(), to.red()), Mth.lerp(blend, from.green(), to.green()), Mth.lerp(blend, from.blue(), to.blue()));
    }

    private void addCurtain(BufferBuilder buffer, Matrix4f matrix, SkyPoint lowerLeft, SkyPoint upperLeft, SkyPoint upperRight, SkyPoint lowerRight, SkyColor lowerColor, SkyColor upperColor, float lowerAlpha, float upperAlpha) {
        var center = normalize(
                lowerLeft.x() + upperLeft.x() + upperRight.x() + lowerRight.x(),
                lowerLeft.y() + upperLeft.y() + upperRight.y() + lowerRight.y(),
                lowerLeft.z() + upperLeft.z() + upperRight.z() + lowerRight.z()
        );
        var centerColor = new SkyColor(
                (lowerColor.red() + upperColor.red()) * 0.5F,
                (lowerColor.green() + upperColor.green()) * 0.5F,
                (lowerColor.blue() + upperColor.blue()) * 0.5F
        );
        var centerAlpha = (lowerAlpha + upperAlpha) * 0.5F;

        addTriangle(buffer, matrix, lowerLeft, lowerColor, lowerAlpha, upperLeft, upperColor, upperAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, upperLeft, upperColor, upperAlpha, upperRight, upperColor, upperAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, upperRight, upperColor, upperAlpha, lowerRight, lowerColor, lowerAlpha, center, centerColor, centerAlpha);
        addTriangle(buffer, matrix, lowerRight, lowerColor, lowerAlpha, lowerLeft, lowerColor, lowerAlpha, center, centerColor, centerAlpha);
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
}
