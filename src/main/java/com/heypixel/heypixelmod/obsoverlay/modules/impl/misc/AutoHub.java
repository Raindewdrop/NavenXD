package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;

@ModuleInfo(
    name = "AutoHub",
    description = "Auto run away",
    category = Category.MISC
)
public class AutoHub extends Module {
    
    private final TimeHelper timer = new TimeHelper();
    private boolean hasExecuted = false;
    
    public AutoHub() {
    }
    
    @Override
    public void onEnable() {
        hasExecuted = false;
        timer.reset();
    }
    
    @EventTarget
    public void onMotion(EventRunTicks e) {
        if (isEnabled() && !hasExecuted) {
            if (mc.player != null) {
                mc.player.connection.sendChat("/hub");
                ChatUtils.addChatMessage("AutoHub: You have run away.");
                hasExecuted = true;
                timer.reset();
            }
        }
        
        if (hasExecuted && timer.delay(100)) {
            this.setEnabled(false);
            hasExecuted = false;
        }
    }
    
    @Override
    public void onDisable() {
        hasExecuted = false;
        timer.reset();
    }
}