package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSetTitle;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NoRender;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Scoreboard;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Gui.class},
   priority = 100
)
public class MixinGui {
   @Shadow
   @Nullable
   protected Component title;
   @Shadow
   protected int titleTime;
   @Shadow
   protected int titleFadeInTime;
   @Shadow
   protected int titleStayTime;
   @Shadow
   protected int titleFadeOutTime;
   @Shadow
   @Nullable
   protected Component subtitle;

   @Inject(
      method = {"displayScoreboardSidebar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookScoreboardHead(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      if (module.isEnabled() && module.gradientBackground.getCurrentValue()) {
         ci.cancel();
         renderCustomScoreboard(pPoseStack, pObjective);
         return;
      }
      if (module.isEnabled()) {
         pPoseStack.pose().pushPose();
         pPoseStack.pose().translate(0.0F, module.down.getCurrentValue(), 0.0F);
      }
   }

   @Inject(
      method = {"displayScoreboardSidebar"},
      at = {@At("RETURN")}
   )
   public void hookScoreboardReturn(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      if (module.isEnabled() && !module.gradientBackground.getCurrentValue()) {
         pPoseStack.pose().popPose();
      }
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
      )
   )
   public int hookRenderScore(GuiGraphics instance, Font p_283343_, String p_281896_, int p_283569_, int p_283418_, int p_281560_, boolean p_282130_) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      return module.isEnabled() && module.hideScore.getCurrentValue() ? 0 : instance.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_);
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
      )
   )
   public MutableComponent hookScoreboardName(Team pPlayerTeam, Component pPlayerName) {
      MutableComponent mutableComponent = PlayerTeam.formatNameForTeam(pPlayerTeam, pPlayerName);
      EventRenderScoreboard event = new EventRenderScoreboard(mutableComponent);
      Naven.getInstance().getEventManager().call(event);
      return (MutableComponent)event.getComponent();
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;"
      )
   )
   public Component hookScoreboardTitle(Objective instance) {
      Component component = instance.getDisplayName();
      EventRenderScoreboard event = new EventRenderScoreboard(component);
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
   }

   @Inject(
      method = {"setTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookTitle(Component pTitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.title = event.getTitle();
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
         ci.cancel();
      }
   }

   @Inject(
      method = {"setSubtitle"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookSubtitle(Component pSubtitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.subtitle = event.getTitle();
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderEffects"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookRenderEffects(GuiGraphics pPoseStack, CallbackInfo ci) {
      NoRender noRender = (NoRender)Naven.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender.isEnabled() && noRender.disableEffects.getCurrentValue()) {
         ci.cancel();
      }
   }

   /**
    * 渲染自定义计分板（包括渐变背景和所有内容）
    */
   private void renderCustomScoreboard(GuiGraphics guiGraphics, Objective objective) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      Font font = Minecraft.getInstance().font;
      float downValue = module.down.getCurrentValue();
      
      // 首先渲染渐变背景
      renderGradientBackground(guiGraphics, objective);
      
      // 获取屏幕尺寸和计分板位置
      int screenWidth = guiGraphics.guiWidth();
      int scoreboardX = screenWidth - 150 - 2;
      int scoreboardY = 10 + (int)downValue; // 应用下移设置

      // 渲染计分板标题
      Component title = hookScoreboardTitle(objective);
      int titleColor = 16777215; // 白色
      guiGraphics.drawString(font, title.getString(), scoreboardX + 150 / 2 - font.width(title.getString()) / 2, scoreboardY + 1, titleColor);
      
      // 渲染计分板条目
      int currentY = scoreboardY + 10;
      
      for (Score score : objective.getScoreboard().getPlayerScores(objective)) {
         String playerName = score.getOwner();
         int scoreValue = score.getScore();
         
         // 获取团队
         PlayerTeam team = objective.getScoreboard().getPlayersTeam(playerName);
         
         // 格式化玩家名称
         MutableComponent formattedName = hookScoreboardName(team, Component.literal(playerName));
         
         // 获取格式化后的文本颜色
         int color = getTextColor(formattedName);
         
         // 渲染分数
         String scoreText = String.valueOf(scoreValue);
         int scoreWidth = font.width(scoreText);
         
         // 渲染玩家名称（使用正确的颜色）
         guiGraphics.drawString(font, formattedName.getString(), scoreboardX + 1, currentY, color);
         
         // 渲染分数（右对齐）
         if (!module.hideScore.getCurrentValue()) {
            guiGraphics.drawString(font, scoreText, scoreboardX + 150 - scoreWidth - 1, currentY, 16777215);
         }
         
         currentY += 9;
      }
   }

   /**
    * 从格式化文本中提取颜色
    */
   private int getTextColor(MutableComponent component) {
      Style style = component.getStyle();
      if (style != null && style.getColor() != null) {
         return style.getColor().getValue();
      }
      return 16777215; // 默认白色
   }

   /**
    * 渲染自左到右渐显的计分板背景
    */
   private void renderGradientBackground(GuiGraphics guiGraphics, Objective objective) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      if (!module.isEnabled() || !module.gradientBackground.getCurrentValue()) {
         return;
      }
      
      int screenWidth = guiGraphics.guiWidth();
      float downValue = module.down.getCurrentValue();
      int scoreboardX = screenWidth - 150 - 2;
      int scoreboardY = 10 + (int)downValue;

      int entryCount = objective.getScoreboard().getPlayerScores(objective).size();
      int titleHeight = 10;
      int entryHeight = entryCount * 9;
      int padding = 3;
      int scoreboardHeight = Math.max(titleHeight + entryHeight + padding, 20);
      
      RenderUtils.drawHorizontalGradientRect(guiGraphics.pose(), 
         scoreboardX, scoreboardY, 
         150, scoreboardHeight, 
         0x00000000, 0x66000000);
   }
}