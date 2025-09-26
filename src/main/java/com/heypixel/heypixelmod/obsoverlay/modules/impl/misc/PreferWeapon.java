package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import java.util.HashSet;
import java.util.Set;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "PreferWeapon",
        description = "Prioritizes a specific weapon for InventoryManager's sword slot",
        category = Category.MISC
)
public class PreferWeapon extends Module {
    private final ModeValue weaponPriority = ValueBuilder.create(this, "Priority")
            .setModes("Sword", "God Axe", "KB Ball", "End Crystal")
            .build()
            .getModeValue();

    private final Set<Integer> pressedKeys = new HashSet<>();

    public PreferWeapon() {
        this.setKey(-1);
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        this.setSuffix(weaponPriority.getCurrentMode());
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (this.isEnabled() && this.getKey() == event.getKey()) {
            if (event.isState()) {
                if (pressedKeys.add(event.getKey())) {
                    int currentIndex = weaponPriority.getCurrentValue();
                    int nextIndex = (currentIndex + 1) % weaponPriority.getValues().length;
                    weaponPriority.setCurrentValue(nextIndex);
                }
            } else {
                pressedKeys.remove(event.getKey());
            }
        }
    }

    public static String getPriority() {
        PreferWeapon module = (PreferWeapon) Naven.getInstance().getModuleManager().getModule(PreferWeapon.class);

        if (module != null && module.isEnabled()) {
            return module.weaponPriority.getCurrentMode();
        }

        return "Sword";
    }
}
