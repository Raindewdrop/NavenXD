package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.utils.ICapabilityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
   targets = {"com.mojang.blaze3d.platform.GlStateManager$BooleanState"}
)
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
   @Shadow
   private boolean enabled;

   @Shadow
   public abstract void setEnabled(boolean var1);

   @Override
   public boolean get() {
      return this.enabled;
   }

   @Override
   public void set(boolean state) {
      this.setEnabled(state);
   }
}
