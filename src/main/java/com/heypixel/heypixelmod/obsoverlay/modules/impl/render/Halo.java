package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

@ModuleInfo(
    name = "Halo",
    description = "Render a halo above player's head",
    category = Category.RENDER
)
public class Halo extends Module {
    private final ModeValue imageValue = ValueBuilder.create(this, "Image")
            .setModes("A", "A1", "A2", "A3", "A4", 
                     "B", "B1", "B2", "B3", "B4",
                     "C", "C1", "C2", "C3", "C4",
                     "D", "D1", "D2", "D3", "D4",
                     "E", "E1", "E2", "E3", "E4",
                     "F", "F1", "F2", "F3", "F4",
                     "G", "G1", "G2", "G3", "G4",
                     "H", "H1", "H2", "H3", "H4",
                     "I", "I1", "I2", "I3", "I4",
                     "J", "J1", "J2", "J3", "J4",
                     "K", "K1", "K2", "K3",
                     "L", "L1", "L2", "L3",
                     "M", "M1", "M2", "M3",
                     "N", "N1", "N2", "N3",
                     "O", "O1", "O2", "O3",
                     "P", "P1", "P2", "P3",
                     "Q", "Q1", "Q2", "Q3",
                     "R", "R1", "R2", "R3",
                     "S", "S1", "S2", "S3",
                     "T", "T1", "T2", "T3",
                     "U", "U1", "U2", "U3",
                     "V", "V1", "V2", "V3",
                     "W", "W1", "W2", "W3",
                     "X", "X1", "X2", "X3",
                     "Y", "Y1", "Y2", "Y3",
                     "Z", "Z1", "Z2", "Z3")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final FloatValue sizeValue = ValueBuilder.create(this, "Size")
            .setDefaultFloatValue(0.5F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(2.0F)
            .build()
            .getFloatValue();

    private final FloatValue heightValue = ValueBuilder.create(this, "Height")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue rotationValue = ValueBuilder.create(this, "Rotation")
            .setDefaultFloatValue(0.0F)
            .setFloatStep(5.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();

    public Halo() {
        this.setEnabled(false);
    }

    @EventTarget
    public void onRender(EventRender event) {
        if (Minecraft.getInstance().player == null) return;
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = event.getPMatrixStack();
        float partialTicks = event.getRenderPartialTicks();
        double playerX = player.xOld + (player.getX() - player.xOld) * partialTicks;
        double playerY = player.yOld + (player.getY() - player.yOld) * partialTicks + player.getEyeHeight() + heightValue.getCurrentValue();
        double playerZ = player.zOld + (player.getZ() - player.zOld) * partialTicks;
        Vec3 cameraPos = RenderUtils.getCameraPos();
        playerX -= cameraPos.x;
        playerY -= cameraPos.y;
        playerZ -= cameraPos.z;
        renderHalo(poseStack, playerX, playerY, playerZ);
    }

    private void renderHalo(PoseStack poseStack, double x, double y, double z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationValue.getCurrentValue()));
        float size = sizeValue.getCurrentValue();
        poseStack.scale(size, size, size);
        String imageName = imageValue.getCurrentMode();
        ResourceLocation texture = new ResourceLocation("ba", imageName + ".png");
        RenderSystem.setShaderTexture(0, texture);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glDisable(2884);
        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, -0.5F, -0.5F, 0.0F).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix, 0.5F, -0.5F, 0.0F).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix, 0.5F, 0.5F, 0.0F).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, -0.5F, 0.5F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glEnable(2884);
        poseStack.popPose();
    }

    @Override
    public String getSuffix() {
        return imageValue.getCurrentMode();
    }
}