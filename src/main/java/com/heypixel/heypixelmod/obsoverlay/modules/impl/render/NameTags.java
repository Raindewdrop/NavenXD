package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.EntityWatcher;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SharedESPData;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;

@ModuleInfo(
   name = "NameTags",
   category = Category.RENDER,
   description = "Renders name tags"
)
public class NameTags extends Module {
   public BooleanValue mcf = ValueBuilder.create(this, "Middle Click Friend").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue showCompassPosition = ValueBuilder.create(this, "Compass Position").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.showCompassPosition.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.showCompassPosition.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue shared = ValueBuilder.create(this, "Shared ESP").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setDefaultFloatValue(0.3F)
      .setFloatStep(0.01F)
      .setMinFloatValue(0.1F)
      .setMaxFloatValue(0.5F)
      .build()
      .getFloatValue();
   private static final int color1 = new Color(0, 0, 0, 40).getRGB();
   private static final int color2 = new Color(0, 0, 0, 80).getRGB();
   private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
   private final List<NameTags.NameTagData> sharedPositions = new CopyOnWriteArrayList<>();

   private BlockPos spawnPosition;
   private Vector2f compassPosition;
   private final Map<Player, Integer> aimTicks = new ConcurrentHashMap<>();
   private Player aimingPlayer;

