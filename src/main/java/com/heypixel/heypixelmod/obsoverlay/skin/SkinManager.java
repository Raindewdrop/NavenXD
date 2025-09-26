package com.heypixel.heypixelmod.obsoverlay.skin;

import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class SkinManager {
    private static SkinManager instance;
    private final Map<UUID, ResourceLocation> customSkins = new HashMap<>();
    private final Map<UUID, Boolean> useDefaultSkin = new HashMap<>();
    private final Map<UUID, String> armTypes = new HashMap<>();
    
    public SkinManager() {
        instance = this;
    }
    
    public static SkinManager getInstance() {
        return instance;
    }
    
    public void setSkinFromUrl(Player player, String skinUrl) {
        setSkinFromUrl(player, skinUrl, null);
    }
    
    public void setSkinFromUrl(Player player, String skinUrl, String armType) {
        // 异步处理皮肤下载和设置，避免游戏卡顿
        CompletableFuture.runAsync(() -> {
            try {
                String actualSkinUrl = getRealSkinUrl(skinUrl);
                if (actualSkinUrl == null) {
                    Minecraft.getInstance().execute(() -> {
                        ChatUtils.addChatMessage("Unable to obtain the URL for the player's skin.");
                    });
                    return;
                }
                
                java.io.File cacheDir = new java.io.File(System.getProperty("java.io.tmpdir"), "minecraft_skins");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                java.io.File skinFile = new java.io.File(cacheDir, player.getUUID().toString() + ".png");
                
                HttpUtils.download(actualSkinUrl, skinFile);
                
                // 回到主线程进行纹理注册和UI更新
                Minecraft.getInstance().execute(() -> {
                    try {
                        ResourceLocation skinLocation = new ResourceLocation("custom_skin", player.getUUID().toString());
                        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                        
                        HttpTexture skinTexture = new HttpTexture(
                            skinFile,
                            actualSkinUrl,
                            Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(player.getGameProfile()), // ResourceLocation fallbackTextureLocation
                            true,
                            null
                        );
                        
                        textureManager.register(skinLocation, skinTexture);
                        
                        customSkins.put(player.getUUID(), skinLocation);
                        useDefaultSkin.put(player.getUUID(), false);
                        if (armType != null && (armType.equals("slim") || armType.equals("classic"))) {
                            armTypes.put(player.getUUID(), armType);
                        } else {
                            armTypes.remove(player.getUUID());
                        }
                        String armTypeMessage = armType != null ? " with " + armType + " arms" : "";
                        ChatUtils.addChatMessage("The skin of player " + player.getName().getString() + " has been set to: " + skinUrl + armTypeMessage);
                    } catch (Exception e) {
                        ChatUtils.addChatMessage("Failed to set the skin: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> {
                    ChatUtils.addChatMessage("Failed to set the skin: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }
    
    public ResourceLocation getPlayerSkin(Player player) {
        if (useDefaultSkin.containsKey(player.getUUID()) && useDefaultSkin.get(player.getUUID())) {
            return getDefaultSkin(player);
        }
        
        ResourceLocation customSkin = customSkins.get(player.getUUID());
        if (customSkin != null) {
            return customSkin;
        }
        
        return getDefaultSkin(player);
    }
    
    public ResourceLocation getDefaultSkin(Player player) {

        return Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(player.getGameProfile());
    }
    
    public void resetToDefaultSkin(Player player) {
        customSkins.remove(player.getUUID());
        useDefaultSkin.put(player.getUUID(), true);
        armTypes.remove(player.getUUID());
        ChatUtils.addChatMessage("The skin of player " + player.getName().getString() + " has been reset to default.");
    }
    
    public boolean hasCustomSkin(Player player) {
        return customSkins.containsKey(player.getUUID()) && !useDefaultSkin.getOrDefault(player.getUUID(), false);
    }
    
    public void setSkinForLocalPlayer(String skinUrl) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            setSkinFromUrl(player, skinUrl);
        }
    }
    
    public void resetLocalPlayerSkin() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            resetToDefaultSkin(player);
        }
    }
    

    public boolean shouldUseDefaultSkin(AbstractClientPlayer player) {
        return useDefaultSkin.containsKey(player.getUUID()) && useDefaultSkin.get(player.getUUID());
    }
    
    public ResourceLocation getSkinLocation(AbstractClientPlayer player) {
        return getPlayerSkin(player);
    }
    
    public void setCustomSkin(Player player, ResourceLocation skinLocation) {
        setCustomSkin(player, skinLocation, null);
    }
    
    public void setCustomSkin(Player player, ResourceLocation skinLocation, String armType) {
        customSkins.put(player.getUUID(), skinLocation);
        useDefaultSkin.put(player.getUUID(), false);
        if (armType != null && (armType.equals("slim") || armType.equals("classic"))) {
            armTypes.put(player.getUUID(), armType);
        } else {
            armTypes.remove(player.getUUID());
        }
        String armTypeMessage = armType != null ? " with " + armType + " arms" : "";
        ChatUtils.addChatMessage("The skin of player " + player.getName().getString() + " has been set to custom skin" + armTypeMessage);
    }
    
    public void setDefaultSkin(Player player) {
        resetToDefaultSkin(player);
    }
    
    public String getArmType(Player player) {
        return armTypes.get(player.getUUID());
    }
    
    public void clearSkin(Player player) {
        customSkins.remove(player.getUUID());
        useDefaultSkin.remove(player.getUUID());
        armTypes.remove(player.getUUID());
        ChatUtils.addChatMessage("The skin of player " + player.getName().getString() + " has been cleared.");
    }
    
    private String getRealSkinUrl(String skinUrl) {
        try {

            if (skinUrl.endsWith(".png") || skinUrl.contains("texture")) {
                return skinUrl;
            }
            

            if (!skinUrl.contains("http") && !skinUrl.contains(".")) {
                String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + skinUrl;
                String response = HttpUtils.get(apiUrl);
                
                if (response != null && !response.isEmpty()) {

                    Pattern uuidPattern = Pattern.compile("\"id\":\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = uuidPattern.matcher(response);
                    if (matcher.find()) {
                        String uuid = matcher.group(1);

                        String skinApiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
                        String skinResponse = HttpUtils.get(skinApiUrl);
                        
                        if (skinResponse != null && !skinResponse.isEmpty()) {

                            Pattern skinPattern = Pattern.compile("\"url\":\"([^\\\"]+)\"");
                            java.util.regex.Matcher skinMatcher = skinPattern.matcher(skinResponse);
                            if (skinMatcher.find()) {
                                String skinUrlFromApi = skinMatcher.group(1);

                                return skinUrlFromApi.replace("\\", "");
                            }
                        }
                    }
                }
            }
            
            return skinUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}