package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderAfterWorld;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.FullBright;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.MotionBlur;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NoHurtCam;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )}
   )
   private void renderLevel(float pPartialTicks, long pFinishTimeNano, PoseStack pMatrixStack, CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventRender(pPartialTicks, pMatrixStack));
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void onRenderWorldTail(CallbackInfo info) {
      Naven.getInstance().getEventManager().call(new EventRenderAfterWorld());
   }

   @Inject(
      method = {"getNightVisionScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime, CallbackInfoReturnable<Float> cir) {
      FullBright module = (FullBright)Naven.getInstance().getModuleManager().getModule(FullBright.class);
      if (module.isEnabled()) {
         cir.setReturnValue(module.brightness.getCurrentValue());
         cir.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   public void render(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
      MotionBlur motionblur = MotionBlur.instance;
      if (motionblur.isEnabled() && this.minecraft.player != null && motionblur.shader != null) {
         motionblur.shader.process(tickDelta);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"
      )}
   )
   public void injectRender2DEvent(float p_109094_, long p_109095_, boolean p_109096_, CallbackInfo ci) {
      GuiGraphics e = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
      EventRender2D event = new EventRender2D(e.pose(), e);
      Naven.getInstance().getEventManager().call(event);
   }

   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bobHurt(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
      NoHurtCam module = (NoHurtCam)Naven.getInstance().getModuleManager().getModule(NoHurtCam.class);
      if (module.isEnabled()) {
         ci.cancel();
      }
   }
}
