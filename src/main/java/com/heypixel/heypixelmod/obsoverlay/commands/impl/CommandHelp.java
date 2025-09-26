package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import java.util.ArrayList;
import java.util.List;

@CommandInfo(
   name = "help",
   description = "List all available commands",
   aliases = {"h", "?"}
)
public class CommandHelp extends Command {
   @Override
   public void onCommand(String[] args) {
      ChatUtils.addChatMessage("§7§m----------------------------------");
      ChatUtils.addChatMessage("§b§lAvailable Commands:");
      
      List<Command> uniqueCommands = new ArrayList<>();
      for (Command command : Naven.getInstance().getCommandManager().aliasMap.values()) {
         if (!uniqueCommands.contains(command)) {
            uniqueCommands.add(command);
         }
      }
      
      for (Command command : uniqueCommands) {
         StringBuilder aliasesStr = new StringBuilder();
         if (command.getAliases().length > 0) {
            aliasesStr.append(" §8(§7");
            for (int i = 0; i < command.getAliases().length; i++) {
               if (i > 0) {
                  aliasesStr.append("§8, §7");
               }
               aliasesStr.append(command.getAliases()[i]);
            }
            aliasesStr.append("§8)");
         }
         
         ChatUtils.addChatMessage("§7" + command.getName() + aliasesStr.toString() + " §8- §f" + command.getDescription());
      }
      
      ChatUtils.addChatMessage("§7§m----------------------------------");
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}