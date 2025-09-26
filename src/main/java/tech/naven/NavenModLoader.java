package tech.naven;

import net.minecraftforge.fml.common.Mod;
import com.heypixel.heypixelmod.obsoverlay.Naven;

@Mod("navenxd")
public class NavenModLoader {
    public NavenModLoader() {
        Naven.modRegister();
    }
}