   private boolean hasPlayer() {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
            return true;
         }
      }

      return false;
   }

   private BlockPos getSpawnPosition(ClientLevel p_117922_) {
      return p_117922_.dimensionType().natural() ? p_117922_.getSharedSpawnPos() : null;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (!this.mcf.getCurrentValue()) {
            this.aimingPlayer = null;
         } else {
            for (Player player : mc.level.players()) {
               if (!(player instanceof BlinkingPlayer) && player != mc.player) {
                  if (isAiming(player, mc.player.getYRot(), mc.player.getXRot())) {
                     if (this.aimTicks.containsKey(player)) {
                        this.aimTicks.put(player, this.aimTicks.get(player) + 1);
                     } else {
                        this.aimTicks.put(player, 1);
                     }

                     if (this.aimTicks.get(player) >= 10) {
                        this.aimingPlayer = player;
                        break;
                     }
                  } else if (this.aimTicks.containsKey(player) && this.aimTicks.get(player) > 0) {
                     this.aimTicks.put(player, this.aimTicks.get(player) - 1);
                  } else {
                     this.aimTicks.put(player, 0);
                  }
               }
            }

            if (this.aimingPlayer != null && this.aimTicks.containsKey(this.aimingPlayer) && this.aimTicks.get(this.aimingPlayer) <= 0) {
               this.aimingPlayer = null;
            }
         }

         this.spawnPosition = null;
         if (!InventoryUtils.hasItem(Items.COMPASS) && this.compassOnly.getCurrentValue()) {
            return;
         }

         if (this.hasPlayer() && this.noPlayerOnly.getCurrentValue()) {
            return;
         }

         this.spawnPosition = this.getSpawnPosition(mc.level);
      }
   }

   public static boolean isAiming(Entity targetEntity, float yaw, float pitch) {
      Vec3 playerEye = new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(), mc.player.getZ());
      HitResult intercept = RotationUtils.getIntercept(targetEntity.getBoundingBox(), new Vector2f(yaw, pitch), playerEye, 150.0);
      if (intercept == null) {
         return false;
      } else {
         return intercept.getType() != Type.ENTITY ? false : intercept.getLocation().distanceTo(playerEye) < 150.0;
      }
   }



   @EventTarget
   public void update(EventRender e) {
      try {
         this.updatePositions(e.getRenderPartialTicks());
         this.compassPosition = null;
         if (this.spawnPosition != null) {
            this.compassPosition = ProjectionUtils.project(
               (double)this.spawnPosition.getX() + 0.5,
               (double)this.spawnPosition.getY() + 1.75,
               (double)this.spawnPosition.getZ() + 0.5,
               e.getRenderPartialTicks()
            );
         }
      } catch (Exception var3) {
      }
   }

   @EventTarget
   public void onMouseKey(EventMouseClick e) {
      if (e.getKey() == 2 && !e.isState() && this.mcf.getCurrentValue() && this.aimingPlayer != null) {
         if (FriendManager.isFriend(this.aimingPlayer)) {
            Notification notification = new Notification(
               NotificationLevel.ERROR, "Removed " + this.aimingPlayer.getName().getString() + " from friends!", 3000L
            );
            Naven.getInstance().getNotificationManager().addNotification(notification);
            FriendManager.removeFriend(this.aimingPlayer);
         } else {
            Notification notification = new Notification(NotificationLevel.SUCCESS, "Added " + this.aimingPlayer.getName().getString() + " as friends!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
            FriendManager.addFriend(this.aimingPlayer);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {

      if (this.compassPosition != null) {
         Vector2f position = this.compassPosition;
         float scale = Math.max(
               80.0F
                  - Mth.sqrt(
                     (float)mc.player
                        .distanceToSqr(
                           (double)this.spawnPosition.getX() + 0.5, (double)this.spawnPosition.getY() + 1.75, (double)this.spawnPosition.getZ() + 0.5
                        )
                  ),
               0.0F
            )
            * this.scale.getCurrentValue()
            / 80.0F;
         String text = "Compass";
         float width = Fonts.harmony.getWidth(text, (double)scale);
         double height = Fonts.harmony.getHeight(true, (double)scale);
         float totalHeight = (float)height + 2.0F;

         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, width + 4.0F, totalHeight, 3.0F, -1);
         StencilUtils.erase(true);
         RenderUtils.drawRoundedRect(e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, width + 4.0F, totalHeight, 3.0F, color1);
         RenderUtils.fill(e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, position.y, new Color(255, 0, 0, 100).getRGB());
         StencilUtils.dispose();
         Fonts.harmony.setAlpha(0.8F);
         Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
      }

      for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
         if (entry.getKey() != mc.player && entry.getKey() instanceof Player) {
            Player living = (Player)entry.getKey();
            e.getStack().pushPose();
            float hp = living.getHealth();
            if (hp > 20.0F) {
               living.setHealth(20.0F);
            }

            Vector2f position = entry.getValue();
            String text = "";
            if (Teams.isSameTeam(living)) {
               text = text + "§aTeam§f | ";
            }

            if (FriendManager.isFriend(living)) {
               text = text + "§aFriend§f | ";
            }

            if (this.aimingPlayer == living) {
               text = text + "§cAiming§f | ";
            }

            text = text + living.getName().getString();
            text = text + "§f | §c" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";
            float scale = this.scale.getCurrentValue();
            float width = Fonts.harmony.getWidth(text, (double)scale);
            float delta = 1.0F - living.getHealth() / living.getMaxHealth();
            double height = Fonts.harmony.getHeight(true, (double)scale);
            float totalHeight = (float)height + 2.0F;

            RenderUtils.drawRoundedRect(e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, width + 4.0F, totalHeight, 3.0F, color1);
            RenderUtils.fill(e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, position.y, new Color(255, 0, 0, 100).getRGB());
            RenderUtils.fill(
               e.getStack(),
               position.x - width / 2.0F - 2.0F,
               position.y - 2.0F,
               position.x + width / 2.0F + 2.0F - (width + 4.0F) * delta,
               (float)((double)position.y + height),
               color2
            );
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
            Fonts.harmony.setAlpha(1.0F);
            e.getStack().popPose();
         }
      }

      if (this.shared.getCurrentValue()) {
         for (NameTags.NameTagData data : this.sharedPositions) {
            e.getStack().pushPose();
            Vector2f positionx = data.getRender();
            String textx = "§aShared§f | " + data.getDisplayName();
            float scale = this.scale.getCurrentValue();
            float width = Fonts.harmony.getWidth(textx, (double)scale);
            double height = Fonts.harmony.getHeight(true, (double)scale);
            double delta = 1.0 - data.getHealth() / data.getMaxHealth();
            float totalHeight = (float)height + 2.0F;

            RenderUtils.drawRoundedRect(e.getStack(), positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, width + 4.0F, totalHeight, 3.0F, color1);
            RenderUtils.fill(e.getStack(), positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, positionx.x + width / 2.0F + 2.0F, positionx.y, new Color(255, 0, 0, 100).getRGB());
            RenderUtils.fill(
               e.getStack(),
               positionx.x - width / 2.0F - 2.0F,
               positionx.y - 2.0F,
               (float)((double)(positionx.x + width / 2.0F + 2.0F) - (double)(width + 4.0F) * delta),
               (float)((double)positionx.y + height),
               color2
            );
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(e.getStack(), textx, (double)(positionx.x - width / 2.0F), (double)(positionx.y - 1.0F), Color.WHITE, true, (double)scale);
            Fonts.harmony.setAlpha(1.0F);
            e.getStack().popPose();
         }
      }
   }

   private void updatePositions(float renderPartialTicks) {
      this.entityPositions.clear();
      this.sharedPositions.clear();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof Player && !entity.getName().getString().startsWith("CIT-")) {
            double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
            double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight() + 0.5;
            double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            vector.setY(vector.getY() - 2.0F);
            this.entityPositions.put(entity, vector);
         }
      }

      if (this.shared.getCurrentValue()) {
         Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

         for (SharedESPData value : dataMap.values()) {
            double x = value.getPosX();
            double y = value.getPosY() + (double)mc.player.getBbHeight() + 0.5;
            double z = value.getPosZ();
            Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
            vector.setY(vector.getY() - 2.0F);
            String displayName = value.getDisplayName();
            displayName = displayName
               + "§f | §c"
               + Math.round(value.getHealth())
               + (value.getAbsorption() > 0.0 ? "+" + Math.round(value.getAbsorption()) : "")
               + "HP";
            this.sharedPositions
               .add(new NameTags.NameTagData(displayName, value.getHealth(), value.getMaxHealth(), value.getAbsorption(), new Vec3(x, y, z), vector));
         }
      }
   }

   private static class NameTagData {
      private final String displayName;
      private final double health;
      private final double maxHealth;
      private final double absorption;
      private final Vec3 position;
      private final Vector2f render;

      public String getDisplayName() {
         return this.displayName;
      }

      public double getHealth() {
         return this.health;
      }

      public double getMaxHealth() {
         return this.maxHealth;
      }

      public double getAbsorption() {
         return this.absorption;
      }

      public Vec3 getPosition() {
         return this.position;
      }

      public Vector2f getRender() {
         return this.render;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof NameTags.NameTagData other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getHealth(), other.getHealth()) != 0) {
            return false;
         } else if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) {
            return false;
         } else if (Double.compare(this.getAbsorption(), other.getAbsorption()) != 0) {
            return false;
         } else {
            Object this$displayName = this.getDisplayName();
            Object other$displayName = other.getDisplayName();
            if (this$displayName == null ? other$displayName == null : this$displayName.equals(other$displayName)) {
               Object this$position = this.getPosition();
               Object other$position = other.getPosition();
               if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                  Object this$render = this.getRender();
                  Object other$render = other.getRender();
                  return this$render == null ? other$render == null : this$render.equals(other$render);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof NameTags.NameTagData;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $health = Double.doubleToLongBits(this.getHealth());
         result = result * 59 + (int)($health >>> 32 ^ $health);
         long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
         result = result * 59 + (int)($maxHealth >>> 32 ^ $maxHealth);
         long $absorption = Double.doubleToLongBits(this.getAbsorption());
         result = result * 59 + (int)($absorption >>> 32 ^ $absorption);
         Object $displayName = this.getDisplayName();
         result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
         Object $position = this.getPosition();
         result = result * 59 + ($position == null ? 43 : $position.hashCode());
         Object $render = this.getRender();
         return result * 59 + ($render == null ? 43 : $render.hashCode());
      }

      @Override
      public String toString() {
         return "NameTags.NameTagData(displayName="
            + this.getDisplayName()
            + ", health="
            + this.getHealth()
            + ", maxHealth="
            + this.getMaxHealth()
            + ", absorption="
            + this.getAbsorption()
            + ", position="
            + this.getPosition()
            + ", render="
            + this.getRender()
            + ")";
      }

      public NameTagData(String displayName, double health, double maxHealth, double absorption, Vec3 position, Vector2f render) {
         this.displayName = displayName;
         this.health = health;
         this.maxHealth = maxHealth;
         this.absorption = absorption;
         this.position = position;
         this.render = render;
      }
   }
}
