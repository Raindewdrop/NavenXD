package com.heypixel.heypixelmod.mixin.O;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientHandshakePacketListenerImpl.class})
public class MixinClientHandshakePacketListenerImpl {
   @Shadow
   @Final
   private Connection connection;
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"handleGameProfile"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/Connection;setProtocol(Lnet/minecraft/network/ConnectionProtocol;)V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   public void onSuccess(ClientboundGameProfilePacket packet, CallbackInfo ci) {
      GameProfile gameProfile = packet.getGameProfile();
   }
}
