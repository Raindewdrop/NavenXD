package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRotationAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void renderHead(
      T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, CallbackInfo ci
   ) {
      EventRotationAnimation.currentEntity = pEntity;
   }

   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F",
         ordinal = 1
      )
   )
   private float rotAnimationYaw(float pDelta, float pStart, float pEnd) {
      EventRotationAnimation event = new EventRotationAnimation(pEnd, pStart, 0.0F, 0.0F);
      if (EventRotationAnimation.currentEntity == Minecraft.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return Mth.rotLerp(pDelta, event.getLastYaw(), event.getYaw());
   }

   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
         ordinal = 0
      )
   )
   private float rotAnimationPitch(float pDelta, float pStart, float pEnd) {
      EventRotationAnimation event = new EventRotationAnimation(0.0F, 0.0F, pEnd, pStart);
      if (EventRotationAnimation.currentEntity == Minecraft.getInstance().player) {
         Naven.getInstance().getEventManager().call(event);
      }

      return Mth.lerp(pDelta, event.getLastPitch(), event.getPitch());
   }
}
