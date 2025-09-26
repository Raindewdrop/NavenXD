package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;

public class ServerUtils {
   private static int grimTransactionCount = 0;
   public static final Map<String, AtomicInteger> HEALTHS = new HashMap<>();

   @EventTarget(0)
   public void onAllPackets(EventGlobalPacket e) {
      if (e.getType() == EventType.RECEIVE) {
         if (e.getPacket() instanceof ClientboundPingPacket) {
            grimTransactionCount++;
         }

         if (e.getPacket() instanceof ClientboundSetScorePacket packet
            && Minecraft.getInstance().level != null
            && ("belowHealth".equals(packet.getObjectiveName()) || "health".equals(packet.getObjectiveName()))
            && !packet.getOwner().equals(Minecraft.getInstance().player.getGameProfile().getName())) {
            if (!HEALTHS.containsKey(packet.getOwner())) {
               AtomicInteger atomic = new AtomicInteger();
               HEALTHS.put(packet.getOwner(), atomic);
            }

            HEALTHS.get(packet.getOwner()).set(packet.getScore());
         }

         if (e.getPacket() instanceof ClientboundSetHealthPacket packet && packet.getHealth() > 20.0F) {
            e.setCancelled(true);
         }
      }
   }

   @EventTarget
   public void onUpdate(EventRender2D event) {
      for (AbstractClientPlayer player : Minecraft.getInstance().level.players()) {
         if (player != Minecraft.getInstance().player && HEALTHS.containsKey(player.getName().getString())) {
            player.setHealth((float)Math.max(1, HEALTHS.get(player.getName().getString()).get()));
         }
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      grimTransactionCount = 0;
   }

   public static int getGrimTransactionCount() {
      return grimTransactionCount;
   }
}
