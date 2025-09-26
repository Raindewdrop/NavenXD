package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LivingEntity.class})
public interface LivingEntityAccessor {
   @Accessor
   void setNoJumpDelay(int var1);

   @Accessor("DATA_EFFECT_COLOR_ID")
   static EntityDataAccessor<Integer> getEffectColorId() {
      return null;
   }
}
