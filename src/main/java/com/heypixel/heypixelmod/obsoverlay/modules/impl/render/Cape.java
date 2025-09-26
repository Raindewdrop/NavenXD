package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import net.minecraft.resources.ResourceLocation;

@ModuleInfo(
    name = "Cape",
    description = "Custom cape module",
    category = Category.RENDER
)
public class Cape extends Module {
    private final ModeValue styleValue = ValueBuilder.create(this, "Style")
            .setModes("Bilibili", "Jiaran", "Staff", "CherryBlossom", "Ba", "C", "C1", "Cat", "Cat2", "Cs", "M", "O1", "O2", "Qx2", "Vape")
            .setDefaultModeIndex(2)
            .build()
            .getModeValue();

    public Cape() {
        this.setEnabled(true);
    }

    public ResourceLocation getCapeLocation() {
        return CapeStyle.valueOf(this.styleValue.getCurrentMode().toUpperCase()).location;
    }

    @Override
    public String getSuffix() {
        return this.styleValue.getCurrentMode();
    }

    public static enum CapeStyle {
        BILIBILI(ResourceLocation.of("navenxd:capes/bilibili.png", ':')),
        JIARAN(ResourceLocation.of("navenxd:capes/jiaran.png", ':')),
        STAFF(ResourceLocation.of("navenxd:capes/staff.png", ':')),
        CHERRYBLOSSOM(ResourceLocation.of("navenxd:capes/cherryblossom.png", ':')),
        BA(ResourceLocation.of("navenxd:capes/ba.png", ':')),
        C(ResourceLocation.of("navenxd:capes/c.png", ':')),
        C1(ResourceLocation.of("navenxd:capes/c1.png", ':')),
        CAT(ResourceLocation.of("navenxd:capes/cat.png", ':')),
        CAT2(ResourceLocation.of("navenxd:capes/cat2.png", ':')),
        CS(ResourceLocation.of("navenxd:capes/cs.png", ':')),
        M(ResourceLocation.of("navenxd:capes/m.png", ':')),
        O1(ResourceLocation.of("navenxd:capes/o1.png", ':')),
        O2(ResourceLocation.of("navenxd:capes/o2.png", ':')),
        QX2(ResourceLocation.of("navenxd:capes/qx2.png", ':')),
        VAPE(ResourceLocation.of("navenxd:capes/vape.png", ':'));

        private final ResourceLocation location;

        private CapeStyle(ResourceLocation location) {
            this.location = location;
        }
    }
}