package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

@CommandInfo(
   name = "binds",
   description = "Display all key bindings",
   aliases = {"bindlist"}
)
public class CommandBinds extends Command {
   @Override
   public void onCommand(String[] args) {
      ChatUtils.addChatMessage("§7§m----------------------------------");
      ChatUtils.addChatMessage("§b§lKey Binds:");
      
      for (Module module : Naven.getInstance().getModuleManager().getModules()) {
         int keyCode = module.getKey();
         Key key = InputConstants.getKey(keyCode, 0);
         String keyName = key.getDisplayName().getString().toUpperCase();
         if (key != InputConstants.UNKNOWN && !keyName.contains("KEY.KEYBOARD.0") && !keyName.contains("SCANCODE.0")) {
            ChatUtils.addChatMessage("§7" + module.getName() + " §8-> §b" + keyName);
         }
      }
      
      ChatUtils.addChatMessage("§7§m----------------------------------");
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}