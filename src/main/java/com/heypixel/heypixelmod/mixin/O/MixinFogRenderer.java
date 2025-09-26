package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.AntiBlindness;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({FogRenderer.class})
public class MixinFogRenderer {
   @Redirect(
      method = {"setupColor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z",
         ordinal = 0
      )
   )
   private static boolean onSetupColor(LivingEntity instance, MobEffect pEffect) {
      return pEffect == MobEffects.BLINDNESS && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()
         ? false
         : instance.hasEffect(pEffect);
   }

   @Redirect(
      method = {"setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"
      )
   )
   private static boolean onSetupFog(LivingEntity instance, MobEffect pEffect) {
      return pEffect == MobEffects.BLINDNESS && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()
         ? false
         : instance.hasEffect(pEffect);
   }
}
