package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventDestroyBlock;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPositionItem;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public class MixinMultiPlayerGameMode {
   @Redirect(
      method = {"useItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
         ordinal = 0
      )
   )
   public void onSendPacket(ClientPacketListener instance, Packet<?> pPacket) {
      EventPositionItem event = new EventPositionItem(pPacket);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.send(event.getPacket());
      }
   }

   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")}
   )
   public void onStartDestroyBlock(BlockPos pLoc, Direction pFace, CallbackInfoReturnable<Boolean> cir) {
      Naven.getInstance().getEventManager().call(new EventDestroyBlock(pLoc, pFace));
   }
}
