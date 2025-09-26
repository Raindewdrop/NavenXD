package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CrossbowItem.class})
public interface CrossbowItemAccessor {
   @Invoker("getShootingPower")
   static float getShootingPower(ItemStack itemStack) {
      return 0.0F;
   }
}
