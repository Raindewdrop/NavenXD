package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import java.awt.Color;

public enum NotificationLevel {
   SUCCESS(new Color(23, 150, 38, 255).getRGB(), "✓"),
   INFO(new Color(23, 22, 38, 255).getRGB(), "ℹ"),
   WARNING(new Color(138, 90, 92, 255).getRGB(), "⚠"),
   ERROR(new Color(148, 42, 43, 255).getRGB(), "✗");

   private final int color;
   private final String symbol;

   private NotificationLevel(int color, String symbol) {
      this.color = color;
      this.symbol = symbol;
   }

   public int getColor() {
      return this.color;
   }

   public String getSymbol() {
      return this.symbol;
   }
}
