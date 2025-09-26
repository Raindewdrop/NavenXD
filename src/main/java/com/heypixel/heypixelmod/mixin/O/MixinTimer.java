package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import net.minecraft.client.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Timer.class})
public class MixinTimer {
   @Shadow
   public float partialTick;
   @Shadow
   private long lastMs;
   @Final
   @Shadow
   private float msPerTick;
   @Shadow
   public float tickDelta;

   @Inject(
      method = {"advanceTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      if (Naven.TICK_TIMER != 1.0F) {
         this.tickDelta = (float)(timeMillis - this.lastMs) / this.msPerTick * Naven.TICK_TIMER;
         this.lastMs = timeMillis;
         this.partialTick = this.partialTick + this.tickDelta;
         int i = (int)this.partialTick;
         this.partialTick -= (float)i;
         cir.setReturnValue(i);
      }
   }
}
