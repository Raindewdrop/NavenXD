package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ModuleInfo(
    name = "ItemPhysics", 
    description = "Adds realistic physics to dropped items", 
    category = Category.RENDER
)

public class ItemPhysics extends Module {
    
    public FloatValue RotateSpeed = ValueBuilder.create(this, "Rotate Speed")
            .setDefaultFloatValue(1.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0f)
            .build()
            .getFloatValue();
    
    public final BooleanValue EnableWaterEffects = ValueBuilder.create(this, "Water Effects")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    
    private final Minecraft mc = Minecraft.getInstance();
    private static long lastTickTime;
    private static final double RANDOM_Y_OFFSET_SCALE = 0.05 / (Math.PI * 2);
    
    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
        lastTickTime = System.nanoTime();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }
    
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            lastTickTime = System.nanoTime();
        }
    }
    
    @EventTarget
    public void onRender(EventRender event) {
        // 这里可以添加物品物理效果的渲染逻辑
    }
    
    public static boolean renderItemPhysics(ItemEntity entity, float entityYaw, float partialTicks, PoseStack pose, 
                                          MultiBufferSource buffer, int packedLight, ItemRenderer itemRenderer, RandomSource rand) {
        if (entity.getAge() == 0)
            return false;
        
        pose.pushPose();
        ItemStack itemstack = entity.getItem();
        rand.setSeed(itemstack.isEmpty() ? 187 : Item.getId(itemstack.getItem()) + itemstack.getDamageValue());
        BakedModel bakedmodel = itemRenderer.getModel(itemstack, entity.level(), (LivingEntity) null, entity.getId());
        boolean is3dModel = bakedmodel.isGui3d();
        int modelCount = getModelCount(itemstack);
        
        // 计算基于时间的旋转角度
        long currentTime = System.nanoTime();
        float timeDelta = (currentTime - lastTickTime) / 1000000000.0f; // 转换为秒
        float rotationSpeed = 180.0f; // 每秒旋转180度
        float rotationAngle = (timeDelta * rotationSpeed) % 360.0f;
        
        if (Minecraft.getInstance().isPaused())
            rotationAngle = 0;
        
        pose.mulPose(Axis.XP.rotation((float) Math.PI / 2));
        pose.mulPose(Axis.ZP.rotation(entity.getYRot()));
        
        boolean applyEffects = entity.getAge() != 0 && (is3dModel || Minecraft.getInstance().options != null);
        
        // 处理旋转效果
        if (applyEffects) {
            // 应用旋转到PoseStack而不是修改entity属性
            if (is3dModel) {
                if (!entity.onGround()) {
                    Fluid fluid = getFluid(entity);
                    if (fluid != null) {
                        // 水中旋转减慢
                        rotationAngle /= (1 + getViscosity(fluid, entity.level()));
                    }
                    pose.mulPose(Axis.YP.rotationDegrees(rotationAngle));
                }
            } else {
                if (!entity.onGround()) {
                    Fluid fluid = getFluid(entity);
                    if (fluid != null) {
                        // 水中旋转减慢
                        rotationAngle /= (1 + getViscosity(fluid, entity.level()));
                    }
                    pose.mulPose(Axis.YP.rotationDegrees(rotationAngle));
                }
            }
            
            // 位置偏移
            if (is3dModel)
                pose.translate(0, -0.2, -0.08);
            else
                pose.translate(0, 0, -0.04 - entity.bobOffs * RANDOM_Y_OFFSET_SCALE);
            
            double height = 0.2;
            if (is3dModel)
                pose.translate(0, height, 0);
            pose.mulPose(Axis.YP.rotation(entity.getXRot()));
            if (is3dModel)
                pose.translate(0, -height, 0);
        }
        
        // 渲染物品
        if (!is3dModel) {
            float offsetX = -0.0F * (modelCount - 1) * 0.5F;
            float offsetY = -0.0F * (modelCount - 1) * 0.5F;
            float offsetZ = -0.09375F * (modelCount - 1) * 0.5F;
            pose.translate(offsetX, offsetY, offsetZ);
        }
        
        for (int i = 0; i < modelCount; ++i) {
            pose.pushPose();
            if (i > 0 && is3dModel) {
                float randomX = (rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                float randomY = (rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                float randomZ = (rand.nextFloat() * 2.0F - 1.0F) * 0.15F;
                pose.translate(randomX, randomY, randomZ);
            }
            
            itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
            pose.popPose();
            
            if (!is3dModel)
                pose.translate(0.0, 0.0, 0.09375F);
        }
        
        pose.popPose();
        return true;
    }
    
    private static Fluid getFluid(ItemEntity item) {
        if (item.level() == null)
            return null;
        
        double yPos = item.position().y;
        BlockPos pos = item.blockPosition();
        
        FluidState state = item.level().getFluidState(pos);
        Fluid fluid = state.getType();
        if (fluid == null || fluid.getTickDelay(item.level()) == 0) {
            return null;
        }
        
        double filled = state.getHeight(item.level(), pos);
        
        if (yPos - pos.getY() - 0.2 <= filled)
            return fluid;
        return null;
    }
    
    private static int getModelCount(ItemStack stack) {
        if (stack.getCount() > 48)
            return 5;
        if (stack.getCount() > 32)
            return 4;
        if (stack.getCount() > 16)
            return 3;
        if (stack.getCount() > 1)
            return 2;
        
        return 1;
    }
    
    private static float getViscosity(Fluid fluid, Level level) {
        if (fluid == null)
            return 0;
        return 1.0f; // 简化版本，使用固定粘度值
    }
}