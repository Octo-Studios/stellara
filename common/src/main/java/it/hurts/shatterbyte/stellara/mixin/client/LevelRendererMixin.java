package it.hurts.shatterbyte.stellara.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.hurts.shatterbyte.stellara.client.sky.StellaraSkyboxRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", ordinal = 1))
    private void stellara$suppressVanillaStars(VertexBuffer buffer, Matrix4f viewModelView, Matrix4f viewProjection, ShaderInstance shader, Operation<Void> original, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean foggy, Runnable skyFogSetup) {
        var poseStack = new PoseStack();
        poseStack.last().pose().set(modelViewMatrix);
        if (!StellaraSkyboxRenderer.shouldSuppressVanillaStars(poseStack, camera, partialTick))
            original.call(buffer, viewModelView, viewProjection, shader);
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void stellara$renderSkybox(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean foggy, Runnable skyFogSetup, CallbackInfo callback) {
        var poseStack = new PoseStack();
        poseStack.last().pose().set(modelViewMatrix);
        StellaraSkyboxRenderer.render(poseStack, camera, partialTick);
    }
}
