package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.skin.SkinManager;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

@CommandInfo(
   name = "skin",
   description = "Set player skin (local visual)",
   aliases = {"skin"}
)
public class CommandSkin extends Command {
    
    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }
        
        String action = args[0].toLowerCase();
        Player targetPlayer = getTargetPlayer(args);
        
        if (targetPlayer == null) {
            ChatUtils.addChatMessage("The target player was not found.");
            return;
        }
        
        switch (action) {
            case "set":
                if (args.length < 2) {
                    ChatUtils.addChatMessage("Usage: .skin set <Player ID> [slim|classic]");
                    return;
                }
                String playerId = args[1];
                String armType = args.length > 2 ? args[2].toLowerCase() : "slim";
                setSkinFromPlayerId(targetPlayer, playerId, armType);
                break;
                
            case "default":
                setDefaultSkin(targetPlayer);
                break;
                
            case "clear":
                clearSkin(targetPlayer);
                break;
                
            case "help":
                showHelp();
                break;
                
            default:
                ChatUtils.addChatMessage("Unknown command: " + action);
                showHelp();
                break;
        }
    }
    
    private Player getTargetPlayer(String[] args) {

        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            return Minecraft.getInstance().player;
        }
        
        if (args.length >= 2) {
            String playerName = args[1];
            if (playerName.equalsIgnoreCase("me") || playerName.equalsIgnoreCase("self")) {
                return Minecraft.getInstance().player;
            }
            
            for (Player player : Minecraft.getInstance().level.players()) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    return player;
                }
            }
        }
        return Minecraft.getInstance().player;
    }
    
    private void setSkinFromPlayerId(Player player, String playerId, String armType) {
        try {

            String skinUrl = getPlayerSkinUrlFromMojang(playerId);
            if (skinUrl == null) {
                ChatUtils.addChatMessage("Failed to get skin URL for player: " + playerId);
                return;
            }
            

            SkinManager.getInstance().setSkinFromUrl(player, skinUrl, armType);
            String armTypeMessage = armType != null ? " with " + armType + " arms" : "";
            ChatUtils.addChatMessage("The skin of " + player.getName().getString() + " has been set to the skin of player " + playerId + armTypeMessage);
        } catch (Exception e) {
            ChatUtils.addChatMessage("Failed to set skin: " + e.getMessage());
        }
    }
    
    private String getPlayerSkinUrlFromMojang(String playerName) {
        try {

            String uuidApiUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            String uuidResponse = com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils.get(uuidApiUrl);
            
            if (uuidResponse == null || uuidResponse.isEmpty()) {
                ChatUtils.addChatMessage("No response from UUID API for player: " + playerName);
                return null;
            }
            
            ChatUtils.addChatMessage("UUID API response: " + uuidResponse);
            

            java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile("\"id\"\s*:\s*\"([^\"]+)\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher uuidMatcher = uuidPattern.matcher(uuidResponse);
            if (!uuidMatcher.find()) {
                ChatUtils.addChatMessage("Could not extract UUID from response: " + uuidResponse);
                return null;
            }
            
            String uuid = uuidMatcher.group(1);
            ChatUtils.addChatMessage("Found UUID: " + uuid);
            

            String profileApiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            String profileResponse = com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils.get(profileApiUrl);
            
            if (profileResponse == null || profileResponse.isEmpty()) {
                ChatUtils.addChatMessage("No response from profile API for UUID: " + uuid);
                return null;
            }
            
            ChatUtils.addChatMessage("Profile API response: " + profileResponse);
            

            java.util.regex.Pattern skinPattern = java.util.regex.Pattern.compile("\"name\"\s*:\s*\"textures\".*?\"value\"\s*:\s*\"([^\"]+)\"", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher skinMatcher = skinPattern.matcher(profileResponse);
            if (skinMatcher.find()) {
                String base64Texture = skinMatcher.group(1);
                ChatUtils.addChatMessage("Found base64 texture data");
                

                try {
                    String decodedTexture = new String(java.util.Base64.getDecoder().decode(base64Texture), java.nio.charset.StandardCharsets.UTF_8);
                    ChatUtils.addChatMessage("Decoded texture: " + decodedTexture);
                    

                    java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("\"url\"\s*:\s*\"([^\"]+)\"", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher urlMatcher = urlPattern.matcher(decodedTexture);
                    if (urlMatcher.find()) {
                        String skinUrl = urlMatcher.group(1);

                        skinUrl = skinUrl.replace("\\", "");
                        ChatUtils.addChatMessage("Found skin URL: " + skinUrl);
                        return skinUrl;
                    } else {
                        ChatUtils.addChatMessage("Could not find skin URL in decoded texture data");
                    }
                } catch (Exception e) {
                    ChatUtils.addChatMessage("Error decoding base64 texture: " + e.getMessage());
                }
            } else {
                ChatUtils.addChatMessage("Could not find texture data in profile response");
            }
            
            return null;
        } catch (Exception e) {
            ChatUtils.addChatMessage("Error getting skin URL: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private void setSkin(Player player, String skinPath) {
        try {
            if (skinPath.startsWith("http://") || skinPath.startsWith("https://")) {
                SkinManager.getInstance().setSkinFromUrl(player, skinPath);
            } else {
                ResourceLocation skinLocation = new ResourceLocation(skinPath);
                SkinManager.getInstance().setCustomSkin(player, skinLocation);
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("Failed to set skin: " + e.getMessage());
        }
    }
    
    private void setDefaultSkin(Player player) {
        SkinManager.getInstance().setDefaultSkin(player);
    }
    
    private void clearSkin(Player player) {
        SkinManager.getInstance().clearSkin(player);
    }
    
    private void showHelp() {
        ChatUtils.addChatMessage("§7§m----------------------------------");
        ChatUtils.addChatMessage("§b§lSkin Command Help:");
        ChatUtils.addChatMessage("§7.skin set <Player ID> [slim|classic] §8- §fSet the skin of the specified player with arm type");
        ChatUtils.addChatMessage("§7.skin default §8- §fUse the default skin");
        ChatUtils.addChatMessage("§7.skin clear §8- §fClear the skin settings");
        ChatUtils.addChatMessage("§7.skin help §8- §fDisplay this help message");
        ChatUtils.addChatMessage("§7§m----------------------------------");
        ChatUtils.addChatMessage("§eTip: Use the PlayerID to get the skin of the player");
    }
    
    @Override
    public String[] onTab(String[] args) {
        if (args.length == 1) {
            return new String[]{"set", "default", "clear", "help"};
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return new String[]{"slim", "classic"};
        }
        
        return new String[0];
    }
}