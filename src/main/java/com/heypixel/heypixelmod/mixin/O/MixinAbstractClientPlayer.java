package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Cape;
import com.heypixel.heypixelmod.obsoverlay.skin.SkinManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class MixinAbstractClientPlayer {
   @Inject(
      method = {"getCloakTextureLocation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getCape(CallbackInfoReturnable<ResourceLocation> callbackInfoReturnable) {
      Cape cape = (Cape) Naven.getInstance().getModuleManager().getModule(Cape.class);
      if (cape != null && cape.isEnabled() && ((AbstractClientPlayer)(Object)this).getUUID().equals(Naven.getInstance().getMinecraft().player.getUUID())) {
         callbackInfoReturnable.setReturnValue(cape.getCapeLocation());
      }
   }
   
   @Inject(
      method = {"getSkinTextureLocation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getSkin(CallbackInfoReturnable<ResourceLocation> callbackInfoReturnable) {
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      SkinManager skinManager = SkinManager.getInstance();
      
      if (skinManager.shouldUseDefaultSkin(player)) {
         callbackInfoReturnable.cancel();
         return;
      }
      
      ResourceLocation customSkin = skinManager.getSkinLocation(player);
      if (customSkin != null) {
         callbackInfoReturnable.setReturnValue(customSkin);
      }
   }
}
