package it.hurts.shatterbyte.stellara.client.sky.content;

import it.hurts.shatterbyte.stellara.client.sky.math.SkyPoint;
import it.hurts.shatterbyte.stellara.client.sky.render.SkyGeometry;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

final class StarClusters {
    private static final float CLUSTER_CHANCE = 0.32F;

    private final float[] centerX;
    private final float[] centerY;
    private final float[] centerZ;
    private final float[] radius;

    private StarClusters(float[] centerX, float[] centerY, float[] centerZ, float[] radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    static StarClusters make(int count, RandomSource random) {
        var clusterCount = Math.max(10, count / 55);
        var centerX = new float[clusterCount];
        var centerY = new float[clusterCount];
        var centerZ = new float[clusterCount];
        var radius = new float[clusterCount];

        for (var i = 0; i < clusterCount; i++) {
            var direction = randomDirection(random);

            centerX[i] = direction[0];
            centerY[i] = direction[1];
            centerZ[i] = direction[2];
            radius[i] = 0.32F + random.nextFloat() * 0.40F;
        }

        return new StarClusters(centerX, centerY, centerZ, radius);
    }

    SkyPoint sample(RandomSource random) {
        float x;
        float y;
        float z;

        boolean clustered;

        if (centerX.length > 0 && random.nextFloat() < CLUSTER_CHANCE) {
            clustered = true;

            var index = random.nextInt(centerX.length);
            var spread = radius[index] * (float) Math.pow(random.nextFloat(), 1.0F);
            var offset = randomDirection(random);

            x = centerX[index] + offset[0] * spread;
            y = centerY[index] + offset[1] * spread;
            z = centerZ[index] + offset[2] * spread;
        } else {
            clustered = false;

            x = random.nextFloat() * 2F - 1F;
            y = random.nextFloat() * 2F - 1F;
            z = random.nextFloat() * 2F - 1F;
        }

        var length = x * x + y * y + z * z;

        if (length <= 0.001F)
            return null;

        if (!clustered && length > 1F)
            return null;

        var inverse = 1F / Mth.sqrt(length);

        return new SkyPoint(x * inverse * SkyGeometry.SKY_RADIUS, y * inverse * SkyGeometry.SKY_RADIUS, z * inverse * SkyGeometry.SKY_RADIUS);
    }

    private static float[] randomDirection(RandomSource random) {
        while (true) {
            var x = random.nextFloat() * 2F - 1F;
            var y = random.nextFloat() * 2F - 1F;
            var z = random.nextFloat() * 2F - 1F;
            var length = x * x + y * y + z * z;

            if (length > 0.001F && length <= 1F) {
                var inverse = 1F / Mth.sqrt(length);

                return new float[]{x * inverse, y * inverse, z * inverse};
            }
        }
    }
}
