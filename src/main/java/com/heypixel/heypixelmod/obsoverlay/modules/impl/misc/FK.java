package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.mixin.O.accessors.MultiPlayerGameModeAccessor;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

@ModuleInfo(
   name = "FuckYou",
   description = "Automatically places water when on fire or in cobweb",
   category = Category.MISC
)
public class FK extends Module {
   private final BooleanValue onFire = ValueBuilder.create(this, "On Fire")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   
   private final BooleanValue inCobweb = ValueBuilder.create(this, "In Cobweb")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   
   private final BooleanValue fireAround = ValueBuilder.create(this, "Fire Around")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   
   private final FloatValue radius = ValueBuilder.create(this, "Radius")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   
   public boolean rotation = false;
   private BlockPos above;
   private boolean placeWater = false;
   private int originalSlot;
   private int timeout;
   private int cobwebDelay = 0;

   public static boolean isOnGround(double height) {
      Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, height, 0.0));
      return collisions.iterator().hasNext();
   }

   @EventTarget
   public void onPre(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         // Check if player is on fire
         if (this.onFire.getCurrentValue() && mc.player.isOnFire()) {
            this.placeWaterBucket();
         }
         
         // Check if player is in cobweb
         if (this.inCobweb.getCurrentValue() && mc.level.getBlockState(mc.player.blockPosition()).getBlock() == net.minecraft.world.level.block.Blocks.COBWEB) {
            if (this.cobwebDelay <= 0) {
               this.placeWaterBucket();
               this.cobwebDelay = 5; // 5 tick delay for cobweb
            }
         }
         
         // Check for fire around player and destroy it with silent rotation
         if (this.fireAround.getCurrentValue()) {
            this.destroyFireSilently();
         }
         
         if (this.cobwebDelay > 0) {
            this.cobwebDelay--;
         }
         
         if (--this.timeout == 0 && this.rotation) {
            this.rotation = false;
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to place water!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
         }
      }
   }

   private void placeWaterBucket() {
      // Switch to water bucket first
      for (int i = 0; i < 9; i++) {
         ItemStack item = mc.player.getInventory().getItem(i);
         if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
            this.originalSlot = mc.player.getInventory().selected;
            mc.player.getInventory().selected = i;
            this.rotation = true;
            this.timeout = 5;
            
            // Set rotation to look at ground
            Rotation rotation = new Rotation(mc.player.getYRot(), 90.0F);
            rotation.apply();
            break;
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (this.placeWater) {
         this.placeWater = false;
         
         // Set rotation to look at ground before placing water
         Rotation rotation = new Rotation(mc.player.getYRot(), 90.0F);
         rotation.apply();
         
         if (mc.hitResult.getType() == Type.BLOCK && ((BlockHitResult)mc.hitResult).getDirection() == Direction.UP) {
            this.above = ((BlockHitResult)mc.hitResult).getBlockPos().above();
            this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
         } else {
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to place water!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
            this.rotation = false;
         }
      } else if (this.above != null) {
         this.rotation = false;
         BlockPos above = ((BlockHitResult)mc.hitResult).getBlockPos().above();
         if (above.equals(this.above)) {
            this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
         } else {
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to recycle the water dues to moving!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
         }

         mc.player.getInventory().selected = this.originalSlot;
         this.above = null;
      }
   }

   public InteractionResult useItem(Player pPlayer, Level pLevel, InteractionHand pHand) {
      MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor)mc.gameMode;
      if (gameMode.getLocalPlayerMode() == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      } else {
         gameMode.invokeEnsureHasSentCarriedItem();
         PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(pHand, id));
         ItemStack itemstack = pPlayer.getItemInHand(pHand);
         if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
            return InteractionResult.PASS;
         } else {
            InteractionResult cancelResult = ForgeHooks.onItemRightClick(pPlayer, pHand);
            if (cancelResult != null) {
               return cancelResult;
            } else {
               InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(pLevel, pPlayer, pHand);
               ItemStack itemstack1 = (ItemStack)interactionresultholder.getObject();
               if (itemstack1 != itemstack) {
                  pPlayer.setItemInHand(pHand, itemstack1);
                  if (itemstack1.isEmpty()) {
                     ForgeEventFactory.onPlayerDestroyItem(pPlayer, itemstack, pHand);
                  }
               }

               return interactionresultholder.getResult();
            }
         }
      }
   }

    private void destroyFireSilently() {
       // Find and destroy fire blocks with silent rotation
       BlockPos playerPos = mc.player.blockPosition();
     float radius = this.radius.getCurrentValue();
        
        for (int x = (int)(playerPos.getX() - radius); x <= playerPos.getX() + radius; x++) {
           for (int y = (int)(playerPos.getY() - radius); y <= playerPos.getY() + radius; y++) {
              for (int z = (int)(playerPos.getZ() - radius); z <= playerPos.getZ() + radius; z++) {
                 BlockPos pos = new BlockPos(x, y, z);
                 if (mc.level.getBlockState(pos).getBlock() == net.minecraft.world.level.block.Blocks.FIRE) {
                    // Calculate rotation to look at the fire block
                    Rotation rotation = RotationUtils.getRotations(new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), mc.player.getEyePosition());
                    rotation.apply();
                    
                    // Destroy fire block directly
                    mc.level.destroyBlock(pos, true);
                    return; // Destroy one fire block per tick
                 }
              }
           }
        }
    }
}
