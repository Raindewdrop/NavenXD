package com.heypixel.heypixelmod.mixin.O;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Glow;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({Minecraft.class})
public class MixinMinecraft {
   @Unique
   private int skipTicks;
   @Unique
   private long naven_Modern$lastFrame;

   @Unique
   private boolean iconSet = false;
   @Unique
   private int iconSetAttempts = 0;
   @Unique
   private static final int MAX_ICON_SET_ATTEMPTS = 10;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
      Naven.modRegister();
      System.setProperty("java.awt.headless", "false");
      ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("NavenXD"));
      List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

      for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
         for (IModInfo modInfo : fileInfo.getMods()) {
            if (modInfo.getModId().contains("NavenXD")) {
               fileInfoToRemove.add(fileInfo);
            }
         }
      }
      ModList.get().getModFiles().removeAll(fileInfoToRemove);
      setWindowIcon();
   }
   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      remap = false
   )
   private void shutdown(CallbackInfo ci) {

      
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
   }
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickPre(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
   }
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }
      if (!iconSet && iconSetAttempts < MAX_ICON_SET_ATTEMPTS) {
         setWindowIcon();
         iconSetAttempts++;
         if (iconSetAttempts >= MAX_ICON_SET_ATTEMPTS) {
            iconSet = true;
         }
      }
      updateWindowTitle();
   }
   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(pEntity)) {
         cir.setReturnValue(true);
      }
   }
   @Inject(
      method = {"runTick"},
      at = {@At("HEAD")}
   )
   private void runTick(CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int)(currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
   }
   @ModifyArg(
      method = {"runTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"
      )
   )
   private float fixSkipTicks(float g) {
      if (this.skipTicks > 0) {
         g = 0.0F;
      }

      return g;
   }
   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
         ordinal = 0,
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void clickEvent(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         EventClick event = new EventClick();
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
   @Unique
   private void setWindowIcon() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getWindow() != null) {
            try {
               net.minecraft.server.packs.resources.ResourceManager resourceManager = mc.getResourceManager();
               net.minecraft.resources.ResourceLocation iconLocation = new net.minecraft.resources.ResourceLocation("navenxd", "icon/icon.png");
               java.util.Optional<net.minecraft.server.packs.resources.Resource> resourceOptional = resourceManager.getResource(iconLocation);
               if (resourceOptional.isPresent()) {
                  net.minecraft.server.packs.resources.Resource resource = resourceOptional.get();
                  java.io.InputStream iconStream = resource.open();
                  java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                  java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                     icon.getWidth(null), 
                     icon.getHeight(null), 
                     java.awt.image.BufferedImage.TYPE_INT_ARGB
                  );
                  java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                  g2d.drawImage(icon, 0, 0, null);
                  g2d.dispose();
                  
                  try {
                     java.lang.reflect.Method setIconMethod = mc.getWindow().getClass().getMethod(
                        "setIcon", 
                        java.awt.Image.class
                     );
                     setIconMethod.invoke(mc.getWindow(), bufferedImage);
                     iconSet = true;
                  } catch (Exception reflectEx) {
                     try {
                        org.lwjgl.glfw.GLFWImage glfwImage = org.lwjgl.glfw.GLFWImage.malloc();
                        org.lwjgl.glfw.GLFWImage.Buffer imageBuffer = org.lwjgl.glfw.GLFWImage.malloc(1);
                        int width = bufferedImage.getWidth();
                        int height = bufferedImage.getHeight();
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);
                        for (int y = 0; y < height; y++) {
                           for (int x = 0; x < width; x++) {
                              int pixel = bufferedImage.getRGB(x, y);
                              buffer.put((byte)((pixel >> 16) & 0xFF));
                              buffer.put((byte)((pixel >> 8) & 0xFF));
                              buffer.put((byte)(pixel & 0xFF));
                              buffer.put((byte)((pixel >> 24) & 0xFF));
                           }
                        }
                        buffer.flip();
                        glfwImage.set(width, height, buffer);
                        imageBuffer.put(0, glfwImage);
                        long window = mc.getWindow().getWindow();
                        if (window != 0) {
                           org.lwjgl.glfw.GLFW.glfwSetWindowIcon(window, imageBuffer);
                           System.out.println("[Naven] 窗口图标设置成功 (GLFW方法)");
                           iconSet = true;
                        } else {
                           System.out.println("[Naven] 窗口句柄为空，无法设置图标");
                        }
                        glfwImage.free();
                        imageBuffer.free();
                     } catch (Exception glfwEx) {
                        System.out.println("[Naven] GLFW设置图标失败: " + glfwEx.getMessage());
                        glfwEx.printStackTrace();
                     }
                  }   
                  iconStream.close();
               } else {
                  System.out.println("[Naven] 图标资源未找到: NavenXD:icon/icon.png");
               }
            } catch (Exception resourceEx) {
               System.out.println("[Naven] 资源管理器加载失败: " + resourceEx.getMessage());
               java.io.InputStream iconStream = getClass().getResourceAsStream("/assets/navenxd/icon/icon.png");
               if (iconStream != null) {
                  System.out.println("[Naven] 使用备用方法加载图标成功");
                  java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                  
                  java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                     icon.getWidth(null), 
                     icon.getHeight(null), 
                     java.awt.image.BufferedImage.TYPE_INT_ARGB
                  );
                  java.awt.Graphics2D g2d = bufferedImage.createGraphics();
                  g2d.drawImage(icon, 0, 0, null);
                  g2d.dispose();

                  try {
                     org.lwjgl.glfw.GLFWImage glfwImage = org.lwjgl.glfw.GLFWImage.malloc();
                     org.lwjgl.glfw.GLFWImage.Buffer imageBuffer = org.lwjgl.glfw.GLFWImage.malloc(1);
                     
                     int width = bufferedImage.getWidth();
                     int height = bufferedImage.getHeight();
                     
                     java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);
                     
                     for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                           int pixel = bufferedImage.getRGB(x, y);
                           buffer.put((byte)((pixel >> 16) & 0xFF)); // R
                           buffer.put((byte)((pixel >> 8) & 0xFF));  // G
                           buffer.put((byte)(pixel & 0xFF));         // B
                           buffer.put((byte)((pixel >> 24) & 0xFF)); // A
                        }
                     }
                     buffer.flip();
                     
                     glfwImage.set(width, height, buffer);
                     imageBuffer.put(0, glfwImage);
                     
                     long window = mc.getWindow().getWindow();
                     if (window != 0) {
                        org.lwjgl.glfw.GLFW.glfwSetWindowIcon(window, imageBuffer);
                        System.out.println("[Naven] 窗口图标设置成功 (备用GLFW方法)");
                        iconSet = true;
                     } else {
                        System.out.println("[Naven] 窗口句柄为空，无法设置图标");
                     }
                     
                     glfwImage.free();
                     imageBuffer.free();
                  } catch (Exception glfwEx) {
                     System.out.println("[Naven] 备用GLFW设置图标失败: " + glfwEx.getMessage());
                     glfwEx.printStackTrace();
                  }
                  
                  iconStream.close();
               } else {
                  System.out.println("[Naven] 备用方法也找不到图标文件: /assets/navenxd/icon/icon.png");
               }
            }
         } else {
            System.out.println("[Naven] 窗口对象为空");
         }
      } catch (Exception e) {
         System.out.println("[Naven] 设置窗口图标时发生错误: " + e.getMessage());
         e.printStackTrace();
      }
   }

   @Unique
   private long lastRandomTextUpdate = 0L;
   @Unique
   private String currentRandomText = "";
   @Unique
   private static final long RANDOM_TEXT_UPDATE_INTERVAL = 10 *1000L;

   @Unique
   private void updateWindowTitle() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getWindow() != null) {
         String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
         
         String[] randomTexts = {
            "盒盒盒",
            "我草这个更多奶粉怎么这么坏啊",
            "你正在使用奶粉兄弟客户端",
            "盒盒盒我是大王",
            "BUILD SUCCESSFUL in 1m 50s",
            "BUILD FAILED in 1m 50s",
            "镇守使驾到",
            "何人不服镇守使",
            "不服镇守使的都出来",
            "我看看谁不服镇守使啊",
            "谁不服镇守使的出来一下",
            "老傻子客户端",
            "妖猫今天开宝马了吗？",
            "不是哥们这都抄",
            "Get Good Get Naven!",
            "迷你世界卡卡大玉",
            "我去这怎么一半敖丙一半哪吒啊",
            "兄弟可惜了",
            "好枪兄弟",
            "So as I pray. Unlimited Blade Works.",
            "Unknown to Death. Nor known to Life",
            "I am the bone of my sword. Steel is my body and fire is my blood.",
            "请输入文本",
            "天地乖离开辟之星",
            "不做无法实现的梦",
            "开挂是违法的，我已经报警了，你就等着进去吧！",
            "妖猫：删除了不存在的宝马座驾"

         };
         
         long now = System.currentTimeMillis();
         if (now - lastRandomTextUpdate >= RANDOM_TEXT_UPDATE_INTERVAL) {
            currentRandomText = randomTexts[(int)(Math.random() * randomTexts.length)];
            lastRandomTextUpdate = now;
         }
         
         String title = "IntelliJ IDEA Ultimate Pro Max | " + currentTime + " | " + "[" + currentRandomText + "]";
         mc.getWindow().setTitle(title);
      }
   }


}
