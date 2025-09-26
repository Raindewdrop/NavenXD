package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandIRC;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;

@ModuleInfo(
   name = "IRCManager",
   description = "IRC在线人数显示和公告管理",
   category = Category.MISC
)
public class IRCManager extends Module {
   private static boolean enabled = false;
   
   public IRCManager() {
      enabled = true;
   }
   
   @EventTarget
   public void onRespawn(EventRespawn e) {
      // 玩家进入世界时自动输出IRC在线人数
      if (enabled) {
         requestIRCUserCount();
      }
   }
   

   
   private void requestIRCUserCount() {
      // 发送获取在线人数的请求
      CommandIRC.sendCommand("USERCOUNT");
   }
   

   
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
   
   public boolean isEnabled() {
      return enabled;
   }
}