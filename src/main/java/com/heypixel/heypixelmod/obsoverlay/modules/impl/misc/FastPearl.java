package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "FastPearl",
   description = "Automatically throw ender pearls when middle mouse button is pressed.",
   category = Category.MISC
)
public class FastPearl extends Module {
   private final Minecraft mc = Minecraft.getInstance();
   private boolean isThrowing = false;
   
   public FastPearl() {
        this.setKey(2);
        this.setEnabled(true);
    }
   
   @EventTarget
   public void onMouseClick(EventMouseClick event) {
      if (event.getKey() == 2 && event.isState() && !isThrowing) {
         this.throwPearl();
      }
   }
   
   private void throwPearl() {
      if (isThrowing) return;
      
      LocalPlayer player = this.mc.player;
      if (player == null || this.mc.level == null || this.mc.gameMode == null) {
         return;
      }
      
      isThrowing = true;
      
      // 首先检查副手是否有珍珠
      if (player.getOffhandItem().getItem() == Items.ENDER_PEARL) {
         // 使用副手珍珠
         PacketUtils.sendSequencedPacket((id) -> {
            return new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id);
         });
         player.swing(InteractionHand.OFF_HAND);
         
         Notification successNotification = new Notification(NotificationLevel.SUCCESS, "Done! (Offhand)", 3000L);
         Naven.getInstance().getNotificationManager().addNotification(successNotification);
         isThrowing = false;
         return;
      }
      
      // 保存当前槽位
      final int originalSlot = player.getInventory().selected;
      
      // 搜索快捷栏（0-8槽位）寻找珍珠
      final int foundPearlSlot = findPearlSlot(player);
      
      if (foundPearlSlot == -1) {
         // 快捷栏中没有珍珠时显示通知
         Notification noPearlNotification = new Notification(NotificationLevel.WARNING, "Couldn't find any pearl in hotbar or offhand!", 3000L);
         Naven.getInstance().getNotificationManager().addNotification(noPearlNotification);
         isThrowing = false;
         return;
      }
      
      // 如果已经在珍珠槽位，直接使用珍珠
      if (player.getInventory().selected == foundPearlSlot) {
         // 使用珍珠
         PacketUtils.sendSequencedPacket((id) -> {
            return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id);
         });
         player.swing(InteractionHand.MAIN_HAND);
         
         // 显示投掷成功通知
         Notification successNotification = new Notification(NotificationLevel.SUCCESS, "Done!", 3000L);
         Naven.getInstance().getNotificationManager().addNotification(successNotification);
         isThrowing = false;
         return;
      }
      
      // 切换到珍珠槽位
      player.getInventory().selected = foundPearlSlot;
      mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(foundPearlSlot));
      
      // 延迟一帧后使用珍珠（确保槽位切换生效）
      mc.execute(() -> {
         // 创建final副本以便在lambda中使用
         final LocalPlayer finalPlayer = mc.player;
         if (finalPlayer != null && finalPlayer.getInventory().selected == foundPearlSlot) {
            // 使用珍珠
            PacketUtils.sendSequencedPacket((id) -> {
               return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id);
            });
            finalPlayer.swing(InteractionHand.MAIN_HAND);
            
            // 延迟切换回原来的槽位
            mc.execute(() -> {
               if (finalPlayer != null) {
                  finalPlayer.getInventory().selected = originalSlot;
                  mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
                  
                  // 显示投掷成功通知
                  Notification successNotification = new Notification(NotificationLevel.SUCCESS, "Done!", 3000L);
                  Naven.getInstance().getNotificationManager().addNotification(successNotification);
               }
               isThrowing = false;
            });
         } else {
            isThrowing = false;
         }
      });
   }
   
   // 辅助方法：查找珍珠槽位
   private int findPearlSlot(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }
      return -1;
   }
}