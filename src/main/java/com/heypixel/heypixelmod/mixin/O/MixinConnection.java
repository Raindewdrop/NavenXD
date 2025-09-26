package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Connection.class})
public abstract class MixinConnection extends SimpleChannelInboundHandler<Packet<?>> {
   @Shadow
   @Final
   private static Logger LOGGER;

   @Shadow
   private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
   }

   @Shadow
   protected abstract void sendPacket(Packet<?> var1, @Nullable PacketSendListener var2);

   // 服务器在线检查功能已移除

   // 服务器在线检查功能已移除

   @Redirect(
      method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
      )
   )
   private void onGenericsFtw(Packet<?> pPacket, PacketListener pListener) {
      EventGlobalPacket event = new EventGlobalPacket(EventType.RECEIVE, pPacket);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         genericsFtw(event.getPacket(), pListener);
      }
   }

   @Redirect(
      method = {"send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"
      )
   )
   private void onSend(Connection instance, Packet<?> pInPacket, PacketSendListener pFutureListeners) {
      if (NetworkUtils.passthroughsPackets.contains(pInPacket)) {
         NetworkUtils.passthroughsPackets.remove(pInPacket);
         this.sendPacket(pInPacket, pFutureListeners);
      } else {
         EventGlobalPacket event = new EventGlobalPacket(EventType.SEND, pInPacket);
         Naven.getInstance().getEventManager().call(event);
         if (!event.isCancelled()) {
            this.sendPacket(event.getPacket(), pFutureListeners);
         }
      }
   }
}
