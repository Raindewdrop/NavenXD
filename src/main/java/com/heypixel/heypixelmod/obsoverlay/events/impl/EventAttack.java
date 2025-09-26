package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import net.minecraft.world.entity.Entity;

public class EventAttack implements Event {
   private boolean post;
   private final Entity target;

   public EventAttack(boolean post, Entity target) {
      this.post = post;
      this.target = target;
   }

   public boolean isPost() {
      return this.post;
   }

   public Entity getTarget() {
      return this.target;
   }

   public void setPost(boolean post) {
      this.post = post;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventAttack other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (this.isPost() != other.isPost()) {
         return false;
      } else {
         Object this$target = this.getTarget();
         Object other$target = other.getTarget();
         return this$target == null ? other$target == null : this$target.equals(other$target);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventAttack;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + (this.isPost() ? 79 : 97);
      Object $target = this.getTarget();
      return result * 59 + ($target == null ? 43 : $target.hashCode());
   }

   @Override
   public String toString() {
      return "EventAttack(post=" + this.isPost() + ", target=" + this.getTarget() + ")";
   }
}
