package it.hurts.shatterbyte.stellara.client.sky.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyColor;
import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class SkyGeometry {
    public static final float SKY_RADIUS = 100F;

    private SkyGeometry() {
    }

    public static SkyPoint point(float yaw, float pitch) {
        var horizontal = Mth.cos(pitch);

        return new SkyPoint(Mth.sin(yaw) * horizontal * SKY_RADIUS, Mth.sin(pitch) * SKY_RADIUS, Mth.cos(yaw) * horizontal * SKY_RADIUS);
    }

    public static void addBillboard(BufferBuilder buffer, Matrix4f matrix, SkyPoint center, float size, float angle, SkyColor color, float alpha) {
        addBillboard(buffer, matrix, center, size, angle, color, alpha, false);
    }

    public static void addTexturedBillboard(BufferBuilder buffer, Matrix4f matrix, SkyPoint center, float size, float angle, SkyColor color, float alpha) {
        addBillboard(buffer, matrix, center, size, angle, color, alpha, true);
    }

    private static void addBillboard(BufferBuilder buffer, Matrix4f matrix, SkyPoint center, float size, float angle, SkyColor color, float alpha, boolean textured) {
        var unitX = center.x() / SKY_RADIUS;
        var unitY = center.y() / SKY_RADIUS;
        var unitZ = center.z() / SKY_RADIUS;
        var yaw = (float) Math.atan2(unitX, unitZ);
        var sinYaw = Mth.sin(yaw);
        var cosYaw = Mth.cos(yaw);
        var pitch = (float) Math.atan2(Mth.sqrt(unitX * unitX + unitZ * unitZ), unitY);
        var sinPitch = Mth.sin(pitch);
        var cosPitch = Mth.cos(pitch);
        var sinAngle = Mth.sin(angle);
        var cosAngle = Mth.cos(angle);

        for (var index = 0; index < 4; index++) {
            var x = ((index & 2) - 1F) * size;
            var y = (((index + 1) & 2) - 1F) * size;
            var rotatedX = x * cosAngle - y * sinAngle;
            var rotatedY = y * cosAngle + x * sinAngle;
            var dy = rotatedX * sinPitch;
            var ae = -rotatedX * cosPitch;
            var dx = ae * sinYaw - rotatedY * cosYaw;
            var dz = rotatedY * sinYaw + ae * cosYaw;

            var vertex = buffer.addVertex(matrix, center.x() + dx, center.y() + dy, center.z() + dz);

            if (textured)
                vertex.setUv((index >> 1) & 1, ((index + 1) >> 1) & 1);

            vertex.setColor(color.red(), color.green(), color.blue(), alpha);
        }
    }

    public static void addLine(BufferBuilder buffer, Matrix4f matrix, SkyPoint from, SkyPoint to, float width, SkyColor color, float alpha) {
        var dx = to.x() - from.x();
        var dy = to.y() - from.y();
        var dz = to.z() - from.z();
        var midX = from.x() + to.x();
        var midY = from.y() + to.y();
        var midZ = from.z() + to.z();
        var sideX = dy * midZ - dz * midY;
        var sideY = dz * midX - dx * midZ;
        var sideZ = dx * midY - dy * midX;
        var sideLength = Mth.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);

        if (sideLength < 0.001F)
            return;

        sideX = sideX / sideLength * width;
        sideY = sideY / sideLength * width;
        sideZ = sideZ / sideLength * width;

        buffer.addVertex(matrix, from.x() + sideX, from.y() + sideY, from.z() + sideZ).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, to.x() + sideX, to.y() + sideY, to.z() + sideZ).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, to.x() - sideX, to.y() - sideY, to.z() - sideZ).setColor(color.red(), color.green(), color.blue(), alpha);
        buffer.addVertex(matrix, from.x() - sideX, from.y() - sideY, from.z() - sideZ).setColor(color.red(), color.green(), color.blue(), alpha);
    }
}
