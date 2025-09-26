package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ItemPhysics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer extends EntityRenderer<ItemEntity> {
    
    @Shadow
    @Final
    private ItemRenderer itemRenderer;
    
    @Shadow
    @Final
    private RandomSource random;
    
    protected MixinItemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Inject(
        method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRender(ItemEntity itemEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        Module module = Naven.getInstance().getModuleManager().getModule(ItemPhysics.class);
        if (module != null && module.isEnabled()) {
            // 调用物品物理渲染
            boolean rendered = ItemPhysics.renderItemPhysics(
                itemEntity, 
                f, 
                g, 
                poseStack, 
                multiBufferSource, 
                i, 
                this.itemRenderer, 
                this.random
            );
            
            if (rendered) {
                ci.cancel();
            }
        }
    }
}